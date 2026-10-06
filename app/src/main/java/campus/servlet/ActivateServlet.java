package campus.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import campus.db.Database;
import campus.util.Passwords;

/**
 * VULNERABILITY 1: account activation.
 * The activation token is a deterministic function of the student ID and the
 * registration timestamp (MD5("ACTIVATE:<student_id>:<epoch>")[0:8]). Those
 * two inputs are exposed publicly on the orientation directory, so anyone can
 * compute the token for a pending account and take it over.
 */
@WebServlet(urlPatterns = "/api/activate")
public class ActivateServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String token = req.getParameter("token");
        if (token == null || token.isBlank()) {
            err(res, 400, "token parameter required");
            return;
        }
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT username, student_id FROM users WHERE status='pending' AND activation_token=?")) {
            ps.setString(1, token.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    err(res, 404, "No pending account matches this token");
                    return;
                }
                ok(res, new JSONObject()
                        .put("username", rs.getString("username"))
                        .put("student_id", rs.getString("student_id")));
            }
        } catch (Exception e) {
            err(res, 500, "Lookup failed: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String token = req.getParameter("token");
        String password = req.getParameter("password");
        if (token == null || token.isBlank() || password == null || password.length() < 8) {
            err(res, 400, "Token and a password of at least 8 characters are required");
            return;
        }
        try (Connection c = Database.get();
             PreparedStatement find = c.prepareStatement(
                     "SELECT id, username FROM users WHERE status='pending' AND activation_token=?")) {
            find.setString(1, token.trim());
            String username;
            int id;
            try (ResultSet rs = find.executeQuery()) {
                if (!rs.next()) {
                    err(res, 404, "No pending account matches this token");
                    return;
                }
                id = rs.getInt("id");
                username = rs.getString("username");
            }
            try (PreparedStatement up = c.prepareStatement(
                    "UPDATE users SET status='active', password_hash=?, activation_token=NULL WHERE id=?")) {
                up.setString(1, Passwords.hash(username, password));
                up.setInt(2, id);
                up.executeUpdate();
            }
            ok(res, new JSONObject().put("redirect", "login.jsp").put("username", username));
        } catch (Exception e) {
            err(res, 500, "Activation failed: " + e.getMessage());
        }
    }
}
