/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SetCommand.java,v 1.25 2006-11-29 17:57:53 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.AbstractCommand;
import henplus.CommandDispatcher;
import henplus.HenPlus;
import henplus.SQLSession;
import henplus.event.ExecutionListener;
import henplus.io.ConfigurationContainer;
import henplus.view.Column;
import henplus.view.ColumnMetaData;
import henplus.view.TableRenderer;
import henplus.view.util.SortedMatchIterator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * document me.
 */
public final class SetCommand extends AbstractCommand {
    private final static String SETTINGS_FILENAME = "settings";
    private final static String SPECIAL_LAST_COMMAND = "_HENPLUS_LAST_COMMAND";
    private final static ColumnMetaData[] SET_META;

    static {
        SET_META = new ColumnMetaData[2];
        SET_META[0] = new ColumnMetaData("Name");
        SET_META[1] = new ColumnMetaData("Value");
    }

    private final Set _specialVariables;
    private final SortedMap _variables;
    private final HenPlus _henplus;
    private final ConfigurationContainer _config;

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
        return new String[] { "set-var", "unset-var", "unset-all" };
    }

    public SetCommand(final HenPlus henplus) {
        _henplus = henplus;
        _variables = new TreeMap();
        _specialVariables = new HashSet();
        _config = _henplus.createConfigurationContainer(SETTINGS_FILENAME);
        _variables.putAll(_config.readProperties());
    }

    public void registerLastCommandListener(final CommandDispatcher dispatcher) {
        _specialVariables.add(SPECIAL_LAST_COMMAND);
        dispatcher.addExecutionListener(new ExecutionListener() {
            public void beforeExecution(final SQLSession session, final String command) {
            }

            public void afterExecution(final SQLSession session, final String command,
                    final int result) {
                setVariable(SPECIAL_LAST_COMMAND, command.trim());
            }
        });
    }

    @Override
    public boolean requiresValidSession(final String cmd) {
        return false;
    }

    public Map getVariableMap() {
        return _variables;
    }

    /**
     * execute the command given.
     */
    public int execute(final SQLSession currentSession, final String cmd, final String param) {
        final StringTokenizer st = new StringTokenizer(param);
        final int argc = st.countTokens();

        if ("set-var".equals(cmd)) {
            /*
             * no args. only show.
             */
            if (argc == 0) {
                SET_META[0].resetWidth();
                SET_META[1].resetWidth();
                final TableRenderer table = new TableRenderer(SET_META, HenPlus.out());
                final Iterator vars = _variables.entrySet().iterator();
                while (vars.hasNext()) {
                    final Map.Entry entry = (Map.Entry) vars.next();
                    final Column[] row = new Column[4];
                    row[0] = new Column((String) entry.getKey());
                    row[1] = new Column((String) entry.getValue());
                    // row[2] = new Column("");
                    // row[3] = new Column("X");
                    table.addRow(row);
                }
                table.closeTable();
                return SUCCESS;
            }
            /*
             * more than one arg
             */
            else if (argc >= 2) {
                final String varname = (String) st.nextElement();
                int pos = 0;
                final int paramLength = param.length();
                // skip whitespace after 'set'
                while (pos < paramLength
                        && Character.isWhitespace(param.charAt(pos))) {
                    ++pos;
                }
                // skip non-whitespace after 'set ': variable name
                while (pos < paramLength
                        && !Character.isWhitespace(param.charAt(pos))) {
                    ++pos;
                }
                // skip whitespace before vlue..
                while (pos < paramLength
                        && Character.isWhitespace(param.charAt(pos))) {
                    ++pos;
                }
                String value = param.substring(pos);
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                } else if (value.startsWith("\'") && value.endsWith("\'")) {
                    value = value.substring(1, value.length() - 1);
                }
                setVariable(varname, value);
                return SUCCESS;
            }
            return SYNTAX_ERROR;
        } else if ("unset-var".equals(cmd)) {
            if (argc >= 1) {
                while (st.hasMoreElements()) {
                    final String varname = (String) st.nextElement();
                    if (!_variables.containsKey(varname)) {
                        HenPlus.msg().println(
                                "unknown variable '" + varname + "'");
                    } else {
                        _variables.remove(varname);
                    }
                }
                return SUCCESS;
            }
            return SYNTAX_ERROR;
        } else if ("unset-all".equals(cmd)) {
            if (argc == 0) {
                _variables.clear();
            } else {
                return SYNTAX_ERROR;
            }
        }
        return SUCCESS;
    }

    private void setVariable(final String name, final String value) {
        _variables.put(name, value);
    }

    /**
     * used, if the command dispatcher notices the attempt to expand a variable.
     * This is a partial variable name, that starts with '$' or '${'.
     */
    public Iterator completeUserVar(final String variable) {
        if (!variable.startsWith("$")) {
            return null; // strange, shouldn't happen.
        }
        final boolean hasBrace = variable.startsWith("${");
        final String prefix = hasBrace ? "${" : "$";
        final String postfix = hasBrace ? "}" : "";
        final String name = variable.substring(prefix.length());
        // HenPlus.msg().println("VAR: " + variable);
        // HenPlus.msg().println("NAME: " + name);
        final SortedMatchIterator it = new SortedMatchIterator(name, _variables);
        it.setPrefix(prefix);
        it.setSuffix(postfix);
        return it;
    }

    /**
     * complete variable names.
     */
    @Override
    public Iterator complete(final CommandDispatcher disp, final String partialCommand,
            final String lastWord) {
        final StringTokenizer st = new StringTokenizer(partialCommand);
        final String cmd = (String) st.nextElement();
        final int argc = st.countTokens();
        final HashSet alreadyGiven = new HashSet();
        if ("set-var".equals(cmd)) {
            if (argc > ("".equals(lastWord) ? 0 : 1)) {
                return null;
            }
        } else { // 'unset'
            /*
             * remember all variables, that have already been given on the
             * commandline and exclude from completion..
             */
            while (st.hasMoreElements()) {
                alreadyGiven.add(st.nextElement());
            }
        }
        return new SortedMatchIterator(lastWord, _variables) {
            @Override
            protected boolean exclude(final String current) {
                return alreadyGiven.contains(current);
            }
        };
    }

    @Override
    public void shutdown() {
        final Map writeMap = new HashMap();
        writeMap.putAll(_variables);
        final Iterator toRemove = _specialVariables.iterator();
        while (toRemove.hasNext()) {
            final String varname = (String) toRemove.next();
            writeMap.remove(varname);
        }

        _config.storeProperties(writeMap, true, "user variables");
    }

    /**
     * return a descriptive string.
     */
    @Override
    public String getShortDescription() {
        return "set/unset variables";
    }

    @Override
    public String getSynopsis(final String cmd) {
        if ("set-var".equals(cmd)) {
            return cmd + " [<varname> <value>]";
        } else if ("unset-var".equals(cmd)) {
            return cmd + " <varname> [<varname> ..]";
        } else if ("unset-all".equals(cmd)) {
            return cmd;
        }
        return cmd;
    }

    @Override
    public String getLongDescription(final String cmd) {
        String dsc = null;
        if ("set-var".equals(cmd)) {
            dsc = "\tWithout parameters,  show all  variable settings.  With\n"
                + "\tparameters, set variable with name <varname> to <value>.\n"
                + "\tVariables are  expanded in any  command you issue on the\n"
                + "\tcommandline.  Variable expansion works like on the shell\n"
                + "\twith the dollarsign. Both forms, $VARNAME and ${VARNAME},\n"
                + "\tare supported.  If the variable is  _not_  set, then the\n"
                + "\ttext is  left untouched.  So  if  there  is  no variable\n"
                + "\t$VARNAME, then it is not replaced by an empty string but\n"
                + "\tstays '$VARNAME'. This is because some scripts use wierd\n"
                + "\tidentifiers  containting  dollars  (esp. Oracle scripts)\n"
                + "\tIf you want to quote the dollarsign explicitly, write\n"
                + "\ttwo dollars: $$FOO means $FOO";
        } else if ("unset-var".equals(cmd)) {
            dsc = "\tunset the variable with name <varname>. You may provide\n"
                + "\tmultiple variables to be unset.";
        } else if ("unset-all".equals(cmd)) {
            dsc = "\tunset all variables.";
        }
        return dsc;
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
