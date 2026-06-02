package dbms;

import dbms.models.*;
import dbms.services.DataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class VenueAnalyticsTest {
    private DataService dataService;

    @BeforeEach
    public void setUp() {
        dataService = new DataService();
    }

    @Test
    public void testConferenceProfileVLDB() {
        // "VLDB" is a well-known conference, let's test if profile fetches correctly
        ConferenceProfile profile = dataService.getConferenceProfile("VLDB");
        
        if (profile != null) {
            System.out.println("--- VLDB Conference Profile ---");
            System.out.println("Name: " + profile.name);
            System.out.println("Rank: " + profile.rank);
            System.out.println("Total Publications: " + profile.totalPublications);
            System.out.println("Avg Authors Per Paper: " + profile.avgAuthorsPerPaper);
            System.out.println("Avg Papers Per Year: " + profile.avgPapersPerYear);
            
            assertNotNull(profile.name);
            assertTrue(profile.totalPublications >= 0);
            assertTrue(profile.avgAuthorsPerPaper >= 0.0);
            assertTrue(profile.avgPapersPerYear >= 0.0);
        } else {
            System.out.println("VLDB Conference profile not found in database.");
        }
    }

    @Test
    public void testJournalProfileVLDBJournal() {
        // "VLDB Journal" is a well-known journal, let's test if profile fetches correctly
        JournalProfile profile = dataService.getJournalProfile("VLDB Journal");
        
        assertNotNull(profile, "VLDB Journal profile should not be null");
        System.out.println("--- VLDB Journal Profile ---");
        System.out.println("Name: " + profile.name);
        System.out.println("Best Quartile: " + profile.rank);
        System.out.println("SJR: " + profile.sjr);
        System.out.println("Total Publications: " + profile.totalPublications);
        System.out.println("Total Authors: " + profile.totalAuthors);
        System.out.println("Avg Authors Per Paper: " + profile.avgAuthorsPerPaper);
        System.out.println("Avg Papers Per Year: " + profile.avgPapersPerYear);
        
        assertEquals("VLDB Journal", profile.name);
        assertTrue(profile.totalPublications > 0, "VLDB Journal should have > 0 publications");
        assertTrue(profile.totalAuthors > 0, "VLDB Journal should have > 0 authors");
        assertTrue(profile.avgAuthorsPerPaper > 0, "Avg authors per paper should be positive");
    }

    @Test
    public void testJournalProfileNature() {
        // Nature is a well-known journal in the DB
        JournalProfile profile = dataService.getJournalProfile("Nature");
        
        if (profile != null) {
            System.out.println("--- Nature Journal Profile ---");
            System.out.println("Rank/Quartile: " + profile.rank);
            System.out.println("SJR: " + profile.sjr);
            System.out.println("Total Publications: " + profile.totalPublications);
            assertTrue(profile.totalPublications >= 0);
        } else {
            System.out.println("Nature Journal profile not found in database.");
        }
    }

    @Test
    public void testVenuesComparisonData() {
        List<String> venuesToCompare = List.of("VLDB Journal", "Nature");
        int startYear = 2000;
        int endYear = 2024;
        
        // 1. Compare by publications metric
        List<ChartPoint> pubComparison = dataService.getVenuesComparisonData(venuesToCompare, "articles", startYear, endYear);
        assertNotNull(pubComparison, "Comparison data for publications should not be null");
        System.out.println("Venues Comparison (articles) points count: " + pubComparison.size());
        
        for (ChartPoint pt : pubComparison) {
            assertNotNull(pt.label, "Year label should not be null");
            assertNotNull(pt.value, "Publication count value should not be null");
            assertTrue(pt.value.doubleValue() >= 0);
            
            // Check secondary value (the venue name associated with this data point)
            assertNotNull(pt.secondaryValue, "Venue name in comparison point should not be null");
            System.out.println("Year: " + pt.label + ", Venue: " + pt.secondaryValue + ", Value: " + pt.value);
        }

        // 2. Compare by authors metric
        List<ChartPoint> authorComparison = dataService.getVenuesComparisonData(venuesToCompare, "authors", startYear, endYear);
        assertNotNull(authorComparison, "Comparison data for authors should not be null");
        System.out.println("Venues Comparison (authors) points count: " + authorComparison.size());
        
        for (ChartPoint pt : authorComparison) {
            assertNotNull(pt.label);
            assertNotNull(pt.value);
            assertTrue(pt.value.doubleValue() >= 0);
        }
    }

    @Test
    public void testNonExistentVenueReturnsNull() {
        ConferenceProfile conf = dataService.getConferenceProfile("Fake Conference 999");
        assertNull(conf, "Fake conference should return null profile");
        
        JournalProfile journal = dataService.getJournalProfile("Fake Journal 999");
        assertNull(journal, "Fake journal should return null profile");
    }
}
