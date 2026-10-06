package campus.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import campus.web.User;
import campus.util.Servlets;

/** Shared helpers for the JSON API servlets. */
public abstract class ApiServlet extends HttpServlet {

    protected void json(HttpServletResponse res, int status, Object body) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json");
        res.getWriter().print(body.toString());
    }

    protected void ok(HttpServletResponse res, JSONObject extra) throws IOException {
        JSONObject o = new JSONObject().put("ok", true);
        if (extra != null) {
            for (String k : extra.keySet()) {
                o.put(k, extra.get(k));
            }
        }
        json(res, 200, o);
    }

    protected void err(HttpServletResponse res, int status, String message) throws IOException {
        json(res, status, new JSONObject().put("ok", false).put("error", message));
    }

    protected User currentUser(HttpServletRequest req) {
        return Servlets.user(req);
    }

    protected User requireLogin(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = currentUser(req);
        if (u == null) {
            err(res, 401, "Authentication required");
            return null;
        }
        return u;
    }

    protected User requireAdmin(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireLogin(req, res);
        if (u == null) {
            return null;
        }
        if (!u.isAdmin()) {
            err(res, 403, "Administrator access required");
            return null;
        }
        return u;
    }

    protected User requireOrganiser(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireLogin(req, res);
        if (u == null) {
            return null;
        }
        if (!u.isOrganiser()) {
            err(res, 403, "Organiser access required");
            return null;
        }
        return u;
    }
}
