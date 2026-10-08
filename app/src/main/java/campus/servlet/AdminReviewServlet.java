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

@WebServlet(urlPatterns = "/api/admin/review")
public class AdminReviewServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireAdmin(req, res);
        if (u == null) {
            return;
        }
        String idParam = req.getParameter("id");
        try (Connection c = Database.get()) {
            if (idParam != null && !idParam.isBlank()) {
                detail(c, res, Integer.parseInt(idParam));
            } else {
                queue(c, res);
            }
        } catch (NumberFormatException e) {
            err(res, 400, "Invalid event id");
        } catch (Exception e) {
            err(res, 500, "Review unavailable");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireAdmin(req, res);
        if (u == null) {
            return;
        }
        String action = req.getParameter("action");
        String idParam = req.getParameter("event_id");
        if (!"approve".equals(action) && !"reject".equals(action)) {
            err(res, 400, "action must be approve or reject");
            return;
        }
        try (Connection c = Database.get()) {
            int eventId = Integer.parseInt(idParam);
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE events SET status=? WHERE id=? AND status='pending_review'")) {
                ps.setString(1, "approve".equals(action) ? "published" : "rejected");
                ps.setInt(2, eventId);
                int updated = ps.executeUpdate();
                if (updated == 0) {
                    err(res, 404, "Event is not pending review");
                    return;
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO audit_log (actor, action, detail) VALUES (?, 'event_review', ?)")) {
                ps.setString(1, u.getUsername());
                ps.setString(2, "event " + eventId + " " + action + "d");
                ps.executeUpdate();
            }
            ok(res, new JSONObject().put("status", "approve".equals(action) ? "published" : "rejected"));
        } catch (NumberFormatException e) {
            err(res, 400, "Invalid event id");
        } catch (Exception e) {
            err(res, 500, "Review action failed");
        }
    }

    private void queue(Connection c, HttpServletResponse res) throws Exception {
        JSONArray out = new JSONArray();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT e.id, e.title, e.created_at, u.username AS organiser FROM events e "
                + "JOIN users u ON u.id = e.organiser_id WHERE e.status='pending_review' ORDER BY e.id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.put(new JSONObject()
                        .put("id", rs.getInt("id"))
                        .put("title", rs.getString("title"))
                        .put("organiser", rs.getString("organiser"))
                        .put("created_at", rs.getTimestamp("created_at").toInstant().toString()));
            }
        }
        ok(res, new JSONObject().put("pending", out));
    }

    private void detail(Connection c, HttpServletResponse res, int id) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT e.*, u.username AS organiser FROM events e "
                + "JOIN users u ON u.id = e.organiser_id WHERE e.id=?")) {
            ps.setInt(1, id);
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
                        .put("location", rs.getString("location"))
                        .put("organiser", rs.getString("organiser"));
                ok(res, new JSONObject().put("event", ev));
            }
        }
    }
}
