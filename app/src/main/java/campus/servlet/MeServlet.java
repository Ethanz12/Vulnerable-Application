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

/** Current session profile plus team memberships and attendance. */
@WebServlet(urlPatterns = "/api/me")
public class MeServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireLogin(req, res);
        if (u == null) {
            return;
        }
        JSONObject me = new JSONObject()
                .put("id", u.getId())
                .put("username", u.getUsername())
                .put("email", u.getEmail())
                .put("student_id", u.getStudentId())
                .put("role", u.getRole());
        try (Connection c = Database.get()) {
            JSONArray teams = new JSONArray();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT et.event_id, e.title, et.role FROM event_team et "
                    + "JOIN events e ON e.id = et.event_id WHERE et.user_id=? ORDER BY et.id")) {
                ps.setInt(1, u.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        teams.put(new JSONObject()
                                .put("event_id", rs.getInt("event_id"))
                                .put("title", rs.getString("title"))
                                .put("role", rs.getString("role")));
                    }
                }
            }
            JSONArray attending = new JSONArray();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT e.id, e.title FROM attendance a JOIN events e ON e.id = a.event_id "
                    + "WHERE a.user_id=? ORDER BY a.id")) {
                ps.setInt(1, u.getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        attending.put(new JSONObject()
                                .put("id", rs.getInt("id"))
                                .put("title", rs.getString("title")));
                    }
                }
            }
            me.put("teams", teams).put("attending", attending);
            ok(res, me);
        } catch (Exception e) {
            err(res, 500, "Profile unavailable");
        }
    }
}
