package campus.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import campus.util.Config;

public final class Database {

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("JDBC driver missing", e);
        }
    }

    private Database() {}

    public static Connection get() throws SQLException {
        String url = Config.get("db.url", "DB_URL");
        String user = Config.get("db.user", "DB_USER");
        String pass = Config.get("db.pass", "DB_PASS");
        SQLException last = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            try {
                return DriverManager.getConnection(url, user, pass);
            } catch (SQLException e) {
                last = e;
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw last;
                }
            }
        }
        throw last;
    }
}
