/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: PrintStreamOutputDevice.java,v 1.2 2004-03-05 23:34:38 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import java.io.PrintStream;

/**
 * The OutputDevice to write to.
 */
public class PrintStreamOutputDevice implements OutputDevice {
    private final PrintStream _outStream;

    public PrintStreamOutputDevice(PrintStream out) {
        _outStream = out;
    }

    public void flush()            {  _outStream.flush();  }
    public void write(byte[] buffer, int off, int len) {
        _outStream.write(buffer, off, len);
    }
    public void print(String s)    { _outStream.print(s);  }
    public void println(String s)  { _outStream.println(s);  }
    public void println()          { _outStream.println(); }

    public void close() { _outStream.close(); }

    public void attributeBold()  { /* no attributes */ }
    public void attributeGrey()  { /* no attributes */ }

    public void attributeReset() { /* no attributes */ }
}
