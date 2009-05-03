/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import henplus.event.ExecutionListener;
import henplus.commands.SetCommand;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.gnu.readline.ReadlineCompleter;

/**
 * The Command Dispatcher for all commands.
 */
public class CommandDispatcher implements ReadlineCompleter {
    private static final boolean VERBOSE = false; // debug
    private final List<Command> _commands; // commands in seq. of addition.
    private final SortedMap<String, Command> _commandMap;
    private final SetCommand _setCommand;
    private final List<ExecutionListener> _executionListeners;
    private int _batchCount;

    public CommandDispatcher(final SetCommand sc) {
        _commandMap = new TreeMap<String, Command>();
        _commands = new ArrayList<Command>();
        _executionListeners = new ArrayList<ExecutionListener>();
        _batchCount = 0;
        _setCommand = sc;
        // FIXME: remove cyclic dependency..
        _setCommand.registerLastCommandListener(this);
    }

    /**
     * returns the commands in the sequence they have been added.
     */
    public Iterator getRegisteredCommands() {
        return _commands.iterator();
    }

    /**
     * returns a sorted list of command names.
     */
    public Iterator getRegisteredCommandNames() {
        return _commandMap.keySet().iterator();
    }

    /**
     * returns a sorted list of command names, starting with the first entry
     * matching the key.
     */
    public Iterator getRegisteredCommandNames(final String key) {
        return _commandMap.tailMap(key).keySet().iterator();
    }

    /*
     * if we start a batch (reading from file), the commands are not shown,
     * except the commands that failed.
     */
    public void startBatch() {
        ++_batchCount;
    }

    public void endBatch() {
        --_batchCount;
    }

    public boolean isInBatch() {
        return _batchCount > 0;
    }

    public void register(final Command c) {
        _commands.add(c);
        final String[] cmdStrings = c.getCommandList();
        for (int i = 0; i < cmdStrings.length; ++i) {
            if (_commandMap.containsKey(cmdStrings[i])) {
                throw new IllegalArgumentException(
                        "attempt to register command '" + cmdStrings[i]
                                                                     + "', that is already used");
            }
            _commandMap.put(cmdStrings[i], c);
        }
    }

    // methods to make aliases work.
    public boolean containsCommand(final String cmd) {
        return _commandMap.containsKey(cmd);
    }

    public void registerAdditionalCommand(final String cmd, final Command c) {
        _commandMap.put(cmd, c);
    }

    public void unregisterAdditionalCommand(final String cmd) {
        _commandMap.remove(cmd);
    }

    /**
     * unregister command. This is an 'expensive' operation, since we go through
     * the internal list until we find the command and remove it. But since the
     * number of commands is low and this is a rare operation (the
     * plugin-mechanism does this) .. we don't care.
     */
    public void unregister(final Command c) {
        _commands.remove(c);
        final Iterator entries = _commandMap.entrySet().iterator();
        while (entries.hasNext()) {
            final Map.Entry e = (Map.Entry) entries.next();
            if (e.getValue() == c) {
                entries.remove();
            }
        }
    }

    /**
     * extracts the command from the commandstring. This even works, if there is
     * not delimiter between the command and its arguments (this is esp. needed
     * for the commands '?', '!', '@' and '@@').
     */
    public String getCommandNameFrom(final String completeCmd) {
        if (completeCmd == null || completeCmd.length() == 0) {
            return null;
        }
        final String cmd = completeCmd.toLowerCase();
        final String startChar = cmd.substring(0, 1);
        final Iterator it = getRegisteredCommandNames(startChar);
        String longestMatch = null;
        while (it.hasNext()) {
            final String testMatch = (String) it.next();
            if (cmd.startsWith(testMatch)) {
                longestMatch = testMatch;
            } else if (!testMatch.startsWith(startChar)) {
                break; // ok, thats it.
            }
        }
        // ok, fallback: grab the first whitespace delimited part.
        if (longestMatch == null) {
            final StringTokenizer tok = new StringTokenizer(completeCmd, " ;\t\n\r\f");
            if (tok.hasMoreElements()) {
                return (String) tok.nextElement();
            }
        }
        return longestMatch;
    }

    public Command getCommandFrom(final String completeCmd) {
        return getCommandFromCooked(getCommandNameFrom(completeCmd));
    }

    private Command getCommandFromCooked(final String completeCmd) {
        if (completeCmd == null) {
            return null;
        }
        Command c = _commandMap.get(completeCmd);
        if (c == null) {
            c = _commandMap.get(""); // "" matches everything.
        }
        return c;
    }

    public void shutdown() {
        final Iterator i = _commands.iterator();
        while (i.hasNext()) {
            final Command c = (Command) i.next();
            try {
                c.shutdown();
            } catch (final Exception e) {
                if (VERBOSE) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Add an execution listener that is informed whenever a command is
     * executed.
     * 
     * @param listener
     *            an Execution Listener
     */
    public void addExecutionListener(final ExecutionListener listener) {
        if (!_executionListeners.contains(listener)) {
            _executionListeners.add(listener);
        }
    }

    /**
     * remove an execution listener.
     * 
     * @param listener
     *            the execution listener to be removed
     * @return true, if this has been successful.
     */
    public boolean removeExecutionListener(final ExecutionListener listener) {
        return _executionListeners.remove(listener);
    }

    private void informBeforeListeners(final SQLSession session, final String cmd) {
        final Iterator it = _executionListeners.iterator();
        while (it.hasNext()) {
            final ExecutionListener listener = (ExecutionListener) it.next();
            listener.beforeExecution(session, cmd);
        }
    }

    private void informAfterListeners(final SQLSession session, final String cmd, final int result) {
        final Iterator it = _executionListeners.iterator();
        while (it.hasNext()) {
            final ExecutionListener listener = (ExecutionListener) it.next();
            listener.afterExecution(session, cmd, result);
        }
    }

    /**
     * execute the command given. This strips whitespaces and trailing
     * semicolons and calls the Command class.
     */
    public void execute(final SQLSession session, final String givenCommand) {
        if (givenCommand == null) {
            return;
        }

        // remove trailing ';' and whitespaces.
        final StringBuffer cmdBuf = new StringBuffer(givenCommand.trim());
        int i = 0;
        for (i = cmdBuf.length() - 1; i > 0; --i) {
            final char c = cmdBuf.charAt(i);
            if (c != ';' && !Character.isWhitespace(c)) {
                break;
            }
        }
        if (i < 0) {
            return;
        }
        cmdBuf.setLength(i + 1);
        final String cmd = cmdBuf.toString();
        // System.err.println("## '" + cmd + "'");
        final String cmdStr = getCommandNameFrom(cmd);
        final Command c = getCommandFromCooked(cmdStr);
        // System.err.println("name: "+ cmdStr + "; c=" + c);
        if (c != null) {
            try {
                final String params = cmd.substring(cmdStr.length());
                if (session == null && c.requiresValidSession(cmdStr)) {
                    HenPlus.msg().println("not connected.");
                    return;
                }

                int result;
                informBeforeListeners(session, givenCommand);
                result = c.execute(session, cmdStr, params);
                informAfterListeners(session, givenCommand, result);

                switch (result) {
                case Command.SYNTAX_ERROR: {
                    final String synopsis = c.getSynopsis(cmdStr);
                    if (synopsis != null) {
                        HenPlus.msg().println("usage: " + synopsis);
                    } else {
                        HenPlus.msg().println("syntax error.");
                    }
                }
                break;
                case Command.EXEC_FAILED: {
                    /*
                     * if we are in batch mode, then no message is written to
                     * the screen by default. Thus we don't know, _what_ command
                     * actually failed. So in this case, write out the offending
                     * command.
                     */
                    if (isInBatch()) {
                        HenPlus.msg().println("-- failed command: ");
                        HenPlus.msg().println(givenCommand);
                    }
                }
                break;
                default:
                    /* nope */
                }
            } catch (final Throwable e) {
                if (VERBOSE) {
                    e.printStackTrace();
                }
                HenPlus.msg().println(e.toString());
                informAfterListeners(session, givenCommand, Command.EXEC_FAILED);
            }
        }
    }

    private Iterator possibleValues;
    private String variablePrefix;

    // -- Readline completer ..
    public String completer(String text, final int state) {
        final HenPlus henplus = HenPlus.getInstance();
        final String completeCommandString = henplus.getPartialLine().trim();
        boolean variableExpansion = false;

        /*
         * ok, do we have a variable expansion ?
         */
        int pos = text.length() - 1;
        while (pos > 0 && text.charAt(pos) != '$'
                && Character.isJavaIdentifierPart(text.charAt(pos))) {
            --pos;
        }
        // either $... or ${...
        if (pos >= 0 && text.charAt(pos) == '$') {
            variableExpansion = true;
        } else if (pos >= 1 && text.charAt(pos - 1) == '$'
            && text.charAt(pos) == '{') {
            variableExpansion = true;
            --pos;
        }

        if (variableExpansion) {
            if (state == 0) {
                variablePrefix = text.substring(0, pos);
                final String varname = text.substring(pos);
                possibleValues = _setCommand.completeUserVar(varname);
            }
            if (possibleValues.hasNext()) {
                return variablePrefix + (String) possibleValues.next();
            }
            return null;
        }
        /*
         * the first word.. the command.
         */
        else if (completeCommandString.equals(text)) {
            text = text.toLowerCase();
            if (state == 0) {
                possibleValues = getRegisteredCommandNames(text);
            }
            while (possibleValues.hasNext()) {
                final String nextKey = (String) possibleValues.next();
                if (nextKey.length() == 0) {
                    continue;
                }
                if (text.length() < 1) {
                    final Command c = _commandMap.get(nextKey);
                    if (!c.participateInCommandCompletion()) {
                        continue;
                    }
                    if (c.requiresValidSession(nextKey)
                            && henplus.getCurrentSession() == null) {
                        continue;
                    }
                }
                if (nextKey.startsWith(text)) {
                    return nextKey;
                }
                return null;
            }
            return null;
        }
        /*
         * .. otherwise get completion from the specific command.
         */
        else {
            if (state == 0) {
                final Command cmd = getCommandFrom(completeCommandString);
                if (cmd == null) {
                    return null;
                }
                possibleValues = cmd
                .complete(this, completeCommandString, text);
            }
            if (possibleValues != null && possibleValues.hasNext()) {
                return (String) possibleValues.next();
            }
            return null;
        }
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
