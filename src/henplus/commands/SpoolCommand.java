/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SpoolCommand.java,v 1.2 2004-01-28 09:25:49 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.SQLSession;
import henplus.HenPlus;
import henplus.AbstractCommand;
import java.util.Stack;
import java.io.PrintStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * prepared ..
 */
public final class SpoolCommand extends AbstractCommand {
    private final Stack/*<PrintStream>*/ _outputStack;

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "spool"
	};
    }
    
    public SpoolCommand(HenPlus hp) {
        _outputStack = new Stack/*<PrintStream>*/();
        _outputStack.push(hp.getOutput());
    }

    public boolean requiresValidSession(String cmd) { return false; }

    /**
     * execute the command given.
     */
    public int execute(SQLSession currentSession, String cmd, String param) {
	System.err.println("[ignoring spool command.]");
	return SUCCESS;
    }
    
    private void openSpool(String filename) throws IOException {
        // open file
        OutputStream newOut = new FileOutputStream(filename);
        OutputStream origOut = (OutputStream) _outputStack.peek();
        PrintStream stream=new PrintStream(new StackStream(origOut, newOut));
        _outputStack.push(stream);
        HenPlus.getInstance().setOutput(stream);
    }

    private boolean closeSpool() throws IOException {
        if (_outputStack.size() == 1) {
            HenPlus.msg().println("no open spool.");
            return false;
        }
        PrintStream toClose = (PrintStream) _outputStack.pop();
        toClose.close();
        HenPlus.getInstance().setOutput((PrintStream) _outputStack.peek());
        return true;
    }

    public String getLongDescription(String cmd) { 
	String dsc;
	dsc= "\tThis command does nothing (yet). For now, it is only\n"
	    +"\tthere to work with Oracle SQLplus scripts. But you are\n"
	    +"\tfree to implement it; be part of the henplus team:\n"
	    +"\thttp://www.sourceforge.net/projects/henplus";
	return dsc;
    }

    /**
     * A stream that writes to two output streams. On close, only
     * the second, stacked stream is closed.
     */
    private static class StackStream extends OutputStream {
        private final OutputStream _a;
        private final OutputStream _b;

        public StackStream(OutputStream a, OutputStream b) {
            _a = a;
            _b = b;
        }
        
        /** closes _only_ the (stacked) second stream */
        public void close() throws IOException { 
            _b.close();
        }

        public void flush() throws IOException {
            _a.flush();
            _b.flush();
        }

        public void write(int b) throws IOException {
            _a.write(b);
            _b.write(b);
        }

        public void write(byte[] buf) throws IOException {
            _a.write(buf);
            _b.write(buf);
        }

        public void write(byte[] buf, int off, int len) throws IOException {
            _a.write(buf, off, len);
            _b.write(buf, off, len);
        }
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
