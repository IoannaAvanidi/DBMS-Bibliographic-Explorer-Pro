package dbms;

import dbms.models.*;
import dbms.services.DataService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class DataServiceTest {
    private final DataService dataService = new DataService();

    @Test
    public void testGetAuthorProfile() {
        // Testing that the new CTE query for authors doesn't crash and returns something (even null for non-existent)
        try {
            AuthorProfile profile = dataService.getAuthorProfile("Philip S. Yu");
            assertNotNull(profile, "Profile should not be null for Philip S. Yu");
            assertNotNull(profile.mostFrequentVenue, "Most frequent venue should not be null");
            assertNotEquals("null", profile.mostFrequentVenue, "Most frequent venue should not be the string 'null'");
            assertNotEquals("N/A", profile.mostFrequentVenue, "Most frequent venue should not be N/A for Philip S. Yu");
            assertTrue(profile.venuePublications > 0, "Venue publications should be > 0");
            System.out.println("Author profile retrieval successful. Venue: " + profile.mostFrequentVenue + " (" + profile.venuePublications + " publications)");
            
            // Test for another user to verify it works generally
            AuthorProfile profile2 = dataService.getAuthorProfile("Jiawei Han");
            if (profile2 != null) {
                assertNotNull(profile2.mostFrequentVenue);
                assertNotEquals("null", profile2.mostFrequentVenue);
                if (profile2.totalPublications > 0) {
                    assertTrue(profile2.venuePublications > 0, "Venue publications should be > 0 if author has publications");
                }
            }
        } catch (Exception e) {
            fail("getAuthorProfile crashed: " + e.getMessage());
        }
    }

    @Test
    public void testGetConferenceProfile() {
        try {
            ConferenceProfile profile = dataService.getConferenceProfile("VLDB");
            System.out.println("Conference profile retrieval successful.");
        } catch (Exception e) {
            fail("getConferenceProfile crashed: " + e.getMessage());
        }
    }

    @Test
    public void testGetJournalProfile() {
        try {
            JournalProfile profile = dataService.getJournalProfile("Nature");
            // This is the specific method that was throwing the PROCEDURE not found error
            System.out.println("Journal profile retrieval successful.");
        } catch (Exception e) {
            fail("getJournalProfile crashed: " + e.getMessage());
        }
    }

    @Test
    public void testVLDBJournalFix() {
        try {
            // Test Profile Retrieval for 'VLDB Journal'
            JournalProfile profile = dataService.getJournalProfile("VLDB Journal");
            assertNotNull(profile, "Profile should not be null for VLDB Journal");
            assertTrue(profile.totalPublications > 0, "totalPublications should be > 0 (Fix verified for VLDB Journal)");
            assertTrue(profile.totalAuthors > 0, "totalAuthors should be > 0");
            assertTrue(profile.avgAuthorsPerPaper > 0, "avgAuthorsPerPaper should be > 0");
            assertTrue(profile.avgPapersPerYear > 0, "avgPapersPerYear should be > 0");
            System.out.println("VLDB Journal Profile Fixed! Pubs: " + profile.totalPublications);

            // Test Paper Details
            PaginatedResult<PaperDetail> papers = dataService.getPaperDetails("venue", "VLDB Journal", 10, 0, null, null, null, "year", "DESC");
            assertNotNull(papers);
            assertTrue(papers.totalCount > 0, "Paper count should be > 0 for VLDB Journal");

            // Test Venue Comparison Data
            List<String> venues = List.of("VLDB Journal");
            List<ChartPoint> comparisonData = dataService.getVenuesComparisonData(venues, "articles", 1990, 2026);
            assertNotNull(comparisonData);
            assertTrue(comparisonData.size() > 0, "Comparison data should not be empty for VLDB Journal");

            // Test unified search details
            List<ChartPoint> searchResult = dataService.searchEntitiesDetailed("VLDB Journal");
            assertNotNull(searchResult);
            boolean foundVldb = false;
            for (ChartPoint cp : searchResult) {
                if (cp.label != null && cp.label.toString().contains("VLDB Journal")) {
                    foundVldb = true;
                    assertTrue(cp.value != null && cp.value.intValue() > 0, "Unified search should return > 0 publications for VLDB Journal");
                }
            }
            assertTrue(foundVldb, "VLDB Journal should be found in detailed search");
            
        } catch (Exception e) {
            fail("VLDB Journal test crashed: " + e.getMessage());
        }
    }

    @Test
    public void testGetPaperDetailsPagination() {
        try {
            PaginatedResult<PaperDetail> result = dataService.getPaperDetails("year", "2022", 10, 0, null, null, null, "year", "DESC");
            assertNotNull(result);
            assertNotNull(result.data);
            System.out.println("Paper details pagination successful. Total count: " + result.totalCount);
        } catch (Exception e) {
            fail("getPaperDetails pagination crashed: " + e.getMessage());
        }
    }

    @Test
    public void testGetEnrichedPubs() {
        try {
            List<ChartPoint> points = dataService.getPublicationsPerYear("", 2000, 2024);
            assertNotNull(points);
        } catch (Exception e) {
            fail("getPublicationsPerYear crashed: " + e.getMessage());
        }
    }

    @Test
    public void testGetCategoryTrend() {
        try {
            List<ChartPoint> points = dataService.getCategoryTrend("Artificial Intelligence");
            assertNotNull(points);
        } catch (Exception e) {
            fail("getCategoryTrend crashed: " + e.getMessage());
        }
    }

    @Test
    public void testSearchVenuesFastPerformance() {
        try {
            // Warmup
            dataService.searchEntitiesDetailed("VLDB");
            dataService.searchVenuesFast("VLDB");

            // Measure searchEntitiesDetailed time
            long startDetailed = System.nanoTime();
            List<ChartPoint> detailedResult = dataService.searchEntitiesDetailed("VLDB");
            long durationDetailed = (System.nanoTime() - startDetailed) / 1_000_000;
            System.out.println("searchEntitiesDetailed took: " + durationDetailed + " ms. Found: " + detailedResult.size());

            // Measure searchVenuesFast time
            long startFast = System.nanoTime();
            List<ChartPoint> fastResult = dataService.searchVenuesFast("VLDB");
            long durationFast = (System.nanoTime() - startFast) / 1_000_000;
            System.out.println("searchVenuesFast took: " + durationFast + " ms. Found: " + fastResult.size());

            assertNotNull(fastResult);
            // Verify it is extremely fast (< 100 ms)
            assertTrue(durationFast < 100, "Fast search should take less than 100ms, actually took: " + durationFast + "ms");
            
            // Print performance improvement ratio
            double ratio = (double) durationDetailed / Math.max(1, durationFast);
            System.out.println("Fast Venue Autocomplete Performance Improvement Ratio: " + ratio + "x faster!");
        } catch (Exception e) {
            fail("testSearchVenuesFastPerformance crashed: " + e.getMessage());
        }
    }
}
