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

@WebServlet(urlPatterns = "/api/events")
public class EventsServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        String idParam = req.getParameter("id");
        try (Connection c = Database.get()) {
            if (idParam != null && !idParam.isBlank()) {
                detail(c, res, Integer.parseInt(idParam));
            } else {
                list(c, res);
            }
        } catch (NumberFormatException e) {
            err(res, 400, "Invalid event id");
        } catch (Exception e) {
            err(res, 500, "Events unavailable");
        }
    }

    private void list(Connection c, HttpServletResponse res) throws Exception {
        JSONArray out = new JSONArray();
        String sql = "SELECT e.id, e.title, e.location, e.starts_at, u.username AS organiser, "
                + "(SELECT token FROM invitations i WHERE i.event_id = e.id LIMIT 1) AS invite_code "
                + "FROM events e JOIN users u ON u.id = e.organiser_id "
                + "WHERE e.status='published' ORDER BY e.starts_at NULLS LAST";
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                JSONObject ev = new JSONObject()
                        .put("id", rs.getInt("id"))
                        .put("title", rs.getString("title"))
                        .put("location", rs.getString("location"))
                        .put("organiser", rs.getString("organiser"));
                putStarts(rs, ev);
                if (rs.getString("invite_code") != null) {
                    ev.put("invite_code", rs.getString("invite_code"));
                }
                out.put(ev);
            }
        }
        ok(res, new JSONObject().put("events", out));
    }

    private void detail(Connection c, HttpServletResponse res, int id) throws Exception {
        String sql = "SELECT e.*, u.username AS organiser, "
                + "(SELECT token FROM invitations i WHERE i.event_id = e.id LIMIT 1) AS invite_code "
                + "FROM events e JOIN users u ON u.id = e.organiser_id "
                + "WHERE e.id=? AND e.status='published'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    err(res, 404, "Event not found or not published");
                    return;
                }
                JSONObject ev = new JSONObject()
                        .put("id", rs.getInt("id"))
                        .put("title", rs.getString("title"))
                        .put("description_html", rs.getString("description_html"))
                        .put("location", rs.getString("location"))
                        .put("organiser", rs.getString("organiser"));
                putStarts(rs, ev);
                if (rs.getString("invite_code") != null) {
                    ev.put("invite_code", rs.getString("invite_code"));
                }
                ok(res, new JSONObject().put("event", ev));
            }
        }
    }

    private void putStarts(ResultSet rs, JSONObject ev) throws Exception {
        java.sql.Timestamp ts = rs.getTimestamp("starts_at");
        if (ts != null) {
            ev.put("starts_at", ts.toInstant().toString());
        }
    }
}
