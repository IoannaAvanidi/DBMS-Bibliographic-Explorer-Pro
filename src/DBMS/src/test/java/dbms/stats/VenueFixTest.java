package dbms.stats;

import dbms.models.*;
import dbms.services.DataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class VenueFixTest {
    private DataService dataService;

    @BeforeEach
    public void setUp() {
        dataService = new DataService();
    }

    @Test
    public void testVLDBJournalDataRetrieval() {
        System.out.println("Running testVLDBJournalDataRetrieval...");
        try {
            // 1. Test Profile Retrieval
            JournalProfile profile = dataService.getJournalProfile("VLDB Journal");
            assertNotNull(profile, "Profile should not be null for VLDB Journal");
            System.out.println("VLDB Journal Total Publications: " + profile.totalPublications);
            assertTrue(profile.totalPublications > 0, "VLDB Journal should have > 0 publications (Acronym-Aware Fix)");

            // 2. Test Paper Details Retrieval
            PaginatedResult<PaperDetail> papers = dataService.getPaperDetails("venue", "VLDB Journal", 10, 0, null, null, null, "year", "DESC");
            assertNotNull(papers, "Papers result should not be null");
            System.out.println("VLDB Journal Total Papers Count: " + papers.totalCount);
            assertTrue(papers.totalCount > 0, "Papers count should be > 0 for VLDB Journal");
            assertFalse(papers.data.isEmpty(), "Papers data list should not be empty");

            // 3. Test Venue Comparison Data
            List<ChartPoint> comparison = dataService.getVenuesComparisonData(List.of("VLDB Journal"), "articles", 1990, 2026);
            assertNotNull(comparison, "Comparison data should not be null");
            System.out.println("Comparison Data points found: " + comparison.size());
            assertTrue(comparison.size() > 0, "Comparison data should have entries for VLDB Journal");
            
            // Check that at least one point has a value > 0
            boolean foundPositive = false;
            for (ChartPoint cp : comparison) {
                if (cp.value.doubleValue() > 0) {
                    foundPositive = true;
                    break;
                }
            }
            assertTrue(foundPositive, "At least one year should have publications for VLDB Journal in comparison chart");

        } catch (Exception e) {
            fail("VenueFixTest failed with exception: " + e.getMessage());
        }
    }
}
