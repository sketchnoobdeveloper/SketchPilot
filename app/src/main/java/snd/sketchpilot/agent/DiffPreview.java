package snd.sketchpilot.agent;

import java.util.ArrayList;
import java.util.List;

/** Small line-diff generator used for Apply/Reject/Undo previews. */
public final class DiffPreview {

    private DiffPreview() {
    }

    public static List<String> generate(String oldText, String newText) {
        String[] a = split(oldText);
        String[] b = split(newText);
        int n = a.length;
        int m = b.length;
        int[][] lcs = new int[n + 1][m + 1];
        for (int i = n - 1; i >= 0; i--) {
            for (int j = m - 1; j >= 0; j--) {
                if (a[i].equals(b[j])) {
                    lcs[i][j] = lcs[i + 1][j + 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }
        List<String> out = new ArrayList<String>();
        int i = 0;
        int j = 0;
        while (i < n && j < m) {
            if (a[i].equals(b[j])) {
                out.add("  " + a[i]);
                i++;
                j++;
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                out.add("- " + a[i]);
                i++;
            } else {
                out.add("+ " + b[j]);
                j++;
            }
        }
        while (i < n) {
            out.add("- " + a[i]);
            i++;
        }
        while (j < m) {
            out.add("+ " + b[j]);
            j++;
        }
        return out;
    }

    /** Applies a diff produced by generate(); lines starting '+ ' are inserted. */
    public static String apply(String oldText, List<String> diff) {
        StringBuilder out = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < diff.size(); i++) {
            String line = diff.get(i);
            String kept = null;
            if (line.startsWith("+ ")) {
                kept = line.substring(2);
            } else if (line.startsWith("  ")) {
                kept = line.substring(2);
            }
            if (kept == null) {
                continue;
            }
            if (!first) {
                out.append('\n');
            }
            out.append(kept);
            first = false;
        }
        return out.toString();
    }

    public static String render(List<String> diff) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < diff.size(); i++) {
            out.append(diff.get(i)).append('\n');
        }
        return out.toString();
    }

    private static String[] split(String text) {
        if (text == null || text.length() == 0) {
            return new String[0];
        }
        return text.split("\n", -1);
    }
}
