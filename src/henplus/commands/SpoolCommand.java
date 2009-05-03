/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SpoolCommand.java,v 1.6 2005-06-18 04:58:13 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.SQLSession;
import henplus.HenPlus;
import henplus.AbstractCommand;
import henplus.OutputDevice;
import henplus.PrintStreamOutputDevice;

import java.util.Stack;
import java.util.Date;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * prepared ..
 */
public final class SpoolCommand extends AbstractCommand {
    private final Stack<OutputDevice> _outStack;
    private final Stack<OutputDevice> _msgStack;

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
        return new String[] { "spool" };
    }

    public SpoolCommand(final HenPlus hp) {
        _outStack = new Stack<OutputDevice> ();
        _msgStack = new Stack<OutputDevice> ();
        _outStack.push(hp.getOutputDevice());
        _msgStack.push(hp.getMessageDevice());
    }

    @Override
    public boolean requiresValidSession(final String cmd) {
        return false;
    }

    /**
     * execute the command given.
     */
    public int execute(final SQLSession currentSession, final String cmd, String param) {
        param = param.trim();
        try {
            if ("off".equals(param.toLowerCase())) {
                closeSpool();
            } else if (param.length() > 0) {
                openSpool(param);
            } else {
                return SYNTAX_ERROR;
            }
        } catch (final Exception e) {
            e.printStackTrace();
            return EXEC_FAILED;
        }
        return SUCCESS;
    }

    /**
     * combine the current output from the stack with the given output, use this
     * as current output and return it.
     */
    private OutputDevice openStackedDevice(final Stack<OutputDevice> stack,
            final OutputDevice newOut) {
        final OutputDevice origOut = stack.peek();
        final OutputDevice outDevice = new StackedDevice(origOut, newOut);
        stack.push(outDevice);
        return outDevice;
    }

    /**
     * close the top device on the stack and return the previous.
     */
    private OutputDevice closeStackedDevice(final Stack<OutputDevice> stack) {
        final OutputDevice out = stack.pop();
        out.close();
        return stack.peek();
    }

    private void openSpool(final String filename) throws IOException {
        // open file
        final OutputDevice spool = new PrintStreamOutputDevice(new PrintStream(
                new FileOutputStream(filename)));
        HenPlus.getInstance().setOutput(openStackedDevice(_outStack, spool),
                openStackedDevice(_msgStack, spool));
        HenPlus.msg().println("-- open spool at " + new Date());
    }

    private boolean closeSpool() throws IOException {
        if (_outStack.size() == 1) {
            HenPlus.msg().println("no open spool.");
            return false;
        }
        HenPlus.msg().println("-- close spool at " + new Date());
        HenPlus.getInstance().setOutput(closeStackedDevice(_outStack),
                closeStackedDevice(_msgStack));
        return true;
    }

    @Override
    public String getShortDescription() {
        return "log output to a file";
    }

    @Override
    public String getSynopsis(final String cmd) {
        return "spool <filename>|off";
    }

    @Override
    public String getLongDescription(final String cmd) {
        String dsc;
        dsc = "\tIf command is followed by a filename, opens a file\n"
            + "\tand writes all subsequent output not only to the terminal\n"
            + "\tbut as well to this file. With\n"
            + "\t spool off\n"
            + "\tspooling is stopped and the file is closed. The spool\n"
            + "\tcommand works recursivly, i.e. you can open more than one \n"
            + "\tfile, and you have to close each of them with 'spool off'\n";
        return dsc;
    }

    /**
     * A stream that writes to two output streams. On close, only the second,
     * stacked stream is closed.
     */
    private static class StackedDevice implements OutputDevice {
        private final OutputDevice _a;
        private final OutputDevice _b;

        public StackedDevice(final OutputDevice a, final OutputDevice b) {
            _a = a;
            _b = b;
        }

        public void flush() {
            _a.flush();
            _b.flush();
        }

        public void write(final byte[] buffer, final int off, final int len) {
            _a.write(buffer, off, len);
            _b.write(buffer, off, len);
        }

        public void print(final String s) {
            _a.print(s);
            _b.print(s);
        }

        public void println(final String s) {
            _a.println(s);
            _b.println(s);
        }

        public void println() {
            _a.println();
            _b.println();
        }

        public void attributeBold() {
            _a.attributeBold();
            _b.attributeBold();
        }

        public void attributeGrey() {
            _a.attributeGrey();
            _b.attributeGrey();
        }

        public void attributeReset() {
            _a.attributeReset();
            _b.attributeReset();
        }

        /** closes _only_ the (stacked) second stream */
        public void close() {
            _b.close();
        }

        public boolean isTerminal() {
            return _a.isTerminal() && _b.isTerminal();
        }
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
