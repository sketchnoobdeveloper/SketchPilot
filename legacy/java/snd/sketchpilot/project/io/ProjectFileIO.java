package snd.sketchpilot.project.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import snd.sketchpilot.project.crypto.SketchwareCrypto;

/** Encrypted read/write of a single project file. */
public final class ProjectFileIO {

    private final SketchwareCrypto crypto;

    public ProjectFileIO(SketchwareCrypto crypto) {
        this.crypto = crypto;
    }

    public byte[] read(File file) throws IOException {
        return SketchwareCrypto.readAll(new java.io.FileInputStream(file));
    }

    public void write(File file, byte[] bytes) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        FileOutputStream out = new FileOutputStream(file, false);
        try {
            out.write(bytes);
            out.flush();
        } finally {
            out.close();
        }
    }

    public String readText(File file, boolean encrypted) throws Exception {
        byte[] bytes = read(file);
        if (bytes.length == 0) {
            return "";
        }
        return encrypted ? crypto.decrypt(bytes) : new String(bytes, "UTF-8");
    }

    public void writeText(File file, String text, boolean encrypted) throws Exception {
        byte[] bytes = encrypted ? crypto.encrypt(text) : text.getBytes("UTF-8");
        write(file, bytes);
    }
}
