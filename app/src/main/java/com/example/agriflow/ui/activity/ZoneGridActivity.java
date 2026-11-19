package com.example.agriflow.ui.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.agriflow.R;
import com.example.agriflow.data.model.ActionType;
import com.example.agriflow.data.model.Cell;
import com.example.agriflow.data.model.CellStatus;
import com.example.agriflow.data.model.Zone;
import com.example.agriflow.data.repository.FarmRepository;
import com.example.agriflow.ui.dialog.CellDetailBottomSheetDialog;
import com.example.agriflow.ui.view.FarmGridView;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class ZoneGridActivity extends AppCompatActivity {

    public static final String EXTRA_ZONE_ID = "extra_zone_id";

    private String zoneId;
    private Zone currentZone;

    private MaterialToolbar toolbar;
    private TextView tvPillHealthy, tvPillWarning, tvPillDiseased, tvPillEmpty;
    private FarmGridView farmGridView;
    private FloatingActionButton btnZoomIn, btnZoomOut, btnFitScreen;

    // Bulk selection card
    private MaterialCardView cardBulkActions;
    private TextView tvSelectedCount;
    private MaterialButton btnSelectAll, btnClearSelection;
    private MaterialButton btnBulkWater, btnBulkFertilize, btnBulkStatus;

    private final FarmRepository.OnDataChangedListener dataChangedListener = () -> runOnUiThread(() -> {
        currentZone = FarmRepository.getInstance().getZoneById(zoneId);
        if (currentZone != null) {
            farmGridView.invalidate();
            updateSummaryPills();
        }
    });

    @Override
    protected void onResume() {
        super.onResume();
        FarmRepository.getInstance().addDataChangedListener(dataChangedListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        FarmRepository.getInstance().removeDataChangedListener(dataChangedListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_zone_grid);

        zoneId = getIntent().getStringExtra(EXTRA_ZONE_ID);
        if (zoneId == null) {
            finish();
            return;
        }

        currentZone = FarmRepository.getInstance().getZoneById(zoneId);
        if (currentZone == null) {
            finish();
            return;
        }

        initViews();
        setupToolbar();
        setupGridView();
        setupControls();
        updateSummaryPills();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvPillHealthy = findViewById(R.id.tv_pill_healthy);
        tvPillWarning = findViewById(R.id.tv_pill_warning);
        tvPillDiseased = findViewById(R.id.tv_pill_diseased);
        tvPillEmpty = findViewById(R.id.tv_pill_empty);

        farmGridView = findViewById(R.id.farm_grid_view);
        btnZoomIn = findViewById(R.id.btn_zoom_in);
        btnZoomOut = findViewById(R.id.btn_zoom_out);
        btnFitScreen = findViewById(R.id.btn_fit_screen);

        cardBulkActions = findViewById(R.id.card_bulk_actions);
        tvSelectedCount = findViewById(R.id.tv_selected_count);
        btnSelectAll = findViewById(R.id.btn_select_all);
        btnClearSelection = findViewById(R.id.btn_clear_selection);
        btnBulkWater = findViewById(R.id.btn_bulk_water);
        btnBulkFertilize = findViewById(R.id.btn_bulk_fertilize);
        btnBulkStatus = findViewById(R.id.btn_bulk_status);
    }

    private void setupToolbar() {
        toolbar.setTitle(currentZone.getName());
        toolbar.setSubtitle("🌱 " + currentZone.getDefaultCrop() + " • " + currentZone.getRows() + "×" + currentZone.getCols() + " ô");
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupGridView() {
        farmGridView.setZone(currentZone);

        // Single click -> Open Cell Detail Bottom Sheet
        farmGridView.setOnCellClickListener(cell -> {
            CellDetailBottomSheetDialog dialog = CellDetailBottomSheetDialog.newInstance(
                    currentZone.getId(),
                    cell.getRowIndex(),
                    cell.getColIndex()
            );
            dialog.setOnCellUpdatedListener(updatedCell -> {
                farmGridView.invalidate();
                updateSummaryPills();
            });
            dialog.show(getSupportFragmentManager(), "CellDetailBottomSheetDialog");
        });

        // Multi selection listener
        farmGridView.setOnSelectionChangedListener(selectedCount -> {
            if (selectedCount > 0) {
                cardBulkActions.setVisibility(View.VISIBLE);
                tvSelectedCount.setText("Đã chọn " + selectedCount + " ô");
            } else {
                cardBulkActions.setVisibility(View.GONE);
            }
        });
    }

    private void setupControls() {
        btnZoomIn.setOnClickListener(v -> farmGridView.zoomIn());
        btnZoomOut.setOnClickListener(v -> farmGridView.zoomOut());
        btnFitScreen.setOnClickListener(v -> farmGridView.fitToScreen());

        btnSelectAll.setOnClickListener(v -> farmGridView.selectAll());
        btnClearSelection.setOnClickListener(v -> {
            farmGridView.setMultiSelectMode(false);
            farmGridView.clearSelection();
        });

        // Bulk Care Actions
        btnBulkWater.setOnClickListener(v -> {
            List<Cell> selected = farmGridView.getSelectedCells();
            if (selected.isEmpty()) return;

            FarmRepository.getInstance().bulkPerformAction(
                    currentZone.getId(),
                    selected,
                    ActionType.WATER,
                    "Tưới nước hàng loạt theo nhóm ô đã chọn"
            );
            farmGridView.clearSelection();
            farmGridView.invalidate();
            updateSummaryPills();
            Toast.makeText(this, "Đã tưới nước cho " + selected.size() + " vị trí!", Toast.LENGTH_SHORT).show();
        });

        btnBulkFertilize.setOnClickListener(v -> {
            List<Cell> selected = farmGridView.getSelectedCells();
            if (selected.isEmpty()) return;

            FarmRepository.getInstance().bulkPerformAction(
                    currentZone.getId(),
                    selected,
                    ActionType.FERTILIZE,
                    "Bón phân dưỡng cây định kỳ"
            );
            farmGridView.clearSelection();
            farmGridView.invalidate();
            updateSummaryPills();
            Toast.makeText(this, "Đã bón phân cho " + selected.size() + " vị trí!", Toast.LENGTH_SHORT).show();
        });

        btnBulkStatus.setOnClickListener(v -> showBulkStatusDialog());
    }

    private void showBulkStatusDialog() {
        List<Cell> selected = farmGridView.getSelectedCells();
        if (selected.isEmpty()) return;

        String[] statuses = new String[]{
                "🟢 Khỏe mạnh",
                "🟡 Cần chăm sóc",
                "🔴 Sâu bệnh",
                "⚪ Ô trống",
                "⬛ Lối đi / Không canh tác"
        };

        new AlertDialog.Builder(this)
                .setTitle("Đổi trạng thái " + selected.size() + " ô đã chọn")
                .setItems(statuses, (dialog, which) -> {
                    CellStatus newStatus = CellStatus.HEALTHY;
                    switch (which) {
                        case 0:
                            newStatus = CellStatus.HEALTHY;
                            break;
                        case 1:
                            newStatus = CellStatus.WARNING;
                            break;
                        case 2:
                            newStatus = CellStatus.DISEASED;
                            break;
                        case 3:
                            newStatus = CellStatus.EMPTY;
                            break;
                        case 4:
                            newStatus = CellStatus.DISABLED;
                            break;
                    }

                    FarmRepository.getInstance().bulkUpdateStatus(currentZone.getId(), selected, newStatus);
                    farmGridView.clearSelection();
                    farmGridView.invalidate();
                    updateSummaryPills();
                    Toast.makeText(this, "Đã cập nhật trạng thái " + selected.size() + " ô!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateSummaryPills() {
        if (currentZone == null) return;
        tvPillHealthy.setText("🟢 " + currentZone.getHealthyCount() + " Khỏe");
        tvPillWarning.setText("🟡 " + currentZone.getWarningCount() + " Cần chăm sóc");
        tvPillDiseased.setText("🔴 " + currentZone.getDiseasedCount() + " Sâu bệnh");
        tvPillEmpty.setText("⚪ " + currentZone.getEmptyCount() + " Trống");
    }
}
