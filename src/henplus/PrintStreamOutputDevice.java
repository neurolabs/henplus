/*
 * This is free software, licensed under the Gnu Public License (GPL) get a copy from <http://www.gnu.org/licenses/gpl.html> $Id:
 * PrintStreamOutputDevice.java,v 1.3 2005-03-24 13:57:46 hzeller Exp $ author: Henner Zeller <H.Zeller@acm.org>
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

    @Override
    public void flush() {
        _outStream.flush();
    }

    @Override
    public void write(final byte[] buffer, final int off, final int len) {
        _outStream.write(buffer, off, len);
    }

    @Override
    public void print(final String s) {
        _outStream.print(s);
    }

    @Override
    public void println(final String s) {
        _outStream.println(s);
    }

    @Override
    public void println() {
        _outStream.println();
    }

    @Override
    public void close() {
        _outStream.close();
    }

    @Override
    public void attributeBold() { /* no attributes */
    }

    @Override
    public void attributeGrey() { /* no attributes */
    }

    @Override
    public void attributeReset() { /* no attributes */
    }

    @Override
    public boolean isTerminal() {
        return false;
    }
}
