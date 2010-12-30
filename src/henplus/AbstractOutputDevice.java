/*
 * This is free software, licensed under the Gnu Public License (GPL) get a copy from <http://www.gnu.org/licenses/gpl.html> $Id:
 * AbstractOutputDevice.java,v 1.2 2005-03-24 13:57:45 hzeller Exp $ author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

/**
 * An OutputDevice that does nothing.
 */
public abstract class AbstractOutputDevice implements OutputDevice {

    @Override
    public void flush() {
    }

    @Override
    public void write(final byte[] buffer, final int off, final int len) {
    }

    @Override
    public void print(final String s) {
    }

    @Override
    public void println(final String s) {
    }

    @Override
    public void println() {
    }

    @Override
    public void attributeBold() {
    }

    @Override
    public void attributeReset() {
    }

    @Override
    public void attributeGrey() {
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isTerminal() {
        return false;
    }
}
