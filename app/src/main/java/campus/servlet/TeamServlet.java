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

/**
 * VULNERABILITY 2: broken access control / IDOR on event-team membership.
 * The handler only confirms that SOME invitation token exists, then trusts
 * the client-supplied event_id and role verbatim. A student can submit a
 * valid volunteer code together with another event's id and role=organiser,
 * joining (and escalating to) an event they do not own.
 */
@WebServlet(urlPatterns = "/api/team")
public class TeamServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String token = req.getParameter("token");
        if (token == null || token.isBlank()) {
            err(res, 400, "token parameter required");
            return;
        }
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT i.event_id, i.allowed_role, e.title FROM invitations i "
                     + "JOIN events e ON e.id = i.event_id WHERE i.token=?")) {
            ps.setString(1, token.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    err(res, 404, "Unknown invite code");
                    return;
                }
                ok(res, new JSONObject()
                        .put("event_id", rs.getInt("event_id"))
                        .put("allowed_role", rs.getString("allowed_role"))
                        .put("event_title", rs.getString("title")));
            }
        } catch (Exception e) {
            err(res, 500, "Lookup failed: " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireLogin(req, res);
        if (u == null) {
            return;
        }
        String token = req.getParameter("token");
        String role = req.getParameter("role");
        String eventIdParam = req.getParameter("event_id");
        if (token == null || token.isBlank() || role == null || eventIdParam == null) {
            err(res, 400, "token, event_id and role are required");
            return;
        }
        int eventId;
        try {
            eventId = Integer.parseInt(eventIdParam);
        } catch (NumberFormatException e) {
            err(res, 400, "event_id must be numeric");
            return;
        }

        try (Connection c = Database.get()) {
            // Only validates that the token exists at all; never checks that it
            // belongs to the submitted event or that the role is permitted.
            boolean tokenExists;
            try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM invitations WHERE token=?")) {
                ps.setString(1, token.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    tokenExists = rs.next();
                }
            }
            if (!tokenExists) {
                err(res, 403, "Invalid invite code");
                return;
            }

            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO event_team (event_id, user_id, role) VALUES (?, ?, ?) "
                    + "ON CONFLICT (event_id, user_id, role) DO NOTHING")) {
                ps.setInt(1, eventId);
                ps.setInt(2, u.getId());
                ps.setString(3, role);
                ps.executeUpdate();
            }
            if ("organiser".equals(role)) {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE users SET role='organiser' WHERE id=?")) {
                    ps.setInt(1, u.getId());
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO audit_log (actor, action, detail) VALUES (?, 'team_join', ?)")) {
                ps.setString(1, u.getUsername());
                ps.setString(2, "user " + u.getId() + " joined event " + eventId + " as " + role
                        + " using invite code " + token.trim());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            err(res, 500, "Could not join team: " + e.getMessage());
            return;
        }
        ok(res, new JSONObject()
                .put("event_id", eventId)
                .put("role", role)
                .put("your_role", "organiser".equals(role) ? "organiser" : "student"));
    }
}
