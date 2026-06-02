package dbms;

import dbms.models.*;
import dbms.services.DataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class YearAndSearchAnalyticsTest {
    private DataService dataService;

    @BeforeEach
    public void setUp() {
        dataService = new DataService();
    }

    @Test
    public void testYearProfileRetrieval() {
        // Let's test a year that has publications in DBLP, e.g. 2012 or 2022
        // We know from YearRangeTest that the data spans up to at least 2014. Let's try 2010.
        int testYear = 2010;
        YearProfile profile = dataService.getYearProfile(testYear);
        
        if (profile != null) {
            System.out.println("--- Year " + testYear + " Profile ---");
            System.out.println("Total Pubs: " + profile.totalPublications);
            System.out.println("Distinct Venues: " + profile.distinctVenues);
            System.out.println("Distinct Authors: " + profile.distinctAuthors);
            
            assertEquals(testYear, profile.year);
            assertTrue(profile.totalPublications >= 0);
            assertTrue(profile.distinctVenues >= 0);
            assertTrue(profile.distinctAuthors >= 0);
        } else {
            System.out.println("No data found for year " + testYear);
        }
    }

    @Test
    public void testPaperDetailsFilteringAndSorting() {
        // Let's query papers from 'VLDB Journal' sorting by title ascending
        PaginatedResult<PaperDetail> resultAsc = dataService.getPaperDetails(
            "venue", 
            "VLDB Journal", 
            10, 
            0, 
            2000, 
            2020, 
            "database", 
            "title", 
            "ASC"
        );
        
        assertNotNull(resultAsc);
        assertNotNull(resultAsc.data);
        System.out.println("Filtered papers (keyword: 'database', sorted title ASC) count: " + resultAsc.data.size());
        
        if (resultAsc.data.size() > 1) {
            PaperDetail first = resultAsc.data.get(0);
            PaperDetail second = resultAsc.data.get(1);
            assertTrue(first.title.compareToIgnoreCase(second.title) <= 0, "Title of first paper should be alphabetically <= second paper in ASC sort");
        }

        // Test sorting by year descending
        PaginatedResult<PaperDetail> resultDesc = dataService.getPaperDetails(
            "venue", 
            "VLDB Journal", 
            10, 
            0, 
            2000, 
            2020, 
            null, 
            "year", 
            "DESC"
        );
        
        assertNotNull(resultDesc);
        if (resultDesc.data.size() > 1) {
            PaperDetail first = resultDesc.data.get(0);
            PaperDetail second = resultDesc.data.get(1);
            assertTrue(first.year >= second.year, "Year of first paper should be >= second paper in DESC sort");
        }
    }

    @Test
    public void testAutocompleteSearchFunctions() {
        String query = "vldb";
        
        // 1. Test searchEntities
        List<String> entities = dataService.searchEntities(query);
        assertNotNull(entities, "searchEntities should not return null");
        System.out.println("searchEntities found: " + entities.size() + " matches");
        for (String entity : entities) {
            assertTrue(entity.toLowerCase().contains(query), "Entity name should contain search query");
        }

        // 2. Test searchVenuesFast
        List<ChartPoint> venuesFast = dataService.searchVenuesFast(query);
        assertNotNull(venuesFast, "searchVenuesFast should not return null");
        System.out.println("searchVenuesFast found: " + venuesFast.size() + " matches");
        for (ChartPoint cp : venuesFast) {
            assertNotNull(cp.label);
            assertTrue(cp.label.toString().toLowerCase().contains(query), "Venue name should contain query");
            assertNotNull(cp.secondaryValue, "Venue type (secondaryValue) should be CONFERENCE or JOURNAL");
        }

        // 3. Test searchEntitiesDetailed
        List<ChartPoint> entitiesDetailed = dataService.searchEntitiesDetailed(query);
        assertNotNull(entitiesDetailed, "searchEntitiesDetailed should not return null");
        System.out.println("searchEntitiesDetailed found: " + entitiesDetailed.size() + " matches");
        for (ChartPoint cp : entitiesDetailed) {
            assertNotNull(cp.label);
            assertNotNull(cp.value, "Publication count should not be null");
            assertNotNull(cp.secondaryValue, "Entity type should not be null");
            System.out.println("Detailed entity: " + cp.label + " | Type: " + cp.secondaryValue + " | Pubs: " + cp.value);
        }
    }
}
