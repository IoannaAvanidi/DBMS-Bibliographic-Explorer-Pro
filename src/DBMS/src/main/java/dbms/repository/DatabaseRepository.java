package dbms.repository;

import dbms.models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseRepository {
    private final String url = "jdbc:mysql://localhost:3306/dbms_project?allowPublicKeyRetrieval=true&useSSL=false";
    private final String user = "mye030";
    private final String password = "mye030";
    private Connection connection;

    protected synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            System.out.println("Repo: Opening new permanent connection to DB...");
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("Repo: Connection established.");
        }
        return connection;
    }

    public AuthorProfile findAuthorByName(String name) throws SQLException {
        System.out.println("Repo: [START] findAuthorByName for [" + name + "]");
        AuthorProfile p = callGetAuthorProfile(name);

        if (p == null) {
            System.out.println("Repo: No exact author match. Trying fuzzy...");
            String bestMatch = findBestMatch("Author", name, null);
            if (bestMatch != null) {
                System.out.println("Repo: Fuzzy match found: [" + bestMatch + "]");
                p = callGetAuthorProfile(bestMatch);
            }
        }
        return p;
    }

    private AuthorProfile callGetAuthorProfile(String name) throws SQLException {
        // Optimized: Using CALL GetAuthorProfile(?) as per Phase 4 requirements for
        // pushdown logic.
        // If the procedure is missing, this falls back to a SARGable query.
        String sql = "{CALL GetAuthorProfile(?)}";
        try (CallableStatement stmt = getConnection().prepareCall(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    AuthorProfile p = new AuthorProfile();
                    p.name = rs.getString("author_name");
                    p.totalPublications = rs.getInt("total_publications");
                    p.firstYear = rs.getObject("first_publication_year") != null ? rs.getInt("first_publication_year")
                            : 0;
                    p.lastYear = rs.getObject("last_publication_year") != null ? rs.getInt("last_publication_year") : 0;
                    p.mostFrequentVenue = rs.getString("most_frequent_venue_name") != null
                            ? rs.getString("most_frequent_venue_name")
                            : "N/A";
                    p.venueType = rs.getString("most_frequent_venue_type") != null
                            ? rs.getString("most_frequent_venue_type")
                            : "N/A";
                    p.venuePublications = rs.getInt("most_frequent_venue_publications");

                    if ("N/A".equals(p.mostFrequentVenue) || "null".equals(p.mostFrequentVenue)
                            || p.mostFrequentVenue == null) {
                        enrichAuthorWithMostFrequentVenue(p);
                    }

                    return p;
                }
            }
        } catch (SQLException e) {
            // Fallback to SARGable inline query if SP doesn't exist (e.g. during initial
            // setup)
            System.err.println("Repo: SP GetAuthorProfile failed, falling back to inline SQL: " + e.getMessage());
            String fallbackSql = "SELECT a.name as author_name, " +
                    "(SELECT COUNT(*) FROM Publication_Author pa WHERE pa.author_id = a.id) as total_publications, " +
                    "(SELECT MIN(p.year) FROM Publication p JOIN Publication_Author pa ON p.id = pa.publication_id WHERE pa.author_id = a.id) as first_year, "
                    +
                    "(SELECT MAX(p.year) FROM Publication p JOIN Publication_Author pa ON p.id = pa.publication_id WHERE pa.author_id = a.id) as last_year "
                    +
                    "FROM Author a WHERE a.name = ?";
            try (PreparedStatement stmt = getConnection().prepareStatement(fallbackSql)) {
                stmt.setString(1, name);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        AuthorProfile p = new AuthorProfile();
                        p.name = rs.getString("author_name");
                        p.totalPublications = rs.getInt("total_publications");
                        p.firstYear = rs.getInt("first_year");
                        p.lastYear = rs.getInt("last_year");
                        enrichAuthorWithMostFrequentVenue(p);
                        return p;
                    }
                }
            }
        }
        return null;
    }

    private void enrichAuthorWithMostFrequentVenue(AuthorProfile p) {
        String sql = "SELECT v.name, v.type, COUNT(*) as cnt " +
                "FROM Publication_Author pa " +
                "JOIN Publication pub ON pa.publication_id = pub.id " +
                "JOIN Venue v ON pub.venue_id = v.id " +
                "JOIN Author a ON pa.author_id = a.id " +
                "WHERE a.name = ? " +
                "GROUP BY v.id, v.name, v.type " +
                "ORDER BY cnt DESC LIMIT 1";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, p.name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    p.mostFrequentVenue = rs.getString("name");
                    p.venueType = rs.getString("type");
                    p.venuePublications = rs.getInt("cnt");
                } else {
                    p.mostFrequentVenue = "N/A";
                    p.venueType = "N/A";
                    p.venuePublications = 0;
                }
            }
        } catch (SQLException ex) {
            System.err.println("Repo: Failed to enrich author venue: " + ex.getMessage());
            p.mostFrequentVenue = "N/A";
            p.venueType = "N/A";
            p.venuePublications = 0;
        }
    }

    public ConferenceProfile findConferenceByName(String name) throws SQLException {
        System.out.println("Repo: [START] findConferenceByName for [" + name + "]");
        ConferenceProfile p = callGetConferenceProfile(name);

        if (p == null) {
            System.out.println("Repo: No exact venue match. Trying fuzzy...");
            String bestMatch = findBestMatch("Venue", name, "CONFERENCE");
            if (bestMatch != null) {
                System.out.println("Repo: Fuzzy match found: [" + bestMatch + "]");
                p = callGetConferenceProfile(bestMatch);
            }
        }
        return p;
    }

    private ConferenceProfile callGetConferenceProfile(String name) throws SQLException {
        String sql = "{CALL GetConferenceProfile(?)}";
        try (CallableStatement stmt = getConnection().prepareCall(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ConferenceProfile p = new ConferenceProfile();
                    p.name = rs.getString("conference_name");
                    p.rank = rs.getString("conference_rank");
                    p.primaryFor = rs.getString("primary_for_name");
                    p.totalPublications = rs.getInt("total_publications");
                    p.firstYear = rs.getObject("first_publication_year") != null ? rs.getInt("first_publication_year")
                            : 0;
                    p.lastYear = rs.getObject("last_publication_year") != null ? rs.getInt("last_publication_year") : 0;
                    p.mostProlificAuthor = rs.getString("most_prolific_author_name");
                    p.authorPublications = rs.getInt("most_prolific_author_publications");
                    p.avgAuthorsPerPaper = rs.getDouble("avg_authors_per_paper");
                    p.avgPapersPerYear = rs.getDouble("avg_papers_per_year");
                    return p;
                }
            }
        } catch (SQLException e) {
            System.err.println("Repo: SP GetConferenceProfile failed, falling back to inline SQL: " + e.getMessage());
            List<Integer> ids = getAssociatedVenueIds(name);
            if (ids.isEmpty()) {
                return null;
            }
            StringBuilder inClause = new StringBuilder();
            for (int i = 0; i < ids.size(); i++) {
                if (i > 0) inClause.append(",");
                inClause.append("?");
            }
            String fallbackSql = "SELECT v.name, c.rank AS conf_rank, " +
                    "COUNT(DISTINCT p.id) as total_p, " +
                    "MIN(p.year) as f_year, " +
                    "MAX(p.year) as l_year, " +
                    "COUNT(DISTINCT pa.author_id) as total_a, " +
                    "CAST(COUNT(pa.author_id) AS DOUBLE) / NULLIF(COUNT(DISTINCT p.id), 0) as avg_a_per_p " +
                    "FROM Venue v JOIN Conference c ON v.id = c.venue_id " +
                    "LEFT JOIN Publication p ON p.venue_id IN (" + inClause.toString() + ") " +
                    "LEFT JOIN Publication_Author pa ON p.id = pa.publication_id " +
                    "WHERE v.name = ? AND v.type = 'CONFERENCE' " +
                    "GROUP BY v.id, v.name, c.rank";
            try (PreparedStatement stmt = getConnection().prepareStatement(fallbackSql)) {
                int pIdx = 1;
                for (Integer id : ids) {
                    stmt.setInt(pIdx++, id);
                }
                stmt.setString(pIdx++, name);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        ConferenceProfile p = new ConferenceProfile();
                        p.name = rs.getString("name");
                        p.rank = rs.getString("conf_rank");
                        p.totalPublications = rs.getInt("total_p");
                        p.firstYear = rs.getInt("f_year");
                        p.lastYear = rs.getInt("l_year");
                        p.avgAuthorsPerPaper = rs.getDouble("avg_a_per_p");
                        if (p.totalPublications > 0 && p.lastYear >= p.firstYear) {
                            p.avgPapersPerYear = (double) p.totalPublications / (p.lastYear - p.firstYear + 1);
                        } else {
                            p.avgPapersPerYear = 0;
                        }
                        return p;
                    }
                }
            }
        }
        return null;
    }

    public JournalProfile findJournalByName(String name) throws SQLException {
        System.out.println("Repo: [START] findJournalByName for [" + name + "]");
        JournalProfile p = callGetJournalProfile(name);

        if (p == null) {
            System.out.println("Repo: No exact journal match. Trying fuzzy...");
            String bestMatch = findBestMatch("Venue", name, "JOURNAL");
            if (bestMatch != null) {
                System.out.println("Repo: Fuzzy match found: [" + bestMatch + "]");
                p = callGetJournalProfile(bestMatch);
            }
        }
        return p;
    }

    private JournalProfile callGetJournalProfile(String name) throws SQLException {
        List<Integer> ids = getAssociatedVenueIds(name);
        if (ids.isEmpty()) {
            return null;
        }

        StringBuilder inClause = new StringBuilder();
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) inClause.append(",");
            inClause.append("?");
        }

        String sql = "SELECT v.name, j.best_quartile, j.sjr, " +
                "COUNT(DISTINCT p.id) as total_p, " +
                "MIN(p.year) as f_year, " +
                "MAX(p.year) as l_year, " +
                "COUNT(DISTINCT pa.author_id) as total_a, " +
                "CAST(COUNT(pa.author_id) AS DOUBLE) / NULLIF(COUNT(DISTINCT p.id), 0) as avg_a_per_p " +
                "FROM Venue v JOIN Journal j ON v.id = j.venue_id " +
                "LEFT JOIN Publication p ON p.venue_id IN (" + inClause.toString() + ") " +
                "LEFT JOIN Publication_Author pa ON p.id = pa.publication_id " +
                "WHERE v.name = ? AND v.type = 'JOURNAL' " +
                "GROUP BY v.id, v.name, j.best_quartile, j.sjr";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            int pIdx = 1;
            for (Integer id : ids) {
                stmt.setInt(pIdx++, id);
            }
            stmt.setString(pIdx++, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    JournalProfile p = new JournalProfile();
                    p.name = rs.getString("name");
                    p.rank = rs.getString("best_quartile");
                    p.sjr = rs.getDouble("sjr");
                    p.totalPublications = rs.getInt("total_p");
                    p.firstYear = rs.getInt("f_year");
                    p.lastYear = rs.getInt("l_year");
                    p.totalAuthors = rs.getInt("total_a");
                    p.avgAuthorsPerPaper = rs.getDouble("avg_a_per_p");
                    if (p.totalPublications > 0 && p.lastYear >= p.firstYear) {
                        p.avgPapersPerYear = (double) p.totalPublications / (p.lastYear - p.firstYear + 1);
                    } else {
                        p.avgPapersPerYear = 0;
                    }
                    return p;
                }
            }
        }
        return null;
    }

    private String findBestMatch(String table, String query, String typeFilter) throws SQLException {
        // Optimized: Try prefix match first for index usage
        String prefixSql = "SELECT name FROM " + table + " WHERE name LIKE ? ";
        if (typeFilter != null)
            prefixSql += "AND type = '" + typeFilter + "' ";
        prefixSql += "LIMIT 1";

        try (PreparedStatement stmt = getConnection().prepareStatement(prefixSql)) {
            stmt.setString(1, query + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getString("name");
            }
        }

        // Fallback to fuzzy if prefix fails
        String fuzzySql = "SELECT name FROM " + table + " WHERE name LIKE ? ";
        if (typeFilter != null)
            fuzzySql += "AND type = '" + typeFilter + "' ";
        fuzzySql += "LIMIT 1";
        try (PreparedStatement stmt = getConnection().prepareStatement(fuzzySql)) {
            stmt.setString(1, "%" + query + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getString("name");
            }
        }
        return null;
    }

    public List<ChartPoint> getPubsPerYear(String filter, int from, int to) throws SQLException {
        boolean hasFilter = (filter != null && !filter.isEmpty());

        if (hasFilter) {
            // If filtering by title, we still need some join, but keep it lean.
            String sql = "SELECT p.year, COUNT(*) as paper_count " +
                    "FROM Publication p " +
                    "WHERE p.year BETWEEN ? AND ? AND p.title LIKE ? " +
                    "GROUP BY p.year ORDER BY p.year";
            List<ChartPoint> list = new ArrayList<>();
            try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                stmt.setInt(1, from);
                stmt.setInt(2, to);
                stmt.setString(3, "%" + filter + "%");
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next())
                        list.add(new ChartPoint(rs.getInt("year"), rs.getInt("paper_count")));
                }
            }
            return list;
        } else {
            // HIGH PERFORMANCE OPTIMIZATION WITHOUT NEW TABLES:
            // We pre-aggregate within each table first, then join the small results.
            // This avoids the O(N*M) explosion and is massively faster.
            String sql = "SELECT t1.year, t1.cnt as paper_count, t2.total_a, t2.distinct_a " +
                    "FROM ( " +
                    "  SELECT year, COUNT(*) as cnt FROM Publication " +
                    "  WHERE year BETWEEN ? AND ? GROUP BY year " +
                    ") t1 " +
                    "LEFT JOIN ( " +
                    "  SELECT p.year, COUNT(*) as total_a, COUNT(DISTINCT pa.author_id) as distinct_a " +
                    "  FROM Publication_Author pa " +
                    "  JOIN Publication p ON pa.publication_id = p.id " +
                    "  WHERE p.year BETWEEN ? AND ? GROUP BY p.year " +
                    ") t2 ON t1.year = t2.year " +
                    "ORDER BY t1.year";

            List<ChartPoint> list = new ArrayList<>();
            try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
                stmt.setInt(1, from);
                stmt.setInt(2, to);
                stmt.setInt(3, from);
                stmt.setInt(4, to);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(new ChartPoint(
                                rs.getInt("year"),
                                rs.getInt("paper_count"),
                                rs.getInt("total_a"),
                                rs.getInt("distinct_a")));
                    }
                }
            }
            return list;
        }
    }

    public List<ChartPoint> getCategoryTrend(String categoryName) throws SQLException {
        // High performance optimization: Using a JOIN on a derived table instead of IN
        // () which causes dependent subqueries
        String sql = "SELECT p.year, COUNT(p.id) as count " +
                "FROM Publication p " +
                "JOIN ( " +
                "    SELECT v.id FROM Venue v " +
                "    JOIN Conference c ON v.id = c.venue_id " +
                "    JOIN Category cat ON c.primary_for = cat.id " +
                "    WHERE cat.name LIKE ? " +
                "    UNION " +
                "    SELECT v.id FROM Venue v " +
                "    JOIN Journal j ON v.id = j.venue_id " +
                "    JOIN Staging_Journal sj ON v.name = sj.title " +
                "    WHERE sj.categories LIKE ? " +
                ") as tv ON p.venue_id = tv.id " +
                "GROUP BY p.year ORDER BY p.year";

        List<ChartPoint> list = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            String pattern = "%" + categoryName + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new ChartPoint(rs.getInt("year"), rs.getInt("count")));
                }
            }
        }
        return list;
    }

    public List<ChartPoint> getScatterAuthorsVsPapers(int from, int to, int limit) throws SQLException {
        // High performance optimization: We first group the filtered publications 
        // to get the author count per paper, and then we group by venue.
        // This avoids redundant JOINs and double filtering.
        String finalSql = "SELECT v.name, COUNT(pg.pub_id) as total_papers, AVG(pg.author_count) as avg_authors " +
                "FROM ( " +
                "    SELECT p.venue_id, p.id as pub_id, COUNT(pa.author_id) as author_count " +
                "    FROM Publication p " +
                "    JOIN Publication_Author pa ON p.id = pa.publication_id " +
                "    WHERE p.year BETWEEN ? AND ? " +
                "    GROUP BY p.venue_id, p.id " +
                ") pg " +
                "JOIN Venue v ON pg.venue_id = v.id " +
                "GROUP BY v.id, v.name " +
                "ORDER BY total_papers DESC LIMIT ?";

        List<ChartPoint> list = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(finalSql)) {
            stmt.setInt(1, from);
            stmt.setInt(2, to);
            stmt.setInt(3, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new ChartPoint(rs.getString("name"), rs.getInt("total_papers"),
                            rs.getDouble("avg_authors")));
                }
            }
        }
        return list;
    }

    public List<ChartPoint> getTopAuthors(int limit, int offset) throws SQLException {
        // Optimized: Group and sort in a subquery first, then join for only the N
        // results
        // This ensures we only join 10 authors instead of the entire table.
        String sql = "SELECT a.name, t.cnt " +
                "FROM (SELECT author_id, COUNT(*) as cnt FROM Publication_Author " +
                "      GROUP BY author_id ORDER BY cnt DESC LIMIT ? OFFSET ?) t " +
                "JOIN Author a ON t.author_id = a.id ORDER BY t.cnt DESC";
        List<ChartPoint> list = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, limit);
            stmt.setInt(2, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next())
                    list.add(new ChartPoint(rs.getString("name"), rs.getInt("cnt")));
            }
        }
        return list;
    }

    public List<ChartPoint> getVenueTypeDistribution() throws SQLException {
        String sql = "SELECT type, COUNT(*) as count FROM Venue GROUP BY type";
        List<ChartPoint> data = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    data.add(new ChartPoint(rs.getString("type"), rs.getInt("count")));
                }
            }
        }
        return data;
    }

    public List<ChartPoint> getScatterData(int fromYear, int toYear, int limit) throws SQLException {
        // High-Performance Optimization: 
        // We force SQLite to stream Journal rows pre-sorted by sjr DESC.
        // Then, the EXISTS subquery runs lazily ONLY for the top journals until LIMIT is reached.
        // This avoids scanning millions of Publication rows or evaluating EXISTS for all journals.
        String sql = "SELECT v.name, j.sjr, j.cite_score " +
                "FROM ( " +
                "  SELECT venue_id, sjr, cite_score FROM Journal " +
                "  WHERE sjr > 0 OR cite_score > 0 " +
                "  ORDER BY sjr DESC " +
                ") j " +
                "JOIN Venue v ON j.venue_id = v.id " +
                "WHERE EXISTS (SELECT 1 FROM Publication p WHERE p.venue_id = j.venue_id AND p.year BETWEEN ? AND ?) " +
                "LIMIT ?";
        List<ChartPoint> list = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, fromYear);
            stmt.setInt(2, toYear);
            stmt.setInt(3, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(new ChartPoint(rs.getString("name"), rs.getDouble("cite_score"), rs.getDouble("sjr")));
                }
            }
        }
        return list;
    }

    public YearProfile findYearProfile(int year) throws SQLException {
        // Optimized: Separate publication counts from distinct author counting
        // Using idx_publication_year and idx_pa_author_id
        String sql = "SELECT " +
                "  (SELECT COUNT(*) FROM Publication WHERE year = ?) as total_publications, " +
                "  (SELECT COUNT(DISTINCT venue_id) FROM Publication WHERE year = ?) as distinct_venues, " +
                "  (SELECT COUNT(DISTINCT pa.author_id) FROM Publication_Author pa JOIN Publication p ON pa.publication_id = p.id WHERE p.year = ?) as distinct_authors";

        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setInt(1, year);
            stmt.setInt(2, year);
            stmt.setInt(3, year);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    YearProfile yp = new YearProfile();
                    yp.year = year;
                    yp.totalPublications = rs.getInt("total_publications");
                    yp.distinctVenues = rs.getInt("distinct_venues");
                    yp.distinctAuthors = rs.getInt("distinct_authors");
                    if (yp.totalPublications == 0)
                        return null;
                    return yp;
                }
            }
        }
        return null;
    }

    public List<String> searchEntities(String query) throws SQLException {
        String sql = "SELECT name FROM ( " +
                "  SELECT name FROM Author WHERE name LIKE ? " +
                "  UNION " +
                "  SELECT name FROM Venue WHERE name LIKE ? " +
                ") t LIMIT 10";
        List<String> results = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, "%" + query + "%");
            stmt.setString(2, "%" + query + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next())
                    results.add(rs.getString("name"));
            }
        }
        return results;
    }

    public List<ChartPoint> searchVenuesFast(String query) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String trimmedQuery = query.trim();
        List<ChartPoint> results = new ArrayList<>();
        String sql = "SELECT name, type FROM Venue WHERE name LIKE ? LIMIT 50";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            stmt.setString(1, "%" + trimmedQuery + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new ChartPoint(rs.getString("name"), 0, rs.getString("type")));
                }
            }
        }
        return results;
    }

    public List<ChartPoint> searchEntitiesDetailed(String query) throws SQLException {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        String trimmedQuery = query.trim();
        List<ChartPoint> results = new ArrayList<>();

        // 1. Get matching authors and their exact publication counts
        String authorSql = "SELECT a.name, (SELECT COUNT(*) FROM Publication_Author WHERE author_id = a.id) as cnt " +
                           "FROM Author a WHERE a.name LIKE ? ORDER BY cnt DESC LIMIT 50";
        try (PreparedStatement stmt = getConnection().prepareStatement(authorSql)) {
            stmt.setString(1, "%" + trimmedQuery + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new ChartPoint(rs.getString("name"), rs.getInt("cnt"), "AUTHOR"));
                }
            }
        }

        // 2. Get matching venues and dynamically compute their aggregated publication counts
        String venueSql = "SELECT name, type FROM Venue WHERE name LIKE ? LIMIT 50";
        try (PreparedStatement stmt = getConnection().prepareStatement(venueSql)) {
            stmt.setString(1, "%" + trimmedQuery + "%");
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    String type = rs.getString("type");
                    int cnt = getVenuePublicationCountForSearch(name);
                    results.add(new ChartPoint(name, cnt, type));
                }
            }
        }

        // 3. Sort all results by publication count descending
        results.sort((a, b) -> Integer.compare(b.value.intValue(), a.value.intValue()));

        // 4. Limit the combined results to 50 items
        if (results.size() > 50) {
            return new ArrayList<>(results.subList(0, 50));
        }
        return results;
    }

    private int getVenuePublicationCountForSearch(String venueName) throws SQLException {
        List<Integer> ids = getAssociatedVenueIds(venueName);
        if (ids.isEmpty()) {
            return 0;
        }

        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM Publication WHERE venue_id IN (");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) countSql.append(",");
            countSql.append("?");
        }
        countSql.append(")");

        try (PreparedStatement stmt = getConnection().prepareStatement(countSql.toString())) {
            int pIdx = 1;
            for (int id : ids) {
                stmt.setInt(pIdx++, id);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public PaginatedResult<PaperDetail> getPapersByEntity(String type, String value, int limit, int offset,
            Integer minYear, Integer maxYear, String keyword, String sortField, String sortOrder) throws SQLException {
        if (value == null)
            return new PaginatedResult<>(new ArrayList<>(), 0, limit, offset);
        String trimmedValue = value.trim();
        System.out.println("Repo: getPapersByEntity type=" + type + ", value=[" + trimmedValue + "], offset=" + offset);

        StringBuilder sql = new StringBuilder();
        StringBuilder countSql = new StringBuilder();
        List<Object> params = new ArrayList<>();

        String baseQuery = "";
        String whereClause = "";

        if ("author".equalsIgnoreCase(type)) {
            // Optimized: Join via ID lookup if possible, but keeping it simple for now.
            // MUST avoid TRIM(a.name) = ? for index usage.
            baseQuery = "FROM Publication p JOIN Publication_Author pa ON p.id = pa.publication_id " +
                    "JOIN Author a ON pa.author_id = a.id LEFT JOIN Venue v ON p.venue_id = v.id ";
            whereClause = "WHERE a.name = ? ";
            params.add(trimmedValue);
        } else if ("venue".equalsIgnoreCase(type)) {
            List<Integer> ids = getAssociatedVenueIds(trimmedValue);
            baseQuery = "FROM Publication p LEFT JOIN Venue v ON p.venue_id = v.id ";
            if (ids.isEmpty()) {
                whereClause = "WHERE 1=0 ";
            } else {
                StringBuilder inClause = new StringBuilder("WHERE p.venue_id IN (");
                for (int i = 0; i < ids.size(); i++) {
                    if (i > 0) inClause.append(",");
                    inClause.append("?");
                    params.add(ids.get(i));
                }
                inClause.append(") ");
                whereClause = inClause.toString();
            }
        } else if ("year".equalsIgnoreCase(type)) {
            baseQuery = "FROM Publication p LEFT JOIN Venue v ON p.venue_id = v.id ";
            whereClause = "WHERE p.year = ? ";
            params.add(trimmedValue);
        }

        if (baseQuery.isEmpty())
            return new PaginatedResult<>(new ArrayList<>(), 0, limit, offset);

        StringBuilder filters = new StringBuilder();
        List<Object> filterParams = new ArrayList<>();
        if (minYear != null) {
            filters.append("AND p.year >= ? ");
            filterParams.add(minYear);
        }
        if (maxYear != null) {
            filters.append("AND p.year <= ? ");
            filterParams.add(maxYear);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            filters.append("AND p.title LIKE ? ");
            filterParams.add("%" + keyword.trim() + "%");
        }

        // 1. Get Total Count
        countSql.append("SELECT COUNT(*) ").append(baseQuery).append(whereClause).append(filters);
        int totalCount = 0;
        try (PreparedStatement stmt = getConnection().prepareStatement(countSql.toString())) {
            int pIdx = 1;
            for (Object p : params) {
                stmt.setObject(pIdx++, p);
            }
            for (Object p : filterParams)
                stmt.setObject(pIdx++, p);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    totalCount = rs.getInt(1);
            }
        }

        // 2. Get Data
        sql.append("SELECT p.title, p.year, v.name as venue_name, v.type as venue_type ")
                .append(baseQuery).append(whereClause).append(filters);

        String dbSortField = "p.year";
        if ("title".equalsIgnoreCase(sortField))
            dbSortField = "p.title";
        else if ("year".equalsIgnoreCase(sortField))
            dbSortField = "p.year";
        else if ("venue".equalsIgnoreCase(sortField))
            dbSortField = "v.name";
        else if ("type".equalsIgnoreCase(sortField))
            dbSortField = "v.type";

        String dbSortOrder = "DESC";
        if ("ASC".equalsIgnoreCase(sortOrder))
            dbSortOrder = "ASC";
        else if ("DESC".equalsIgnoreCase(sortOrder))
            dbSortOrder = "DESC";

        sql.append("ORDER BY ").append(dbSortField).append(" ").append(dbSortOrder).append(" ");
        sql.append("LIMIT ? OFFSET ?");

        List<PaperDetail> list = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            int pIdx = 1;
            for (Object p : params) {
                stmt.setObject(pIdx++, p);
            }
            for (Object p : filterParams)
                stmt.setObject(pIdx++, p);
            stmt.setInt(pIdx++, limit);
            stmt.setInt(pIdx++, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PaperDetail pd = new PaperDetail();
                    pd.title = rs.getString("title");
                    pd.year = rs.getInt("year");
                    pd.venueName = rs.getString("venue_name");
                    pd.venueType = rs.getString("venue_type");
                    list.add(pd);
                }
            }
        }
        return new PaginatedResult<>(list, totalCount, limit, offset);
    }

    public String getEntityType(String name) throws SQLException {
        // Fast identification using indexes
        String authorSql = "SELECT 'AUTHOR' FROM Author WHERE name = ? LIMIT 1";
        try (PreparedStatement stmt = getConnection().prepareStatement(authorSql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return "AUTHOR";
            }
        }

        String venueSql = "SELECT type FROM Venue WHERE name = ? LIMIT 1";
        try (PreparedStatement stmt = getConnection().prepareStatement(venueSql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return rs.getString("type");
            }
        }
        return null;
    }

    public List<PublisherStat> getPublisherStats() throws SQLException {
        String sql = "SELECT p.name, " +
                "SUM(CASE WHEN j.best_quartile = 'Q1' THEN 1 ELSE 0 END) as q1, " +
                "SUM(CASE WHEN j.best_quartile = 'Q2' THEN 1 ELSE 0 END) as q2, " +
                "SUM(CASE WHEN j.best_quartile = 'Q3' THEN 1 ELSE 0 END) as q3, " +
                "SUM(CASE WHEN j.best_quartile = 'Q4' THEN 1 ELSE 0 END) as q4, " +
                "COUNT(j.venue_id) as total " +
                "FROM Publisher p JOIN Journal j ON p.id = j.publisher_id " +
                "GROUP BY p.id, p.name ORDER BY total DESC LIMIT 10";
        List<PublisherStat> list = new ArrayList<>();
        try (PreparedStatement stmt = getConnection().prepareStatement(sql)) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PublisherStat ps = new PublisherStat();
                    ps.publisherName = rs.getString("name");
                    ps.q1Count = rs.getInt("q1");
                    ps.q2Count = rs.getInt("q2");
                    ps.q3Count = rs.getInt("q3");
                    ps.q4Count = rs.getInt("q4");
                    ps.totalJournals = rs.getInt("total");
                    list.add(ps);
                }
            }
        }
        return list;
    }

    public List<ChartPoint> getVenuesComparisonData(List<String> venueNames, String metric, int fromYear, int toYear)
            throws SQLException {
        String cleanMetric = (metric == null) ? "articles" : metric.trim().toLowerCase();
        if (venueNames == null || venueNames.isEmpty())
            return new ArrayList<>();

        List<ChartPoint> results = new ArrayList<>();
        for (String venueName : venueNames) {
            List<Integer> ids = getAssociatedVenueIds(venueName);
            if (ids.isEmpty()) continue;

            StringBuilder sql = new StringBuilder();
            if ("authors".equals(cleanMetric)) {
                sql.append("SELECT p.year, COUNT(pa.author_id) as cnt FROM Publication p ")
                   .append("JOIN Publication_Author pa ON p.id = pa.publication_id ")
                   .append("WHERE p.venue_id IN (");
            } else {
                sql.append("SELECT p.year, COUNT(DISTINCT p.id) as cnt FROM Publication p ")
                   .append("WHERE p.venue_id IN (");
            }
            
            for (int i = 0; i < ids.size(); i++) {
                if (i > 0) sql.append(",");
                sql.append("?");
            }
            sql.append(") AND p.year BETWEEN ? AND ? GROUP BY p.year ORDER BY p.year");

            try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
                int pIdx = 1;
                for (Integer id : ids) stmt.setInt(pIdx++, id);
                stmt.setInt(pIdx++, fromYear);
                stmt.setInt(pIdx++, toYear);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(new ChartPoint(rs.getInt("year"), rs.getInt("cnt"), venueName));
                    }
                }
            }
        }
        return results;
    }

    private List<Integer> getAssociatedVenueIds(String venueName) throws SQLException {
        List<Integer> ids = new ArrayList<>();
        if (venueName == null || venueName.trim().isEmpty()) {
            return ids;
        }
        String trimmed = venueName.trim();
        
        Set<String> candidateNames = new HashSet<>();
        candidateNames.add(trimmed);
        candidateNames.add(trimmed.replace("Journal", "J."));
        candidateNames.add(trimmed.replace("The ", ""));
        candidateNames.add("The " + trimmed);
        
        List<String> acronyms = new ArrayList<>();
        String acSql = "SELECT DISTINCT acronym FROM Staging_Conference WHERE ? LIKE CONCAT('%', acronym, '%') AND acronym != ''";
        try (PreparedStatement stmt = getConnection().prepareStatement(acSql)) {
            stmt.setString(1, trimmed);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) acronyms.add(rs.getString(1));
            }
        }

        for (String ac : acronyms) {
            String titleSql = "SELECT DISTINCT title FROM Staging_Conference WHERE acronym = ? AND title != ''";
            try (PreparedStatement stmt = getConnection().prepareStatement(titleSql)) {
                stmt.setString(1, ac);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        String title = rs.getString(1);
                        candidateNames.add(title);
                        candidateNames.add(title.replace("The ", ""));
                        candidateNames.add("The " + title);
                    }
                }
            }
        }

        if (candidateNames.isEmpty()) {
            return ids;
        }

        StringBuilder sql = new StringBuilder("SELECT id FROM Venue WHERE name IN (");
        int size = candidateNames.size();
        for (int i = 0; i < size; i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");
        
        try (PreparedStatement stmt = getConnection().prepareStatement(sql.toString())) {
            int pIdx = 1;
            for (String name : candidateNames) {
                stmt.setString(pIdx++, name);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) ids.add(rs.getInt(1));
            }
        }
        return ids;
    }

    public int[] getYearRange() throws SQLException {
        // Optimized: Finding global min and max year across all publications
        String sql = "SELECT MIN(year) as min_y, MAX(year) as max_y FROM Publication WHERE year > 0 AND year <= YEAR(CURRENT_DATE) + 1";
        try (PreparedStatement stmt = getConnection().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                int minYear = rs.getInt("min_y");
                int maxYear = rs.getInt("max_y");
                if (minYear > 0 && maxYear >= minYear) {
                    return new int[] { minYear, maxYear };
                }
            }
        }
        return new int[] { 1900, 2026 }; // Fallback
    }
}

//

//

//

//
