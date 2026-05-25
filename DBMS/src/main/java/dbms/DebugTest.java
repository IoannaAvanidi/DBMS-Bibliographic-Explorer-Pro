package dbms;

import dbms.models.*;
import dbms.repository.DatabaseRepository;
import java.sql.SQLException;
import java.util.List;

public class DebugTest {
    public static void main(String[] args) {
        DatabaseRepository repo = new DatabaseRepository();
        
        System.out.println("=== SYSTEM OPTIMIZATION VERIFICATION ===");
        
        // 1. Author Search (Philip S. Yu)
        String authorName = "Philip S. Yu";
        System.out.println("\n--- Testing Author Search: [" + authorName + "] ---");
        try {
            AuthorProfile profile = repo.findAuthorByName(authorName);
            if (profile != null) {
                System.out.println("✓ Success: Profile found [" + profile.name + "]");
                System.out.println("✓ Publication Count: " + profile.totalPublications);
                
                // Check papers
                PaginatedResult<PaperDetail> papers = repo.getPapersByEntity("author", authorName, 10, 0, null, null, null, "year", "DESC");
                System.out.println("✓ List size returned: " + papers.data.size());
                if (papers.data.isEmpty() && profile.totalPublications > 0) {
                    System.err.println("✗ FAILURE: Count is " + profile.totalPublications + " but list is empty!");
                } else {
                    System.out.println("✓ First paper: " + papers.data.get(0).title);
                }
            } else {
                System.err.println("✗ FAILURE: Profile NOT found for " + authorName);
            }
        } catch (SQLException e) {
            System.err.println("✗ SQL ERROR: " + e.getMessage());
        }

        // 2. Optimized Trend Query
        System.out.println("\n--- Testing Optimized Trend Query (getPubsPerYear) ---");
        long start = System.currentTimeMillis();
        try {
            List<ChartPoint> trend = repo.getPubsPerYear(null, 1995, 2024);
            long end = System.currentTimeMillis();
            System.out.println("✓ Success: " + trend.size() + " years of data retrieved.");
            System.out.println("✓ Time taken: " + (end - start) + "ms (Target: < 1000ms, was 22000ms)");
            if (!trend.isEmpty()) {
                System.out.println("✓ Latest year in data: " + trend.get(trend.size()-1).label);
            }
        } catch (SQLException e) {
            System.err.println("✗ SQL ERROR: " + e.getMessage());
        }

        // 3. Scatter Data Limit
        System.out.println("\n--- Testing Scatter Plot Limits ---");
        try {
            List<ChartPoint> scatter = repo.getScatterData(2010, 2024, 300);
            System.out.println("✓ Scatter points returned: " + scatter.size() + " (Limit should be 300)");
            if (scatter.size() > 300) {
                System.err.println("✗ FAILURE: Returned too many points (" + scatter.size() + ")");
            }
        } catch (SQLException e) {
            System.err.println("✗ SQL ERROR: " + e.getMessage());
        }

        System.out.println("\n=== VERIFICATION COMPLETE ===");
    }
}
