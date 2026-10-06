package campus.filter;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Exposes the decrypted envelope JSON as ordinary request parameters.
 * Query-string parameters still pass through untouched.
 */
public class EnvelopeRequest extends HttpServletRequestWrapper {

    private final Map<String, String[]> envelopeParams;

    public EnvelopeRequest(HttpServletRequest request, Map<String, String[]> envelopeParams) {
        super(request);
        Map<String, String[]> merged = new HashMap<>(request.getParameterMap());
        merged.putAll(envelopeParams);
        this.envelopeParams = merged;
    }

    @Override
    public String getParameter(String name) {
        String[] v = envelopeParams.get(name);
        return v == null || v.length == 0 ? null : v[0];
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Collections.unmodifiableMap(envelopeParams);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(envelopeParams.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return envelopeParams.get(name);
    }
}
