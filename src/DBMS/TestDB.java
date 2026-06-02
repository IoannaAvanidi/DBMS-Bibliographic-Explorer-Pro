import java.sql.*;

public class TestDB {
    public static void main(String[] args) throws Exception {
        String url = "jdbc:mysql://localhost:3306/dbms_project?allowPublicKeyRetrieval=true&useSSL=false";
        String user = "mye030";
        String password = "mye030";
        
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            System.out.println("Connected!");
            
            String sql = "{CALL GetJournalProfile(?)}";
            try (CallableStatement stmt = conn.prepareCall(sql)) {
                stmt.setString(1, "The VLDB Journal");
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("SP Name: " + rs.getString("journal_name"));
                        System.out.println("SP Total Pubs: " + rs.getInt("total_publications"));
                        System.out.println("SP Total Authors: " + rs.getInt("total_authors"));
                    } else {
                        System.out.println("SP returned empty set for The VLDB Journal");
                    }
                }
            } catch (Exception e) {
                System.out.println("SP error: " + e.getMessage());
            }

            // Also test VLDB Journal exactly
            sql = "SELECT * FROM Venue WHERE name LIKE '%VLDB Journal%'";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        System.out.println("Venue found: " + rs.getString("name") + " (type: " + rs.getString("type") + ")");
                    }
                }
            }
        }
    }
}
