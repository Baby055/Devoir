import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {
    private DBConnection dbConnection;

    // Constructeur : on injecte la DBConnection
    public DataRetriever(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    // 1. Lire toutes les catégories
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

    // 2. Lire les produits avec pagination
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

    // 3. Lire les produits par critères
    public List<Product> getProductsByCriteria(String productName, String categoryName,
                                               Instant creationMin, Instant creationMax) {
        List<Product> products = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT p.id, p.name, p.creation_datetime, c.id AS cat_id, c.name AS cat_name " +
                        "FROM Product p " +
                        "LEFT JOIN Product_category c ON p.id = c.product_id WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (productName != null) {
            sql.append("AND p.name ILIKE ? ");
            params.add("%" + productName + "%");
        }
        if (categoryName != null) {
            sql.append("AND c.name ILIKE ? ");
            params.add("%" + categoryName + "%");
        }
        if (creationMin != null) {
            sql.append("AND p.creation_datetime >= ? ");
            params.add(Timestamp.from(creationMin));
        }
        if (creationMax != null) {
            sql.append("AND p.creation_datetime <= ? ");
            params.add(Timestamp.from(creationMax));
        }

        try (Connection conn = dbConnection.getDBConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

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

    // 4. Lire les produits par critères + pagination
    public List<Product> getProductsByCriteria(String productName, String categoryName,
                                               Instant creationMin, Instant creationMax,
                                               int page, int size) {
        List<Product> filtered = getProductsByCriteria(productName, categoryName, creationMin, creationMax);

        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, filtered.size());

        if (fromIndex >= filtered.size()) {
            return new ArrayList<>();
        }
        return filtered.subList(fromIndex, toIndex);
    }
}