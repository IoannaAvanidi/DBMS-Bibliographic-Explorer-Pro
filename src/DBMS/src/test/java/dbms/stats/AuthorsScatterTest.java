package dbms.stats;

import dbms.models.ChartPoint;
import dbms.services.DataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class AuthorsScatterTest {
    private DataService dataService;

    @BeforeEach
    public void setUp() {
        dataService = new DataService();
    }

    @Test
    public void testGetScatterAuthorsVsPapers() {
        try { 
            List<ChartPoint> data = dataService.getScatterAuthorsVsPapers(2010, 2024);
            assertNotNull(data, "Authors vs Papers scatter data should not be null");
            if (!data.isEmpty()) {
                ChartPoint topVenue = data.get(0);
                System.out.println("Top venue by papers in 2010-2024: " + topVenue.label);
                assertTrue(topVenue.value.doubleValue() > 0, "Top venue should have > 0 papers");
                assertTrue(((Number) topVenue.secondaryValue).doubleValue() >= 0, "Top venue should have >= 0 avg authors");
            }
        } catch (Exception e) {
            fail("AuthorsScatter test failed: " + e.getMessage());
        }
    }

    @Test
    public void testVLDBJournalInScatter() {
        try {
            List<ChartPoint> data = dataService.getScatterAuthorsVsPapers(1990, 2026);
            boolean foundVLDB = false;
            for (ChartPoint cp : data) {
                String label = String.valueOf(cp.label);
                if (label.contains("VLDB") || label.contains("Very Large Databases")) {
                    foundVLDB = true;
                    assertTrue(cp.value.doubleValue() > 0, "VLDB related venue should have papers in scatter plot");
                    System.out.println("Scatter found VLDB: " + label + " with " + cp.value + " papers");
                }
            }
            assertTrue(foundVLDB, "VLDB should be present in scatter data given the high publication count");
        } catch (Exception e) {
            fail("testVLDBJournalInScatter failed: " + e.getMessage());
        }
    }
}
