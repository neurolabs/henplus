/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: AbstractPropertyCommand.java,v 1.1 2004-02-01 14:12:52 hzeller Exp $ 
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
import henplus.view.util.Terminal;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;

/**
 * The command, that allows to set properties. This abstract
 * command is used for the session specific and global properties.
 */
public abstract class AbstractPropertyCommand extends AbstractCommand {
    private final static ColumnMetaData[] PROP_META;

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
     * the PropertyRegistry associcaed with the current
     */
    protected abstract PropertyRegistry getRegistry();

    /**
     * execute the command given.
     */
    public int execute(SQLSession currentSession, String cmd,  String param) {
	StringTokenizer st = new StringTokenizer(param);
	int argc = st.countTokens();
	
        if (cmd.startsWith("re")) { // 'reset-property'
            if (argc == 1) {
                String name = st.nextToken();
                PropertyHolder holder;
                holder = (PropertyHolder) (getRegistry()
                                           .getPropertyMap().get(name));
                if (holder == null) {
                    return EXEC_FAILED;
                }
                String defaultValue = holder.getDefaultValue();
                try {
                    holder.setValue(defaultValue);
                }
                catch (Exception e) {
                    HenPlus.msg().println("setting to default '" 
                                       + defaultValue + "' failed.");
                    return EXEC_FAILED;
                }
                return SUCCESS;
            }
            return SYNTAX_ERROR;
        }
        else {
        /*
         * no args. show available properties
         */
        if (argc == 0) {
            PROP_META[0].resetWidth();
            PROP_META[1].resetWidth();
            TableRenderer table = new TableRenderer(PROP_META, HenPlus.out());
            Iterator propIt = (getRegistry()
                               .getPropertyMap()
                               .entrySet().iterator());
            while (propIt.hasNext()) {
                Map.Entry entry = (Map.Entry) propIt.next();
                Column[] row = new Column[3];
                PropertyHolder holder = (PropertyHolder) entry.getValue();
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
            String name = st.nextToken();
            PropertyHolder holder;
            holder = (PropertyHolder) (getRegistry()
                                       .getPropertyMap().get(name));
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
            String varname = (String) st.nextElement();
            int pos = 0;
            int paramLength = param.length();
            // skip whitespace after 'set'
            while (pos < paramLength
                   && Character.isWhitespace(param.charAt(pos))) {
                ++pos;
            }
            // skip non-whitespace after 'set  ': variable name
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
                value = value.substring(1, value.length()-1);
            }
            else if (value.startsWith("\'") && value.endsWith("\'")) {
                value = value.substring(1, value.length()-1);
            }

            try {
                getRegistry().setProperty(varname, value);
            }
            catch (Exception e) {
                HenPlus.msg().println(e.getMessage());
                return EXEC_FAILED;
            }
            return SUCCESS;
        }
        }
	return SUCCESS;
    }

    private void printDescription(String propName, PropertyHolder prop) {

        if (prop.getShortDescription() != null) {
	    HenPlus.msg().attributeBold();
	    HenPlus.msg().println("PROPERTY");
            HenPlus.msg().attributeReset();
            HenPlus.msg().println("\t" + propName + " : " 
                                  + prop.getShortDescription());
            HenPlus.msg().println();
        }

	String desc = prop.getLongDescription();
	if (desc != null) {
	    HenPlus.msg().attributeBold();
	    HenPlus.msg().println("DESCRIPTION");
            HenPlus.msg().attributeReset();
	    HenPlus.msg().println(desc);
	}
	else {
	    HenPlus.msg().println("no detailed help for '" + propName + "'");
	}
    }

    /**
     * complete property names.
     */
    public Iterator complete(CommandDispatcher disp,
			     String partialCommand, final String lastWord) 
    {
	StringTokenizer st = new StringTokenizer(partialCommand);
	String cmd = st.nextToken();
	int argc = st.countTokens();

        if (argc > ("".equals(lastWord) ? 0 : 1)) { /* one arg given */
            if (getSetCommand().equals(cmd)) {
                String name = st.nextToken();
                PropertyHolder holder;
                holder = (PropertyHolder) (getRegistry()
                                           .getPropertyMap().get(name));
                if (holder == null) {
                    return null;
                }
                return holder.completeValue(lastWord);
	    }
            return null;
	}

        SortedMap props = getRegistry().getPropertyMap();

	final Iterator it = props.tailMap(lastWord).keySet().iterator();
	return new Iterator() {
		String var = null;
		public boolean hasNext() {
		    while (it.hasNext()) {
			var = (String) it.next();
			if (!var.startsWith(lastWord)) {
			    return false;
			}
			return true;
		    }
		    return false;
		}
		public Object  next() { return var; }
		public void remove() { 
		    throw new UnsupportedOperationException("no!");
		}
	    };
    }

    protected abstract String getHelpHeader();
	
    /**
     * return a descriptive string.
     */
    public String getShortDescription() {
	return "set " + getHelpHeader() + " properties";
    }

    public String getSynopsis(String cmd) {
        if (cmd.startsWith("re")) {
            return cmd + " <propery-name>";
        }
        return cmd + " [<property-name> [<value>]]"; 
    }
    
    public String getLongDescription(String cmd) { 
	String dsc = null;
        if (cmd.startsWith("re")) {
            dsc= "\tReset the given " + getHelpHeader() + " property\n"
                +"\tto its default value";
        }
        else {
            dsc= "\tWithout parameters, show available " + getHelpHeader() + "\n"
                +"\tproperties and their settings.\n\n"
                +"\tWith only the property name given as parameter,\n"
                +"\tshow the long help associated with that property.\n\n"
                +"\tIs the property name followed by a value, the property is\n"
                +"\tset to that value.";
        }
	return dsc;
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
