package com.example.agriflow.ui.activity;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class ZoneGridActivity extends AppCompatActivity {

    public static final String EXTRA_ZONE_ID = "extra_zone_id";

    private String zoneId;
    private Zone currentZone;

    private MaterialToolbar toolbar;
    private TextView tvPillHealthy, tvPillWarning, tvPillDiseased, tvPillEmpty;
    private TextView chipFilterAll, chipFilterDue, chipFilterRecent, chipFilterNever;

    private FarmGridView farmGridView;
    private FloatingActionButton btnZoomIn, btnZoomOut, btnFitScreen;

    // Multi-Select Banner & mode toggle
    private LinearLayout layoutMultiSelectBanner;
    private TextView tvModeHint;
    private MaterialButtonToggleGroup toggleGroupSelectMode;
    private MaterialButton btnModeTap, btnModeBrush, btnExitMultiSelect;

    // Bulk selection card
    private MaterialCardView cardBulkActions;
    private TextView tvSelectedCount;
    private MaterialButton btnSelectAll, btnInvertSelection, btnClearSelection;
    private MaterialButton btnBulkWater, btnBulkFertilize, btnBulkPesticide;
    private MaterialButton btnBulkHarvest, btnBulkPlant, btnBulkStatus;

    private final FarmRepository.OnDataChangedListener dataChangedListener = () -> runOnUiThread(() -> {
        currentZone = FarmRepository.getInstance().getZoneById(zoneId);
        if (currentZone != null) {
            farmGridView.invalidate();
            updateSummaryPills();
            updateHarvestFilterChips();
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
        updateHarvestFilterChips();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        tvPillHealthy = findViewById(R.id.tv_pill_healthy);
        tvPillWarning = findViewById(R.id.tv_pill_warning);
        tvPillDiseased = findViewById(R.id.tv_pill_diseased);
        tvPillEmpty = findViewById(R.id.tv_pill_empty);

        chipFilterAll = findViewById(R.id.chip_filter_all);
        chipFilterDue = findViewById(R.id.chip_filter_due);
        chipFilterRecent = findViewById(R.id.chip_filter_recent);
        chipFilterNever = findViewById(R.id.chip_filter_never);

        farmGridView = findViewById(R.id.farm_grid_view);
        btnZoomIn = findViewById(R.id.btn_zoom_in);
        btnZoomOut = findViewById(R.id.btn_zoom_out);
        btnFitScreen = findViewById(R.id.btn_fit_screen);

        layoutMultiSelectBanner = findViewById(R.id.layout_multi_select_banner);
        tvModeHint = findViewById(R.id.tv_mode_hint);
        toggleGroupSelectMode = findViewById(R.id.toggle_group_select_mode);
        btnModeTap = findViewById(R.id.btn_mode_tap);
        btnModeBrush = findViewById(R.id.btn_mode_brush);
        btnExitMultiSelect = findViewById(R.id.btn_exit_multi_select);

        cardBulkActions = findViewById(R.id.card_bulk_actions);
        tvSelectedCount = findViewById(R.id.tv_selected_count);
        btnSelectAll = findViewById(R.id.btn_select_all);
        btnInvertSelection = findViewById(R.id.btn_invert_selection);
        btnClearSelection = findViewById(R.id.btn_clear_selection);
        btnBulkWater = findViewById(R.id.btn_bulk_water);
        btnBulkFertilize = findViewById(R.id.btn_bulk_fertilize);
        btnBulkPesticide = findViewById(R.id.btn_bulk_pesticide);
        btnBulkHarvest = findViewById(R.id.btn_bulk_harvest);
        btnBulkPlant = findViewById(R.id.btn_bulk_plant);
        btnBulkStatus = findViewById(R.id.btn_bulk_status);
    }

    private void setupToolbar() {
        updateToolbarTitles();
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_zone_grid);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_toggle_multi_select) {
                boolean newMode = !farmGridView.isMultiSelectMode();
                setMultiSelectModeUI(newMode);
                return true;
            } else if (id == R.id.action_toggle_display_mode) {
                boolean nextHarvestMode = !farmGridView.isShowHarvestInfo();
                farmGridView.setShowHarvestInfo(nextHarvestMode);
                item.setIcon(nextHarvestMode ? R.drawable.ic_sprout : R.drawable.ic_calendar);
                item.setTitle(nextHarvestMode ? "Hiện tên cây" : "Hiện chu kỳ thu hoạch");
                Toast.makeText(this, nextHarvestMode ? "Đang hiện số ngày thu hoạch từng cây" : "Đang hiện tên cây trồng", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_zone_stats) {
                showHarvestStatsDialog();
                return true;
            } else if (id == R.id.action_edit_zone) {
                showEditZoneDialog();
                return true;
            } else if (id == R.id.action_delete_zone) {
                showDeleteZoneConfirmation();
                return true;
            }
            return false;
        });
    }

    private void updateToolbarTitles() {
        if (currentZone == null) return;
        toolbar.setTitle(currentZone.getName());
        toolbar.setSubtitle("🌱 " + currentZone.getDefaultCrop() + " • " + currentZone.getRows() + "×" + currentZone.getCols() + " ô • Chu kỳ: " + currentZone.getHarvestCycleDays() + " ngày");
    }

    private void setMultiSelectModeUI(boolean active) {
        farmGridView.setMultiSelectMode(active);
        layoutMultiSelectBanner.setVisibility(active ? View.VISIBLE : View.GONE);
        MenuItem item = toolbar.getMenu().findItem(R.id.action_toggle_multi_select);
        if (item != null) {
            item.setIcon(active ? R.drawable.ic_check_box : R.drawable.ic_check_box_outline);
            item.setTitle(active ? "Thoát chọn nhiều" : "Chọn nhiều ô");
        }
        if (active) {
            Toast.makeText(this, "Chế độ chọn nhiều ô: Chạm hoặc quét để chọn!", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupGridView() {
        farmGridView.setZone(currentZone);

        // Single click -> Open Cell Detail Bottom Sheet when not in multi-select mode
        farmGridView.setOnCellClickListener(cell -> {
            CellDetailBottomSheetDialog dialog = CellDetailBottomSheetDialog.newInstance(
                    currentZone.getId(),
                    cell.getRowIndex(),
                    cell.getColIndex()
            );
            dialog.setOnCellUpdatedListener(updatedCell -> {
                farmGridView.invalidate();
                updateSummaryPills();
                updateHarvestFilterChips();
            });
            dialog.show(getSupportFragmentManager(), "CellDetailBottomSheetDialog");
        });

        // Multi selection listener
        farmGridView.setOnSelectionChangedListener(selectedCount -> {
            if (farmGridView.isMultiSelectMode() && selectedCount > 0) {
                cardBulkActions.setVisibility(View.VISIBLE);
                tvSelectedCount.setText("Đã chọn " + selectedCount + " ô");
            } else {
                cardBulkActions.setVisibility(View.GONE);
            }
            layoutMultiSelectBanner.setVisibility(farmGridView.isMultiSelectMode() ? View.VISIBLE : View.GONE);
            MenuItem item = toolbar.getMenu().findItem(R.id.action_toggle_multi_select);
            if (item != null) {
                item.setIcon(farmGridView.isMultiSelectMode() ? R.drawable.ic_check_box : R.drawable.ic_check_box_outline);
            }
        });
    }

    private void setupControls() {
        // Zoom & Fit
        btnZoomIn.setOnClickListener(v -> farmGridView.zoomIn());
        btnZoomOut.setOnClickListener(v -> farmGridView.zoomOut());
        btnFitScreen.setOnClickListener(v -> farmGridView.fitToScreen());

        // Multi-select banner mode controls (Tap vs Brush)
        toggleGroupSelectMode.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            if (checkedId == R.id.btn_mode_brush) {
                farmGridView.setBrushMode(true);
                tvModeHint.setText("Chế độ quét: Vuốt qua các ô");
                Toast.makeText(this, "Chế độ quét: Vuốt ngón tay qua các ô để chọn nhanh!", Toast.LENGTH_SHORT).show();
            } else {
                farmGridView.setBrushMode(false);
                tvModeHint.setText("Chế độ chạm: Chạm từng ô");
                Toast.makeText(this, "Chế độ chạm: Chạm từng ô để bật/tắt chọn", Toast.LENGTH_SHORT).show();
            }
        });

        btnExitMultiSelect.setOnClickListener(v -> setMultiSelectModeUI(false));

        // Status pills as quick selectors
        tvPillHealthy.setOnClickListener(v -> toggleStatusSelection(CellStatus.HEALTHY, "Khỏe mạnh"));
        tvPillWarning.setOnClickListener(v -> toggleStatusSelection(CellStatus.WARNING, "Cần chăm sóc"));
        tvPillDiseased.setOnClickListener(v -> toggleStatusSelection(CellStatus.DISEASED, "Sâu bệnh"));
        tvPillEmpty.setOnClickListener(v -> toggleStatusSelection(CellStatus.EMPTY, "Trống"));

        // Harvest Filter chips
        chipFilterAll.setOnClickListener(v -> {
            setMultiSelectModeUI(true);
            farmGridView.selectAll();
            Toast.makeText(this, "Đã chọn tất cả cây trồng (" + farmGridView.getSelectedCount() + " ô)", Toast.LENGTH_SHORT).show();
        });

        chipFilterDue.setOnClickListener(v -> {
            setMultiSelectModeUI(true);
            farmGridView.selectDueForHarvest();
            int count = farmGridView.getSelectedCount();
            if (count > 0) {
                Toast.makeText(this, "Đã chọn " + count + " cây đến/vượt kỳ thu hoạch (" + currentZone.getHarvestCycleDays() + " ngày)!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Chưa có cây nào đến kỳ thu hoạch!", Toast.LENGTH_SHORT).show();
            }
        });

        chipFilterRecent.setOnClickListener(v -> {
            setMultiSelectModeUI(true);
            farmGridView.selectRecentlyHarvested(15);
            int count = farmGridView.getSelectedCount();
            Toast.makeText(this, "Đã chọn " + count + " cây vừa thu hoạch trong 15 ngày qua", Toast.LENGTH_SHORT).show();
        });

        chipFilterNever.setOnClickListener(v -> {
            setMultiSelectModeUI(true);
            farmGridView.selectNeverHarvested();
            int count = farmGridView.getSelectedCount();
            Toast.makeText(this, "Đã chọn " + count + " cây chưa từng ghi nhận thu hoạch", Toast.LENGTH_SHORT).show();
        });

        // Selection actions
        btnSelectAll.setOnClickListener(v -> farmGridView.selectAll());
        btnInvertSelection.setOnClickListener(v -> farmGridView.invertSelection());
        btnClearSelection.setOnClickListener(v -> farmGridView.clearSelection());

        // Bulk Care Actions: 1. Water
        btnBulkWater.setOnClickListener(v -> {
            List<Cell> selected = farmGridView.getSelectedCells();
            if (selected.isEmpty()) return;

            new AlertDialog.Builder(this)
                    .setTitle("Tưới nước đồng loạt")
                    .setMessage("Tưới nước cho " + selected.size() + " ô đã chọn? Trạng thái cảnh báo sẽ tự động chuyển về Khỏe mạnh.")
                    .setPositiveButton("Xác nhận tưới", (dialog, which) -> {
                        FarmRepository.getInstance().bulkPerformAction(
                                currentZone.getId(),
                                selected,
                                ActionType.WATER,
                                "Tưới nước hàng loạt theo nhóm ô đã chọn"
                        );
                        farmGridView.clearSelection();
                        farmGridView.invalidate();
                        updateSummaryPills();
                        updateHarvestFilterChips();
                        Toast.makeText(this, "Đã tưới nước thành công cho " + selected.size() + " vị trí!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        // Bulk Care Actions: 2. Fertilize
        btnBulkFertilize.setOnClickListener(v -> {
            List<Cell> selected = farmGridView.getSelectedCells();
            if (selected.isEmpty()) return;

            String[] fertilizers = new String[]{
                    "NPK 20-20-15 (Tăng trưởng lá)",
                    "Hữu cơ vi sinh Tricho (Bảo vệ rễ)",
                    "Phân Kali Clorua (Ngọt trái, chắc ruột dừa)",
                    "Phân trùn quế nguyên chất",
                    "Dinh dưỡng thủy canh AB tiêu chuẩn"
            };

            new AlertDialog.Builder(this)
                    .setTitle("Bón phân cho " + selected.size() + " ô")
                    .setItems(fertilizers, (dialog, which) -> {
                        String chosen = fertilizers[which];
                        FarmRepository.getInstance().bulkPerformAction(
                                currentZone.getId(),
                                selected,
                                ActionType.FERTILIZE,
                                "Bón phân " + chosen
                        );
                        farmGridView.clearSelection();
                        farmGridView.invalidate();
                        updateSummaryPills();
                        updateHarvestFilterChips();
                        Toast.makeText(this, "Đã bón phân cho " + selected.size() + " vị trí!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        // Bulk Care Actions: 3. Pesticide
        btnBulkPesticide.setOnClickListener(v -> {
            List<Cell> selected = farmGridView.getSelectedCells();
            if (selected.isEmpty()) return;

            String[] pesticides = new String[]{
                    "Thuốc sinh học bẫy bọ cánh cứng / đuông dừa",
                    "Chế phẩm sinh học Bacillus subtilis (Trừ nấm lá)",
                    "Dung dịch thảo mộc gừng tỏi ớt (Trừ sâu ăn lá)",
                    "Dầu khoáng nông nghiệp SK Enspray (Trừ bọ trĩ)"
            };

            new AlertDialog.Builder(this)
                    .setTitle("Phun thuốc phòng trừ cho " + selected.size() + " ô")
                    .setItems(pesticides, (dialog, which) -> {
                        String chosen = pesticides[which];
                        FarmRepository.getInstance().bulkPerformAction(
                                currentZone.getId(),
                                selected,
                                ActionType.PESTICIDE,
                                "Phun " + chosen
                        );
                        farmGridView.clearSelection();
                        farmGridView.invalidate();
                        updateSummaryPills();
                        updateHarvestFilterChips();
                        Toast.makeText(this, "Đã phun phòng trừ cho " + selected.size() + " vị trí!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        // Bulk Care Actions: 4. Harvest (Supports perennial crops like Coconut Trees)
        btnBulkHarvest.setOnClickListener(v -> {
            List<Cell> selected = farmGridView.getSelectedCells();
            if (selected.isEmpty()) return;

            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 20, 50, 10);

            final EditText etYield = new EditText(this);
            etYield.setHint("Sản lượng / Chi tiết (VD: Dừa xiêm lứa 4, 12 trái/cây)");
            layout.addView(etYield);

            final CheckBox cbKeepTree = new CheckBox(this);
            cbKeepTree.setText("Giữ cây tiếp tục sinh trưởng (Cây lâu năm / dừa)");
            cbKeepTree.setChecked(true); // Default to keep tree
            layout.addView(cbKeepTree);

            new AlertDialog.Builder(this)
                    .setTitle("Ghi nhận thu hoạch " + selected.size() + " cây")
                    .setMessage("Hệ thống sẽ cập nhật mốc thời gian thu hoạch mới nhất để tính toán chu kỳ cho các lần sau:")
                    .setView(layout)
                    .setPositiveButton("Xác nhận thu hoạch", (dialog, which) -> {
                        String yieldText = etYield.getText().toString().trim();
                        if (yieldText.isEmpty()) yieldText = "Thu hoạch đồng loạt " + selected.size() + " cây";
                        boolean keepTree = cbKeepTree.isChecked();

                        FarmRepository.getInstance().bulkHarvest(
                                currentZone.getId(),
                                selected,
                                yieldText,
                                keepTree
                        );
                        farmGridView.clearSelection();
                        farmGridView.invalidate();
                        updateSummaryPills();
                        updateHarvestFilterChips();
                        Toast.makeText(this, "Đã ghi nhận thu hoạch thành công cho " + selected.size() + " cây!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        // Bulk Care Actions: 5. Plant / Assign Crop
        btnBulkPlant.setOnClickListener(v -> {
            List<Cell> selected = farmGridView.getSelectedCells();
            if (selected.isEmpty()) return;

            final EditText input = new EditText(this);
            input.setHint("Nhập tên giống cây trồng mới...");
            input.setText(currentZone.getDefaultCrop());
            input.setSelection(input.getText().length());
            input.setPadding(40, 30, 40, 30);

            new AlertDialog.Builder(this)
                    .setTitle("Gieo trồng mới cho " + selected.size() + " ô")
                    .setMessage("Cây giống sẽ được gán và trạng thái các ô chuyển sang Khỏe mạnh:")
                    .setView(input)
                    .setPositiveButton("Gieo trồng", (dialog, which) -> {
                        String cropName = input.getText().toString().trim();
                        if (cropName.isEmpty()) cropName = currentZone.getDefaultCrop();

                        FarmRepository.getInstance().bulkPlantCrops(
                                currentZone.getId(),
                                selected,
                                cropName,
                                "Gieo trồng mới giống " + cropName
                        );
                        farmGridView.clearSelection();
                        farmGridView.invalidate();
                        updateSummaryPills();
                        updateHarvestFilterChips();
                        Toast.makeText(this, "Đã gieo trồng " + cropName + " cho " + selected.size() + " ô!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });

        // Bulk Care Actions: 6. Change Status
        btnBulkStatus.setOnClickListener(v -> showBulkStatusDialog());
    }

    private void toggleStatusSelection(CellStatus status, String statusName) {
        setMultiSelectModeUI(true);
        farmGridView.toggleSelectByStatus(status);
        int count = farmGridView.getSelectedCount();
        Toast.makeText(this, "Đã chọn nhóm ô " + statusName + " (" + count + " ô)", Toast.LENGTH_SHORT).show();
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
                    updateHarvestFilterChips();
                    Toast.makeText(this, "Đã cập nhật trạng thái " + selected.size() + " ô!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showHarvestStatsDialog() {
        if (currentZone == null) return;
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_zone_harvest_stats, null);

        TextView tvTitle = dialogView.findViewById(R.id.tv_stats_title);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_stats_subtitle);
        TextView tvDueCount = dialogView.findViewById(R.id.tv_stats_due_count);
        TextView tvRecentCount = dialogView.findViewById(R.id.tv_stats_recent_count);
        TextView tvNeverCount = dialogView.findViewById(R.id.tv_stats_never_count);
        TextView tvHealthSummary = dialogView.findViewById(R.id.tv_stats_health_summary);
        MaterialButton btnSelectDue = dialogView.findViewById(R.id.btn_stats_select_due);
        MaterialButton btnToggleView = dialogView.findViewById(R.id.btn_stats_toggle_view_mode);

        int totalActive = currentZone.getActivePlantCount();
        int dueCount = currentZone.getDueForHarvestCount();
        int recentCount = currentZone.getHarvestedRecentlyCount(15);
        int neverCount = currentZone.getNeverHarvestedCount();

        int duePercent = totalActive > 0 ? (dueCount * 100 / totalActive) : 0;
        int recentPercent = totalActive > 0 ? (recentCount * 100 / totalActive) : 0;
        int neverPercent = totalActive > 0 ? (neverCount * 100 / totalActive) : 0;

        tvTitle.setText("Thống kê thu hoạch: " + currentZone.getName());
        tvSubtitle.setText("🌱 " + currentZone.getDefaultCrop() + " • Chu kỳ tiêu chuẩn: " + currentZone.getHarvestCycleDays() + " ngày/lứa");

        tvDueCount.setText(dueCount + " cây (" + duePercent + "%)");
        tvRecentCount.setText(recentCount + " cây (" + recentPercent + "%)");
        tvNeverCount.setText(neverCount + " cây (" + neverPercent + "%)");

        tvHealthSummary.setText("Sức khỏe: 🟢 " + currentZone.getHealthyCount() + " Khỏe • 🟡 " + currentZone.getWarningCount() + " Cần chăm sóc • 🔴 " + currentZone.getDiseasedCount() + " Sâu bệnh");

        btnToggleView.setText(farmGridView.isShowHarvestInfo() ? "Chuyển về xem tên cây" : "Hiện số ngày thu hoạch trên ô");

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Đóng", null)
                .create();

        btnSelectDue.setOnClickListener(v -> {
            dialog.dismiss();
            setMultiSelectModeUI(true);
            farmGridView.selectDueForHarvest();
            Toast.makeText(this, "Đã chọn " + farmGridView.getSelectedCount() + " cây đến kỳ thu hoạch!", Toast.LENGTH_SHORT).show();
        });

        btnToggleView.setOnClickListener(v -> {
            dialog.dismiss();
            boolean next = !farmGridView.isShowHarvestInfo();
            farmGridView.setShowHarvestInfo(next);
            Toast.makeText(this, next ? "Đang hiện số ngày thu hoạch từng cây" : "Đang hiện tên cây trồng", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showEditZoneDialog() {
        if (currentZone == null) return;
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

        etName.setText(currentZone.getName());
        etCrop.setText(currentZone.getDefaultCrop());
        etDesc.setText(currentZone.getDescription());
        etCycle.setText(String.valueOf(currentZone.getHarvestCycleDays()));
        etRows.setText(String.valueOf(currentZone.getRows()));
        etCols.setText(String.valueOf(currentZone.getCols()));

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

                    if (newName.isEmpty()) newName = currentZone.getName();
                    if (newCrop.isEmpty()) newCrop = currentZone.getDefaultCrop();
                    int cycle = 30;
                    try {
                        cycle = Integer.parseInt(cycleStr);
                    } catch (NumberFormatException ignored) {}

                    int newRows = currentZone.getRows();
                    int newCols = currentZone.getCols();
                    try {
                        newRows = Integer.parseInt(rowsStr);
                    } catch (NumberFormatException ignored) {}
                    try {
                        newCols = Integer.parseInt(colsStr);
                    } catch (NumberFormatException ignored) {}

                    if (newRows < 1) newRows = 1;
                    if (newCols < 1) newCols = 1;

                    FarmRepository.getInstance().updateZoneInfo(currentZone.getId(), newName, newDesc, newCrop, cycle, newRows, newCols);
                    currentZone = FarmRepository.getInstance().getZoneById(zoneId);
                    updateToolbarTitles();
                    farmGridView.setZone(currentZone);
                    farmGridView.post(() -> farmGridView.fitToScreen());
                    farmGridView.invalidate();
                    updateSummaryPills();
                    updateHarvestFilterChips();
                    Toast.makeText(this, "Đã cập nhật thông tin và kích thước khu (" + newRows + "×" + newCols + ")!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteZoneConfirmation() {
        if (currentZone == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa khu: " + currentZone.getName())
                .setMessage("Bạn có chắc chắn muốn xóa khu vườn này?\n\nToàn bộ dữ liệu ô đất và lịch sử công việc canh tác của khu sẽ bị xóa khỏi bộ nhớ máy và đồng bộ lên Firebase.")
                .setPositiveButton("Xác nhận xóa", (d, w) -> {
                    FarmRepository.getInstance().deleteZone(currentZone.getId());
                    Toast.makeText(this, "Đã xóa khu " + currentZone.getName() + " thành công!", Toast.LENGTH_SHORT).show();
                    finish();
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

    private void updateHarvestFilterChips() {
        if (currentZone == null) return;
        chipFilterAll.setText("🌴 Tất cả (" + currentZone.getActivePlantCount() + ")");
        chipFilterDue.setText("⏳ Đến kỳ (" + currentZone.getDueForHarvestCount() + ")");
        chipFilterRecent.setText("🕒 Vừa thu (" + currentZone.getHarvestedRecentlyCount(15) + ")");
        chipFilterNever.setText("❓ Chưa thu (" + currentZone.getNeverHarvestedCount() + ")");
    }
}
