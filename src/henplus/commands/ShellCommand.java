/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package commands;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Iterator;

import SQLSession;
import AbstractCommand;
import CommandDispatcher;

/**
 * This command executes stuff on the shell. Supports the most common
 * shell commands for convenience.
 */
public class ShellCommand extends AbstractCommand {
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "system"
	};
    }

    /**
     * shell commands always have the semicolon as special character.
     */
    public boolean isComplete(String command) {
	return (!command.endsWith(";"));
    }

    public boolean requiresValidSession(String cmd) { return false; }

    /**
     * filename completion by default.
     */
    public Iterator complete(CommandDispatcher disp, String partialCommand, 
		      String lastWord) {
	return new FileCompletionIterator(lastWord);
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String command) {
	int argc = argumentCount(command);
	if (argc == 1)
	    return SYNTAX_ERROR;
	command = command.substring("system".length()); // cut off 'system'
	Process p = null;
	IOHandler  ioHandler = null;
	try {
	    p = Runtime.getRuntime().exec(new String[] { "sh", "-c",
							 command });
	    ioHandler = new IOHandler(p);
	}
	catch (IOException e) {
	    ioHandler.stop();
	    return EXEC_FAILED;
	}
	for (;;) {
	    try {
		p.waitFor();
		break;
	    }
	    catch (InterruptedException e) {
		continue;
	    }
	}
	ioHandler.stop();
	return SUCCESS;
    }

    // -- description
    public String getShortDescription() {
	return "execute system commands";
    }

    public String getSynopsis(String cmd) {
	return "system <system-shell-commandline>";
    }

    /**
     * provide a long description just in case the user types
     * 'help ls'.
     */
    public String getLongDescription(String cmd) {
	return "\tExecute a system command in the shell. You can only invoke\n"
	    +  "\tcommands,  that do  not expect  anything  from  stdin: the\n"
	    +  "\tinteractive  input  from HenPlus  is disconnected from the\n"
	    +  "\tsubprocess' stdin.";
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
	private boolean running;

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
	    running = false;
	    //stdinThread.interrupt(); // this does not work for blocked IO!
	    try { stdoutThread.join(); } catch(InterruptedException e) {}
	    try { stderrThread.join(); } catch(InterruptedException e) {}
	    //try { stdinThread.join();  } catch(InterruptedException e) {}
	    System.out.flush();
	    System.err.flush();
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
		    while (running && (r = source.read(buf)) > 0) {
			dest.write(buf, 0, r);
		    }
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
