package snd.sketchpilot.project.crypto;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Exact file crypto used by Sketchware Pro's oB class.
 *
 * Evidence: docs/SKETCHWARE_INTERNALS.md E6 (javap of oB.b/c/d) and E8 (empirical
 * decryption of real on-device project files).
 *
 * Algorithm: AES/CBC/PKCS5Padding, key = "sketchwaresecure".getBytes() (16 bytes),
 * IV = the SAME bytes. No Base64, no salt, no per-file metadata.
 *
 * The literal is version-pinned to v7.1.0 / versionCode 150. Callers must obtain
 * an instance from CryptoRegistry for the detected host version; do not assume
 * this literal works on every fork.
 */
public final class SketchwareCrypto {

    public static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    public static final String KEY_LITERAL = "sketchwaresecure";

    private final byte[] key;

    public SketchwareCrypto() {
        this(KEY_LITERAL);
    }

    public SketchwareCrypto(String keyLiteral) {
        this.key = keyLiteral.getBytes(StandardCharsets.UTF_8);
        if (this.key.length != 16) {
            throw new IllegalArgumentException("AES-128 requires a 16-byte key/IV literal");
        }
    }

    public byte[] encrypt(String plain) throws GeneralSecurityException {
        byte[] input;
        try {
            input = plain.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 unavailable", e);
        }
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(key));
        return cipher.doFinal(input);
    }

    public String decrypt(byte[] cipherBytes) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(key));
        byte[] plain = cipher.doFinal(cipherBytes);
        try {
            return new String(plain, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 unavailable", e);
        }
    }

    /**
     * Heuristic used by the offline repository to decide whether a file must be
     * decrypted before parsing. AES-CBC ciphertext is always a multiple of 16
     * bytes and its decryption with the known key must yield UTF-8 text.
     */
    public boolean looksEncrypted(byte[] data) {
        if (data == null || data.length == 0 || data.length % 16 != 0) {
            return false;
        }
        try {
            String plain = decrypt(data);
            return plain.indexOf('@') >= 0 || plain.startsWith("{") || plain.startsWith("[");
        } catch (Throwable t) {
            return false;
        }
    }

    public static byte[] readAll(java.io.InputStream in) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        try {
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        } finally {
            in.close();
        }
        return out.toByteArray();
    }
}
