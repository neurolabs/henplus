/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: HenPlus.java,v 1.54 2003-02-01 13:18:42 hzeller Exp $
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
import henplus.util.Terminal;

public class HenPlus implements Interruptable {
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
    private File              _configDir;
    private boolean           _alreadyShutDown;
    private SetCommand        _settingStore;
    private boolean           _fromTerminal;
    private BufferedReader    _fileReader;
    private boolean           _optQuiet;
    private SQLStatementSeparator _commandSeparator;
    private final StringBuffer    _historyLine;
    private String             _previousHistoryLine;
    private boolean            _quiet;
    private volatile boolean   _interrupted;

    private HenPlus(String argv[]) throws IOException {
	terminated = false;
	_alreadyShutDown = false;
	_quiet = false;
	_commandSeparator = new SQLStatementSeparator();
	_historyLine = new StringBuffer();
	boolean readlineLoaded = false;
	// read options .. like -q

	try {
	    Readline.load(ReadlineLibrary.GnuReadline);
	    readlineLoaded = true;
	} 
        catch (UnsatisfiedLinkError ignore_me) {
	    System.err.println("no readline found ("
			       + ignore_me.getMessage()
			       + "). Using simple stdin.");
	}


	_fromTerminal = Readline.hasTerminal();
	if (!_fromTerminal && !_quiet) {
	    System.err.println("reading from stdin");
	}
	_quiet = _quiet || !_fromTerminal; // not from terminal: always quiet.

	// output of special characters.
	Terminal.setTerminalAvailable(_fromTerminal);

	if (!_quiet) {
	    System.err.println("using GNU readline (Brian Fox, Chet Ramey), Java wrapper by Bernhard Bablok");
	}
	Readline.initReadline("HenPlus");
	try {
	    HistoryWriter.readReadlineHistory(getHistoryLocation());
	}
	catch (Exception ignore) {}
	
	Readline.setWordBreakCharacters(" ,/()<>=\t\n");
	setDefaultPrompt();

        // fixme: to many cross dependencies of commands now. clean up.
	_settingStore = new SetCommand(this);
	ListUserObjectsCommand objectLister = new ListUserObjectsCommand(this);
	dispatcher = new CommandDispatcher(_settingStore);
	dispatcher.register(new HelpCommand());

        /*
         * this one prints as well the initial copyright header.
         */
	dispatcher.register(new AboutCommand(_quiet));

	dispatcher.register(new ExitCommand());
	dispatcher.register(new EchoCommand());
	PluginCommand pluginCommand = new PluginCommand(this);
	dispatcher.register(pluginCommand);
	dispatcher.register(new DriverCommand(this));
	AliasCommand aliasCommand = new AliasCommand(this);
	dispatcher.register(aliasCommand);
        LoadCommand loadCommand = new LoadCommand();
	dispatcher.register(loadCommand);

	dispatcher.register(new ConnectCommand( argv, this ));
	dispatcher.register(new StatusCommand());

	dispatcher.register(objectLister);
	dispatcher.register(new DescribeCommand(objectLister));
        
        /*** experimental ***/
	dispatcher.register(new TreeCommand(objectLister));

	dispatcher.register(new SQLCommand(objectLister));

	//dispatcher.register(new ImportCommand());
	//dispatcher.register(new ExportCommand());
	dispatcher.register(new DumpCommand(objectLister, loadCommand));

	dispatcher.register(new AutocommitCommand()); // replace with 'set'
        dispatcher.register(new CommentRemoveCommand(this));
	dispatcher.register(new ShellCommand());

	dispatcher.register(new SpoolCommand()); // dummy command
	dispatcher.register(_settingStore);

	pluginCommand.load();
	aliasCommand.load();

	Readline.setCompleter( dispatcher );

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
	/*
	 * if your compiler/system/whatever does not support the sun.misc.*
	 * classes, then just disable this call and the SigIntHandler class.
	 */
	SigIntHandler.install();

        /* TESTING  for ^Z support in the shell.
        sun.misc.SignalHandler stoptest = new sun.misc.SignalHandler () {
                public void handle(sun.misc.Signal sig) {
                    System.out.println("caught: " + sig);
                }
            };
        try {
            sun.misc.Signal.handle(new sun.misc.Signal("TSTP"), stoptest);
        }
        catch (Exception e) {
            // ignore.
        }

        end testing */
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

    private void storeLineInHistory() {
        String line = _historyLine.toString();
        if (!"".equals(line)
            && !line.equals( _previousHistoryLine )) 
        {
            Readline.addToHistory( line );
            _previousHistoryLine = line;
        }
        _historyLine.setLength(0);
    }

    /**
     * add a new line. returns one of LINE_EMPTY, LINE_INCOMPLETE
     * or LINE_EXECUTED.
     */
    public byte executeLine(String line) {
	byte result = LINE_EMPTY;
	/*
	 * special oracle comment 'rem'ark; should be in the comment parser.
         * ONLY if it is on the beginning of the line, no whitespace.
	 */
	int startWhite = 0;
        /*
	while (startWhite < line.length() 
	       && Character.isWhitespace(line.charAt(startWhite))) {
	    ++startWhite;
	}
        */
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
                /*
                 * do not shadow successful executions with the 'line-empty'
                 * message. Background is: when we consumed a command, that
                 * is complete with a trailing ';', then the following newline
                 * would be considered as empty command. So return only the
                 * LINE_EMPTY, if we haven't got a succesfully executed line.
                 */
                if (result != LINE_EXECUTED) {
                    result = LINE_EMPTY;
                }
	    }
	    else if (!c.isComplete(completeCommand)) {
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
    
    public String getPartialLine() {
	return _historyLine.toString() + Readline.getLineBuffer();
    }

    public void run() {
	String cmdLine = null;
	String displayPrompt = prompt;
	while (!terminated) {
	    _interrupted = false;
	    /*
	     * a CTRL-C will not interrupt the current reading
	     * thus it does not make much sense here to interrupt.
	     * WORKAROUND: Print message in the interrupt() method.
	     * TODO: find out, if we can do something that behaves
	     *       like a shell. This requires, that CTRL-C makes
	     *       Readline.readline() return..
	     */
	    SigIntHandler.getInstance().pushInterruptable(this);

	    try {
		cmdLine = (_fromTerminal)
		    ? Readline.readline( displayPrompt, false )
		    : readlineFromFile();
	    }
	    catch (EOFException e) {
		// EOF on CTRL-D
		if (session != null) {
		    dispatcher.execute(session, "disconnect");
		    displayPrompt = prompt;
		    continue;
		}
		else {
		    break; // last session closed -> exit.
		}
	    }
	    catch (Exception e) {
		if (verbose) e.printStackTrace();
	    }

	    SigIntHandler.getInstance().reset();

	    // anyone pressed CTRL-C
	    if (_interrupted) {
		if ((cmdLine == null || cmdLine.trim().length() == 0) 
		    &&  _historyLine.length() == 0) {
		    terminated = true;  // terminate if we press CTRL on empty line.
		}
		_historyLine.setLength(0);
		_commandSeparator.discard();
		displayPrompt = prompt;
		continue;
	    }

	    if (cmdLine == null)
		continue;

	    /*
	     * if there is already some line in the history, then add
	     * newline. But if the only thing we added was a delimiter (';'),
	     * then this would be annoying.
	     */
	    if (_historyLine.length() > 0
		&& !cmdLine.trim().equals(";")) {
		_historyLine.append("\n");
	    }
	    _historyLine.append(cmdLine);
	    byte lineExecState = executeLine(cmdLine);
	    if (lineExecState == LINE_INCOMPLETE) {
		displayPrompt = emptyPrompt;
	    }
	    else {
		displayPrompt = prompt;
	    }
	    if (lineExecState != LINE_INCOMPLETE) {
                storeLineInHistory();
	    }
	}
        SigIntHandler.getInstance().reset();
    }

    /**
     * called at the very end; on signal or called from the shutdown-hook
     */
    private void shutdown() {
	if (_alreadyShutDown) {
	    return;
	}
	if (!_quiet) {
	    System.err.println("storing settings..");
	}
        /*
         * allow hard resetting.
         */
        SigIntHandler.getInstance().reset();
	try {
	    if (dispatcher != null) {
		dispatcher.shutdown();
	    }
	    try {
		HistoryWriter.writeReadlineHistory(getHistoryLocation());
	    }
	    catch (Exception ignore) {}
	    Readline.cleanup();
	}
	finally {
	    _alreadyShutDown = true;
	}
        /*
         * some JDBC-Drivers (notably hsqldb) do some important cleanup
         * (closing open threads, for instance) in finalizers. Force
         * them to do their duty:
         */
        System.gc();
        System.gc();
    }

    public void terminate() {
	terminated = true;
    }

    public CommandDispatcher getDispatcher() { return dispatcher; }
    
    /**
     * set current session. This is called from commands, that switch
     * the sessions (i.e. the ConnectCommand.
     */
    public void setSession(SQLSession session) {
	this.session = session;
    }

    /**
     * get current session.
     */
    public SQLSession getSession() {
	return session;
    }

    public void setPrompt(String p) {
	this.prompt = p;
	StringBuffer tmp = new StringBuffer();
	for (int i=prompt.length(); i > 0; --i) {
	    tmp.append(' ');
	}
	emptyPrompt = tmp.toString();
	// readline won't know anything about these extra characters:
//  	if (_fromTerminal) {
//  	    prompt = Terminal.BOLD + prompt + Terminal.NORMAL;
//  	}
    }
    
    public void setDefaultPrompt() {
	setPrompt( _fromTerminal ? PROMPT : "" );
    }

    public void removeComments(boolean rc) {
        _commandSeparator.removeComments(rc);
    }

    /**
     * substitute the variables in String 'in', that are in the
     * form $VARNAME or ${VARNAME} with the equivalent value that is found
     * in the Map. Return the varsubstituted String.
     * @param in the input string containing variables to be
     *           substituted (with leading $)
     * @param variables the Map containing the mapping from variable name
     *                  to value.
     */
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
		result.append(in.substring (endpos, pos));
		endpos = pos+1;
		pos+= 2;
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
	    if (endpos > in.length()) {
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

    // -- Interruptable interface
    public void interrupt() { 
	// watchout: Readline.getLineBuffer() will cause a segmentation fault!
	Terminal.boldface(System.err);
	System.err.println(" ..discard current line; press [RETURN]");
	Terminal.reset(System.err);
	_interrupted = true; 
    }

    
    //*****************************************************************
    public static HenPlus getInstance() {
	return instance;
    }

    public static final void main(String argv[]) throws Exception {
	instance = new HenPlus(argv);
	instance.run();
	instance.shutdown();
        /*
         * hsqldb does not always stop its log-thread. So do an
         * explicit exit() here.
         */
        System.exit(0);
    }

    public File getConfigDir() {
	if (_configDir != null) {
	    return _configDir;
	}
	/*
	 * test local directory and superdirectories.
	 */
	File dir = (new File(".")).getAbsoluteFile();
	while (dir != null) {
	    _configDir = new File(dir,  HENPLUSDIR );
	    if (_configDir.exists() && _configDir.isDirectory()) {
		break;
	    }
	    else {
		_configDir = null;
	    }
	    dir = dir.getParentFile();
	}

	/*
	 * fallback: home directory.
	 */
	if (_configDir == null) {
	    String homeDir = System.getProperty("user.home", ".");
	    _configDir = new File(homeDir + File.separator + HENPLUSDIR);
	    if (!_configDir.exists()) {
                if (!_quiet) {
                    System.err.println("creating henplus config dir");
                }
		_configDir.mkdir();
	    }
            try {
                /*
                 * Make this directory accessible only for the user
                 * in question.
                 * works only on unix. Ignore Exception other OSes
                 */
                String params[] = new String[] {"chmod", "700", 
                                                _configDir.toString() };
                Runtime.getRuntime().exec( params );
            }
            catch (Exception e) {
                e.printStackTrace();
            }
	}
	_configDir = _configDir.getAbsoluteFile();
	try {
	    _configDir = _configDir.getCanonicalFile();
	} catch (IOException ign) {}
	
	if (!_quiet) {
	    System.err.println("henplus config at " + _configDir);
	}
	return _configDir;
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
