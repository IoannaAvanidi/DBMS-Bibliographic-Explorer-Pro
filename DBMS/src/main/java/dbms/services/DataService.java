package dbms.services;

import dbms.models.*;
import dbms.repository.DatabaseRepository;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataService {
    private final DatabaseRepository repository;
    private final Map<String, Object> cache = new ConcurrentHashMap<>();

    public DataService() {
        this.repository = new DatabaseRepository();
    }

    public DataService(DatabaseRepository repository) {
        this.repository = repository;
    }

    public AuthorProfile getAuthorProfile(String authorName) {
        if (authorName == null) return null;
        String key = "author_profile_" + authorName.trim();
        if (cache.containsKey(key)) return (AuthorProfile) cache.get(key);
        try {
            AuthorProfile result = repository.findAuthorByName(authorName);
            if (result != null) cache.put(key, result);
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ConferenceProfile getConferenceProfile(String confName) {
        if (confName == null) return null;
        String key = "conf_profile_" + confName.trim();
        if (cache.containsKey(key)) return (ConferenceProfile) cache.get(key);
        try {
            ConferenceProfile result = repository.findConferenceByName(confName);
            if (result != null) cache.put(key, result);
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public JournalProfile getJournalProfile(String journalName) {
        if (journalName == null) return null;
        String key = "journal_profile_" + journalName.trim();
        if (cache.containsKey(key)) return (JournalProfile) cache.get(key);
        try {
            JournalProfile result = repository.findJournalByName(journalName);
            if (result != null) cache.put(key, result);
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Object getUnifiedProfile(String name) {
        if (name == null) return null;
        String key = "unified_profile_" + name.trim();
        if (cache.containsKey(key)) return cache.get(key);
        try {
            // First, quickly identify the type using indexed lookups
            String type = repository.getEntityType(name);
            Object result = null;
            if (type == null) {
                // If no exact match, try the old fuzzy sequential approach 
                // but keep it as a fallback.
                AuthorProfile ap = getAuthorProfile(name);
                if (ap != null) result = ap;
                else {
                    ConferenceProfile cp = getConferenceProfile(name);
                    if (cp != null) result = cp;
                    else {
                        JournalProfile jp = getJournalProfile(name);
                        if (jp != null) result = jp;
                    }
                }
            } else {
                if ("AUTHOR".equalsIgnoreCase(type)) result = getAuthorProfile(name);
                else if ("CONFERENCE".equalsIgnoreCase(type)) result = getConferenceProfile(name);
                else if ("JOURNAL".equalsIgnoreCase(type)) result = getJournalProfile(name);
            }
            
            if (result != null) {
                cache.put(key, result);
            }
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<ChartPoint> getPublicationsPerYear(String filter, int from, int to) {
        String key = "pubs_" + filter + "_" + from + "_" + to;
        if (cache.containsKey(key)) return (List<ChartPoint>) cache.get(key);
        try {
            List<ChartPoint> result = repository.getPubsPerYear(filter, from, to);
            cache.put(key, result);
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<ChartPoint> getCategoryTrend(String categoryName) {
        String key = "cat_trend_" + categoryName;
        if (cache.containsKey(key)) return (List<ChartPoint>) cache.get(key);
        try {
            List<ChartPoint> result = repository.getCategoryTrend(categoryName);
            cache.put(key, result);
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<ChartPoint> getScatterAuthorsVsPapers(int from, int to) {
        String key = "scatter_avp_" + from + "_" + to;
        if (cache.containsKey(key)) return (List<ChartPoint>) cache.get(key);
        try {
            // Reduced limit from 1000 to 300 for better UI performance
            List<ChartPoint> result = repository.getScatterAuthorsVsPapers(from, to, 300);
            cache.put(key, result);
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<ChartPoint> getTopAuthors(int limit, int offset) {
        String key = "top_authors_" + limit + "_" + offset;
        if (cache.containsKey(key)) return (List<ChartPoint>) cache.get(key);
        try {
            List<ChartPoint> result = repository.getTopAuthors(limit, offset);
            cache.put(key, result);
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<ChartPoint> getVenueTypeDistribution() {
        if (cache.containsKey("venue_dist")) return (List<ChartPoint>) cache.get("venue_dist");
        try {
            List<ChartPoint> result = repository.getVenueTypeDistribution();
            cache.put("venue_dist", result);
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<ChartPoint> getScatterData(int fromYear, int toYear) {
        String key = "scatter_" + fromYear + "_" + toYear;
        if (cache.containsKey(key)) return (List<ChartPoint>) cache.get(key);
        try {
            // Reduced limit from 1000 to 300 for better UI performance
            List<ChartPoint> result = repository.getScatterData(fromYear, toYear, 300);
            cache.put(key, result);
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public YearProfile getYearProfile(int year) {
        try {
            return repository.findYearProfile(year);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public PaginatedResult<PaperDetail> getPaperDetails(String type, String value, int limit, int offset, Integer minYear, Integer maxYear, String keyword, String sortField, String sortOrder) {
        String key = "papers_" + type + "_" + value + "_" + limit + "_" + offset + "_" + minYear + "_" + maxYear + "_" + keyword + "_" + sortField + "_" + sortOrder;
        if (cache.containsKey(key)) return (PaginatedResult<PaperDetail>) cache.get(key);
        try {
            System.out.println("Service: Fetching papers for " + type + " [" + value + "]");
            PaginatedResult<PaperDetail> result = repository.getPapersByEntity(type, value, limit, offset, minYear, maxYear, keyword, sortField, sortOrder);
            cache.put(key, result);
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return new PaginatedResult<>(Collections.emptyList(), 0, limit, offset);
        }
    }

    public List<String> searchEntities(String query) {
        try {
            return repository.searchEntities(query);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<ChartPoint> searchVenuesFast(String query) {
        try {
            return repository.searchVenuesFast(query);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<ChartPoint> searchEntitiesDetailed(String query) {
        try {
            return repository.searchEntitiesDetailed(query);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<PublisherStat> getPublisherStats() {
        if (cache.containsKey("pub_stats")) return (List<PublisherStat>) cache.get("pub_stats");
        try {
            List<PublisherStat> result = repository.getPublisherStats();
            cache.put("pub_stats", result);
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public List<ChartPoint> getVenuesComparisonData(List<String> venueNames, String metric, int fromYear, int toYear) {
        try {
            return repository.getVenuesComparisonData(venueNames, metric, fromYear, toYear);
        } catch (SQLException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    public int[] getYearRange() {
        if (cache.containsKey("global_year_range")) return (int[]) cache.get("global_year_range");
        try {
            int[] range = repository.getYearRange();
            cache.put("global_year_range", range);
            return range;
        } catch (SQLException e) {
            e.printStackTrace();
            return new int[] { 1900, 2026 }; // Fallback on failure
        }
    }
}
