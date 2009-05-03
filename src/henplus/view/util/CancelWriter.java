/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: CancelWriter.java,v 1.2 2005-04-27 14:37:15 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.view.util;

import henplus.OutputDevice;

/**
 * Little utility that allows to write a string to the screen and cancel it
 * afterwards (with Backspaces). Will only write, if the Output is indeed a
 * terminal.
 */
public final class CancelWriter {
    private static final String BACKSPACE = "\b";

    private final OutputDevice _out;
    private final boolean _doWrite;

    /**
     * the string that has been written. 'null', if nothing has been written or
     * it is cancelled
     */
    private String _writtenString;

    public CancelWriter(final OutputDevice out) {
        _out = out;
        _doWrite = _out.isTerminal();
        _writtenString = null;
    }

    /**
     * returns, wether this cancel writer will print anything. Depends on the
     * fact, that the output is a terminal.
     */
    public boolean isPrinting() {
        return _doWrite;
    }

    /**
     * returns, if this cancel writer has any cancellable output.
     */
    public boolean hasCancellableOutput() {
        return _writtenString != null;
    }

    /**
     * Print message to screen. Cancel out any previous message. If the output
     * is no terminal, do not write anything.
     * 
     * @param str
     *            string to print. Must not be null.
     */
    public void print(final String str) {
        if (!_doWrite) {
            return;
        }
        final int charCount = cancel(false);
        _out.print(str);
        _writtenString = str;

        /*
         * wipe the difference between the previous message and this one
         */
        final int lenDiff = charCount - str.length();
        if (lenDiff > 0) {
            writeChars(lenDiff, " ");
            writeChars(lenDiff, BACKSPACE);
        }
        _out.flush();
    }

    /**
     * cancel out the written string and wipe it with spaces.
     */
    public int cancel() {
        return cancel(true);
    }

    /**
     * cancel the output.
     * 
     * @param wipeOut
     *            'true', if the written characters should be wiped out with
     *            spaces. Otherwise, the cursor is placed at the beginning of
     *            the string without wiping.
     * @return number of characters cancelled.
     */
    public int cancel(final boolean wipeOut) {
        if (_writtenString == null) {
            return 0;
        }
        final int backspaceCount = _writtenString.length();
        writeChars(backspaceCount, BACKSPACE);
        if (wipeOut) {
            writeChars(backspaceCount, " ");
            writeChars(backspaceCount, BACKSPACE);
            _out.flush();
        }
        _writtenString = null;
        return backspaceCount;
    }

    private void writeChars(final int count, final String str) {
        for (int i = 0; i < count; ++i) {
            _out.print(str);
        }
    }
}
