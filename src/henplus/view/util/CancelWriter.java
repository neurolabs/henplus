/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: CancelWriter.java,v 1.1 2005-03-24 13:57:46 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.view.util;

import henplus.OutputDevice;

/**
 * Little utility that allows to write a string to the
 * screen and cancel it afterwards (with Backspaces). Will only
 * write, if the Output is indeed a terminal.
 */
public final class CancelWriter {
    private final static String BACKSPACE = "\b";

    private final OutputDevice _out;
    private final boolean _doWrite;

    /** the string that has been written. 'null', if
     * nothing has been written or it is cancelled 
     */
    private String _writtenString;

    public CancelWriter(OutputDevice out) {
        _out = out;
        _doWrite = _out.isTerminal();
        _writtenString = null;
    }

    /**
     * returns, wether this cancel writer will print
     * anything. Depends on the fact, that the output
     * is a terminal.
     */
    public boolean isPrinting() {
        return _doWrite;
    }

    /**
     * returns, if this cancel writer has any cancellable
     * output.
     */
    public boolean hasCancellableOutput() {
        return _writtenString != null;
    }

    /**
     * Print message to screen. Cancel out any previous
     * message. If the output is no terminal, do not
     * write anything.
     * @param str string to print. Must not be null.
     */
    public void print(String str) {
        if (!_doWrite) return;
        int charCount = cancel(false);
        _out.print(str);
        _writtenString = str;
        
        /* wipe the difference between the previous
         * message and this one */
        final int lenDiff = charCount - str.length();
        if (lenDiff > 0) {
            writeChars(lenDiff, " ");
            writeChars(lenDiff, BACKSPACE);
        }
    }

    /**
     * cancel out the written string and wipe it
     * with spaces.
     */
    public int cancel() {
        return cancel(true);
    }

    /**
     * cancel the output.
     * @param wipeOut 'true', if the written characters 
     *        should be wiped out with spaces. Otherwise,
     *        the cursor is placed at the beginning of
     *        the string without wiping.
     * @return number of characters cancelled.
     */
    public int cancel(boolean wipeOut) {
        if (_writtenString == null) 
            return 0;
        final int backspaceCount = _writtenString.length();
        writeChars(backspaceCount, BACKSPACE);
        if (wipeOut) {
            writeChars(backspaceCount, " ");
            writeChars(backspaceCount, BACKSPACE );
        }
        _writtenString = null;
        return backspaceCount;
    }

    private void writeChars(int count, String str) {
        for (int i=0; i < count; ++i) {
            _out.print( str );
        }
    }
}
