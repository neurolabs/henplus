/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: AbstractPropertyCommand.java,v 1.1 2003-05-01 18:26:28 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.util.*;
import henplus.property.PropertyHolder;
import henplus.PropertyRegistry;

import java.util.Map;
import java.util.SortedMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.StringTokenizer;

import henplus.HenPlus;
import henplus.SQLSession;
import henplus.AbstractCommand;
import henplus.CommandDispatcher;

/**
 * The command, that allows to set properties.
 */
public abstract class AbstractPropertyCommand extends AbstractCommand {
    private final static ColumnMetaData[] PROP_META;

    static {
	PROP_META = new ColumnMetaData[3];
	PROP_META[0] = new ColumnMetaData("Name");
	PROP_META[1] = new ColumnMetaData("Value");
        PROP_META[2] = new ColumnMetaData("Description");
    }

    public AbstractPropertyCommand() {
    }

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
        final String setCmd = getSetCommand();
        final String hlpCmd = getHelpCommand();
	return new String[] {
            setCmd, hlpCmd
	};
    }
    
    protected abstract String getSetCommand();
    protected abstract String getHelpCommand();
    protected abstract PropertyRegistry getRegistry();

    /**
     * execute the command given.
     */
    public int execute(SQLSession currentSession, String cmd,  String param) {
	StringTokenizer st = new StringTokenizer(param);
	int argc = st.countTokens();
	
	if (getSetCommand().equals(cmd)) {
            /*
             * no args. only show.
             */
	    if (argc == 0) {
		PROP_META[0].resetWidth();
		PROP_META[1].resetWidth();
		TableRenderer table = new TableRenderer(PROP_META, System.out);
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
                    System.err.println(e.getMessage());
                    return EXEC_FAILED;
                }
		return SUCCESS;
	    }
	    return SYNTAX_ERROR;
	}
	else if (getHelpCommand().equals(cmd)) {
	    if (argc == 1) {
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
	    return SYNTAX_ERROR;
	}
	return SUCCESS;
    }

    private void printDescription(String propName, PropertyHolder prop) {
	String desc = prop.getLongDescription();
	if (desc == null) {
	    if (prop.getShortDescription() != null) {
		desc = "\t[short description]: " + prop.getShortDescription();
	    }
	}

	if (desc != null) {
	    Terminal.boldface(System.err);
	    System.err.println("DESCRIPTION");
	    Terminal.reset(System.err);
	    System.err.println(desc);
	}
	if (desc == null) {
	    System.err.println("no detailed help for '" + propName + "'");
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
	
    /**
     * return a descriptive string.
     */
    public String getShortDescription() {
	return "set properties";
    }

    public String getSynopsis(String cmd) {
	if (getSetCommand().equals(cmd)) {
	    return cmd + " [<property-name> <value>]"; 
	}
	else if (getHelpCommand().equals(cmd)) {
	    return cmd + " <property-name>";
	}
	return cmd;
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
