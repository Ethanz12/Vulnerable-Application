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

/**
 * Administrator user management. POST promotes/demotes accounts. This is the
 * endpoint the stored XSS in the review bot invokes to escalate the attacker
 * to administrator (VULNERABILITY 3).
 */
@WebServlet(urlPatterns = "/api/admin/users")
public class AdminUsersServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireAdmin(req, res);
        if (u == null) {
            return;
        }
        JSONArray out = new JSONArray();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, username, email, student_id, role, status FROM users ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.put(new JSONObject()
                        .put("id", rs.getInt("id"))
                        .put("username", rs.getString("username"))
                        .put("email", rs.getString("email"))
                        .put("student_id", rs.getString("student_id"))
                        .put("role", rs.getString("role"))
                        .put("status", rs.getString("status")));
            }
            ok(res, new JSONObject().put("users", out));
        } catch (Exception e) {
            err(res, 500, "Users unavailable: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireAdmin(req, res);
        if (u == null) {
            return;
        }
        String idParam = req.getParameter("user_id");
        String role = req.getParameter("role");
        if (idParam == null || role == null || !role.matches("student|organiser|admin")) {
            err(res, 400, "user_id and role (student|organiser|admin) required");
            return;
        }
        try (Connection c = Database.get()) {
            int userId = Integer.parseInt(idParam);
            String username;
            try (PreparedStatement find = c.prepareStatement("SELECT username FROM users WHERE id=?")) {
                find.setInt(1, userId);
                try (ResultSet rs = find.executeQuery()) {
                    if (!rs.next()) {
                        err(res, 404, "User not found");
                        return;
                    }
                    username = rs.getString("username");
                }
            }
            try (PreparedStatement ps = c.prepareStatement("UPDATE users SET role=? WHERE id=?")) {
                ps.setString(1, role);
                ps.setInt(2, userId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO audit_log (actor, action, detail) VALUES (?, 'role_change', ?)")) {
                ps.setString(1, u.getUsername());
                ps.setString(2, "user " + username + " (#" + userId + ") role set to " + role);
                ps.executeUpdate();
            }
            ok(res, new JSONObject().put("username", username).put("role", role));
        } catch (NumberFormatException e) {
            err(res, 400, "Invalid user id");
        } catch (Exception e) {
            err(res, 500, "Role change failed: " + e.getMessage());
        }
    }
}
