package com.example.agriflow.ui.dialog;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.agriflow.R;
import com.example.agriflow.data.model.Zone;
import com.example.agriflow.data.repository.FarmRepository;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.UUID;

public class CreateZoneBottomSheetDialog extends BottomSheetDialogFragment {

    public interface OnZoneCreatedListener {
        void onZoneCreated(Zone newZone);
    }

    private OnZoneCreatedListener listener;

    private TextInputEditText etZoneName;
    private TextInputEditText etDefaultCrop;
    private TextInputEditText etRows;
    private TextInputEditText etCols;
    private TextInputEditText etZoneDesc;
    private MaterialButton btnPreset5x5, btnPreset8x10, btnPreset10x15;
    private MaterialButton btnSubmit;

    public void setOnZoneCreatedListener(OnZoneCreatedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_create_zone, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etZoneName = view.findViewById(R.id.et_zone_name);
        etDefaultCrop = view.findViewById(R.id.et_default_crop);
        etRows = view.findViewById(R.id.et_rows);
        etCols = view.findViewById(R.id.et_cols);
        etZoneDesc = view.findViewById(R.id.et_zone_desc);

        btnPreset5x5 = view.findViewById(R.id.btn_preset_5x5);
        btnPreset8x10 = view.findViewById(R.id.btn_preset_8x10);
        btnPreset10x15 = view.findViewById(R.id.btn_preset_10x15);
        btnSubmit = view.findViewById(R.id.btn_create_zone_submit);

        // Presets
        btnPreset5x5.setOnClickListener(v -> {
            etRows.setText("5");
            etCols.setText("5");
        });

        btnPreset8x10.setOnClickListener(v -> {
            etRows.setText("8");
            etCols.setText("10");
        });

        btnPreset10x15.setOnClickListener(v -> {
            etRows.setText("10");
            etCols.setText("15");
        });

        btnSubmit.setOnClickListener(v -> createZone());
    }

    private void createZone() {
        String name = etZoneName.getText() != null ? etZoneName.getText().toString().trim() : "";
        String crop = etDefaultCrop.getText() != null ? etDefaultCrop.getText().toString().trim() : "";
        String rowsStr = etRows.getText() != null ? etRows.getText().toString().trim() : "";
        String colsStr = etCols.getText() != null ? etCols.getText().toString().trim() : "";
        String desc = etZoneDesc.getText() != null ? etZoneDesc.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            etZoneName.setError("Vui lòng nhập tên khu vườn");
            return;
        }

        if (TextUtils.isEmpty(crop)) {
            crop = "Cây trồng";
        }

        int rows = 8;
        int cols = 10;
        try {
            rows = Integer.parseInt(rowsStr);
            cols = Integer.parseInt(colsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(getContext(), "Số dòng và số cột phải là số nguyên", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rows < 1 || rows > 50 || cols < 1 || cols > 50) {
            Toast.makeText(getContext(), "Kích thước lưới hợp lệ từ 1×1 đến 50×50", Toast.LENGTH_SHORT).show();
            return;
        }

        String zoneId = "zone_" + UUID.randomUUID().toString().substring(0, 8);
        Zone newZone = new Zone(zoneId, name, desc, rows, cols, crop);

        FarmRepository.getInstance().addZone(newZone);

        Toast.makeText(getContext(), "Đã khởi tạo khu vườn " + name, Toast.LENGTH_SHORT).show();

        if (listener != null) {
            listener.onZoneCreated(newZone);
        }

        dismiss();
    }
}
