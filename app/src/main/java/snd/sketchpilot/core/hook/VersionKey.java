package snd.sketchpilot.core.hook;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;

/** Cache key for resolved hooks: versionCode + SHA-256 of the host APK. */
public final class VersionKey {

    private VersionKey() {
    }

    public static String of(int versionCode, String apkPath) {
        String hash = hashFile(apkPath);
        return versionCode + ":" + (hash == null ? "nohash" : hash);
    }

    private static String hashFile(String path) {
        if (path == null) {
            return null;
        }
        try {
            File file = new File(path);
            if (!file.isFile()) {
                return null;
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            FileInputStream in = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int read;
            try {
                while ((read = in.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            } finally {
                in.close();
            }
            byte[] bytes = digest.digest();
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (int i = 0; i < bytes.length; i++) {
                String hex = Integer.toHexString(bytes[i] & 0xff);
                if (hex.length() == 1) {
                    out.append('0');
                }
                out.append(hex);
            }
            return out.toString();
        } catch (Throwable t) {
            return null;
        }
    }
}
