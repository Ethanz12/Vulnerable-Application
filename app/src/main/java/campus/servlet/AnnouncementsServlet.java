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
import campus.util.HtmlSanitizer;
import campus.web.User;

@WebServlet(urlPatterns = "/api/admin/announcements")
public class AnnouncementsServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireAdmin(req, res);
        if (u == null) {
            return;
        }
        JSONArray out = new JSONArray();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, name, body_html FROM announcement_templates ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.put(new JSONObject()
                        .put("id", rs.getInt("id"))
                        .put("name", rs.getString("name"))
                        .put("body_html", rs.getString("body_html")));
            }
            ok(res, new JSONObject().put("templates", out));
        } catch (Exception e) {
            err(res, 500, "Templates unavailable");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireAdmin(req, res);
        if (u == null) {
            return;
        }
        if ("delete".equals(req.getParameter("action"))) {
            String idParam = req.getParameter("id");
            try (Connection c = Database.get();
                 PreparedStatement ps = c.prepareStatement("DELETE FROM announcement_templates WHERE id=?")) {
                ps.setInt(1, Integer.parseInt(idParam));
                ps.executeUpdate();
            } catch (Exception e) {
                err(res, 500, "Delete failed");
                return;
            }
            ok(res, null);
            return;
        }
        String name = req.getParameter("name");
        String body = req.getParameter("body_html");
        if (name == null || name.isBlank()) {
            err(res, 400, "Name is required");
            return;
        }
        
        String sanitizedBody = HtmlSanitizer.sanitize(body == null ? "" : body);
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO announcement_templates (name, body_html) VALUES (?, ?)")) {
            ps.setString(1, name.trim());
            ps.setString(2, sanitizedBody);
            ps.executeUpdate();
        } catch (Exception e) {
            err(res, 500, "Save failed");
            return;
        }
        ok(res, null);
    }
}
