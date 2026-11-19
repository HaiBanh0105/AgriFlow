package com.example.agriflow.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import androidx.annotation.Nullable;

import com.example.agriflow.data.model.Cell;
import com.example.agriflow.data.model.CellStatus;
import com.example.agriflow.data.model.Zone;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FarmGridView extends View {

    public interface OnCellClickListener {
        void onCellClick(Cell cell);
    }

    public interface OnSelectionChangedListener {
        void onSelectionChanged(int selectedCount);
    }

    private Zone zone;
    private OnCellClickListener cellClickListener;
    private OnSelectionChangedListener selectionChangedListener;

    // Grid sizing
    private float baseCellSize = 130f; // in pixels
    private float cellMargin = 10f;
    private float headerSize = 75f;
    private float cornerRadius = 14f;

    // Zoom & Pan state
    private float scaleFactor = 1.0f;
    private float minScale = 0.4f;
    private float maxScale = 3.0f;
    private float posX = 0f;
    private float posY = 0f;
    private float lastTouchX;
    private float lastTouchY;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;

    // Selection
    private final Set<String> selectedCellIds = new HashSet<>();
    private boolean isMultiSelectMode = false;

    // Paints
    private Paint cellBackgroundPaint;
    private Paint cellBorderPaint;
    private Paint selectedBorderPaint;
    private Paint headerBackgroundPaint;
    private Paint headerTextPaint;
    private Paint cellTitlePaint;
    private Paint cellSubtitlePaint;
    private Paint statusDotPaint;
    private Paint disabledHatchPaint;

    // Gesture Detectors
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    public FarmGridView(Context context) {
        super(context);
        init(context);
    }

    public FarmGridView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FarmGridView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // Paints initialization
        cellBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        cellBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellBorderPaint.setStyle(Paint.Style.STROKE);
        cellBorderPaint.setStrokeWidth(3f);

        selectedBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedBorderPaint.setStyle(Paint.Style.STROKE);
        selectedBorderPaint.setStrokeWidth(6f);
        selectedBorderPaint.setColor(Color.parseColor("#1976D2")); // Deep Blue selection

        headerBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerBackgroundPaint.setColor(Color.parseColor("#EEF2F0"));

        headerTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerTextPaint.setColor(Color.parseColor("#37474F"));
        headerTextPaint.setTextSize(26f);
        headerTextPaint.setTextAlign(Paint.Align.CENTER);
        headerTextPaint.setFakeBoldText(true);

        cellTitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellTitlePaint.setColor(Color.parseColor("#263238"));
        cellTitlePaint.setTextSize(24f);
        cellTitlePaint.setTextAlign(Paint.Align.CENTER);
        cellTitlePaint.setFakeBoldText(true);

        cellSubtitlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cellSubtitlePaint.setColor(Color.parseColor("#546E7A"));
        cellSubtitlePaint.setTextSize(18f);
        cellSubtitlePaint.setTextAlign(Paint.Align.CENTER);

        statusDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        disabledHatchPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        disabledHatchPaint.setColor(Color.parseColor("#B0BEC5"));
        disabledHatchPaint.setStyle(Paint.Style.STROKE);
        disabledHatchPaint.setStrokeWidth(2f);
        disabledHatchPaint.setPathEffect(new DashPathEffect(new float[]{6, 6}, 0));

        // Scale detector
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());

        // Gesture detector for taps
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public void setZone(Zone zone) {
        this.zone = zone;
        selectedCellIds.clear();
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(0);
        }
        post(() -> fitToScreen());
        invalidate();
    }

    public void setOnCellClickListener(OnCellClickListener listener) {
        this.cellClickListener = listener;
    }

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.selectionChangedListener = listener;
    }

    public boolean isMultiSelectMode() {
        return isMultiSelectMode;
    }

    public void setMultiSelectMode(boolean multiSelectMode) {
        isMultiSelectMode = multiSelectMode;
        if (!multiSelectMode) {
            selectedCellIds.clear();
            if (selectionChangedListener != null) {
                selectionChangedListener.onSelectionChanged(0);
            }
            invalidate();
        }
    }

    public List<Cell> getSelectedCells() {
        List<Cell> result = new ArrayList<>();
        if (zone == null) return result;
        for (Cell cell : zone.getCells()) {
            if (selectedCellIds.contains(cell.getId())) {
                result.add(cell);
            }
        }
        return result;
    }

    public void clearSelection() {
        selectedCellIds.clear();
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(0);
        }
        invalidate();
    }

    public void selectRow(int rowIndex) {
        if (zone == null || rowIndex < 0 || rowIndex >= zone.getRows()) return;
        for (int c = 0; c < zone.getCols(); c++) {
            Cell cell = zone.getCell(rowIndex, c);
            if (cell != null && cell.getStatus() != CellStatus.DISABLED) {
                selectedCellIds.add(cell.getId());
            }
        }
        notifySelectionChanged();
        invalidate();
    }

    public void selectColumn(int colIndex) {
        if (zone == null || colIndex < 0 || colIndex >= zone.getCols()) return;
        for (int r = 0; r < zone.getRows(); r++) {
            Cell cell = zone.getCell(r, colIndex);
            if (cell != null && cell.getStatus() != CellStatus.DISABLED) {
                selectedCellIds.add(cell.getId());
            }
        }
        notifySelectionChanged();
        invalidate();
    }

    public void selectAll() {
        if (zone == null) return;
        for (Cell cell : zone.getCells()) {
            if (cell.getStatus() != CellStatus.DISABLED) {
                selectedCellIds.add(cell.getId());
            }
        }
        notifySelectionChanged();
        invalidate();
    }

    private void notifySelectionChanged() {
        if (selectionChangedListener != null) {
            selectionChangedListener.onSelectionChanged(selectedCellIds.size());
        }
    }

    public void zoomIn() {
        scaleFactor = Math.min(scaleFactor * 1.3f, maxScale);
        invalidate();
    }

    public void zoomOut() {
        scaleFactor = Math.max(scaleFactor / 1.3f, minScale);
        invalidate();
    }

    public void fitToScreen() {
        if (zone == null || getWidth() == 0 || getHeight() == 0) return;

        float totalGridWidth = headerSize + zone.getCols() * (baseCellSize + cellMargin) + cellMargin;
        float totalGridHeight = headerSize + zone.getRows() * (baseCellSize + cellMargin) + cellMargin;

        float availableWidth = getWidth() - 40;
        float availableHeight = getHeight() - 40;

        float scaleW = availableWidth / totalGridWidth;
        float scaleH = availableHeight / totalGridHeight;
        scaleFactor = Math.max(minScale, Math.min(Math.min(scaleW, scaleH), 1.2f));

        posX = (getWidth() - totalGridWidth * scaleFactor) / 2f;
        posY = Math.max(30f, (getHeight() - totalGridHeight * scaleFactor) / 2f);

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (zone == null) return;

        canvas.save();
        canvas.translate(posX, posY);
        canvas.scale(scaleFactor, scaleFactor);

        int rows = zone.getRows();
        int cols = zone.getCols();

        // 1. Draw Column Headers (Top)
        for (int c = 0; c < cols; c++) {
            float left = headerSize + c * (baseCellSize + cellMargin);
            float top = 0;
            float right = left + baseCellSize;
            float bottom = headerSize - 10f;
            RectF headerRect = new RectF(left, top, right, bottom);

            headerBackgroundPaint.setColor(Color.parseColor("#E8EBE9"));
            canvas.drawRoundRect(headerRect, 8f, 8f, headerBackgroundPaint);

            String colLabel = Cell.getColLetter(c);
            float textY = headerRect.centerY() - ((headerTextPaint.descent() + headerTextPaint.ascent()) / 2);
            canvas.drawText(colLabel, headerRect.centerX(), textY, headerTextPaint);
        }

        // 2. Draw Row Headers (Left)
        for (int r = 0; r < rows; r++) {
            float left = 0;
            float top = headerSize + r * (baseCellSize + cellMargin);
            float right = headerSize - 10f;
            float bottom = top + baseCellSize;
            RectF headerRect = new RectF(left, top, right, bottom);

            headerBackgroundPaint.setColor(Color.parseColor("#E8EBE9"));
            canvas.drawRoundRect(headerRect, 8f, 8f, headerBackgroundPaint);

            String rowLabel = String.valueOf(r + 1);
            float textY = headerRect.centerY() - ((headerTextPaint.descent() + headerTextPaint.ascent()) / 2);
            canvas.drawText(rowLabel, headerRect.centerX(), textY, headerTextPaint);
        }

        // 3. Draw Top-Left Corner Origin
        RectF cornerRect = new RectF(0, 0, headerSize - 10f, headerSize - 10f);
        headerBackgroundPaint.setColor(Color.parseColor("#D7DFDB"));
        canvas.drawRoundRect(cornerRect, 8f, 8f, headerBackgroundPaint);
        float cornerTextY = cornerRect.centerY() - ((headerTextPaint.descent() + headerTextPaint.ascent()) / 2);
        canvas.drawText("#", cornerRect.centerX(), cornerTextY, headerTextPaint);

        // 4. Draw Cells
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = zone.getCell(r, c);
                if (cell == null) continue;

                float left = headerSize + c * (baseCellSize + cellMargin);
                float top = headerSize + r * (baseCellSize + cellMargin);
                float right = left + baseCellSize;
                float bottom = top + baseCellSize;
                RectF cellRect = new RectF(left, top, right, bottom);

                boolean isSelected = selectedCellIds.contains(cell.getId());
                CellStatus status = cell.getStatus();

                // Draw background
                cellBackgroundPaint.setColor(status.getBackgroundColor());
                canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, cellBackgroundPaint);

                // Draw border
                if (isSelected) {
                    canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, selectedBorderPaint);
                } else {
                    cellBorderPaint.setColor(status.getBorderColor());
                    canvas.drawRoundRect(cellRect, cornerRadius, cornerRadius, cellBorderPaint);
                }

                // If disabled, draw hatched lines or path indication
                if (status == CellStatus.DISABLED) {
                    canvas.drawLine(left + 15, top + 15, right - 15, bottom - 15, disabledHatchPaint);
                    canvas.drawLine(left + 15, bottom - 15, right - 15, top + 15, disabledHatchPaint);
                    cellSubtitlePaint.setColor(Color.parseColor("#78909C"));
                    canvas.drawText("Lối đi", cellRect.centerX(), cellRect.centerY() + 8f, cellSubtitlePaint);
                    continue;
                }

                // Draw Coordinate Code (e.g. "A1", "C4")
                String coord = Cell.getColLetter(c) + (r + 1);
                cellTitlePaint.setColor(isSelected ? Color.parseColor("#1565C0") : Color.parseColor("#263238"));
                canvas.drawText(coord, cellRect.centerX(), top + 36f, cellTitlePaint);

                // Draw Status Dot
                statusDotPaint.setColor(status.getPrimaryColor());
                canvas.drawCircle(cellRect.centerX(), top + 60f, 10f, statusDotPaint);

                // Draw Status text or Crop Short label
                if (status == CellStatus.EMPTY) {
                    cellSubtitlePaint.setColor(Color.parseColor("#9E9E9E"));
                    canvas.drawText("Trống", cellRect.centerX(), bottom - 20f, cellSubtitlePaint);
                } else if (cell.getPlantType() != null && !cell.getPlantType().isEmpty()) {
                    String shortCrop = cell.getPlantType();
                    if (shortCrop.length() > 8) {
                        shortCrop = shortCrop.substring(0, 7) + "…";
                    }
                    cellSubtitlePaint.setColor(status.getPrimaryColor());
                    canvas.drawText(shortCrop, cellRect.centerX(), bottom - 20f, cellSubtitlePaint);
                }

                // Selection checkmark or badge
                if (isSelected) {
                    Paint selectBadge = new Paint(Paint.ANTI_ALIAS_FLAG);
                    selectBadge.setColor(Color.parseColor("#1976D2"));
                    canvas.drawCircle(right - 14f, top + 14f, 10f, selectBadge);

                    Paint checkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    checkPaint.setColor(Color.WHITE);
                    checkPaint.setStrokeWidth(3f);
                    checkPaint.setStyle(Paint.Style.STROKE);
                    canvas.drawLine(right - 18f, top + 14f, right - 14f, top + 18f, checkPaint);
                    canvas.drawLine(right - 14f, top + 18f, right - 10f, top + 10f, checkPaint);
                }
            }
        }

        canvas.restore();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean scaleHandled = scaleGestureDetector.onTouchEvent(event);
        boolean gestureHandled = gestureDetector.onTouchEvent(event);

        final int action = event.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                final int pointerIndex = event.getActionIndex();
                final float x = event.getX(pointerIndex);
                final float y = event.getY(pointerIndex);

                lastTouchX = x;
                lastTouchY = y;
                activePointerId = event.getPointerId(0);
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (!scaleGestureDetector.isInProgress()) {
                    final int pointerIndex = event.findPointerIndex(activePointerId);
                    if (pointerIndex != -1) {
                        final float x = event.getX(pointerIndex);
                        final float y = event.getY(pointerIndex);

                        final float dx = x - lastTouchX;
                        final float dy = y - lastTouchY;

                        posX += dx;
                        posY += dy;

                        lastTouchX = x;
                        lastTouchY = y;

                        invalidate();
                    }
                }
                break;
            }

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                break;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                final int pointerIndex = event.getActionIndex();
                final int pointerId = event.getPointerId(pointerIndex);

                if (pointerId == activePointerId) {
                    final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                    lastTouchX = event.getX(newPointerIndex);
                    lastTouchY = event.getY(newPointerIndex);
                    activePointerId = event.getPointerId(newPointerIndex);
                }
                break;
            }
        }

        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float previousScale = scaleFactor;
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(minScale, Math.min(scaleFactor, maxScale));

            // Zoom focusing around the focus point
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            posX = focusX - (focusX - posX) * (scaleFactor / previousScale);
            posY = focusY - (focusY - posY) * (scaleFactor / previousScale);

            invalidate();
            return true;
        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (zone == null) return false;

            // Translate tap coordinates to grid coordinates
            float tapX = (e.getX() - posX) / scaleFactor;
            float tapY = (e.getY() - posY) / scaleFactor;

            // Check if tapped on Row Header
            if (tapX >= 0 && tapX < headerSize && tapY >= headerSize) {
                int clickedRow = (int) ((tapY - headerSize) / (baseCellSize + cellMargin));
                if (clickedRow >= 0 && clickedRow < zone.getRows()) {
                    selectRow(clickedRow);
                    return true;
                }
            }

            // Check if tapped on Column Header
            if (tapY >= 0 && tapY < headerSize && tapX >= headerSize) {
                int clickedCol = (int) ((tapX - headerSize) / (baseCellSize + cellMargin));
                if (clickedCol >= 0 && clickedCol < zone.getCols()) {
                    selectColumn(clickedCol);
                    return true;
                }
            }

            // Check if tapped on Corner (#) -> Select All / Deselect All
            if (tapX >= 0 && tapX < headerSize && tapY >= 0 && tapY < headerSize) {
                if (selectedCellIds.isEmpty()) {
                    selectAll();
                } else {
                    clearSelection();
                }
                return true;
            }

            // Check if tapped on a Cell
            if (tapX >= headerSize && tapY >= headerSize) {
                int col = (int) ((tapX - headerSize) / (baseCellSize + cellMargin));
                int row = (int) ((tapY - headerSize) / (baseCellSize + cellMargin));

                if (row >= 0 && row < zone.getRows() && col >= 0 && col < zone.getCols()) {
                    Cell cell = zone.getCell(row, col);
                    if (cell != null) {
                        if (isMultiSelectMode) {
                            if (selectedCellIds.contains(cell.getId())) {
                                selectedCellIds.remove(cell.getId());
                            } else {
                                selectedCellIds.add(cell.getId());
                            }
                            notifySelectionChanged();
                            invalidate();
                        } else {
                            // If in normal mode, select this single cell and trigger listener
                            selectedCellIds.clear();
                            selectedCellIds.add(cell.getId());
                            notifySelectionChanged();
                            invalidate();

                            if (cellClickListener != null) {
                                cellClickListener.onCellClick(cell);
                            }
                        }
                        return true;
                    }
                }
            }

            return super.onSingleTapUp(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // Long press toggles multi-select mode
            isMultiSelectMode = true;

            float tapX = (e.getX() - posX) / scaleFactor;
            float tapY = (e.getY() - posY) / scaleFactor;

            if (tapX >= headerSize && tapY >= headerSize && zone != null) {
                int col = (int) ((tapX - headerSize) / (baseCellSize + cellMargin));
                int row = (int) ((tapY - headerSize) / (baseCellSize + cellMargin));
                if (row >= 0 && row < zone.getRows() && col >= 0 && col < zone.getCols()) {
                    Cell cell = zone.getCell(row, col);
                    if (cell != null) {
                        selectedCellIds.add(cell.getId());
                        notifySelectionChanged();
                        invalidate();
                    }
                }
            }
        }
    }
}
