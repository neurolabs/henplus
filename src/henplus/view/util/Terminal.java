/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: Terminal.java,v 1.2 2004-01-27 18:16:34 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.view.util;

import java.io.PrintStream;

/**
 * This is a bit hacky way to use the terminals code .. hardcoded.
 */
public class Terminal {
    public static final String BOLD   = "\033[1m";
    public static final String NORMAL = "\033[m";
    public static final String REVERSE= "\033[7m";
    public static final String LINED  = "\033[4m";
    public static final String GREY   = "\033[1;30m";
    public static final String RED    = "\033[1;31m";
    public static final String GREEN  = "\033[1;32m";
    public static final String BLUE   = "\033[1;34m";
    public static final String INVISIBLE= "\033[8m";

    static boolean hasTerminal = false;
    
    public static boolean hasTerminal() {
	return hasTerminal;
    }

    public static void setTerminalAvailable(boolean t) {
	hasTerminal = t;
    }

    public static void boldface(PrintStream out) {
	if (hasTerminal) try { out.print( BOLD ); } catch (Exception e) {}
    }

    public static void red(PrintStream out) {
	if (hasTerminal) try { out.print( RED ); } catch (Exception e) {}
    }

    public static void green(PrintStream out) {
	if (hasTerminal) try { out.print( GREEN ); } catch (Exception e) {}
    }

    public static void grey(PrintStream out) {
	if (hasTerminal) try { out.print( GREY ); } catch (Exception e) {}
    }

    public static void blue(PrintStream out) {
	if (hasTerminal) try { out.print( BLUE ); } catch (Exception e) {}
    }
    
    /**
     * Set the terminal to a specific mode, this must be one of those defined in this class.
     * @param face
     * @param out
     */
    public static void set( String mode, PrintStream out ) {
        if (hasTerminal) try { out.print( mode ); } catch (Exception e) {}
    }

    public static void reset(PrintStream out) {
	if (hasTerminal) try { out.print( NORMAL ); } catch (Exception e) {}
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
