/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import java.io.InputStream;
import java.io.IOException;
import java.util.Iterator;

import henplus.HenPlus;
import henplus.OutputDevice;
import henplus.Interruptable;
import henplus.SQLSession;
import henplus.AbstractCommand;
import henplus.CommandDispatcher;
import henplus.SigIntHandler;

/**
 * This command executes stuff on the shell. Supports the most common shell
 * commands for convenience.
 */
public final class ShellCommand extends AbstractCommand implements Interruptable {
    private Thread _myThread;

    /**
     * @return the command-strings this command can handle.
     */
    public final String[] getCommandList() {
        return new String[] { "system", "!" };
    }

    /**
     * shell commands always have the semicolon as special character.
     */
    /*
     * public boolean isComplete(String command) { return
     * (!command.endsWith(";")); }
     */

    @Override
    public boolean requiresValidSession(final String cmd) {
        return false;
    }

    /**
     * filename completion by default.
     */
    @Override
    public Iterator complete(final CommandDispatcher disp, final String partialCommand,
            final String lastWord) {
        return new FileCompletionIterator(partialCommand, lastWord);
    }

    public void interrupt() {
        _myThread.interrupt();
    }

    /**
     * execute the command given.
     */
    public int execute(final SQLSession session, final String cmd, final String param) {
        if (param.trim().length() == 0) {
            return SYNTAX_ERROR;
        }
        Process p = null;
        IOHandler ioHandler = null;
        int exitStatus = -1;
        _myThread = Thread.currentThread();
        SigIntHandler.getInstance().pushInterruptable(this);
        try {
            try {
                p = Runtime.getRuntime().exec(
                        new String[] { "sh", "-c", param });
                ioHandler = new IOHandler(p);
            } catch (final IOException e) {
                return EXEC_FAILED;
            }

            exitStatus = p.waitFor();
        } catch (final InterruptedException e) {
            p.destroy();
            HenPlus.msg().println("Shell command interrupted.");
        }
        ioHandler.stop();
        HenPlus.msg().attributeGrey();
        HenPlus.msg().println("[exit " + exitStatus + "]");
        HenPlus.msg().attributeReset();
        return SUCCESS;
    }

    // -- description
    @Override
    public String getShortDescription() {
        return "execute system commands";
    }

    @Override
    public String getSynopsis(final String cmd) {
        return cmd + " <system-shell-commandline>";
    }

    /**
     * provide a long description just in case the user types 'help ls'.
     */
    @Override
    public String getLongDescription(final String cmd) {
        return "\tExecute a system command in the shell. You can only invoke\n"
        + "\tcommands,  that do  not expect  anything  from  stdin: the\n"
        + "\tinteractive  input  from HenPlus  is disconnected from the\n"
        + "\tsubprocess' stdin. But this is useful to call some small\n"
        + "\tcommands in the middle of the session. There are two syntaxes\n"
        + "\tsupported: system <command> or even shorter with the\n"
        + "\texclamation mark: !<command>.\n" + "\tExample:\n"
        + "\t!ls";
    }

    // -------- Helper class to handle the output of an process.

    /**
     * The output handler handles the output streams from the process.
     */
    private static class IOHandler {
        // private final Thread stdinThread;
        private final Thread stdoutThread;
        private final Thread stderrThread;
        private final Process process;
        private volatile boolean running;

        public IOHandler(final Process p) throws IOException {
            this.process = p;
            stdoutThread = new Thread(new CopyWorker(p.getInputStream(),
                    HenPlus.out()));
            stdoutThread.setDaemon(true);
            stderrThread = new Thread(new CopyWorker(p.getErrorStream(),
                    HenPlus.msg()));
            stderrThread.setDaemon(true);
            /*
             * stdinThread = new Thread(new CopyWorker(System.in,
             * p.getOutputStream())); stdinThread.setDaemon(true);
             */
            p.getOutputStream().close();
            running = true;
            start();
        }

        private void start() {
            stdoutThread.start();
            stderrThread.start();
            // stdinThread.start();
        }

        public void stop() {
            synchronized (this) {
                running = false;
            }
            // stdinThread.interrupt(); // this does not work for blocked IO!
            try {
                stdoutThread.join();
            } catch (final InterruptedException e) {
            }
            try {
                stderrThread.join();
            } catch (final InterruptedException e) {
            }
            // try { stdinThread.join(); } catch(InterruptedException e) {}
            try {
                process.getInputStream().close();
            } catch (final IOException e) {
            }
            try {
                process.getErrorStream().close();
            } catch (final IOException e) {
            }
        }

        /**
         * Thread, that copies from an input stream to an output stream until
         * EOF is reached.
         */
        private class CopyWorker implements Runnable {
            InputStream source;
            OutputDevice dest;

            public CopyWorker(final InputStream source, final OutputDevice dest) {
                this.source = source;
                this.dest = dest;
            }

            public void run() {
                final byte[] buf = new byte[256];
                int r;
                try {
                    /*
                     * some sort of 'select' would be good here.
                     */
                    while ((running || source.available() > 0)
                            && (r = source.read(buf)) > 0) {
                        dest.write(buf, 0, r);
                    }
                    dest.flush();
                } catch (final IOException ignoreMe) {
                }
            }
        }
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
