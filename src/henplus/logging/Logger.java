package henplus.logging;

import henplus.HenPlus;

public class Logger {

    public static void debug(final String message, final Object... args) {
        if (HenPlus.getInstance().isVerbose() && !HenPlus.getInstance().isQuiet()) {
            HenPlus.msg().println(String.format(message, args));
        }
    }

    public static void debug(final String message, final Throwable t, final Object... args) {
        if (HenPlus.getInstance().isVerbose() && !HenPlus.getInstance().isQuiet()) {
            HenPlus.msg().println(String.format(message, args));
            t.printStackTrace();
        }
    }

    public static void info(final String message, final Object... args) {
        if (!HenPlus.getInstance().isQuiet()) {
            HenPlus.msg().println(String.format(message, args));
        }
    }

    public static void error(final String message, final Object... args) {
        HenPlus.msg().println(String.format(message, args));
    }

    public static void error(final String message, final Throwable t, final Object... args) {
        HenPlus.msg().println(String.format(message, args));
        t.printStackTrace();
    }
}
