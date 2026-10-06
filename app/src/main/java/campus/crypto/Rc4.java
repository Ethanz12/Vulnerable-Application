package campus.crypto;

/**
 * RC4 as used by the application envelope. Deliberately weak by design:
 * a single static key (see app.properties) is shared by the server and the
 * browser, and the keystream restarts for every message.
 * SECURITY TRAINING LAB ONLY.
 */
public final class Rc4 {

    private Rc4() {}

    public static byte[] crypt(byte[] key, byte[] data) {
        int[] s = new int[256];
        for (int i = 0; i < 256; i++) {
            s[i] = i;
        }
        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + s[i] + (key[i % key.length] & 0xff)) & 0xff;
            int t = s[i];
            s[i] = s[j];
            s[j] = t;
        }
        byte[] out = new byte[data.length];
        int i = 0;
        j = 0;
        for (int k = 0; k < data.length; k++) {
            i = (i + 1) & 0xff;
            j = (j + s[i]) & 0xff;
            int t = s[i];
            s[i] = s[j];
            s[j] = t;
            out[k] = (byte) (data[k] ^ s[(s[i] + s[j]) & 0xff]);
        }
        return out;
    }
}
