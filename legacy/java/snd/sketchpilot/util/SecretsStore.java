package snd.sketchpilot.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.security.Key;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;

import org.json.JSONObject;

/** Keystore-backed encrypted storage for provider keys and project secrets. */
public final class SecretsStore {

    private static final String PREFS = "sketchpilot_secrets";
    private static final String ALIAS = "sketchpilot_aes_key";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    private final SharedPreferences preferences;

    public SecretsStore(Context context) {
        this.preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        try {
            ensureKey();
        } catch (Throwable t) {
            Logger.hookFailure("SecretsStore.ensureKey", t);
        }
    }

    private static void ensureKey() throws Exception {
        KeyStore store = KeyStore.getInstance(ANDROID_KEYSTORE);
        store.load(null);
        if (store.containsAlias(ALIAS)) {
            return;
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build();
        generator.init(spec);
        generator.generateKey();
    }

    public void put(String key, String secret) {
        if (key == null || secret == null) {
            return;
        }
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(secret.getBytes("UTF-8"));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            preferences.edit().putString(key, android.util.Base64.encodeToString(combined,
                    android.util.Base64.DEFAULT)).commit();
        } catch (Throwable t) {
            Logger.hookFailure("SecretsStore.put", t);
        }
    }

    public String get(String key) {
        if (key == null) {
            return "";
        }
        String encoded = preferences.getString(key, "");
        if (encoded.length() == 0) {
            return "";
        }
        try {
            byte[] combined = android.util.Base64.decode(encoded, android.util.Base64.DEFAULT);
            byte[] iv = new byte[12];
            byte[] encrypted = new byte[combined.length - 12];
            System.arraycopy(combined, 0, iv, 0, 12);
            System.arraycopy(combined, 12, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(encrypted), "UTF-8");
        } catch (Throwable t) {
            Logger.hookFailure("SecretsStore.get", t);
            return "";
        }
    }

    public JSONObject redacted() {
        JSONObject out = new JSONObject();
        java.util.Map<String, ?> all = preferences.getAll();
        java.util.Iterator<String> keys = all.keySet().iterator();
        while (keys.hasNext()) {
            String key = keys.next();
            try {
                out.put(key, Redactor.MASK);
            } catch (Throwable ignored) {
                // safe
            }
        }
        return out;
    }

    private static Key key() throws Exception {
        KeyStore store = KeyStore.getInstance(ANDROID_KEYSTORE);
        store.load(null);
        return store.getKey(ALIAS, null);
    }
}
