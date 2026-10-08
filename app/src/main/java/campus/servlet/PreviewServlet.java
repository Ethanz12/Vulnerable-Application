package campus.servlet;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import campus.web.User;

/**
 * VULNERABILITY 4 (dispatch half): signage template preview forwards the raw
 * user-supplied path to the JSP dispatcher with no canonicalisation, no
 * path-traversal checks and no allowlist. Uploaded .jsp files execute in the
 * Tomcat process when previewed, e.g.
 *   /admin/templates/preview?path=../uploads/templates/shell.jsp
 */
@WebServlet(urlPatterns = "/admin/templates/preview")
public class PreviewServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireAdmin(req, res);
        if (u == null) {
            return;
        }
        String path = req.getParameter("path");
        if (path == null || path.isBlank()) {
            err(res, 400, "path parameter required");
            return;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        RequestDispatcher rd = getServletContext().getRequestDispatcher(path);
        if (rd == null) {
            err(res, 404, "Template not found: " + path);
            return;
        }
        try {
            rd.forward(req, res);
        } catch (Exception e) {
            if (!res.isCommitted()) {
                err(res, 500, "Preview failed");
            }
        }
    }
}
