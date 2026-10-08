package campus.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Config {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = Config.class.getResourceAsStream("/app.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (IOException e) {
            throw new IllegalStateException("app.properties missing", e);
        }
    }

    private Config() {}

    
    public static String get(String key, String envVar) {
        String v = envVar == null ? null : System.getenv(envVar);
        if (v != null && !v.isBlank()) {
            return v;
        }
        return PROPS.getProperty(key);
    }
}
