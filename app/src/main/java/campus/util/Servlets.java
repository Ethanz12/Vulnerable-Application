package campus.util;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import campus.db.Database;
import campus.web.User;

public final class Servlets {

    private Servlets() {}

    
    public static User user(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) {
            return null;
        }
        User u = (User) s.getAttribute("user");
        if (u == null) {
            return null;
        }
        User fresh = refresh(u.getId());
        if (fresh == null) {
            s.removeAttribute("user");
            return null;
        }
        s.setAttribute("user", fresh);
        return fresh;
    }

    private static User refresh(int id) {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, username, email, student_id, role, status FROM users WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !"active".equals(rs.getString("status"))) {
                    return null;
                }
                return new User(rs.getInt("id"), rs.getString("username"), rs.getString("email"),
                        rs.getString("student_id"), rs.getString("role"), rs.getString("status"));
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static void notifyBot(int eventId) {
        String hook = Config.get("bot.webhook", "BOT_WEBHOOK");
        Thread t = new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(hook).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");
                byte[] body = ("{\"event_id\":" + eventId + "}").getBytes(StandardCharsets.UTF_8);
                conn.setFixedLengthStreamingMode(body.length);
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(body);
                }
                conn.getResponseCode();
            } catch (Exception ignored) {
                
            }
        }, "bot-notify");
        t.setDaemon(true);
        t.start();
    }
}
