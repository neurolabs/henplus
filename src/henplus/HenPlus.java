/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: HenPlus.java,v 1.70 2004-05-31 10:48:21 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import henplus.commands.*;
import henplus.commands.properties.*;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineLibrary;

public class HenPlus implements Interruptable {
    public static final boolean verbose = false; // debug.
    private static final String EXIT_MSG   = "good bye.";
    private static final String HENPLUSDIR = ".henplus";
    private static final String PROMPT     = "Hen*Plus> ";

    public static final byte LINE_EXECUTED   = 1;
    public static final byte LINE_EMPTY      = 2;
    public static final byte LINE_INCOMPLETE = 3;
    
    private static HenPlus instance = null; // singleton.

    private final boolean               _fromTerminal;    
    private final SQLStatementSeparator _commandSeparator;
    private final StringBuffer          _historyLine;

    private final boolean               _quiet;

    private SetCommand             _settingStore;
    private SessionManager         _sessionManager;
    private CommandDispatcher      _dispatcher;
    private PropertyRegistry       _henplusProperties;
    private ListUserObjectsCommand _objectLister; 
    private String                 _previousHistoryLine;
    private boolean                _terminated;
    private String                 _prompt;
    private String                 _emptyPrompt;
    private File                   _configDir;
    private boolean                _alreadyShutDown;
    private BufferedReader         _fileReader;
    private OutputDevice           _output;
    private OutputDevice           _msg;

    private volatile boolean   _interrupted;

    private HenPlus(String argv[]) throws IOException {
	_terminated = false;
	_alreadyShutDown = false;
	boolean quiet = false;

	_commandSeparator = new SQLStatementSeparator();
	_historyLine = new StringBuffer();
	// read options .. like -q

	try {
	    Readline.load(ReadlineLibrary.GnuReadline);
	} 
        catch (UnsatisfiedLinkError ignore_me) {
	    System.err.println("no readline found ("
			       + ignore_me.getMessage()
			       + "). Using simple stdin.");
	}


	_fromTerminal = Readline.hasTerminal();
	if (!_fromTerminal && !quiet) {
	    System.err.println("reading from stdin");
	}
	_quiet = quiet || !_fromTerminal; // not from terminal: always quiet.
        
        if (_fromTerminal) {
            setOutput(new TerminalOutputDevice(System.out),
                      new TerminalOutputDevice(System.err));
        } 
        else {
            setOutput(new PrintStreamOutputDevice(System.out),
                      new PrintStreamOutputDevice(System.err));
        }

	if (!_quiet) {
	    System.err.println("using GNU readline (Brian Fox, Chet Ramey), Java wrapper by Bernhard Bablok");
	}
	Readline.initReadline("HenPlus");
	try {
	    HistoryWriter.readReadlineHistory(getHistoryLocation());
	}
	catch (Exception ignore) { /* ign */ }
	
	Readline.setWordBreakCharacters(" ,/()<>=\t\n"); // TODO..
	setDefaultPrompt();
    }

    public void initializeCommands(String argv[]) {
        _henplusProperties = new PropertyRegistry();
        _henplusProperties
            .registerProperty("comments-remove",
                              _commandSeparator.getRemoveCommentsProperty());

        _sessionManager = SessionManager.getInstance(); 

        // FIXME: to many cross dependencies of commands now. clean up.
	_settingStore = new SetCommand(this);
        _dispatcher = new CommandDispatcher(_settingStore);
	_objectLister = new ListUserObjectsCommand(this);
        _henplusProperties
            .registerProperty("echo-commands", 
                              new EchoCommandProperty(_dispatcher));

	_dispatcher.register(new HelpCommand());

        /*
         * this one prints as well the initial copyright header.
         */
	_dispatcher.register(new AboutCommand(_quiet));

	_dispatcher.register(new ExitCommand());
	_dispatcher.register(new EchoCommand());
	PluginCommand pluginCommand = new PluginCommand(this);
	_dispatcher.register(pluginCommand);
	_dispatcher.register(new DriverCommand(this));
	AliasCommand aliasCommand = new AliasCommand(this);
	_dispatcher.register(aliasCommand);
        if (_fromTerminal) {
            _dispatcher.register(new KeyBindCommand(this));
        }

        LoadCommand loadCommand = new LoadCommand();
	_dispatcher.register(loadCommand);

	_dispatcher.register(new ConnectCommand( argv, this, _sessionManager ));
	_dispatcher.register(new StatusCommand());

	_dispatcher.register(_objectLister);
	_dispatcher.register(new DescribeCommand(_objectLister));
        
	_dispatcher.register(new TreeCommand(_objectLister));

	_dispatcher.register(new SQLCommand(_objectLister, _henplusProperties));

	//_dispatcher.register(new ImportCommand());
	//_dispatcher.register(new ExportCommand());
	_dispatcher.register(new DumpCommand(_objectLister, loadCommand));

	_dispatcher.register(new ShellCommand());
	_dispatcher.register(new SpoolCommand(this));
	_dispatcher.register(_settingStore);

        PropertyCommand propertyCommand;
        propertyCommand = new PropertyCommand(this, _henplusProperties);
        _dispatcher.register(propertyCommand);
        _dispatcher.register(new SessionPropertyCommand(this));
    
    _dispatcher.register( new SystemInfoCommand() );
     
	pluginCommand.load();
	aliasCommand.load();
        propertyCommand.load();

	Readline.setCompleter( _dispatcher );

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

    /** 
     * push the current state of the command execution buffer, e.g.
     * to parse a new file.
     */
    public void pushBuffer() {
	_commandSeparator.push();
    }

    /**
     * pop the command execution buffer.
     */
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
	    Command c = _dispatcher.getCommandFrom(completeCommand);
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
		_dispatcher.execute(_sessionManager.getCurrentSession(), completeCommand);
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
	String displayPrompt = _prompt;
	while (!_terminated) {
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
		if (_sessionManager.getCurrentSession() != null) {
		    _dispatcher.execute(_sessionManager.getCurrentSession(), "disconnect");
		    displayPrompt = _prompt;
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
		    _terminated = true;  // terminate if we press CTRL on empty line.
		}
		_historyLine.setLength(0);
		_commandSeparator.discard();
		displayPrompt = _prompt;
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
		displayPrompt = _emptyPrompt;
	    }
	    else {
		displayPrompt = _prompt;
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
	    if (_dispatcher != null) {
		_dispatcher.shutdown();
	    }
	    try {
		HistoryWriter.writeReadlineHistory(getHistoryLocation());
	    }
	    catch (Exception ignore) { /* ign */ }
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
	   _terminated = true;
    }

    public CommandDispatcher getDispatcher() { return _dispatcher; }
    
    /**
     * Provides access to the session manager. He maintains the list of opened
     * sessions with their names.
     * @return the session manager.
     */
    public SessionManager getSessionManager() {
        return _sessionManager;
    }
    
    /**
     * set current session. This is called from commands, that switch
     * the sessions (i.e. the ConnectCommand.)
     */
    public void setCurrentSession(SQLSession session) {
	   getSessionManager().setCurrentSession(session);
    }

    /**
     * get current session.
     */
    public SQLSession getCurrentSession() {
	   return getSessionManager().getCurrentSession();
    }
    
    public ListUserObjectsCommand getObjectLister() {
        return _objectLister;
    }

    public void setPrompt(String p) {
	_prompt = p;
	StringBuffer tmp = new StringBuffer();
        int emptyLength = p.length();
	for (int i = emptyLength; i > 0; --i) {
	    tmp.append(' ');
	}
	_emptyPrompt = tmp.toString();
	// readline won't know anything about these extra characters:
//  	if (_fromTerminal) {
//  	    prompt = Terminal.BOLD + prompt + Terminal.NORMAL;
//  	}
    }
    
    public void setDefaultPrompt() {
	setPrompt( _fromTerminal ? PROMPT : "" );
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
        getMessageDevice().attributeBold();
	getMessageDevice().print(" ..discard current line; press [RETURN]");
        getMessageDevice().attributeReset();

	_interrupted = true; 
    }

    
    //*****************************************************************
    public static HenPlus getInstance() {
	return instance;
    }
    
    public void setOutput(OutputDevice out, OutputDevice msg) {
        _output = out;
        _msg = msg;
    }
    
    public OutputDevice getOutputDevice() {
        return _output;
    }

    public OutputDevice getMessageDevice() {
        return _msg;
    }

    public static OutputDevice out() {
        return getInstance().getOutputDevice();
    }

    public static OutputDevice msg() {
        return getInstance().getMessageDevice();
    }
    
    public static final void main(String argv[]) throws Exception {
	instance = new HenPlus(argv);
        instance.initializeCommands(argv);
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
                if (verbose) e.printStackTrace();
            }
	}
	_configDir = _configDir.getAbsoluteFile();
	try {
	    _configDir = _configDir.getCanonicalFile();
	} catch (IOException ign) { /* ign */ }
	
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
