/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: TimeRenderer.java,v 1.8 2005-11-27 16:20:28 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.OutputDevice;
import henplus.AbstractOutputDevice;

/**
 * document me.
 */
public class TimeRenderer {
    private final static long SECOND_MILLIS = 1000;
    private final static long MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private final static long HOUR_MILLIS   = 60 * MINUTE_MILLIS;

    public static void printFraction(long execTime,
				     long number, OutputDevice out) {
	if (number == 0) {
	    out.print(" -- ");
	    return;
	}
	long milli = execTime / number;
	long micro = (execTime - number * milli) * 1000 / number;
	printTime(milli, micro, out);
    }

    /** render time as string */
    public static String renderTime(long execTimeInMs) {
        return renderTime(execTimeInMs, 0);
    }

    /** render time as string */
    public static String renderTime(long execTimeInMs, long usec) {
        final StringBuffer result = new StringBuffer();
        printTime(execTimeInMs, usec, new AbstractOutputDevice() {
                public void print(String s) { result.append(s); }
            });
        return result.toString();
    }

    /** print time to output device */
    public static void printTime(long execTimeInMs, OutputDevice out) {
	printTime(execTimeInMs, 0, out);
    }

    /** print time to output device */
    public static void printTime(long execTimeInMs, long usec, OutputDevice out) {
	final long totalTime = execTimeInMs;

        boolean hourPrinted = false;
        boolean minutePrinted = false;

        if (execTimeInMs > HOUR_MILLIS) {
            out.print(String.valueOf(execTimeInMs/HOUR_MILLIS));
            out.print("h ");
            execTimeInMs %= HOUR_MILLIS;
            hourPrinted = true;
        }

	if (hourPrinted || execTimeInMs > MINUTE_MILLIS) {
            long minute = execTimeInMs/60000;
            if (hourPrinted && minute < 10) {
                out.print("0"); // need padding.
            }
	    out.print(String.valueOf(minute));
	    out.print("m ");
	    execTimeInMs %= MINUTE_MILLIS;
            minutePrinted = true;
	}

	if (minutePrinted || execTimeInMs >= SECOND_MILLIS) {
            long seconds = execTimeInMs/SECOND_MILLIS;
	    if (minutePrinted && seconds < 10) {
		out.print("0"); // need padding.
	    }
	    out.print(String.valueOf(seconds));
	    out.print(".");
	    execTimeInMs %= SECOND_MILLIS;
            // milliseconds
	    if (execTimeInMs < 100) out.print("0");
	    if (execTimeInMs < 10)  out.print("0");
	    out.print(String.valueOf(execTimeInMs));
	}
	else if (execTimeInMs > 0) {
	    out.print(String.valueOf(execTimeInMs));
	}
	
	if (usec > 0) {
	    if (totalTime > 0) {  // need delimiter and padding.
		out.print(".");
		if (usec < 100) out.print("0");
		if (usec < 10)  out.print("0");
	    }
	    out.print(String.valueOf(usec));
	}
	else if (execTimeInMs == 0) {
	    out.print("0 ");
	}

	if (totalTime > MINUTE_MILLIS) {
            out.print("s");
            return;
        }
        else if (totalTime >= SECOND_MILLIS) out.print(" ");
	else if (totalTime > 0 && totalTime < SECOND_MILLIS) out.print(" m");
	else if (totalTime == 0 && usec > 0) out.print(" µ");
	out.print("sec");
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
