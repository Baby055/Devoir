import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {
    private DBConnection dbConnection;

    public DataRetriever(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT id, name FROM Product_category";

        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                categories.add(new Category(id, name));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    public List<Product> getProductList(int page, int size) {
        List<Product> products = new ArrayList<>();
        int offset = (page - 1) * size;
        String sql = "SELECT p.id, p.name, p.creation_datetime, c.id AS cat_id, c.name AS cat_name " +
                "FROM Product p " +
                "LEFT JOIN Product_category c ON p.id = c.product_id " +
                "ORDER BY p.id " +
                "LIMIT ? OFFSET ?";

        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, size);
            stmt.setInt(2, offset);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    Instant creationDatetime = rs.getTimestamp("creation_datetime").toInstant();

                    Category category = null;
                    int catId = rs.getInt("cat_id");
                    if (!rs.wasNull()) {
                        category = new Category(catId, rs.getString("cat_name"));
                    }

                    products.add(new Product(id, name, creationDatetime, category));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return products;
    }
}