package dbms.stats;

public class TestRunner {
    public static void main(String[] args) {
        System.out.println("=== STARTING TEST SUITE ===");
        
        // 1. VenueFixTest
        VenueFixTest venueFixTest = new VenueFixTest();
        venueFixTest.setUp();
        try {
            venueFixTest.testVLDBJournalDataRetrieval();
            System.out.println("SUCCESS: VenueFixTest PASSED!");
        } catch (Throwable e) {
            System.err.println("FAILURE: VenueFixTest FAILED!");
            e.printStackTrace();
        }

        // 2. AuthorsScatterTest
        AuthorsScatterTest authorsScatterTest = new AuthorsScatterTest();
        authorsScatterTest.setUp();
        try {
            authorsScatterTest.testGetScatterAuthorsVsPapers();
            authorsScatterTest.testVLDBJournalInScatter();
            System.out.println("SUCCESS: AuthorsScatterTest PASSED!");
        } catch (Throwable e) {
            System.err.println("FAILURE: AuthorsScatterTest FAILED!");
            e.printStackTrace();
        }

        System.out.println("=== TEST SUITE COMPLETE ===");
    }
}
