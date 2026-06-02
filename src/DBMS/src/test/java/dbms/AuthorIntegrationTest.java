package dbms;

import dbms.models.*;
import dbms.services.DataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

public class AuthorIntegrationTest {
    private DataService dataService;

    @BeforeEach
    public void setUp() {
        dataService = new DataService();
    }

    @Test
    public void testPhilipSYuProfile() {
        String authorName = "Philip S. Yu";
        AuthorProfile profile = dataService.getAuthorProfile(authorName);
        
        assertNotNull(profile, "Profile for Philip S. Yu should not be null");
        assertEquals(authorName, profile.name, "Author name should match");
        assertTrue(profile.totalPublications > 0, "Philip S. Yu should have at least some publications in the DB");
        assertTrue(profile.firstYear > 0, "First publication year should be a positive number");
        assertTrue(profile.lastYear >= profile.firstYear, "Last publication year should be >= first publication year");
        assertNotNull(profile.mostFrequentVenue, "Most frequent venue should not be null");
        assertNotEquals("null", profile.mostFrequentVenue, "Most frequent venue should not be the string 'null'");
        assertNotEquals("N/A", profile.mostFrequentVenue, "Most frequent venue should not be 'N/A' for a prominent author");
        assertTrue(profile.venuePublications > 0, "Publications in the most frequent venue should be > 0");

        System.out.println("--- Philip S. Yu Profile ---");
        System.out.println("Total Pubs: " + profile.totalPublications);
        System.out.println("Years: " + profile.firstYear + " - " + profile.lastYear);
        System.out.println("Top Venue: " + profile.mostFrequentVenue + " (" + profile.venuePublications + " papers)");
    }

    @Test
    public void testJiaweiHanProfile() {
        String authorName = "Jiawei Han";
        AuthorProfile profile = dataService.getAuthorProfile(authorName);
        
        // Han might or might not be in the database, but let's test if we fetch it without crashes
        if (profile != null) {
            assertEquals(authorName, profile.name);
            assertTrue(profile.totalPublications >= 0);
            System.out.println("--- Jiawei Han Profile ---");
            System.out.println("Total Pubs: " + profile.totalPublications);
        } else {
            System.out.println("Jiawei Han not found in the database. This is acceptable.");
        }
    }

    @Test
    public void testAuthorPaperDetailsPagination() {
        String authorName = "Philip S. Yu";
        int limit = 15;
        int offset = 0;
        
        PaginatedResult<PaperDetail> result = dataService.getPaperDetails(
            "author", 
            authorName, 
            limit, 
            offset, 
            null, 
            null, 
            null, 
            "year", 
            "DESC"
        );

        assertNotNull(result, "PaginatedResult should not be null");
        assertNotNull(result.data, "Data list should not be null");
        assertTrue(result.totalCount > 0, "Total count of papers for Philip S. Yu should be > 0");
        assertTrue(result.data.size() <= limit, "Result size should be less than or equal to limit");
        
        if (!result.data.isEmpty()) {
            PaperDetail paper = result.data.get(0);
            assertNotNull(paper.title, "Paper title should not be null");
            assertTrue(paper.year > 0, "Paper year should be a positive number");
            assertNotNull(paper.venueName, "Paper venue should not be null");
            System.out.println("Sample Paper: " + paper.title + " (" + paper.year + ", " + paper.venueName + ")");
        }
    }

    @Test
    public void testNonExistentAuthorReturnsNullOrEmpty() {
        String fakeAuthor = "NonExistent Author XYZ123";
        AuthorProfile profile = dataService.getAuthorProfile(fakeAuthor);
        
        // It should return null since the author doesn't exist
        assertNull(profile, "Profile for non-existent author should be null");

        PaginatedResult<PaperDetail> papers = dataService.getPaperDetails(
            "author", 
            fakeAuthor, 
            10, 
            0, 
            null, 
            null, 
            null, 
            "year", 
            "DESC"
        );
        assertNotNull(papers);
        assertEquals(0, papers.totalCount, "Papers count for fake author should be 0");
        assertTrue(papers.data.isEmpty(), "Papers list for fake author should be empty");
    }
}
