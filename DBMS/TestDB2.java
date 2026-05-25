import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TestDB2 {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/dbms_project?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        String user = "mye030";
        String pass = "mye030";
        
        long start = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("Connected to DB!");
            
            String query = "vldb";
            String sql = "SELECT name, type FROM Venue WHERE name LIKE ? LIMIT 20";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, "%" + query + "%");
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        System.out.println("Found Venue: '" + rs.getString("name") + "', Type: " + rs.getString("type"));
                    }
                }
            }
            
            long duration = System.currentTimeMillis() - start;
            System.out.println("Done! Time taken: " + duration + " ms");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
