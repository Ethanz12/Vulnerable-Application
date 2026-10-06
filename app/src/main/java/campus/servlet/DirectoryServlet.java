package campus.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import campus.db.Database;

/**
 * Public orientation directory: students who registered and still need to
 * activate. Shows each pending account's student ID and registration epoch -
 * exactly the two inputs of the activation-token algorithm.
 */
@WebServlet(urlPatterns = "/api/directory/pending")
public class DirectoryServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        JSONArray out = new JSONArray();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT username, student_id, extract(epoch from created_at)::bigint AS reg_epoch "
                     + "FROM users WHERE status='pending' ORDER BY created_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.put(new JSONObject()
                        .put("username", rs.getString("username"))
                        .put("student_id", rs.getString("student_id"))
                        .put("registered_epoch", rs.getLong("reg_epoch")));
            }
        } catch (Exception e) {
            err(res, 500, "Directory unavailable: " + e.getMessage());
            return;
        }
        ok(res, new JSONObject().put("pending", out));
    }
}
