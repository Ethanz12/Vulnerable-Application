package campus.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import campus.crypto.Rc4;
import campus.util.Config;

/**
 * Application-level encrypted envelope. Every dynamic request/response body
 * travels as base64(RC4(staticKey, body)). Request bodies are decrypted into
 * request parameters; response bodies are re-encrypted, HTML pages being
 * wrapped in a small bootstrap page that decrypts in the browser.
 * The static key is hardcoded in app.properties AND shipped to the client in
 * /static/js/app-crypto.js - by design, this is the lab's stage-0 weakness.
 * SECURITY TRAINING LAB ONLY.
 */
@WebFilter(urlPatterns = "/*")
public class EnvelopeFilter implements Filter {

    private byte[] key;

    @Override
    public void init(FilterConfig filterConfig) {
        key = Config.get("crypto.key", "CRYPTO_KEY").getBytes(StandardCharsets.UTF_8);
    }

    private boolean isStatic(HttpServletRequest req) {
        String uri = req.getRequestURI();
        String ctx = req.getContextPath();
        return uri.startsWith(ctx + "/static/")
                || uri.equals(ctx + "/favicon.ico")
                || uri.equals(ctx + "/robots.txt");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (isStatic(req)) {
            chain.doFilter(request, response);
            return;
        }

        req.setCharacterEncoding("UTF-8");
        res.setCharacterEncoding("UTF-8");

        HttpServletRequest effectiveReq = req;
        String method = req.getMethod().toUpperCase();
        if ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method)) {
            byte[] raw = readBody(req);
            if (raw.length > 0) {
                Map<String, String[]> params = decryptBody(raw);
                if (params == null) {
                    fail(res, 400, "Request body must be a valid encrypted envelope: {\"enc\": \"<base64>\"}");
                    return;
                }
                effectiveReq = new EnvelopeRequest(req, params);
            }
        }

        CaptureResponse captured = new CaptureResponse(res);
        try {
            chain.doFilter(effectiveReq, captured);
        } catch (Exception e) {
            if (res.isCommitted()) {
                throw new ServletException(e);
            }
            captured.reset();
            fail(res, 500, "Internal error: " + e.getClass().getSimpleName());
            return;
        }

        byte[] body = captured.getCapturedBytes();
        int status = captured.getStatus();
        if (body.length == 0 || (status >= 300 && status < 400)) {
            return; // redirects and empty bodies carry no envelope
        }

        String contentType = captured.getContentType();
        if (contentType == null) {
            contentType = "text/html";
        }

        String envelope = Base64.getEncoder().encodeToString(Rc4.crypt(key, body));
        byte[] out;
        if (contentType.contains("json") || contentType.contains("text/plain")) {
            out = new JSONObject().put("enc", envelope).toString().getBytes(StandardCharsets.UTF_8);
            res.setContentType(contentType.contains("json") ? "application/json" : contentType);
        } else {
            out = bootstrapPage(req.getContextPath(), envelope).getBytes(StandardCharsets.UTF_8);
            res.setContentType("text/html;charset=UTF-8");
        }
        res.setContentLength(out.length);
        res.getOutputStream().write(out);
        res.getOutputStream().flush();
    }

    private byte[] readBody(HttpServletRequest req) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int n;
        try (var in = req.getInputStream()) {
            while ((n = in.read(chunk)) > 0) {
                bos.write(chunk, 0, n);
            }
        }
        return bos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private Map<String, String[]> decryptBody(byte[] raw) {
        try {
            JSONObject wrapper = new JSONObject(new String(raw, StandardCharsets.UTF_8));
            if (!wrapper.has("enc")) {
                return null;
            }
            byte[] plain = Rc4.crypt(key, Base64.getDecoder().decode(wrapper.getString("enc")));
            JSONObject json = new JSONObject(new String(plain, StandardCharsets.UTF_8));
            Map<String, String[]> params = new HashMap<>();
            for (String k : json.keySet()) {
                Object v = json.get(k);
                if (v instanceof JSONArray) {
                    JSONArray arr = (JSONArray) v;
                    String[] vals = new String[arr.length()];
                    for (int i = 0; i < arr.length(); i++) {
                        vals[i] = String.valueOf(arr.get(i));
                    }
                    params.put(k, vals);
                } else if (JSONObject.NULL.equals(v)) {
                    params.put(k, new String[] { null });
                } else {
                    params.put(k, new String[] { String.valueOf(v) });
                }
            }
            return params;
        } catch (JSONException | IllegalArgumentException e) {
            return null;
        }
    }

    private void fail(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        String json = new JSONObject()
                .put("ok", false)
                .put("error", message)
                .toString();
        String envelope = Base64.getEncoder().encodeToString(Rc4.crypt(key, json.getBytes(StandardCharsets.UTF_8)));
        byte[] out = new JSONObject().put("enc", envelope).toString().getBytes(StandardCharsets.UTF_8);
        res.setContentType("application/json");
        res.setContentLength(out.length);
        res.getOutputStream().write(out);
    }

    private String bootstrapPage(String contextPath, String envelope) {
        return "<!DOCTYPE html>\n"
                + "<html><head><meta charset=\"utf-8\">"
                + "<title>Campus Events</title>"
                + "<link rel=\"stylesheet\" href=\"" + contextPath + "/static/css/app.css\">"
                + "<script src=\"" + contextPath + "/static/js/app-crypto.js\"></script>"
                + "</head><body>\n"
                + "<script>\n"
                + "(function(){\n"
                + "  document.open();\n"
                + "  document.write(window.AppCrypto.decryptDocument('" + envelope + "'));\n"
                + "  document.close();\n"
                + "})();\n"
                + "</script>\n"
                + "</body></html>";
    }
}
