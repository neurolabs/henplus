/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Iterator;

import henplus.Interruptable;
import henplus.SQLSession;
import henplus.AbstractCommand;
import henplus.CommandDispatcher;
import henplus.SigIntHandler;
import henplus.util.Terminal;

/**
 * This command executes stuff on the shell. Supports the most common
 * shell commands for convenience.
 */
public class ShellCommand 
    extends AbstractCommand
    implements Interruptable 
{
    Thread _myThread;

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "system", "!"
	};
    }

    /**
     * shell commands always have the semicolon as special character.
     */
    /*
    public boolean isComplete(String command) {
	return (!command.endsWith(";"));
    }
    */

    public boolean requiresValidSession(String cmd) { return false; }

    /**
     * filename completion by default.
     */
    public Iterator complete(CommandDispatcher disp, String partialCommand, 
		      String lastWord) {
	return new FileCompletionIterator(partialCommand, lastWord);
    }

    public void interrupt() {
	_myThread.interrupt();
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String cmd, String param) {
	if (param.trim().length() == 0) {
	    return SYNTAX_ERROR;
	}
	Process   p = null;
	IOHandler ioHandler = null;
	int exitStatus = -1;
	_myThread = Thread.currentThread();
	SigIntHandler.getInstance().registerInterrupt(this);
	try {
	    try {
		p = Runtime.getRuntime().exec(new String[] { "sh", "-c",
							     param });
		ioHandler = new IOHandler(p);
	    }
	    catch (IOException e) {
		ioHandler.stop();
		return EXEC_FAILED;
	    }
	    
	    exitStatus = p.waitFor();
	}
	catch (InterruptedException e) {
	    p.destroy();
	    System.err.println("Shell command interrupted.");
	}
	ioHandler.stop();
	Terminal.grey(System.err);
	System.err.println("[exit "+ exitStatus + "]");
	Terminal.reset(System.err);
	return SUCCESS;
    }

    // -- description
    public String getShortDescription() {
	return "execute system commands";
    }

    public String getSynopsis(String cmd) {
	return cmd + " <system-shell-commandline>";
    }

    /**
     * provide a long description just in case the user types
     * 'help ls'.
     */
    public String getLongDescription(String cmd) {
	return "\tExecute a system command in the shell. You can only invoke\n"
	    +  "\tcommands,  that do  not expect  anything  from  stdin: the\n"
	    +  "\tinteractive  input  from HenPlus  is disconnected from the\n"
	    +  "\tsubprocess' stdin. But this is useful to call some small\n"
	    +  "\tcommands in the middle of the sesion. Like 'ls':\n"
	    +  "\t!ls";
    }
    
    //-------- Helper class to handle the output of an process.
    
    /**
     * The output handler handles the output streams from the process.
     */
    private static class IOHandler {
	//private final Thread stdinThread;
	private final Thread stdoutThread;
	private final Thread stderrThread;
	private final Process process;
	private volatile boolean running;

	public IOHandler(Process p) throws IOException {
	    this.process = p;
	    stdoutThread = new Thread(new CopyWorker(p.getInputStream(), 
						     System.out));
	    stdoutThread.setDaemon(true);
	    stderrThread = new Thread(new CopyWorker(p.getErrorStream(),
						     System.err));
	    stderrThread.setDaemon(true);
	    /*
	    stdinThread = new Thread(new CopyWorker(System.in,
						    p.getOutputStream()));
	    stdinThread.setDaemon(true);
	    */
	    p.getOutputStream().close();
	    running = true;
	    start();
	}
	
	private void start() {
	    stdoutThread.start();
	    stderrThread.start();
	    //stdinThread.start();
	}

	public void stop() {
            synchronized (this) {
                running = false;
            }
	    //stdinThread.interrupt(); // this does not work for blocked IO!
	    try { stdoutThread.join(); } catch(InterruptedException e) {}
	    try { stderrThread.join(); } catch(InterruptedException e) {}
	    //try { stdinThread.join();  } catch(InterruptedException e) {}
	    try { process.getInputStream().close(); } catch (IOException e) {}
	    try { process.getErrorStream().close(); } catch (IOException e) {}
	}

	/**
	 * Thread, that copies from an input stream to an output stream
	 * until EOF is reached.
	 */
	private class CopyWorker implements Runnable {
	    InputStream  source;
	    OutputStream dest;
	    
	    public CopyWorker(InputStream source, OutputStream dest) {
		this.source = source;
		this.dest   = dest;
	    }
	    
	    public void run() {
		byte[] buf = new byte [ 256 ];
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
		}
		catch (IOException ignore_me) {
		}
	    }
	}
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
