package dbms.stats;

import dbms.models.PublisherStat;
import dbms.services.DataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class PublisherStatsTest {
    private DataService dataService;

    @BeforeEach
    public void setUp() {
        dataService = new DataService();
    }

    @Test
    public void testGetPublisherStats() {
        try {
            List<PublisherStat> stats = dataService.getPublisherStats();
            assertNotNull(stats, "Publisher stats list should not be null");
            if (!stats.isEmpty()) {
                System.out.println("Top Publisher: " + stats.get(0).publisherName);
                assertTrue(stats.get(0).totalJournals > 0, "Top publisher should have journals");
                // Verify quartiles add up correctly or at least one is >= 0
                assertTrue(stats.get(0).q1Count >= 0);
                assertTrue(stats.get(0).q2Count >= 0);
                assertTrue(stats.get(0).q3Count >= 0);
                assertTrue(stats.get(0).q4Count >= 0);
            }
        } catch (Exception e) {
            fail("PublisherStats test failed: " + e.getMessage());
        }
    }
}
