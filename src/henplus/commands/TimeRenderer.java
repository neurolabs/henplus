/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: TimeRenderer.java,v 1.5 2004-02-01 14:12:52 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.OutputDevice;

/**
 * document me.
 */
public class TimeRenderer {
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

    public static void printTime(long execTime, OutputDevice out) {
	printTime(execTime, 0, out);
    }

    public static void printTime(long execTime, long usec, OutputDevice out) {
	final long totalTime = execTime;

	if (execTime > 60000) {
	    out.print(String.valueOf(execTime/60000));
	    out.print(":");
	    execTime %= 60000;
	    if (execTime < 10000) {
		out.print("0");
	    }
	}
	if (execTime >= 1000) {
	    out.print(String.valueOf(execTime / 1000));
	    out.print(".");
	    execTime %= 1000;
	    if (execTime < 100) out.print("0");
	    if (execTime < 10)  out.print("0");
	    out.print(String.valueOf(execTime));
	    out.print(" ");
	}
	else if (execTime > 0) {
	    out.print(String.valueOf(execTime));
	}
	
	if (usec > 0) {
	    if (totalTime > 0) {  // after comma.
		out.print(".");
		if (usec < 100) out.print("0");
		if (usec < 10)  out.print("0");
	    }
	    out.print(String.valueOf(usec));
	}
	else if (execTime == 0) {
	    out.print("0 ");
	}

	if (totalTime > 60000) return;
	else if (totalTime > 0 && totalTime < 1000) out.print(" m");
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
