package dbms.stats;

import dbms.models.ChartPoint;
import dbms.services.DataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class YearRangeTest {

    private DataService dataService;

    @BeforeEach
    public void setup() {
        dataService = new DataService();
    }

    @Test
    public void testGetYearRange() {
        int[] range = dataService.getYearRange();
        
        assertNotNull(range, "Year range should not be null");
        assertTrue(range.length == 2, "Year range array should have exactly two elements (min and max)");
        
        int minYear = range[0];
        int maxYear = range[1];
        
        // Data integrity validation
        assertTrue(minYear > 1900, "Min year should be greater than 1900");
        assertTrue(maxYear <= java.time.Year.now().getValue() + 1, "Max year should not be far in the future");
        assertTrue(minYear <= maxYear, "Min year must be less than or equal to Max year");
        
        System.out.println("YearRangeTest: Validated year range [" + minYear + " - " + maxYear + "]");
    }
}
