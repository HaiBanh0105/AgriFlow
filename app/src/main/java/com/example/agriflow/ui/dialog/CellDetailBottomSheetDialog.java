package com.example.agriflow.ui.dialog;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.agriflow.R;
import com.example.agriflow.data.model.ActionType;
import com.example.agriflow.data.model.CareLog;
import com.example.agriflow.data.model.Cell;
import com.example.agriflow.data.model.CellStatus;
import com.example.agriflow.data.repository.FarmRepository;
import com.example.agriflow.ui.adapter.CareLogAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class CellDetailBottomSheetDialog extends BottomSheetDialogFragment {

    public interface OnCellUpdatedListener {
        void onCellUpdated(Cell cell);
    }

    private static final String ARG_ZONE_ID = "arg_zone_id";
    private static final String ARG_ROW = "arg_row";
    private static final String ARG_COL = "arg_col";

    private String zoneId;
    private int rowIndex;
    private int colIndex;
    private Cell currentCell;
    private OnCellUpdatedListener updateListener;

    private TextView tvSheetCoord;
    private TextView tvSheetStatusText;
    private TextView tvSheetHarvestInfo;
    private ImageButton btnClose;
    private ChipGroup chipGroupStatus;
    private Chip chipHealthy, chipWarning, chipDiseased, chipEmpty, chipDisabled;
    private TextInputEditText etCropName;
    private TextInputEditText etCellNotes;
    private MaterialButton btnActionWater, btnActionFertilize, btnActionPesticide, btnActionHarvest, btnSaveCell;
    private RecyclerView rvCareLogs;
    private CareLogAdapter careLogAdapter;

    public static CellDetailBottomSheetDialog newInstance(String zoneId, int row, int col) {
        CellDetailBottomSheetDialog dialog = new CellDetailBottomSheetDialog();
        Bundle args = new Bundle();
        args.putString(ARG_ZONE_ID, zoneId);
        args.putInt(ARG_ROW, row);
        args.putInt(ARG_COL, col);
        dialog.setArguments(args);
        return dialog;
    }

    public void setOnCellUpdatedListener(OnCellUpdatedListener listener) {
        this.updateListener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_cell_detail_bottom_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            zoneId = getArguments().getString(ARG_ZONE_ID);
            rowIndex = getArguments().getInt(ARG_ROW);
            colIndex = getArguments().getInt(ARG_COL);
        }

        currentCell = FarmRepository.getInstance().getZoneById(zoneId).getCell(rowIndex, colIndex);
        if (currentCell == null) {
            dismiss();
            return;
        }

        initViews(view);
        bindData();
        setupEvents();
    }

    private void initViews(View view) {
        tvSheetCoord = view.findViewById(R.id.tv_sheet_coord);
        tvSheetStatusText = view.findViewById(R.id.tv_sheet_status_text);
        tvSheetHarvestInfo = view.findViewById(R.id.tv_sheet_harvest_info);
        btnClose = view.findViewById(R.id.btn_close_sheet);
        chipGroupStatus = view.findViewById(R.id.chip_group_status);
        chipHealthy = view.findViewById(R.id.chip_status_healthy);
        chipWarning = view.findViewById(R.id.chip_status_warning);
        chipDiseased = view.findViewById(R.id.chip_status_diseased);
        chipEmpty = view.findViewById(R.id.chip_status_empty);
        chipDisabled = view.findViewById(R.id.chip_status_disabled);
        etCropName = view.findViewById(R.id.et_crop_name);
        etCellNotes = view.findViewById(R.id.et_cell_notes);
        btnActionWater = view.findViewById(R.id.btn_action_water);
        btnActionFertilize = view.findViewById(R.id.btn_action_fertilize);
        btnActionPesticide = view.findViewById(R.id.btn_action_pesticide);
        btnActionHarvest = view.findViewById(R.id.btn_action_harvest);
        btnSaveCell = view.findViewById(R.id.btn_save_cell);
        rvCareLogs = view.findViewById(R.id.rv_care_logs);

        rvCareLogs.setLayoutManager(new LinearLayoutManager(getContext()));
        careLogAdapter = new CareLogAdapter();
        rvCareLogs.setAdapter(careLogAdapter);

        // Edit and Delete Log Callbacks
        careLogAdapter.setOnLogActionListener(new CareLogAdapter.OnLogActionListener() {
            @Override
            public void onEditLog(CareLog log) {
                showEditLogDialog(log);
            }

            @Override
            public void onDeleteLog(CareLog log) {
                showDeleteLogConfirmation(log);
            }
        });
    }

    private void bindData() {
        String coordName = Cell.getColLetter(colIndex) + (rowIndex + 1);
        tvSheetCoord.setText("Hàng " + (rowIndex + 1) + " • Cột " + Cell.getColLetter(colIndex) + " (" + coordName + ")");
        updateStatusText(currentCell.getStatus());
        updateHarvestInfoText();

        // Select matching status chip
        switch (currentCell.getStatus()) {
            case HEALTHY:
                chipHealthy.setChecked(true);
                break;
            case WARNING:
                chipWarning.setChecked(true);
                break;
            case DISEASED:
                chipDiseased.setChecked(true);
                break;
            case EMPTY:
                chipEmpty.setChecked(true);
                break;
            case DISABLED:
                chipDisabled.setChecked(true);
                break;
        }

        etCropName.setText(currentCell.getPlantType());
        etCellNotes.setText(currentCell.getNotes());

        careLogAdapter.setLogs(currentCell.getCareLogs());
    }

    private void updateStatusText(CellStatus status) {
        tvSheetStatusText.setText("Trạng thái: " + status.getDisplayName());
        tvSheetStatusText.setTextColor(status.getPrimaryColor());
    }

    private void updateHarvestInfoText() {
        if (tvSheetHarvestInfo == null) return;
        if (currentCell.getLastHarvested() > 0) {
            int days = currentCell.getDaysSinceLastHarvest();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String dateStr = sdf.format(new Date(currentCell.getLastHarvested()));
            tvSheetHarvestInfo.setText("✂️ Đã thu hoạch " + days + " ngày trước (" + dateStr + ")");
            tvSheetHarvestInfo.setVisibility(View.VISIBLE);
        } else {
            tvSheetHarvestInfo.setText("✂️ Cây chưa từng thu hoạch");
            tvSheetHarvestInfo.setVisibility(View.VISIBLE);
        }
    }

    private void setupEvents() {
        btnClose.setOnClickListener(v -> dismiss());

        chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chip_status_healthy)) {
                updateStatusText(CellStatus.HEALTHY);
            } else if (checkedIds.contains(R.id.chip_status_warning)) {
                updateStatusText(CellStatus.WARNING);
            } else if (checkedIds.contains(R.id.chip_status_diseased)) {
                updateStatusText(CellStatus.DISEASED);
            } else if (checkedIds.contains(R.id.chip_status_empty)) {
                updateStatusText(CellStatus.EMPTY);
            } else if (checkedIds.contains(R.id.chip_status_disabled)) {
                updateStatusText(CellStatus.DISABLED);
            }
        });

        // Quick actions
        btnActionWater.setOnClickListener(v -> addCareAction(ActionType.WATER, "Đã tưới nước bổ sung"));
        btnActionFertilize.setOnClickListener(v -> addCareAction(ActionType.FERTILIZE, "Đã bón phân vi lượng"));
        btnActionPesticide.setOnClickListener(v -> addCareAction(ActionType.PESTICIDE, "Đã phun thuốc sinh học"));

        // Perennial harvest dialog (Dừa / Cây ăn trái)
        btnActionHarvest.setOnClickListener(v -> showHarvestDialog());

        // Save
        btnSaveCell.setOnClickListener(v -> saveChanges());
    }

    private void showHarvestDialog() {
        if (getContext() == null) return;

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 10);

        final EditText etYield = new EditText(getContext());
        etYield.setHint("Nhập sản lượng & ghi chú (vd: 16 trái dừa, nước ngọt...)");
        etYield.setText("Thu hoạch đợt rộ: 15 trái dừa");
        layout.addView(etYield);

        final CheckBox cbKeepTree = new CheckBox(getContext());
        cbKeepTree.setText("Giữ cây tiếp tục sinh trưởng (Cây lâu năm / Dừa)");
        cbKeepTree.setChecked(true);
        layout.addView(cbKeepTree);

        new AlertDialog.Builder(getContext())
                .setTitle("Thu hoạch cây này")
                .setMessage("Ghi chú mốc thời gian thu hoạch để theo dõi khoảng thời gian cho đợt kế tiếp:")
                .setView(layout)
                .setPositiveButton("Lưu thu hoạch", (dialog, which) -> {
                    String note = etYield.getText().toString().trim();
                    if (note.isEmpty()) note = "Thu hoạch nông sản";
                    boolean keep = cbKeepTree.isChecked();

                    long now = System.currentTimeMillis();
                    currentCell.setLastHarvested(now);
                    if (keep) {
                        currentCell.setStatus(CellStatus.HEALTHY);
                        chipHealthy.setChecked(true);
                        updateStatusText(CellStatus.HEALTHY);
                    } else {
                        currentCell.setStatus(CellStatus.EMPTY);
                        currentCell.setPlantType("");
                        chipEmpty.setChecked(true);
                        updateStatusText(CellStatus.EMPTY);
                    }

                    CareLog log = new CareLog(UUID.randomUUID().toString(), currentCell.getId(), ActionType.HARVEST, note, now);
                    FarmRepository.getInstance().updateCell(zoneId, currentCell);
                    FarmRepository.getInstance().addCareLog(zoneId, currentCell.getId(), log);

                    careLogAdapter.setLogs(currentCell.getCareLogs());
                    updateHarvestInfoText();
                    if (updateListener != null) {
                        updateListener.onCellUpdated(currentCell);
                    }
                    Toast.makeText(getContext(), "Đã lưu mốc thu hoạch!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showEditLogDialog(CareLog log) {
        if (getContext() == null) return;

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 10);

        // Action type choices
        final ActionType[] allTypes = ActionType.values();
        String[] typeNames = new String[allTypes.length];
        int selectedTypeIndex = 0;
        for (int i = 0; i < allTypes.length; i++) {
            typeNames[i] = allTypes[i].getIconEmoji() + " " + allTypes[i].getDisplayName();
            if (allTypes[i] == log.getActionType()) {
                selectedTypeIndex = i;
            }
        }

        final TextView tvActionChosen = new TextView(getContext());
        tvActionChosen.setText("Công việc: " + log.getActionType().getIconEmoji() + " " + log.getActionType().getDisplayName());
        tvActionChosen.setTextSize(14f);
        tvActionChosen.setPadding(0, 10, 0, 10);
        layout.addView(tvActionChosen);

        final int[] chosenTypeHolder = new int[]{selectedTypeIndex};
        MaterialButton btnPickType = new MaterialButton(getContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnPickType.setText("Đổi loại công việc");
        btnPickType.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("Chọn loại công việc")
                    .setSingleChoiceItems(typeNames, chosenTypeHolder[0], (d, which) -> {
                        chosenTypeHolder[0] = which;
                        tvActionChosen.setText("Công việc: " + typeNames[which]);
                        d.dismiss();
                    })
                    .show();
        });
        layout.addView(btnPickType);

        // Date & Time picker
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(log.getTimestamp());
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());

        final TextView tvTimeDisplay = new TextView(getContext());
        tvTimeDisplay.setText("Thời gian: " + sdf.format(cal.getTime()));
        tvTimeDisplay.setTextSize(14f);
        tvTimeDisplay.setPadding(0, 16, 0, 10);
        layout.addView(tvTimeDisplay);

        MaterialButton btnPickTime = new MaterialButton(getContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        btnPickTime.setText("Chỉnh ngày & giờ");
        btnPickTime.setOnClickListener(v -> {
            new DatePickerDialog(getContext(), (dp, year, month, day) -> {
                cal.set(Calendar.YEAR, year);
                cal.set(Calendar.MONTH, month);
                cal.set(Calendar.DAY_OF_MONTH, day);
                new TimePickerDialog(getContext(), (tp, hour, minute) -> {
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    cal.set(Calendar.MINUTE, minute);
                    tvTimeDisplay.setText("Thời gian: " + sdf.format(cal.getTime()));
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });
        layout.addView(btnPickTime);

        // Notes
        final EditText etNote = new EditText(getContext());
        etNote.setHint("Nội dung ghi chú chi tiết / sản lượng");
        etNote.setText(log.getNote());
        layout.addView(etNote);

        new AlertDialog.Builder(getContext())
                .setTitle("Chỉnh sửa nhật ký công việc")
                .setView(layout)
                .setPositiveButton("Lưu sửa đổi", (d, which) -> {
                    log.setActionType(allTypes[chosenTypeHolder[0]]);
                    log.setTimestamp(cal.getTimeInMillis());
                    log.setNote(etNote.getText().toString().trim());

                    FarmRepository.getInstance().updateCareLog(zoneId, currentCell.getId(), log);
                    careLogAdapter.setLogs(currentCell.getCareLogs());
                    updateHarvestInfoText();
                    if (updateListener != null) {
                        updateListener.onCellUpdated(currentCell);
                    }
                    Toast.makeText(getContext(), "Đã sửa nhật ký thành công!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showDeleteLogConfirmation(CareLog log) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("Xóa nhật ký sai sót")
                .setMessage("Bạn có chắc muốn xóa nhật ký \"" + log.getActionType().getDisplayName() + ": " + log.getNote() + "\"?\n\nMốc thời gian của cây sẽ tự động được tính toán lại từ các nhật ký còn lại.")
                .setPositiveButton("Xóa nhật ký", (dialog, which) -> {
                    FarmRepository.getInstance().deleteCareLog(zoneId, currentCell.getId(), log.getId());
                    careLogAdapter.setLogs(currentCell.getCareLogs());
                    updateHarvestInfoText();
                    if (updateListener != null) {
                        updateListener.onCellUpdated(currentCell);
                    }
                    Toast.makeText(getContext(), "Đã xóa nhật ký sai sót!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void addCareAction(ActionType actionType, String defaultNote) {
        CareLog log = new CareLog(
                UUID.randomUUID().toString(),
                currentCell.getId(),
                actionType,
                defaultNote,
                System.currentTimeMillis()
        );
        if (actionType == ActionType.WATER) {
            currentCell.setLastWatered(System.currentTimeMillis());
            if (currentCell.getStatus() == CellStatus.WARNING) {
                chipHealthy.setChecked(true);
                updateStatusText(CellStatus.HEALTHY);
            }
        } else if (actionType == ActionType.FERTILIZE) {
            currentCell.setLastFertilized(System.currentTimeMillis());
        }

        FarmRepository.getInstance().addCareLog(zoneId, currentCell.getId(), log);
        careLogAdapter.setLogs(currentCell.getCareLogs());
        updateHarvestInfoText();
        Toast.makeText(getContext(), "Ghi nhận: " + actionType.getDisplayName(), Toast.LENGTH_SHORT).show();
    }

    private void saveChanges() {
        CellStatus selectedStatus = CellStatus.HEALTHY;
        int checkedChipId = chipGroupStatus.getCheckedChipId();
        if (checkedChipId == R.id.chip_status_warning) {
            selectedStatus = CellStatus.WARNING;
        } else if (checkedChipId == R.id.chip_status_diseased) {
            selectedStatus = CellStatus.DISEASED;
        } else if (checkedChipId == R.id.chip_status_empty) {
            selectedStatus = CellStatus.EMPTY;
        } else if (checkedChipId == R.id.chip_status_disabled) {
            selectedStatus = CellStatus.DISABLED;
        }

        currentCell.setStatus(selectedStatus);
        String cropInput = etCropName.getText() != null ? etCropName.getText().toString().trim() : "";
        if (selectedStatus != CellStatus.DISABLED && (cropInput.isEmpty() || "Lối đi".equals(cropInput) || "Lối đi kỹ thuật".equals(cropInput))) {
            com.example.agriflow.data.model.Zone zone = FarmRepository.getInstance().getZoneById(zoneId);
            if (zone != null && zone.getDefaultCrop() != null && !zone.getDefaultCrop().isEmpty()) {
                cropInput = zone.getDefaultCrop();
            }
        }
        currentCell.setPlantType(cropInput);
        currentCell.setNotes(etCellNotes.getText() != null ? etCellNotes.getText().toString().trim() : "");

        FarmRepository.getInstance().updateCell(zoneId, currentCell);

        if (updateListener != null) {
            updateListener.onCellUpdated(currentCell);
        }

        Toast.makeText(getContext(), "Đã lưu cập nhật vị trí!", Toast.LENGTH_SHORT).show();
        dismiss();
    }
}
