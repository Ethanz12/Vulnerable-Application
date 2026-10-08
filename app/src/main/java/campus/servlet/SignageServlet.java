package campus.servlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Base64;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import campus.web.User;

@WebServlet(urlPatterns = "/api/admin/templates")
public class SignageServlet extends ApiServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireAdmin(req, res);
        if (u == null) {
            return;
        }
        JSONArray packaged = new JSONArray();
        File[] builtin = list(new File(getServletContext().getRealPath("/WEB-INF/templates/signage")));
        if (builtin != null) {
            for (File f : builtin) {
                packaged.put("WEB-INF/templates/signage/" + f.getName());
            }
        }
        JSONArray uploads = new JSONArray();
        File[] uploaded = list(new File(getServletContext().getRealPath("/uploads/templates")));
        if (uploaded != null) {
            for (File f : uploaded) {
                uploads.put("uploads/templates/" + f.getName());
            }
        }
        ok(res, new JSONObject().put("packaged", packaged).put("uploads", uploads));
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        User u = requireAdmin(req, res);
        if (u == null) {
            return;
        }
        String filename = req.getParameter("filename");
        String contentB64 = req.getParameter("content_b64");
        if (filename == null || filename.isBlank() || contentB64 == null || contentB64.isBlank()) {
            err(res, 400, "filename and content_b64 are required");
            return;
        }
        
        
        String lowerFilename = filename.toLowerCase().trim();
        if (lowerFilename.endsWith(".jsp")) {
            err(res, 400, "JSP files are not allowed for security reasons");
            return;
        }
        
        
        
        
        
        
        File dir = new File(getServletContext().getRealPath("/uploads/templates"));
        if (!dir.isDirectory() && !dir.mkdirs()) {
            err(res, 500, "Upload directory unavailable");
            return;
        }
        File target = new File(dir, filename.trim());
        try {
            Files.write(target.toPath(), Base64.getDecoder().decode(contentB64.trim()));
        } catch (IllegalArgumentException e) {
            err(res, 400, "content_b64 is not valid base64");
            return;
        } catch (IOException e) {
            err(res, 500, "Could not write file");
            return;
        }
        try (java.sql.Connection ignore = campus.db.Database.get();
             java.sql.PreparedStatement ps = ignore.prepareStatement(
                     "INSERT INTO audit_log (actor, action, detail) VALUES (?, 'template_upload', ?)")) {
            ps.setString(1, u.getUsername());
            ps.setString(2, "uploaded signage template " + filename.trim()
                    + " (" + target.length() + " bytes)");
            ps.executeUpdate();
        } catch (Exception ignored) {
        }
        ok(res, new JSONObject()
                .put("path", "uploads/templates/" + target.getName())
                .put("size", target.length()));
    }

    private File[] list(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return new File[0];
        }
        Arrays.sort(files);
        return files;
    }
}
