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
import campus.web.User;

/** Administrator audit trail - shows role changes, uploads and review actions. */
@WebServlet(urlPatterns = "/api/admin/audit")
public class AdminAuditServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireAdmin(req, res);
        if (u == null) {
            return;
        }
        JSONArray out = new JSONArray();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT actor, action, detail, at FROM audit_log ORDER BY id DESC LIMIT 100");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.put(new JSONObject()
                        .put("actor", rs.getString("actor"))
                        .put("action", rs.getString("action"))
                        .put("detail", rs.getString("detail"))
                        .put("at", rs.getTimestamp("at").toInstant().toString()));
            }
            ok(res, new JSONObject().put("entries", out));
        } catch (Exception e) {
            err(res, 500, "Audit log unavailable");
        }
    }
}
