/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SetCommand.java,v 1.15 2003-05-01 18:26:29 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.util.*;

import java.util.Map;
import java.util.SortedMap;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Properties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import henplus.HenPlus;
import henplus.SQLSession;
import henplus.AbstractCommand;
import henplus.CommandDispatcher;

/**
 * document me.
 */
public final class SetCommand extends AbstractCommand {
    private final static String SETTINGS_FILENAME = "settings";
    private final static ColumnMetaData[] SET_META;

    static {
	SET_META = new ColumnMetaData[2];
	SET_META[0] = new ColumnMetaData("Name");
	SET_META[1] = new ColumnMetaData("Value");
    }

    private final SortedMap _variables;
    private final HenPlus   _henplus;

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "set-var", "unset-var"
	};
    }
    
    public SetCommand(HenPlus henplus) {
	_henplus = henplus;
	_variables = new TreeMap();
	try {
	    File settingsFile = new File(henplus.getConfigDir(),
					 SETTINGS_FILENAME);
	    InputStream stream = new FileInputStream(settingsFile);
	    Properties p = new Properties();
	    p.load(stream);
	    _variables.putAll(p);
	}
	catch (IOException dont_care) {}

    }

    public boolean requiresValidSession(String cmd) { return false; }

    public Map getVariableMap() {
	return _variables;
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession currentSession, String cmd,  String param) {
	StringTokenizer st = new StringTokenizer(param);
	int argc = st.countTokens();
	
	if ("set-var".equals(cmd)) {
            /*
             * no args. only show.
             */
	    if (argc == 0) {
		SET_META[0].resetWidth();
		SET_META[1].resetWidth();
		TableRenderer table = new TableRenderer(SET_META, System.out);
		Iterator vars = _variables.entrySet().iterator();
		while (vars.hasNext()) {
		    Map.Entry entry = (Map.Entry) vars.next();
		    Column[] row = new Column[4];
		    row[0] = new Column((String) entry.getKey());
		    row[1] = new Column((String) entry.getValue());
		    //row[2] = new Column("");
		    //row[3] = new Column("X");
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
		_variables.put(varname, value);
		return SUCCESS;
	    }
	    return SYNTAX_ERROR;
	}
	else if ("unset-var".equals(cmd)) {
	    if (argc >= 1) {
		while (st.hasMoreElements()) {
		    String varname = (String) st.nextElement();
		    if (!_variables.containsKey(varname)) {
			System.err.println("unknown variable '" 
					   + varname + "'");
		    }
		    else {
			_variables.remove(varname);
		    }
		}
		return SUCCESS;
	    }
	    return SYNTAX_ERROR;
	}
	return SUCCESS;
    }

    /**
     * used, if the command dispatcher notices the attempt to expand
     * a variable. This is a partial variable name, that starts with '$'
     * or '${'.
     */
    public Iterator completeUserVar(String variable) {
	if (!variable.startsWith("$")) {
	    return null; // strange, shouldn't happen.
	}
	final boolean hasBrace = variable.startsWith("${");
	final String prefix = (hasBrace ? "${" : "$");
	final String name   =  variable.substring(prefix.length());
	final Iterator it   = _variables.tailMap(name).keySet().iterator();
	//System.err.println("VAR: " + variable);
	//System.err.println("NAME: " + name);
	return new Iterator() {
		String current = null;
		public boolean hasNext() {
		    while (it.hasNext()) {
			current = (String) it.next();
			if (!current.startsWith(name)) {
			    return false;
			}
			return true;
		    }
		    return false;
		}
		public Object  next() { 
		    return prefix + current + (hasBrace ? "}" : "");
		}
		public void remove() { 
		    throw new UnsupportedOperationException("no!");
		}
	    };
    }

    /**
     * complete variable names.
     */
    public Iterator complete(CommandDispatcher disp,
			     String partialCommand, final String lastWord) 
    {
	StringTokenizer st = new StringTokenizer(partialCommand);
	String cmd = (String) st.nextElement();
	int argc = st.countTokens();
	final HashSet  alreadyGiven = new HashSet();
	if ("set-var".equals(cmd)) {
	    if (argc > ("".equals(lastWord) ? 0 : 1)) {
		return null;
	    }
	}
	else { // 'unset'
	    /*
	     * remember all variables, that have already been given on
	     * the commandline and exclude from completion..
	     */
	    while (st.hasMoreElements()) {
		alreadyGiven.add((String) st.nextElement());
	    }
	}
	final Iterator it = _variables.tailMap(lastWord).keySet().iterator();
	return new Iterator() {
		String var = null;
		public boolean hasNext() {
		    while (it.hasNext()) {
			var = (String) it.next();
			if (!var.startsWith(lastWord)) {
			    return false;
			}
			if (alreadyGiven.contains(var)) {
			    continue;
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
	
	
    public void shutdown() {
	try {
	    File settingsFile = new File(_henplus.getConfigDir(),
					 SETTINGS_FILENAME);
	    OutputStream stream = new FileOutputStream(settingsFile);
	    Properties p = new Properties();
	    p.putAll(_variables);
	    p.store(stream, "user variables");
	}
	catch (IOException dont_care) {}
    }

    /**
     * return a descriptive string.
     */
    public String getShortDescription() {
	return "set/unset variables";
    }

    public String getSynopsis(String cmd) {
	if ("set-var".equals(cmd)) {
	    return cmd + " [<varname> <value>]"; 
	}
	else if ("unset-var".equals(cmd)) {
	    return cmd + " <varname> [<varname> ..]";
	}
	return cmd;
    }

    public String getLongDescription(String cmd) { 
	String dsc = null;
	if ("set-var".equals(cmd)) {
	    dsc= "\twithout parameters,  show all  variable settings.  With\n"
		+"\tparameters, set variable with name <varname> to <value>.\n"
		+"\tVariables are  expanded in any  command you issue on the\n"
		+"\tcommandline.  Variable expansion works like on the shell\n"
		+"\twith the dollarsign. Both forms, $VARNAME and ${VARNAME},\n"
		+"\tare supported.  If the variable is  _not_  set, then the\n"
		+"\ttext is  left untouched.  So  if  there  is  no variable\n"
		+"\t$VARNAME, then it is not replaced by an empty string but\n"
		+"\tstays '$VARNAME'. This is because some scripts use wierd\n"
		+"\tidentifiers  containting  dollars  (esp. Oracle scripts)\n"
		+"\tIf you want to quote the dollarsign explicitly, write\n"
		+"\ttwo dollars: $$FOO means $FOO";
	}
	else if ("unset-var".equals(cmd)) {
	    dsc="\tunset the variable with name <varname>. You may provide\n"
		+"\tmultiple variables to be unset.";
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
