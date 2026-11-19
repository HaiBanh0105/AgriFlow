package com.example.agriflow.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    }

    private void bindData() {
        String coordName = Cell.getColLetter(colIndex) + (rowIndex + 1);
        tvSheetCoord.setText("Hàng " + (rowIndex + 1) + " • Cột " + Cell.getColLetter(colIndex) + " (" + coordName + ")");
        updateStatusText(currentCell.getStatus());

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
        btnActionHarvest.setOnClickListener(v -> {
            addCareAction(ActionType.HARVEST, "Thu hoạch nông sản");
            chipEmpty.setChecked(true);
            updateStatusText(CellStatus.EMPTY);
        });

        // Save
        btnSaveCell.setOnClickListener(v -> saveChanges());
    }

    private void addCareAction(ActionType actionType, String defaultNote) {
        CareLog log = new CareLog(
                UUID.randomUUID().toString(),
                currentCell.getId(),
                actionType,
                defaultNote,
                System.currentTimeMillis()
        );
        currentCell.addCareLog(log);
        if (actionType == ActionType.WATER) {
            currentCell.setLastWatered(System.currentTimeMillis());
            if (currentCell.getStatus() == CellStatus.WARNING) {
                chipHealthy.setChecked(true);
                updateStatusText(CellStatus.HEALTHY);
            }
        } else if (actionType == ActionType.FERTILIZE) {
            currentCell.setLastFertilized(System.currentTimeMillis());
        }

        careLogAdapter.setLogs(currentCell.getCareLogs());
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
        currentCell.setPlantType(etCropName.getText() != null ? etCropName.getText().toString().trim() : "");
        currentCell.setNotes(etCellNotes.getText() != null ? etCellNotes.getText().toString().trim() : "");

        FarmRepository.getInstance().updateCell(zoneId, currentCell);

        if (updateListener != null) {
            updateListener.onCellUpdated(currentCell);
        }

        Toast.makeText(getContext(), "Đã lưu cập nhật vị trí!", Toast.LENGTH_SHORT).show();
        dismiss();
    }
}
