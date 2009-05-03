/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: TerminalOutputDevice.java,v 1.2 2005-03-24 13:57:46 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import java.io.PrintStream;

/**
 * The OutputDevice to write to.
 */
public class TerminalOutputDevice extends PrintStreamOutputDevice {
    private static final String BOLD = "\033[1m";
    private static final String NORMAL = "\033[m";
    private static final String GREY = "\033[1;30m";

    public TerminalOutputDevice(final PrintStream out) {
        super(out);
    }

    @Override
    public void attributeBold() {
        print(BOLD);
    }

    @Override
    public void attributeGrey() {
        print(GREY);
    }

    @Override
    public void attributeReset() {
        print(NORMAL);
    }

    @Override
    public boolean isTerminal() {
        return true;
    }
}
