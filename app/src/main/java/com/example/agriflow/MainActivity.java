package com.example.agriflow;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agriflow.data.model.Zone;
import com.example.agriflow.data.repository.FarmRepository;
import com.example.agriflow.data.sync.SyncManager;
import com.example.agriflow.ui.activity.ZoneGridActivity;
import com.example.agriflow.ui.adapter.ZoneAdapter;
import com.example.agriflow.ui.dialog.CreateZoneBottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatZones;
    private TextView tvStatPlants;
    private TextView tvStatHealthyRate;
    private TextView tvStatIssues;
    private TextView tvSyncBadge;

    private ChipGroup chipGroupFilter;
    private RecyclerView rvZones;
    private LinearLayout layoutEmptyState;
    private ExtendedFloatingActionButton fabAddZone;

    private ZoneAdapter zoneAdapter;
    private FarmRepository repository;
    private SyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        repository = FarmRepository.getInstance(this);
        syncManager = SyncManager.getInstance(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_coordinator), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initViews();
        setupRecyclerView();
        setupEvents();
        setupSyncMonitoring();
        repository.addDataChangedListener(() -> runOnUiThread(this::refreshDashboardData));
        refreshDashboardData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshDashboardData();
    }

    private void initViews() {
        tvStatZones = findViewById(R.id.tv_stat_zones);
        tvStatPlants = findViewById(R.id.tv_stat_plants);
        tvStatHealthyRate = findViewById(R.id.tv_stat_healthy_rate);
        tvStatIssues = findViewById(R.id.tv_stat_issues);
        tvSyncBadge = findViewById(R.id.tv_sync_badge);

        chipGroupFilter = findViewById(R.id.chip_group_filter);
        rvZones = findViewById(R.id.rv_zones);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        fabAddZone = findViewById(R.id.fab_add_zone);
    }

    private void setupSyncMonitoring() {
        if (syncManager != null) {
            syncManager.addListener((state, message) -> runOnUiThread(() -> {
                if (tvSyncBadge != null) {
                    switch (state) {
                        case SYNCING:
                            tvSyncBadge.setText("🔄 Đang đồng bộ Cloud...");
                            break;
                        case SYNCED:
                            tvSyncBadge.setText("☁️ Đã đồng bộ Firebase");
                            break;
                        case OFFLINE:
                            tvSyncBadge.setText("💾 Ngoại tuyến • Đã lưu Room (SQLite)");
                            break;
                        case ERROR:
                            tvSyncBadge.setText("⚠️ Ngoại tuyến • Lưu an toàn trong Room");
                            break;
                        default:
                            tvSyncBadge.setText("💾 Lưu cục bộ (Room) • Sẵn sàng Cloud");
                            break;
                    }
                }
            }));
        }
    }

    private void setupRecyclerView() {
        rvZones.setLayoutManager(new LinearLayoutManager(this));
        zoneAdapter = new ZoneAdapter(new ZoneAdapter.OnZoneActionListener() {
            @Override
            public void onZoneClick(Zone zone) {
                Intent intent = new Intent(MainActivity.this, ZoneGridActivity.class);
                intent.putExtra(ZoneGridActivity.EXTRA_ZONE_ID, zone.getId());
                startActivity(intent);
            }

            @Override
            public void onEditZone(Zone zone) {
                showEditZoneDialog(zone);
            }

            @Override
            public void onDeleteZone(Zone zone) {
                showDeleteZoneConfirmation(zone);
            }
        });
        rvZones.setAdapter(zoneAdapter);
    }

    private void setupEvents() {
        fabAddZone.setOnClickListener(v -> {
            CreateZoneBottomSheetDialog dialog = new CreateZoneBottomSheetDialog();
            dialog.setOnZoneCreatedListener(newZone -> {
                refreshDashboardData();
                Intent intent = new Intent(MainActivity.this, ZoneGridActivity.class);
                intent.putExtra(ZoneGridActivity.EXTRA_ZONE_ID, newZone.getId());
                startActivity(intent);
            });
            dialog.show(getSupportFragmentManager(), "CreateZoneBottomSheetDialog");
        });

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> {
            filterZones();
        });
    }

    private void refreshDashboardData() {
        // Update Stats
        tvStatZones.setText(String.valueOf(repository.getTotalZonesCount()));
        tvStatPlants.setText(repository.getTotalPlantsCount() + " cây");
        tvStatHealthyRate.setText(repository.getOverallHealthyPercentage() + "%");
        tvStatIssues.setText(repository.getTotalIssueCount() + " cây");

        filterZones();
    }

    private void filterZones() {
        List<Zone> allZones = repository.getZones();
        List<Zone> filtered = new ArrayList<>();

        int checkedId = chipGroupFilter.getCheckedChipId();
        if (checkedId == R.id.chip_issues_only) {
            for (Zone z : allZones) {
                if (z.getWarningCount() > 0 || z.getDiseasedCount() > 0) {
                    filtered.add(z);
                }
            }
        } else if (checkedId == R.id.chip_healthy_only) {
            for (Zone z : allZones) {
                if (z.getWarningCount() == 0 && z.getDiseasedCount() == 0 && z.getActivePlantCount() > 0) {
                    filtered.add(z);
                }
            }
        } else {
            filtered.addAll(allZones);
        }

        zoneAdapter.setZones(filtered);

        if (filtered.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvZones.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvZones.setVisibility(View.VISIBLE);
        }
    }

    private void showEditZoneDialog(Zone zone) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_zone, null);
        EditText etName = dialogView.findViewById(R.id.et_edit_zone_name);
        EditText etCrop = dialogView.findViewById(R.id.et_edit_default_crop);
        EditText etDesc = dialogView.findViewById(R.id.et_edit_zone_description);
        EditText etCycle = dialogView.findViewById(R.id.et_edit_harvest_cycle);
        EditText etRows = dialogView.findViewById(R.id.et_edit_zone_rows);
        EditText etCols = dialogView.findViewById(R.id.et_edit_zone_cols);

        MaterialButton btnPreset6x8 = dialogView.findViewById(R.id.btn_preset_dim_6x8);
        MaterialButton btnPreset8x10 = dialogView.findViewById(R.id.btn_preset_dim_8x10);
        MaterialButton btnPreset10x15 = dialogView.findViewById(R.id.btn_preset_dim_10x15);

        MaterialButton btn28d = dialogView.findViewById(R.id.btn_cycle_28d);
        MaterialButton btn30d = dialogView.findViewById(R.id.btn_cycle_30d);
        MaterialButton btn45d = dialogView.findViewById(R.id.btn_cycle_45d);

        etName.setText(zone.getName());
        etCrop.setText(zone.getDefaultCrop());
        etDesc.setText(zone.getDescription());
        etCycle.setText(String.valueOf(zone.getHarvestCycleDays()));
        etRows.setText(String.valueOf(zone.getRows()));
        etCols.setText(String.valueOf(zone.getCols()));

        btnPreset6x8.setOnClickListener(v -> { etRows.setText("6"); etCols.setText("8"); });
        btnPreset8x10.setOnClickListener(v -> { etRows.setText("8"); etCols.setText("10"); });
        btnPreset10x15.setOnClickListener(v -> { etRows.setText("10"); etCols.setText("15"); });

        btn28d.setOnClickListener(v -> etCycle.setText("28"));
        btn30d.setOnClickListener(v -> etCycle.setText("30"));
        btn45d.setOnClickListener(v -> etCycle.setText("45"));

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Lưu thay đổi", (d, w) -> {
                    String newName = etName.getText().toString().trim();
                    String newCrop = etCrop.getText().toString().trim();
                    String newDesc = etDesc.getText().toString().trim();
                    String cycleStr = etCycle.getText().toString().trim();
                    String rowsStr = etRows.getText().toString().trim();
                    String colsStr = etCols.getText().toString().trim();

                    if (newName.isEmpty()) newName = zone.getName();
                    if (newCrop.isEmpty()) newCrop = zone.getDefaultCrop();
                    int cycle = 30;
                    try {
                        cycle = Integer.parseInt(cycleStr);
                    } catch (NumberFormatException ignored) {}

                    int newRows = zone.getRows();
                    int newCols = zone.getCols();
                    try {
                        newRows = Integer.parseInt(rowsStr);
                    } catch (NumberFormatException ignored) {}
                    try {
                        newCols = Integer.parseInt(colsStr);
                    } catch (NumberFormatException ignored) {}

                    if (newRows < 1) newRows = 1;
                    if (newCols < 1) newCols = 1;

                    repository.updateZoneInfo(zone.getId(), newName, newDesc, newCrop, cycle, newRows, newCols);
                    Toast.makeText(this, "Đã cập nhật thông tin và kích thước khu!", Toast.LENGTH_SHORT).show();
                    refreshDashboardData();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteZoneConfirmation(Zone zone) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa khu: " + zone.getName())
                .setMessage("Bạn có chắc chắn muốn xóa khu này?\nToàn bộ dữ liệu ô đất và nhật ký canh tác của khu sẽ bị xóa khỏi thiết bị và Cloud Firestore.")
                .setPositiveButton("Xác nhận xóa", (d, w) -> {
                    repository.deleteZone(zone.getId());
                    Toast.makeText(this, "Đã xóa khu " + zone.getName() + " thành công!", Toast.LENGTH_SHORT).show();
                    refreshDashboardData();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
