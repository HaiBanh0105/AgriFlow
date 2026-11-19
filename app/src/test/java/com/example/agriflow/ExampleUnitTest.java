package com.example.agriflow;

import org.junit.Test;

import static org.junit.Assert.*;

import com.example.agriflow.data.model.ActionType;
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
        // Initially all 4 are healthy -> 100%
        assertEquals(100, zone.getHealthPercentage());

        // Make 1 diseased
        zone.getCell(0, 0).setStatus(CellStatus.DISEASED);
        // 3 healthy out of 4 active -> 75%
        assertEquals(75, zone.getHealthPercentage());

        // Make 1 empty
        zone.getCell(0, 1).setStatus(CellStatus.EMPTY);
        // 2 healthy out of 3 active (1 diseased + 2 healthy) -> 67%
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

        // Bulk action test
        repo.bulkPerformAction(zone.getId(), Collections.singletonList(cell), ActionType.WATER, "Test tưới");
        assertFalse(cell.getCareLogs().isEmpty());
        assertEquals(ActionType.WATER, cell.getCareLogs().get(0).getActionType());
    }
}
