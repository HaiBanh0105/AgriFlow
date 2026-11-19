package com.example.agriflow;

import org.junit.Test;

import static org.junit.Assert.*;

import com.example.agriflow.data.local.entity.CareLogEntity;
import com.example.agriflow.data.local.entity.CellEntity;
import com.example.agriflow.data.local.entity.ZoneEntity;
import com.example.agriflow.data.model.ActionType;
import com.example.agriflow.data.model.CareLog;
import com.example.agriflow.data.model.Cell;
import com.example.agriflow.data.model.CellStatus;
import com.example.agriflow.data.model.Zone;
import com.example.agriflow.data.repository.FarmRepository;

import java.util.Collections;

public class ExampleUnitTest {

    @Test
    public void testCoordinateLetterConversion() {
        assertEquals("A", Cell.getColLetter(0));
        assertEquals("B", Cell.getColLetter(1));
        assertEquals("C", Cell.getColLetter(2));
        assertEquals("Z", Cell.getColLetter(25));
    }

    @Test
    public void testZoneInitialization() {
        Zone zone = new Zone("test_zone", "Vườn Test", "Mô tả", 5, 5, "Cà chua");
        assertEquals(25, zone.getTotalCells());
        assertNotNull(zone.getCell(0, 0));
        assertNotNull(zone.getCell(4, 4));
        assertNull(zone.getCell(5, 5)); // Out of bounds
    }

    @Test
    public void testHealthPercentageCalculation() {
        Zone zone = new Zone("test_zone", "Vườn Test", "Mô tả", 2, 2, "Cà chua");
        assertEquals(100, zone.getHealthPercentage());

        zone.getCell(0, 0).setStatus(CellStatus.DISEASED);
        assertEquals(75, zone.getHealthPercentage());

        zone.getCell(0, 1).setStatus(CellStatus.EMPTY);
        assertEquals(67, zone.getHealthPercentage());
    }

    @Test
    public void testFarmRepositoryBulkOperations() {
        FarmRepository repo = FarmRepository.getInstance();
        assertNotNull(repo.getZones());
        assertTrue(repo.getTotalZonesCount() >= 3);

        Zone zone = repo.getZones().get(0);
        Cell cell = zone.getCell(0, 0);
        assertNotNull(cell);

        repo.bulkPerformAction(zone.getId(), Collections.singletonList(cell), ActionType.WATER, "Test tưới");
        assertFalse(cell.getCareLogs().isEmpty());
        assertEquals(ActionType.WATER, cell.getCareLogs().get(0).getActionType());
    }

    @Test
    public void testBulkPlantAndPesticideAndHarvest() {
        FarmRepository repo = FarmRepository.getInstance();
        Zone zone = new Zone("test_bulk_zone", "Khu Thử Nghiệm", "Mô tả", 3, 3, "Dưa lưới");
        repo.addZone(zone);

        Cell c00 = zone.getCell(0, 0);
        Cell c01 = zone.getCell(0, 1);
        Cell c02 = zone.getCell(0, 2);
        assertNotNull(c00);
        assertNotNull(c01);
        assertNotNull(c02);

        // 1. Bulk plant crops
        java.util.List<Cell> plantTargets = java.util.Arrays.asList(c00, c01, c02);
        repo.bulkPlantCrops(zone.getId(), plantTargets, "Dưa lưới F1", "Gieo đồng loạt 3 ô");
        assertEquals("Dưa lưới F1", c00.getPlantType());
        assertEquals(CellStatus.HEALTHY, c00.getStatus());
        assertEquals("Dưa lưới F1", c01.getPlantType());
        assertEquals("Dưa lưới F1", c02.getPlantType());

        // 2. Mark diseased and bulk pesticide
        c00.setStatus(CellStatus.DISEASED);
        c01.setStatus(CellStatus.DISEASED);
        repo.bulkPerformAction(zone.getId(), java.util.Arrays.asList(c00, c01), ActionType.PESTICIDE, "Phun trừ sâu");
        assertEquals(CellStatus.WARNING, c00.getStatus()); // Improved to WARNING
        assertEquals(CellStatus.WARNING, c01.getStatus());

        // 3. Bulk harvest
        repo.bulkPerformAction(zone.getId(), Collections.singletonList(c02), ActionType.HARVEST, "Thu hoạch trái chín");
        assertEquals(CellStatus.EMPTY, c02.getStatus());
        assertEquals("", c02.getPlantType());
    }

    @Test
    public void testRoomEntityMapping() {
        Zone zone = new Zone("z_test", "Khu D", "Mô tả test", 6, 8, "Dưa hấu");
        ZoneEntity zoneEntity = ZoneEntity.fromDomain(zone, false);
        assertEquals("z_test", zoneEntity.getId());
        assertEquals("Khu D", zoneEntity.getName());
        assertEquals(6, zoneEntity.getRows());
        assertEquals(8, zoneEntity.getCols());
        assertFalse(zoneEntity.isSynced());

        Zone restoredZone = zoneEntity.toDomain();
        assertEquals(zone.getId(), restoredZone.getId());
        assertEquals(zone.getName(), restoredZone.getName());

        Cell cell = zone.getCell(1, 2);
        cell.setStatus(CellStatus.WARNING);
        CellEntity cellEntity = CellEntity.fromDomain(cell, false);
        assertEquals("WARNING", cellEntity.getStatus());
        assertEquals(1, cellEntity.getRowIndex());
        assertEquals(2, cellEntity.getColIndex());

        Cell restoredCell = cellEntity.toDomain();
        assertEquals(CellStatus.WARNING, restoredCell.getStatus());

        CareLog log = new CareLog("log_1", cell.getId(), ActionType.FERTILIZE, "Bón phân NPK", 123456789L);
        CareLogEntity logEntity = CareLogEntity.fromDomain(zone.getId(), log, false);
        assertEquals("FERTILIZE", logEntity.getActionType());
        assertEquals(123456789L, logEntity.getTimestamp());

        CareLog restoredLog = logEntity.toDomain();
        assertEquals(ActionType.FERTILIZE, restoredLog.getActionType());
    }

    @Test
    public void testCoconutHarvestCycleAndMetrics() {
        FarmRepository repo = FarmRepository.getInstance();
        Zone coconutZone = repo.getZoneById("zone_coconut");
        assertNotNull(coconutZone);
        assertEquals(28, coconutZone.getHarvestCycleDays());
        assertEquals("Dừa xiêm xanh", coconutZone.getDefaultCrop());

        // Test stats
        int dueCount = coconutZone.getDueForHarvestCount();
        int recentCount = coconutZone.getHarvestedRecentlyCount(15);
        int neverCount = coconutZone.getNeverHarvestedCount();
        assertTrue("Due count should be > 0", dueCount > 0);
        assertTrue("Recent count should be > 0", recentCount > 0);
        assertTrue("Never count should be > 0", neverCount > 0);

        // Test bulk harvest with keepTree = true
        Cell testTree = coconutZone.getCell(0, 0);
        assertNotNull(testTree);
        repo.bulkHarvest(coconutZone.getId(), Collections.singletonList(testTree), "Thu hoạch buồng 14 trái", true);
        assertEquals(CellStatus.HEALTHY, testTree.getStatus());
        assertEquals("Dừa xiêm xanh", testTree.getPlantType());
        assertTrue("Last harvested should be set to current time", testTree.getLastHarvested() > 0);
        assertEquals(0, testTree.getDaysSinceLastHarvest());
    }

    @Test
    public void testCareLogEditAndDeleteRecalculation() {
        FarmRepository repo = FarmRepository.getInstance();
        Zone zone = repo.getZoneById("zone_coconut");
        assertNotNull(zone);

        Cell cell = zone.getCell(3, 0); // Has care logs
        assertNotNull(cell);
        int initialLogsCount = cell.getCareLogs().size();
        assertTrue("Cell should have care logs", initialLogsCount > 0);

        CareLog firstLog = cell.getCareLogs().get(0);
        long originalTimestamp = firstLog.getTimestamp();

        // 1. Edit log
        firstLog.setNote("Đã chỉnh sửa ghi chú: NPK 20-20-15 lượng 500g");
        firstLog.setTimestamp(originalTimestamp - 86400000L); // 1 day earlier
        repo.updateCareLog(zone.getId(), cell.getId(), firstLog);

        assertEquals("Đã chỉnh sửa ghi chú: NPK 20-20-15 lượng 500g", cell.getCareLogs().get(0).getNote());

        // 2. Add mistaken log then delete it
        CareLog mistakenLog = new CareLog(java.util.UUID.randomUUID().toString(), cell.getId(), ActionType.HARVEST, "Ghi nhầm thu hoạch", System.currentTimeMillis());
        repo.addCareLog(zone.getId(), cell.getId(), mistakenLog);
        assertEquals(initialLogsCount + 1, cell.getCareLogs().size());

        // Delete the mistaken log
        repo.deleteCareLog(zone.getId(), cell.getId(), mistakenLog.getId());
        assertEquals(initialLogsCount, cell.getCareLogs().size());
    }

    @Test
    public void testZoneEditAndDelete() {
        FarmRepository repo = FarmRepository.getInstance();
        // Create test zone
        Zone testZone = new Zone("zone_temp_test", "Khu Chuối", "Khu canh tác thử nghiệm", 4, 4, "Chuối Nam Mỹ");
        testZone.setHarvestCycleDays(60);
        repo.addZone(testZone);

        // Edit zone with new dimensions (e.g. 5 rows x 6 cols)
        repo.updateZoneInfo("zone_temp_test", "Khu Chuối Cấy Mô", "Đã cập nhật hệ thống tưới", "Chuối Tiêu Hồng", 75, 5, 6);
        Zone updated = repo.getZoneById("zone_temp_test");
        assertNotNull(updated);
        assertEquals("Khu Chuối Cấy Mô", updated.getName());
        assertEquals("Chuối Tiêu Hồng", updated.getDefaultCrop());
        assertEquals(75, updated.getHarvestCycleDays());
        assertEquals(5, updated.getRows());
        assertEquals(6, updated.getCols());
        assertEquals(30, updated.getCells().size());

        // Delete zone
        repo.deleteZone("zone_temp_test");
        assertNull(repo.getZoneById("zone_temp_test"));
    }

    @Test
    public void testNoDuplicateCareLogs() {
        FarmRepository repo = FarmRepository.getInstance();
        Zone zone = repo.getZoneById("zone_coconut");
        assertNotNull(zone);
        Cell cell = zone.getCell(1, 1);
        assertNotNull(cell);

        int countBefore = cell.getCareLogs().size();
        CareLog log = new CareLog("unique_log_test", cell.getId(), ActionType.WATER, "Tưới test chống trùng", System.currentTimeMillis());

        // Adding via repo
        repo.addCareLog(zone.getId(), cell.getId(), log);
        assertEquals(countBefore + 1, cell.getCareLogs().size());

        // Attempting to add duplicate log with same id
        repo.addCareLog(zone.getId(), cell.getId(), log);
        assertEquals(countBefore + 1, cell.getCareLogs().size()); // Must remain countBefore + 1!

        // Clean up
        repo.deleteCareLog(zone.getId(), cell.getId(), "unique_log_test");
        assertEquals(countBefore, cell.getCareLogs().size());
    }

    @Test
    public void testDisabledPathwayCanBeEditedToNormal() {
        FarmRepository repo = FarmRepository.getInstance();
        Zone zone = repo.getZoneById("zone_coconut");
        assertNotNull(zone);

        // Row 5 is pathway (CellStatus.DISABLED)
        Cell aisleCell = zone.getCell(5, 0);
        assertNotNull(aisleCell);
        assertEquals(CellStatus.DISABLED, aisleCell.getStatus());

        // 1. Bulk update to HEALTHY
        repo.bulkUpdateStatus(zone.getId(), Collections.singletonList(aisleCell), CellStatus.HEALTHY);
        assertEquals(CellStatus.HEALTHY, aisleCell.getStatus());
        assertEquals("Dừa xiêm xanh", aisleCell.getPlantType()); // Restored to zone default crop!

        // 2. Can set back to DISABLED
        repo.bulkUpdateStatus(zone.getId(), Collections.singletonList(aisleCell), CellStatus.DISABLED);
        assertEquals(CellStatus.DISABLED, aisleCell.getStatus());
        assertEquals("Lối đi", aisleCell.getPlantType());
    }

    @Test
    public void testFirebaseSyncAndTombstoneOperations() {
        FarmRepository repo = FarmRepository.getInstance();
        Zone testZone = new Zone("zone_sync_test", "Khu Đồng Bộ", "Mô tả kiểm thử", 4, 4, "Bưởi Da Xanh");
        repo.addZone(testZone);

        assertNotNull(repo.getZoneById("zone_sync_test"));
        assertEquals(16, testZone.getCells().size());

        // 1. Test update care log
        Cell c0 = testZone.getCell(0, 0);
        CareLog log1 = new CareLog("log_sync_1", c0.getId(), ActionType.WATER, "Tưới nước lần 1", System.currentTimeMillis());
        repo.addCareLog(testZone.getId(), c0.getId(), log1);
        assertEquals(1, c0.getCareLogs().size());

        // Update care log note
        CareLog updatedLog = new CareLog("log_sync_1", c0.getId(), ActionType.FERTILIZE, "Sửa thành Bón phân NPK", System.currentTimeMillis());
        repo.updateCareLog(testZone.getId(), c0.getId(), updatedLog);
        assertEquals(ActionType.FERTILIZE, c0.getCareLogs().get(0).getActionType());
        assertEquals("Sửa thành Bón phân NPK", c0.getCareLogs().get(0).getNote());

        // Delete care log
        repo.deleteCareLog(testZone.getId(), c0.getId(), "log_sync_1");
        assertTrue(c0.getCareLogs().isEmpty());

        // 2. Test zone resize and cell trimming
        repo.updateZoneInfo(testZone.getId(), "Khu Đồng Bộ Thu Nhỏ", "Đã thu nhỏ", "Bưởi Da Xanh", 45, 2, 2);
        Zone resized = repo.getZoneById("zone_sync_test");
        assertEquals(2, resized.getRows());
        assertEquals(2, resized.getCols());
        assertEquals(4, resized.getCells().size());

        // 3. Test zone deletion
        repo.deleteZone("zone_sync_test");
        assertNull(repo.getZoneById("zone_sync_test"));
    }
}
