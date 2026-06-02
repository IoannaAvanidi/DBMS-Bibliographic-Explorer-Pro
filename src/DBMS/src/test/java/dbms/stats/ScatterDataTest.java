package dbms.stats;

import dbms.models.ChartPoint;
import dbms.services.DataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class ScatterDataTest {
    private DataService dataService;

    @BeforeEach
    public void setUp() {
        dataService = new DataService();
    }

    @Test
    public void testGetScatterData() {
        try {
            // Test for a typical year range
            List<ChartPoint> scatterData = dataService.getScatterData(2015, 2024);
            assertNotNull(scatterData, "Scatter data list should not be null");
            
            if (!scatterData.isEmpty()) {
                System.out.println("Found " + scatterData.size() + " journals for scatter plot.");
                ChartPoint firstPoint = scatterData.get(0);
                assertNotNull(firstPoint.label, "Journal name should not be null");
                // x is CiteScore (value), y is SJR (secondaryValue) typically, or vice versa depending on mapping
                assertTrue(firstPoint.value.doubleValue() >= 0 || ((Number) firstPoint.secondaryValue).doubleValue() >= 0, "Metrics should be valid numbers");
            }
        } catch (Exception e) {
            fail("ScatterData test failed: " + e.getMessage());
        }
    }
}
