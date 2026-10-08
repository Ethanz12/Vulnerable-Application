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
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO attendance (event_id, user_id) VALUES (?, ?) "
                     + "ON CONFLICT (event_id, user_id) DO NOTHING")) {
            ps.setInt(1, eventId);
            ps.setInt(2, u.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            err(res, 500, "Could not record attendance");
            return;
        }
        ok(res, null);
    }
}
