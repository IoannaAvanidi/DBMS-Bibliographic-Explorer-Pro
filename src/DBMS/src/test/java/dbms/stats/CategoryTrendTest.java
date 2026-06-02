package dbms.stats;

import dbms.models.ChartPoint;
import dbms.services.DataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class CategoryTrendTest {
    private DataService dataService;

    @BeforeEach
    public void setUp() {
        dataService = new DataService();
    }

    @Test
    public void testGetCategoryTrend_ArtificialIntelligence() {
        try {
            List<ChartPoint> trend = dataService.getCategoryTrend("Artificial Intelligence");
            assertNotNull(trend, "Trend data should not be null");
            if (!trend.isEmpty()) {
                System.out.println("Category 'Artificial Intelligence' has data spanning " + trend.size() + " years.");
                assertTrue(trend.get(0).value.doubleValue() > 0, "First year value should be > 0");
            } else {
                System.out.println("No data found for Category 'Artificial Intelligence'.");
            }
        } catch (Exception e) {
            fail("CategoryTrend test failed: " + e.getMessage());
        }
    }
    
    @Test
    public void testGetCategoryTrend_NonExistent() {
        try {
            List<ChartPoint> trend = dataService.getCategoryTrend("SomeNonExistentCategoryxyz123");
            assertNotNull(trend, "Trend data should not be null even if category doesn't exist");
            assertTrue(trend.isEmpty(), "Trend data should be empty for non-existent category");
        } catch (Exception e) {
            fail("CategoryTrend test failed: " + e.getMessage());
        }
    }
}
