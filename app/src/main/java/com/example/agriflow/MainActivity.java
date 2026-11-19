package com.example.agriflow;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agriflow.data.model.Zone;
import com.example.agriflow.data.repository.FarmRepository;
import com.example.agriflow.ui.activity.ZoneGridActivity;
import com.example.agriflow.ui.adapter.ZoneAdapter;
import com.example.agriflow.ui.dialog.CreateZoneBottomSheetDialog;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatZones;
    private TextView tvStatPlants;
    private TextView tvStatHealthyRate;
    private TextView tvStatIssues;

    private ChipGroup chipGroupFilter;
    private RecyclerView rvZones;
    private LinearLayout layoutEmptyState;
    private ExtendedFloatingActionButton fabAddZone;

    private ZoneAdapter zoneAdapter;
    private final FarmRepository repository = FarmRepository.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_coordinator), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        initViews();
        setupRecyclerView();
        setupEvents();
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

        chipGroupFilter = findViewById(R.id.chip_group_filter);
        rvZones = findViewById(R.id.rv_zones);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        fabAddZone = findViewById(R.id.fab_add_zone);
    }

    private void setupRecyclerView() {
        rvZones.setLayoutManager(new LinearLayoutManager(this));
        zoneAdapter = new ZoneAdapter(zone -> {
            Intent intent = new Intent(MainActivity.this, ZoneGridActivity.class);
            intent.putExtra(ZoneGridActivity.EXTRA_ZONE_ID, zone.getId());
            startActivity(intent);
        });
        rvZones.setAdapter(zoneAdapter);
    }

    private void setupEvents() {
        fabAddZone.setOnClickListener(v -> {
            CreateZoneBottomSheetDialog dialog = new CreateZoneBottomSheetDialog();
            dialog.setOnZoneCreatedListener(newZone -> {
                refreshDashboardData();
                // Optionally open the newly created zone immediately
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
}
