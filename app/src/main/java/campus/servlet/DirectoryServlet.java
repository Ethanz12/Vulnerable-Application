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
                
                long exactEpoch = rs.getLong("reg_epoch");
                long approxEpoch = (exactEpoch / 3600) * 3600;
                out.put(new JSONObject()
                        .put("username", rs.getString("username"))
                        .put("student_id", rs.getString("student_id"))
                        .put("registered_hour_epoch", approxEpoch)
                        .put("note", "Registration time approximate (within 1 hour window)"));
            }
        } catch (Exception e) {
            err(res, 500, "Directory unavailable");
            return;
        }
        ok(res, new JSONObject().put("pending", out));
    }
}
