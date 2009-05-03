/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.AbstractCommand;
import henplus.CommandDispatcher;
import henplus.HenPlus;
import henplus.SQLSession;
import henplus.io.ConfigurationContainer;
import henplus.view.Column;
import henplus.view.ColumnMetaData;
import henplus.view.TableRenderer;
import henplus.view.util.NameCompleter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.gnu.readline.Readline;

/**
 * Command to bind function keys to commands.
 */
public class KeyBindCommand extends AbstractCommand {
    private static final String KEYBIND_FILENAME = "key-bindings";
    private static final ColumnMetaData[] DRV_META;
    static {
        DRV_META = new ColumnMetaData[2];
        DRV_META[0] = new ColumnMetaData("Key");
        DRV_META[1] = new ColumnMetaData("execute command");
    }

    private final ConfigurationContainer _config;

    private final Map/* <String:keyname,String:readlinename> */_keyNames;
    private final NameCompleter _functionKeyNameCompleter;
    private final Map/* <String:key,String:cmd> */_bindings;

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
        return new String[] { "list-key-bindings", "bind-key-cmd",
                // "unbind-key",
                // "test-bind-all" // uncomment for tests.
        };
    }

    public KeyBindCommand(final HenPlus henplus) {
        _keyNames = new HashMap();
        // -- there are different mappings for some function keys and terminals
        _keyNames.put("F1", new String[] { "\"\\e[11~\"", "\"\\e[[a\"",
        "\"\\eOP\"" });
        _keyNames.put("Shift-F1", new String[] { "\"\\e[25~\"", "\"\\eO2P\"" });
        _keyNames.put("F2", new String[] { "\"\\e[12~\"", "\"\\e[[b\"",
        "\"\\eOQ\"" });
        _keyNames.put("Shift-F2", new String[] { "\"\\e[26~\"", "\"\\eO2Q\"" });
        _keyNames.put("F3", new String[] { "\"\\e[13~\"", "\"\\e[[c\"",
        "\"\\eOR\"" });
        _keyNames.put("Shift-F3", new String[] { "\"\\e[28~\"", "\"\\eO2R\"" });
        _keyNames.put("F4", new String[] { "\"\\e[14~\"", "\"\\e[[d\"",
        "\"\\eOS\"" });
        _keyNames.put("Shift-F4", new String[] { "\"\\e[29~\"", "\"\\eO2S\"" });
        _keyNames.put("F5", new String[] { "\"\\e[15~\"", "\"\\e[[e\"" });
        _keyNames.put("Shift-F5",
                new String[] { "\"\\e[15;2~\"", "\"\\e[31~\"" });
        _keyNames.put("F6", new String[] { "\"\\e[17~\"" });
        _keyNames.put("Shift-F6",
                new String[] { "\"\\e[17;2~\"", "\"\\e[32~\"" });
        _keyNames.put("F7", new String[] { "\"\\e[18~\"" });
        _keyNames.put("Shift-F7",
                new String[] { "\"\\e[18;2~\"", "\"\\e[33~\"" });
        _keyNames.put("F8", new String[] { "\"\\e[19~\"" });
        _keyNames.put("Shift-F8",
                new String[] { "\"\\e[19;2~\"", "\"\\e[34~\"" });
        // for the linux console, there seem no bindings for F9-F12
        _keyNames.put("F9", new String[] { "\"\\e[20~\"" });
        _keyNames.put("Shift-F9", new String[] { "\"\\e[20;2~\"" });
        _keyNames.put("F10", new String[] { "\"\\e[21~\"" });
        _keyNames.put("Shift-F10", new String[] { "\"\\e[21;2~\"" });
        _keyNames.put("F11", new String[] { "\"\\e[23~\"" });
        _keyNames.put("Shift-F11", new String[] { "\"\\e[23;2~\"" });
        _keyNames.put("F12", new String[] { "\"\\e[24~\"" });
        _keyNames.put("Shift-F12", new String[] { "\"\\e[24;2~\"" });
        _functionKeyNameCompleter = new NameCompleter(_keyNames.keySet());
        _config = henplus.createConfigurationContainer(KEYBIND_FILENAME);
        _bindings = new TreeMap();
        bindKey("F1", "help\n"); // a common default binding.
        load();
    }

    /**
     * execute the command given.
     */
    public int execute(final SQLSession session, final String cmd, final String param) {
        final StringTokenizer st = new StringTokenizer(param);
        final int argc = st.countTokens();

        if ("list-key-bindings".equals(cmd)) {
            if (argc != 0) {
                return SYNTAX_ERROR;
            }
            showKeyBindings();
            return SUCCESS;
        }

        else if ("test-bind-all".equals(cmd)) {
            testBindAll();
            return SUCCESS;
        }

        else /* bind-key-cmd-string */{
            if (!st.hasMoreTokens()) {
                return SYNTAX_ERROR;
            }

            String key = st.nextToken();
            key = _functionKeyNameCompleter.findCaseInsensitive(key);
            if (key == null) {
                return SYNTAX_ERROR;
            }

            String value = param.substring(key.length() + 1).trim();
            value = stripQuotes(value);
            if (value.length() == 0) {
                return SYNTAX_ERROR;
            }

            return bindKey(key, value) ? SUCCESS : EXEC_FAILED;
        }
    }

    @Override
    public Iterator complete(final CommandDispatcher disp, final String partialCommand,
            final String lastWord) {
        if (argumentCount(partialCommand) > ("".equals(lastWord) ? 1 : 2)) {
            return null;
        }
        return _functionKeyNameCompleter.getAlternatives(lastWord);
    }

    private String stripQuotes(String value) {
        if (value.length() < 2) {
            return value;
        }
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        } else if (value.startsWith("\'") && value.endsWith("\'")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * For tests: bind the key to its name representation.
     */
    private void testBindAll() {
        final Iterator it = _keyNames.keySet().iterator();
        while (it.hasNext()) {
            final String keyName = (String) it.next();
            bindKey(keyName, keyName);
        }
    }

    /**
     * Bind a key with the symbolic name to the given string.
     */
    private boolean bindKey(final String keyName, final String cmd) {
        final String[] terminalValues = (String[]) _keyNames.get(keyName);
        if (terminalValues == null) {
            return false;
        }
        _bindings.put(keyName, cmd);

        /* quote "-characters */
        final StringBuffer binding = new StringBuffer();
        int pos = 0;
        int nowPos = 0;
        while ((nowPos = cmd.indexOf('"', pos)) >= 0) {
            binding.append(cmd.substring(pos, nowPos));
            binding.append("\\\"");
            pos = nowPos + 1;
        }
        if (pos < cmd.length()) {
            binding.append(cmd.substring(pos));
        }

        final String bindCmd = binding.toString();
        for (int i = 0; i < terminalValues.length; ++i) {
            Readline.parseAndBind(terminalValues[i] + ": \"" + bindCmd + "\"");
        }
        return true;
    }

    @Override
    public void shutdown() {
        _config.storeProperties(_bindings, true, "Key-Bindings..");
    }

    /**
     * initial load of key bindings
     */
    private void load() {
        final Map bindings = _config.readProperties(_bindings);
        final Iterator it = bindings.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry entry = (Map.Entry) it.next();
            bindKey((String) entry.getKey(), (String) entry.getValue());
        }
    }

    private void showKeyBindings() {
        DRV_META[0].resetWidth();
        DRV_META[1].resetWidth();
        final TableRenderer table = new TableRenderer(DRV_META, HenPlus.out());
        final Iterator it = _bindings.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry entry = (Map.Entry) it.next();
            final Column[] row = new Column[2];
            row[0] = new Column((String) entry.getKey());
            row[1] = new Column((String) entry.getValue());
            table.addRow(row);
        }
        table.closeTable();
    }

    @Override
    public boolean requiresValidSession(final String cmd) {
        return false;
    }

    /**
     * return a descriptive string.
     */
    @Override
    public String getShortDescription() {
        return "handle function key bindings";
    }

    @Override
    public String getSynopsis(final String cmd) {
        if ("bind-key-cmd".equals(cmd)) {
            return cmd + " <function-key> <command>";
        }
        return cmd;
    }

    @Override
    public String getLongDescription(final String cmd) {
        String dsc = null;
        if ("list-key-bindings".equals(cmd)) {
            dsc = "\tList all key bindings that have been set with\n"
                + "\tbind-key-cmd";
        } else if ("bind-key-cmd".equals(cmd)) {
            dsc = "\tBind a key to a command. Keys can be the function keys\n"
                + "\tF1, F2...F12 or Shift-F1...Shift-F12";
        }
        return dsc;
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
