package dbms;

import dbms.models.*;
import dbms.services.DataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class SystemIntegrityStressTest {
    private DataService dataService;

    @BeforeEach
    public void setUp() {
        dataService = new DataService();
    }

    @Test
    public void testSqlInjectionSafety() {
        // 1. Author search with single quotes (e.g. O'Hara)
        String complexName = "O'Hara";
        try {
            // Should not throw SQL syntax errors
            dataService.getAuthorProfile(complexName);
            System.out.println("SQL Injection Test 1 (O'Hara) - Success, no crashes!");
        } catch (Exception e) {
            fail("Query with single quote crashed: " + e.getMessage());
        }

        // 2. Malicious author string with SQL command injection
        String maliciousName = "Philip S. Yu'; DROP TABLE Author; --";
        try {
            AuthorProfile profile = dataService.getAuthorProfile(maliciousName);
            // It should safely return null as it won't find this exact author name and the SQL will compile safely via PreparedStatement/CallableStatement
            assertNull(profile, "Malicious name should return null and not execute SQL injection");
            System.out.println("SQL Injection Test 2 (Embedded DROP TABLE) - Success, safely handled!");
        } catch (Exception e) {
            fail("Malicious name query crashed (potential injection vulnerability): " + e.getMessage());
        }
    }

    @Test
    public void testEdgeCaseInputs() {
        // 1. Extreme year range (from 0 to 9999)
        try {
            List<ChartPoint> result = dataService.getScatterAuthorsVsPapers(0, 9999);
            assertNotNull(result, "Data list should not be null for extreme year range");
            System.out.println("Extreme year range (0-9999) returned: " + result.size() + " records");
        } catch (Exception e) {
            fail("Extreme year range query crashed: " + e.getMessage());
        }

        // 2. Null and empty inputs
        try {
            assertNull(dataService.getAuthorProfile(null));
            assertNull(dataService.getConferenceProfile(null));
            assertNull(dataService.getJournalProfile(null));

            // Empty strings and spaces invoke fuzzy fallback queries in DatabaseRepository.
            // We verify they run safely without throwing database syntax exceptions.
            dataService.getAuthorProfile("");
            dataService.getAuthorProfile("   ");
            dataService.getConferenceProfile("");
            dataService.getJournalProfile("");

            PaginatedResult<PaperDetail> paperResult = dataService.getPaperDetails(
                "author", null, 10, 0, null, null, null, null, null
            );
            assertNotNull(paperResult);
            assertTrue(paperResult.data.isEmpty());

            System.out.println("Edge cases (nulls/empty strings) - Success, safely handled!");
        } catch (Exception e) {
            fail("Null/empty string queries crashed the system: " + e.getMessage());
        }
    }

    @Test
    public void testConcurrentQueryExecutionStress() throws InterruptedException, ExecutionException {
        // Set up executor to run concurrent queries on DataService to ensure thread-safety
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Callable<Boolean>> tasks = new ArrayList<>();

        for (int i = 0; i < 30; i++) {
            final int index = i;
            tasks.add(() -> {
                // Each task creates its own DataService and thus its own DatabaseRepository 
                // and Connection to prevent thread sharing of the same connection.
                DataService localService = new DataService();
                try {
                    // Alternating queries
                    if (index % 3 == 0) {
                        localService.getAuthorProfile("Philip S. Yu");
                    } else if (index % 3 == 1) {
                        localService.getJournalProfile("VLDB Journal");
                    } else {
                        localService.getPublicationsPerYear("", 2010, 2012);
                    }
                    return true;
                } catch (Exception e) {
                    System.err.println("Thread query failed: " + e.getMessage());
                    return false;
                }
            });
        }

        System.out.println("Starting stress test: invoking " + tasks.size() + " concurrent database tasks using " + threadCount + " threads...");
        List<Future<Boolean>> futures = executor.invokeAll(tasks);
        executor.shutdown();
        boolean finished = executor.awaitTermination(30, TimeUnit.SECONDS);
        
        assertTrue(finished, "Stress test did not finish in 30 seconds");

        for (Future<Boolean> future : futures) {
            assertTrue(future.get(), "A concurrent query task failed!");
        }
        System.out.println("Stress test complete. All concurrent queries completed successfully without thread exceptions or connection issues.");
    }
}
