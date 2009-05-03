/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: PrintStreamOutputDevice.java,v 1.3 2005-03-24 13:57:46 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import java.io.PrintStream;

/**
 * The OutputDevice to write to.
 */
public class PrintStreamOutputDevice implements OutputDevice {
    private final PrintStream _outStream;

    public PrintStreamOutputDevice(final PrintStream out) {
        _outStream = out;
    }

    public void flush() {
        _outStream.flush();
    }

    public void write(final byte[] buffer, final int off, final int len) {
        _outStream.write(buffer, off, len);
    }

    public void print(final String s) {
        _outStream.print(s);
    }

    public void println(final String s) {
        _outStream.println(s);
    }

    public void println() {
        _outStream.println();
    }

    public void close() {
        _outStream.close();
    }

    public void attributeBold() { /* no attributes */
    }

    public void attributeGrey() { /* no attributes */
    }

    public void attributeReset() { /* no attributes */
    }

    public boolean isTerminal() {
        return false;
    }
}
