package campus.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import campus.db.Database;
import campus.web.User;

@WebServlet(urlPatterns = "/api/attendance")
public class AttendanceServlet extends ApiServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireLogin(req, res);
        if (u == null) {
            return;
        }
        int eventId;
        try {
            eventId = Integer.parseInt(req.getParameter("event_id"));
        } catch (NumberFormatException e) {
            err(res, 400, "event_id required");
            return;
        }
        if ("cancel".equals(req.getParameter("action"))) {
            try (Connection c = Database.get();
                 PreparedStatement ps = c.prepareStatement(
                         "DELETE FROM attendance WHERE event_id=? AND user_id=?")) {
                ps.setInt(1, eventId);
                ps.setInt(2, u.getId());
                if (ps.executeUpdate() == 0) {
                    err(res, 404, "You are not registered for this event");
                    return;
                }
            } catch (Exception e) {
                err(res, 500, "Could not cancel attendance");
                return;
            }
            ok(res, null);
            return;
        }
        try (Connection c = Database.get()) {
            try (PreparedStatement check = c.prepareStatement(
                    "SELECT status, starts_at FROM events WHERE id = ?")) {
                check.setInt(1, eventId);
                var rs = check.executeQuery();
                if (!rs.next()) {
                    err(res, 404, "Event not found");
                    return;
                }
                if (!"published".equals(rs.getString("status"))) {
                    err(res, 403, "Event is not published");
                    return;
                }
                var startsAt = rs.getTimestamp("starts_at");
                if (startsAt != null && startsAt.toLocalDateTime().isBefore(java.time.LocalDateTime.now())) {
                    err(res, 403, "Event has already ended");
                    return;
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO attendance (event_id, user_id) VALUES (?, ?) "
                     + "ON CONFLICT (event_id, user_id) DO NOTHING")) {
                ps.setInt(1, eventId);
                ps.setInt(2, u.getId());
                ps.executeUpdate();
            }
        } catch (Exception e) {
            err(res, 500, "Could not record attendance");
            return;
        }
        ok(res, null);
    }
}
