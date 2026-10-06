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
import campus.web.User;
import campus.util.Passwords;

/**
 * Signup / login / logout.
 * Signup deliberately creates a PENDING account whose activation token is
 * derived deterministically from the student ID and registration time.
 */
@WebServlet(urlPatterns = "/api/auth")
public class AuthServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String action = req.getParameter("action");
        if ("signup".equals(action)) {
            signup(req, res);
        } else if ("login".equals(action)) {
            login(req, res);
        } else if ("logout".equals(action)) {
            req.getSession().invalidate();
            ok(res, null);
        } else {
            err(res, 400, "Unknown action");
        }
    }

    private void signup(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String username = req.getParameter("username");
        String email = req.getParameter("email");
        String studentId = req.getParameter("student_id");
        if (username == null || username.isBlank() || studentId == null || studentId.isBlank()) {
            err(res, 400, "Username and student ID are required");
            return;
        }
        long epoch = System.currentTimeMillis() / 1000L;
        String token = Passwords.activationToken(studentId.trim(), epoch);
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO users (username, email, student_id, role, status, created_at, activation_token) "
                     + "VALUES (?, ?, ?, 'student', 'pending', now(), ?)")) {
            ps.setString(1, username.trim());
            ps.setString(2, email == null ? null : email.trim());
            ps.setString(3, studentId.trim());
            ps.setString(4, token);
            ps.executeUpdate();
        } catch (Exception e) {
            err(res, 400, "Could not create account (username or student ID already known?)");
            return;
        }
        ok(res, new JSONObject()
                .put("message", "Account created and awaiting activation")
                .put("student_id", studentId.trim()));
    }

    private void login(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String username = req.getParameter("username");
        String password = req.getParameter("password");
        if (username == null || password == null) {
            err(res, 400, "Username and password required");
            return;
        }
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, username, email, student_id, role, status, password_hash "
                     + "FROM users WHERE lower(username) = lower(?)")) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !"active".equals(rs.getString("status"))
                        || !Passwords.hash(rs.getString("username"), password).equals(rs.getString("password_hash"))) {
                    err(res, 401, "Invalid credentials or account not activated");
                    return;
                }
                User u = new User(rs.getInt("id"), rs.getString("username"), rs.getString("email"),
                        rs.getString("student_id"), rs.getString("role"), rs.getString("status"));
                req.getSession().setAttribute("user", u);
                String redirect = switch (u.getRole()) {
                    case "admin" -> "admin.jsp";
                    case "organiser" -> "organiser.jsp";
                    default -> "student.jsp";
                };
                ok(res, new JSONObject().put("redirect", redirect).put("role", u.getRole()));
            }
        } catch (Exception e) {
            err(res, 500, "Login failed: " + e.getMessage());
        }
    }
}
