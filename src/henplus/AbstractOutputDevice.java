/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: AbstractOutputDevice.java,v 1.2 2005-03-24 13:57:45 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

/**
 * An OutputDevice that does nothing.
 */
public abstract class AbstractOutputDevice implements OutputDevice {
    public void flush() { }
    public void write(byte[] buffer, int off, int len) { }
    public void print(String s) { }
    public void println(String s) { }
    public void println() { }

    public void attributeBold() { }
    public void attributeReset() { }
    public void attributeGrey() { }

    public void close() { }
    public boolean isTerminal() { return false; }
}
