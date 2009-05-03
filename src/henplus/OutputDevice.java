/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: OutputDevice.java,v 1.2 2005-03-24 13:57:46 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

/**
 * The OutputDevice to write to.
 */
public interface OutputDevice {
    void flush();

    void write(byte[] buffer, int off, int len);

    void print(String s);

    void println(String s);

    void println();

    void attributeBold();

    void attributeReset();

    void attributeGrey();

    void close();

    boolean isTerminal();
}
