package campus.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import campus.db.Database;
import campus.web.User;
import campus.util.Servlets;

@WebServlet(urlPatterns = "/api/organiser")
public class OrganiserServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireOrganiser(req, res);
        if (u == null) {
            return;
        }
        String idParam = req.getParameter("id");
        if (idParam != null && !idParam.isBlank()) {
            detail(req, res, u, Integer.parseInt(idParam));
            return;
        }
        JSONArray out = new JSONArray();
        String sql = "SELECT DISTINCT e.id, e.title, e.status, e.location, e.starts_at FROM events e "
                + "LEFT JOIN event_team et ON et.event_id = e.id AND et.user_id = ? AND et.role='organiser' "
                + "WHERE e.organiser_id = ? OR et.user_id IS NOT NULL ORDER BY e.id";
        try (Connection c = Database.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, u.getId());
            ps.setInt(2, u.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JSONObject ev = new JSONObject()
                            .put("id", rs.getInt("id"))
                            .put("title", rs.getString("title"))
                            .put("status", rs.getString("status"))
                            .put("location", rs.getString("location"));
                    Timestamp ts = rs.getTimestamp("starts_at");
                    if (ts != null) {
                        ev.put("starts_at", ts.toLocalDateTime().toString().substring(0, 16));
                    }
                    out.put(ev);
                }
            }
            ok(res, new JSONObject().put("events", out));
        } catch (Exception e) {
            err(res, 500, "Could not load events");
        }
    }

    private void detail(HttpServletRequest req, HttpServletResponse res, User u, int eventId) throws IOException {
        try (Connection c = Database.get()) {
            if (!canManage(c, u, eventId)) {
                err(res, 403, "You do not manage this event");
                return;
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, title, status, description_html, location, starts_at FROM events WHERE id=?")) {
                ps.setInt(1, eventId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        err(res, 404, "Event not found");
                        return;
                    }
                    JSONObject ev = new JSONObject()
                            .put("id", rs.getInt("id"))
                            .put("title", rs.getString("title"))
                            .put("status", rs.getString("status"))
                            .put("description_html", rs.getString("description_html"))
                            .put("location", rs.getString("location"));
                    Timestamp ts = rs.getTimestamp("starts_at");
                    if (ts != null) {
                        ev.put("starts_at", ts.toLocalDateTime().toString().substring(0, 16));
                    }
                    ok(res, new JSONObject().put("event", ev));
                }
            }
        } catch (Exception e) {
            err(res, 500, "Could not load event");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireOrganiser(req, res);
        if (u == null) {
            return;
        }
        String action = req.getParameter("action");
        if ("submit".equals(action)) {
            submit(req, res, u);
        } else if ("save".equals(action)) {
            save(req, res, u);
        } else if ("delete".equals(action)) {
            delete(req, res, u);
        } else {
            err(res, 400, "Unknown action");
        }
    }

    private boolean canManage(Connection c, User u, int eventId) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM events e WHERE e.id=? AND (e.organiser_id=? "
                + "OR EXISTS (SELECT 1 FROM event_team et WHERE et.event_id=e.id AND et.user_id=? AND et.role='organiser'))")) {
            ps.setInt(1, eventId);
            ps.setInt(2, u.getId());
            ps.setInt(3, u.getId());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void save(HttpServletRequest req, HttpServletResponse res, User u) throws IOException {
        String title = req.getParameter("title");
        String description = req.getParameter("description_html");
        String location = req.getParameter("location");
        String startsAt = req.getParameter("starts_at");
        if (startsAt != null && !startsAt.isBlank()) {
            try {
                java.time.LocalDateTime eventTime = java.time.LocalDateTime.parse(startsAt.substring(0, 16));
                if (eventTime.isBefore(java.time.LocalDateTime.now())) {
                    err(res, 400, "Event start time must be in the future");
                    return;
                }
            } catch (Exception e) {
                err(res, 400, "Invalid start time format");
                return;
            }
        }
        String idParam = req.getParameter("event_id");
        if (title == null || title.isBlank()) {
            err(res, 400, "Title is required");
            return;
        }
        
        
        
        if (description != null) {
            String lowerDesc = description.toLowerCase();
            if (lowerDesc.contains("<script") || lowerDesc.contains("</script>")) {
                err(res, 400, "Script tags are not allowed in descriptions");
                return;
            }
            
        }
        
        try (Connection c = Database.get()) {
            int eventId;
            if (idParam == null || idParam.isBlank()) {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO events (title, description_html, status, organiser_id, starts_at, location) "
                        + "VALUES (?, ?, 'draft', ?, ?, ?) RETURNING id")) {
                    ps.setString(1, title.trim());
                    ps.setString(2, description == null ? "" : description);
                    ps.setInt(3, u.getId());
                    setTimestamp(ps, 4, startsAt);
                    ps.setString(5, location);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        eventId = rs.getInt(1);
                    }
                }
            } else {
                eventId = Integer.parseInt(idParam);
                if (!canManage(c, u, eventId)) {
                    err(res, 403, "You do not manage this event");
                    return;
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE events SET title=?, description_html=?, starts_at=?, location=? WHERE id=?")) {
                    ps.setString(1, title.trim());
                    ps.setString(2, description == null ? "" : description);
                    setTimestamp(ps, 3, startsAt);
                    ps.setString(4, location);
                    ps.setInt(5, eventId);
                    ps.executeUpdate();
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO audit_log (actor, action, detail) VALUES (?, 'event_save', ?)")) {
                ps.setString(1, u.getUsername());
                ps.setString(2, "event " + eventId + " saved (" + title.trim() + ")");
                ps.executeUpdate();
            }
            ok(res, new JSONObject().put("event_id", eventId));
        } catch (NumberFormatException e) {
            err(res, 400, "Invalid event id");
        } catch (Exception e) {
            err(res, 500, "Save failed");
        }
    }

    private void submit(HttpServletRequest req, HttpServletResponse res, User u) throws IOException {
        String idParam = req.getParameter("event_id");
        if (idParam == null) {
            err(res, 400, "event_id required");
            return;
        }
        try (Connection c = Database.get()) {
            int eventId = Integer.parseInt(idParam);
            if (!canManage(c, u, eventId)) {
                err(res, 403, "You do not manage this event");
                return;
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE events SET status='pending_review' WHERE id=?")) {
                ps.setInt(1, eventId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO audit_log (actor, action, detail) VALUES (?, 'event_submit_review', ?)")) {
                ps.setString(1, u.getUsername());
                ps.setString(2, "event " + eventId + " submitted for administrator review");
                ps.executeUpdate();
            }
            Servlets.notifyBot(eventId);
            ok(res, new JSONObject().put("status", "pending_review"));
        } catch (NumberFormatException e) {
            err(res, 400, "Invalid event id");
        } catch (Exception e) {
            err(res, 500, "Submit failed");
        }
    }

    private void delete(HttpServletRequest req, HttpServletResponse res, User u) throws IOException {
        String idParam = req.getParameter("event_id");
        if (idParam == null) {
            err(res, 400, "event_id required");
            return;
        }
        try (Connection c = Database.get()) {
            int eventId = Integer.parseInt(idParam);
            if (!canManage(c, u, eventId)) {
                err(res, 403, "You do not manage this event");
                return;
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM attendance WHERE event_id=?")) {
                ps.setInt(1, eventId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM event_team WHERE event_id=?")) {
                ps.setInt(1, eventId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM invitations WHERE event_id=?")) {
                ps.setInt(1, eventId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM events WHERE id=?")) {
                ps.setInt(1, eventId);
                int deleted = ps.executeUpdate();
                if (deleted == 0) {
                    err(res, 404, "Event not found");
                    return;
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO audit_log (actor, action, detail) VALUES (?, 'event_delete', ?)")) {
                ps.setString(1, u.getUsername());
                ps.setString(2, "event " + eventId + " deleted");
                ps.executeUpdate();
            }
            ok(res, new JSONObject().put("deleted", eventId));
        } catch (NumberFormatException e) {
            err(res, 400, "Invalid event id");
        } catch (Exception e) {
            err(res, 500, "Delete failed");
        }
    }

    private void setTimestamp(PreparedStatement ps, int idx, String value) throws Exception {
        if (value == null || value.isBlank()) {
            ps.setNull(idx, java.sql.Types.TIMESTAMP);
        } else {
            ps.setTimestamp(idx, Timestamp.valueOf(LocalDateTime.parse(value.substring(0, 16))));
        }
    }
}
