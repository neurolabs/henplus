/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: HenPlus.java,v 1.25 2002-02-15 00:02:25 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;

import java.io.File;
import java.io.IOException;
import java.io.EOFException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import henplus.commands.*;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;

public class HenPlus {
    public static final boolean verbose = false; // debug.
    private static final String EXIT_MSG   = "good bye.";
    private static final String HENPLUSDIR = ".henplus";
    private static final String PROMPT     = "Hen*Plus> ";

    public static final byte LINE_EXECUTED   = 1;
    public static final byte LINE_EMPTY      = 2;
    public static final byte LINE_INCOMPLETE = 3;
    
    private static HenPlus instance = null; // singleton.
    
    private CommandDispatcher dispatcher;
    private SQLSession        session;
    private boolean           terminated;
    private String            prompt;
    private String            emptyPrompt;
    private boolean           _alreadyShutDown;
    private SetCommand        _settingStore;
    private boolean           _fromTerminal;
    private BufferedReader    _fileReader;
    private boolean           _optQuiet;
    private SQLStatementSeparator _commandSeparator;

    private HenPlus(String argv[]) throws IOException {
	terminated = false;
	_alreadyShutDown = false;
	_commandSeparator = new SQLStatementSeparator();
	
	try {
	    Readline.load(ReadlineLibrary.GnuReadline);
	    System.err.println("using GNU readline.");
	} catch (UnsatisfiedLinkError ignore_me) {
	    System.err.println("no readline found ("
			       + ignore_me.getMessage()
			       + "). Using simple stdin.");
	}
	Readline.initReadline("HenPlus");
	try {
	    Readline.readHistoryFile(getHistoryLocation());
	}
	catch (Exception ignore) {}
	
	Readline.setWordBreakCharacters(" ");
	_fromTerminal = Readline.hasTerminal();
	if (!_fromTerminal) {
	    System.err.println("input not a terminal; disabling TAB-completion");
	}

	_settingStore = new SetCommand(this);
	dispatcher = new CommandDispatcher(_settingStore);
	dispatcher.register(new HelpCommand());
	dispatcher.register(new DescribeCommand());
	dispatcher.register(new SQLCommand());
	dispatcher.register(new ListUserObjectsCommand());
	dispatcher.register(new ExportCommand());
	dispatcher.register(new ImportCommand());
	dispatcher.register(new ShellCommand());
	dispatcher.register(new EchoCommand());
	dispatcher.register(new ExitCommand());
	dispatcher.register(new StatusCommand());
	dispatcher.register(new ConnectCommand( argv, this ));
	dispatcher.register(new LoadCommand());
	dispatcher.register(new DriverCommand(this));
	dispatcher.register(new AutocommitCommand()); // replace with 'set'
	dispatcher.register(_settingStore);
	Readline.setCompleter( dispatcher );
	setDefaultPrompt();
	// in case someone presses Ctrl-C
	try {
	    Runtime.getRuntime()
		.addShutdownHook(new Thread() {
			public void run() {
			    shutdown();
			}
		    });
	}
	catch (NoSuchMethodError e) {
	    // compiled with jdk >= 1.3, executed with <= 1.2.x
	    System.err.println("== This JDK is OLD. ==");
	    System.err.println(" - No final save on CTRL-C supported.");
	    System.err.println(" - and if your shell is broken after use of henplus: same reason.");
	    System.err.println("Bottomline: update your JDK (>= 1.3)!");
	}
    }
    
    public void pushBuffer() {
	_commandSeparator.push();
    }

    public void popBuffer() {
	_commandSeparator.pop();
    }

    public String readlineFromFile() throws IOException {
	if (_fileReader == null) {
	    _fileReader = new BufferedReader(new InputStreamReader(System.in));
	}
	String line = _fileReader.readLine();
	if (line == null) {
	    throw new EOFException("EOF");
	}
	return (line.length() == 0) ? null : line;
    }

    /**
     * add a new line. returns true if the line completes a command.
     */
    public byte addLine(String line) {
	byte result = LINE_EMPTY;
	/*
	 * special oracle comment 'rem'ark; should be in the comment parser.
	 */
	int startWhite = 0;
	while (startWhite < line.length() 
	       && Character.isWhitespace(line.charAt(startWhite))) {
	    ++startWhite;
	}
	if (line.length() >= (3 + startWhite)
	    && (line.substring(startWhite,startWhite+3)
		.toUpperCase()
		.equals("REM"))
	    && (line.length() == 3 || Character.isWhitespace(line.charAt(3))))
	    {
		return LINE_EMPTY;
	    }

	StringBuffer lineBuf = new StringBuffer(line);
	lineBuf.append('\n');
	_commandSeparator.append(lineBuf.toString());
	result = LINE_INCOMPLETE;
	while (_commandSeparator.hasNext()) {
	    String completeCommand = _commandSeparator.next();
	    //System.err.println(">'" + completeCommand + "'<");
	    completeCommand = varsubst(completeCommand,
				       _settingStore.getVariableMap());
	    Command c = dispatcher.getCommandFrom(completeCommand);
	    if (c == null) {
		_commandSeparator.consumed();
		result = LINE_EMPTY;
	    }
	    else if(!c.isComplete(completeCommand)) {
		_commandSeparator.cont();
		result = LINE_INCOMPLETE;
	    }
	    else {
		//System.err.println("SUBST: " + completeCommand);
		dispatcher.execute(session, completeCommand);
		_commandSeparator.consumed();
		result = LINE_EXECUTED;
	    }
	}
	return result;
    }

    public void run() {
	String cmdLine = null;
	String displayPrompt = prompt;
	while (!terminated) {
	    try {
		cmdLine = (_fromTerminal)
		    ? Readline.readline( displayPrompt )
		    : readlineFromFile();
	    }
	    catch (EOFException e) {
		if (session != null) {
		    dispatcher.execute(session, "disconnect");
		    displayPrompt = prompt;
		    continue;
		}
		else {
		    break; // last session closed.
		}
	    }
	    catch (Exception e) {
		if (verbose) e.printStackTrace();
	    }
	    if (cmdLine == null)
		continue;
	    if (addLine(cmdLine) == LINE_INCOMPLETE) {
		displayPrompt = emptyPrompt;
	    }
	    else {
		displayPrompt = prompt;
	    }
	}
    }
    
    private void shutdown() {
	if (_alreadyShutDown) {
	    return;
	}
	System.err.println("storing settings..");
	try {
	    if (dispatcher != null) {
		dispatcher.shutdown();
	    }
	    try {
		Readline.writeHistoryFile(getHistoryLocation());
	    }
	    catch (Exception ignore) {}
	    Readline.cleanup();
	}
	finally {
	    _alreadyShutDown = true;
	}
    }

    public void terminate() {
	terminated = true;
    }
    public CommandDispatcher getDispatcher() { return dispatcher; }
    
    public void setSession(SQLSession session) {
	this.session = session;
    }

    public SQLSession getSession() {
	return session;
    }

    public void setPrompt(String prompt) {
	this.prompt = prompt;
	StringBuffer tmp = new StringBuffer();
	for (int i=prompt.length(); i > 0; --i) {
	    tmp.append(' ');
	}
	emptyPrompt = tmp.toString();
    }
    
    public void setDefaultPrompt() {
	setPrompt( PROMPT );
    }

    public String varsubst (String in, Map variables) {
        int pos             = 0;
        int endpos          = 0;
	int startVar        = 0;
        StringBuffer result = new StringBuffer();
        String      varname;
        boolean     hasBrace= false;
        boolean     knownVar= false;
        
        if (in == null) {
            return null;
        }
        
        if (variables == null) {
            return in;
        }
        
        while ((pos = in.indexOf ('$', pos)) >= 0) {
	    startVar = pos;
            if (in.charAt(pos+1) == '$') { // quoting '$'
                pos++;
                continue;
            }
            
            hasBrace = (in.charAt(pos+1) == '{');
            
            // text between last variable and here
            result.append(in.substring (endpos, pos));
            
            if (hasBrace) {
                pos++;
            }

            endpos = pos+1;
            while (endpos < in.length() 
                   && Character.isJavaIdentifierPart(in.charAt(endpos))) {
                endpos++;
            }
            varname=in.substring(pos+1,endpos);
         
            if (hasBrace) {
                while (endpos < in.length() && in.charAt(endpos) != '}') {
                    ++endpos;
                }
                ++endpos;
            }
	    if (endpos >= in.length()) {
		if (variables.containsKey(varname)) {
		    System.err.println("warning: missing '}' for variable '"
				       + varname + "'.");
		}
		result.append(in.substring(startVar));
		break;
	    }

            if (variables.containsKey(varname)) {
		result.append(variables.get(varname));
	    }
	    else {
		System.err.println("warning: variable '" 
				   + varname + "' not set.");
		result.append(in.substring(startVar, endpos));
	    }
   
            pos = endpos;
        }
	if (endpos < in.length()) {
	    result.append(in.substring(endpos));
	}
        return result.toString();
    }
    
    //*****************************************************************
    public static HenPlus getInstance() {
	return instance;
    }

    public static final void main(String argv[]) throws Exception {
	String cpy;
	cpy = 
"-------------------------------------------------------------------------\n"
+" HenPlus II 0.1 Copyright(C) 1997, 2001 Henner Zeller <H.Zeller@acm.org>\n"
+" HenPlus is provided AS IS and comes with ABSOLUTELY NO WARRANTY\n"
+" This is free software, and you are welcome to redistribute it under the\n"
+" conditions of the GNU Public License <http://www.gnu.org/>\n"
+"----------------------------------------------------[$Revision: 1.25 $]--\n";
	System.err.println(cpy);

	instance = new HenPlus(argv);
	instance.run();
	instance.shutdown();
	System.err.println( EXIT_MSG );
    }

    public File getConfigDir() {
	/*
	 * test local directory.
	 */
	File henplusDir = new File( HENPLUSDIR );
	if (henplusDir.exists() && henplusDir.isDirectory()) {
	    return henplusDir;
	}

	/*
	 * fallback: home directory.
	 */
	String homeDir = System.getProperty("user.home", ".");
	henplusDir = new File(homeDir + File.separator + HENPLUSDIR);
	if (!henplusDir.exists()) {
	    henplusDir.mkdir();
	}
	return henplusDir;
    }

    private String getHistoryLocation() {
	return getConfigDir().getAbsolutePath() + File.separator + "history";
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
