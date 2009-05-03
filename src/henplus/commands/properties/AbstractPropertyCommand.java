/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: AbstractPropertyCommand.java,v 1.4 2005-06-18 04:58:13 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands.properties;

import henplus.HenPlus;
import henplus.AbstractCommand;
import henplus.CommandDispatcher;
import henplus.PropertyRegistry;
import henplus.SQLSession;
import henplus.property.PropertyHolder;
import henplus.view.Column;
import henplus.view.ColumnMetaData;
import henplus.view.TableRenderer;
import henplus.view.util.SortedMatchIterator;

import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * The command, that allows to set properties. This abstract command is used for
 * the session specific and global properties.
 */
public abstract class AbstractPropertyCommand extends AbstractCommand {
    private static final ColumnMetaData[] PROP_META;

    static {
        PROP_META = new ColumnMetaData[3];
        PROP_META[0] = new ColumnMetaData("Name");
        PROP_META[1] = new ColumnMetaData("Value");
        PROP_META[2] = new ColumnMetaData("Description");
    }

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
        final String setCmd = getSetCommand();
        return new String[] { setCmd, "re" + setCmd };
    }

    /**
     * returns the name of the command this command reacts on.
     */
    protected abstract String getSetCommand();

    /**
     * the PropertyRegistry associcaed with the current.
     */
    protected abstract PropertyRegistry getRegistry();

    /**
     * execute the command given.
     */
    public int execute(final SQLSession currentSession, final String cmd, final String param) {
        final StringTokenizer st = new StringTokenizer(param);
        final int argc = st.countTokens();

        if (cmd.startsWith("re")) { // 'reset-property'
            if (argc == 1) {
                final String name = st.nextToken();
                PropertyHolder holder;
                holder = getRegistry().getPropertyMap()
                        .get(name);
                if (holder == null) {
                    return EXEC_FAILED;
                }
                final String defaultValue = holder.getDefaultValue();
                try {
                    holder.setValue(defaultValue);
                } catch (final Exception e) {
                    HenPlus.msg()
                    .println(
                            "setting to default '" + defaultValue
                            + "' failed.");
                    return EXEC_FAILED;
                }
                return SUCCESS;
            }
            return SYNTAX_ERROR;
        } else {
            /*
             * no args. show available properties
             */
            if (argc == 0) {
                PROP_META[0].resetWidth();
                PROP_META[1].resetWidth();
                final TableRenderer table = new TableRenderer(PROP_META, HenPlus
                        .out());
                final Iterator propIt = getRegistry().getPropertyMap().entrySet()
                        .iterator();
                while (propIt.hasNext()) {
                    final Map.Entry entry = (Map.Entry) propIt.next();
                    final Column[] row = new Column[3];
                    final PropertyHolder holder = (PropertyHolder) entry.getValue();
                    row[0] = new Column((String) entry.getKey());
                    row[1] = new Column(holder.getValue());
                    row[2] = new Column(holder.getShortDescription());
                    table.addRow(row);
                }
                table.closeTable();
                return SUCCESS;
            }

            /*
             * one arg: show help
             */
            else if (argc == 1) {
                final String name = st.nextToken();
                PropertyHolder holder;
                holder = getRegistry().getPropertyMap()
                        .get(name);
                if (holder == null) {
                    return EXEC_FAILED;
                }
                printDescription(name, holder);
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

                try {
                    getRegistry().setProperty(varname, value);
                } catch (final Exception e) {
                    HenPlus.msg().println(e.getMessage());
                    return EXEC_FAILED;
                }
                return SUCCESS;
            }
        }
        return SUCCESS;
    }

    private void printDescription(final String propName, final PropertyHolder prop) {

        if (prop.getShortDescription() != null) {
            HenPlus.msg().attributeBold();
            HenPlus.msg().println("PROPERTY");
            HenPlus.msg().attributeReset();
            HenPlus.msg().println(
                    "\t" + propName + " : " + prop.getShortDescription());
            HenPlus.msg().println();
        }

        final String desc = prop.getLongDescription();
        if (desc != null) {
            HenPlus.msg().attributeBold();
            HenPlus.msg().println("DESCRIPTION");
            HenPlus.msg().attributeReset();
            HenPlus.msg().println(desc);
        } else {
            HenPlus.msg().println("no detailed help for '" + propName + "'");
        }
    }

    /**
     * complete property names.
     */
    @Override
    public Iterator complete(final CommandDispatcher disp, final String partialCommand,
            final String lastWord) {
        final StringTokenizer st = new StringTokenizer(partialCommand);
        final String cmd = st.nextToken();
        final int argc = st.countTokens();

        if (argc > ("".equals(lastWord) ? 0 : 1)) { /* one arg given */
            if (getSetCommand().equals(cmd)) {
                final String name = st.nextToken();
                PropertyHolder holder;
                holder = getRegistry().getPropertyMap()
                        .get(name);
                if (holder == null) {
                    return null;
                }
                return holder.completeValue(lastWord);
            }
            return null;
        }

        return new SortedMatchIterator(lastWord, getRegistry().getPropertyMap());
    }

    protected abstract String getHelpHeader();

    /**
     * return a descriptive string.
     */
    @Override
    public String getShortDescription() {
        return "set " + getHelpHeader() + " properties";
    }

    @Override
    public String getSynopsis(final String cmd) {
        if (cmd.startsWith("re")) {
            return cmd + " <propery-name>";
        }
        return cmd + " [<property-name> [<value>]]";
    }

    @Override
    public String getLongDescription(final String cmd) {
        String dsc = null;
        if (cmd.startsWith("re")) {
            dsc = "\tReset the given " + getHelpHeader() + " property\n"
            + "\tto its default value";
        } else {
            dsc = "\tWithout parameters, show available "
                + getHelpHeader()
                + "\n"
                + "\tproperties and their settings.\n\n"
                + "\tWith only the property name given as parameter,\n"
                + "\tshow the long help associated with that property.\n\n"
                + "\tIs the property name followed by a value, the property is\n"
                + "\tset to that value.";
        }
        return dsc;
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
