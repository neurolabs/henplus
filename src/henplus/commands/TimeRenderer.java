/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: TimeRenderer.java,v 1.2 2002-02-09 12:21:56 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import java.io.PrintStream;

/**
 * document me.
 */
public class TimeRenderer {
    public static void printTime(long execTime, PrintStream out) {
	if (execTime > 60000) {
	    out.print(execTime/60000);
	    out.print(":");
	    execTime %= 60000;
	    if (execTime < 10000) {
		out.print("0");
	    }
	}
	if (execTime >= 1000) {
	    out.print(execTime / 1000);
	    out.print(".");
	    execTime %= 1000;
	    if (execTime < 100) out.print("0");
	    if (execTime < 10)  out.print("0");
	    out.print(execTime);
	    out.print(" ");
	}
	else {
	    out.print(execTime + " m");
	}
	out.print("sec");
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
