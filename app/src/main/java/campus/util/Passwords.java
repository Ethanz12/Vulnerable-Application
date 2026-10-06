package campus.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class Passwords {

    private Passwords() {}

    public static String hash(String username, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((username.toLowerCase() + ":" + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** Weak, documented activation-token derivation: MD5("ACTIVATE:<student_id>:<epoch>")[:8]. */
    public static String activationToken(String studentId, long epochSeconds) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(("ACTIVATE:" + studentId + ":" + epochSeconds).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.substring(0, 8);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
