package snd.sketchpilot.view;

import java.util.ArrayList;
import java.util.List;

/**
 * Widget type table recovered from Sketchware Pro wq.b(int) (evidence E13).
 * The runtime bridge can extend this list with Pro/custom widgets; the static
 * table is the offline fallback and is never used to invent unknown types.
 */
public final class WidgetRegistry {

    private static final String[] NAMES = {
            "linear", "relativelayout", "hscroll", "button", "textview",
            "edittext", "imageview", "webview", "progressbar", "listview",
            "spinner", "checkbox", "vscroll", "switch", "seekbar",
            "calendarview", "fab", "adview", "mapview", "radiobutton",
            "ratingbar", "videoview", "searchview", "autocomplete", "multiautocomplete",
            "gridview", "analogclock", "datepicker", "timepicker", "digitalclock",
            "tablayout", "viewpager", "bottomnavigation", "badgeview", "patternlockview",
            "wavesidebar", "cardview", "collapsingtoolbar", "textinputlayout", "swiperefreshlayout",
            "radiogroup", "materialbutton", "signinbutton", "circleimageview", "lottie",
            "youtube", "otpview", "codeview", "recyclerview"
    };

    private final List<String> extraTypes = new ArrayList<String>();

    public boolean isValidType(int type) {
        return type >= 0 && type < NAMES.length;
    }

    public boolean isValidType(String name) {
        if (name == null) {
            return false;
        }
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) {
                return true;
            }
        }
        return extraTypes.contains(name);
    }

    public String nameOf(int type) {
        return isValidType(type) ? NAMES[type] : "widget";
    }

    public int typeOf(String name) {
        for (int i = 0; i < NAMES.length; i++) {
            if (NAMES[i].equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public void registerExtraType(String name) {
        if (name != null && name.length() > 0 && !extraTypes.contains(name)) {
            extraTypes.add(name);
        }
    }

    public List<String> allNames() {
        List<String> result = new ArrayList<String>();
        for (int i = 0; i < NAMES.length; i++) {
            result.add(NAMES[i]);
        }
        result.addAll(extraTypes);
        return result;
    }

    public static int size() {
        return NAMES.length;
    }
}
