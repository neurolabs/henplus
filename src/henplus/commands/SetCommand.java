/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SetCommand.java,v 1.2 2002-01-26 21:33:26 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package commands;

import java.util.Map;
import java.util.SortedMap;
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

import HenPlus;
import SQLSession;
import AbstractCommand;
import CommandDispatcher;

/**
 * document me.
 */
public final class SetCommand extends AbstractCommand {
    private final static String SETTINGS_FILENAME = "settings";
    private final SortedMap _variables;
    private final HenPlus   _henplus;

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "set", "unset"
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
    public int execute(SQLSession currentSession, String command) {
	command = command.trim();
	StringTokenizer st = new StringTokenizer(command);
	String cmd = (String) st.nextElement();
	int argc = st.countTokens();
	
	if ("set".equals(cmd)) {
	    if (argc == 0) {
		Iterator vars = _variables.entrySet().iterator();
		while (vars.hasNext()) {
		    Map.Entry entry = (Map.Entry) vars.next();
		    System.err.print(entry.getKey());
		    System.err.print('\t');
		    System.err.println(entry.getValue());
		}
		return SUCCESS;
	    }
	    else if (argc >= 2) {
		String varname = (String) st.nextElement();
		int pos = "set".length();
		// skip whitespace after 'set'
		while (pos < command.length() 
		       && Character.isWhitespace(command.charAt(pos))) {
		    ++pos;
		}
		// skip non-whitespace after 'set  ': variable name
		while (pos < command.length() 
		       && !Character.isWhitespace(command.charAt(pos))) {
		    ++pos;
		}
		// skip whitespace before vlue..
		while (pos < command.length() 
		       && Character.isWhitespace(command.charAt(pos))) {
		    ++pos;
		}
		String value = command.substring(pos);
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
	else if ("unset".equals(cmd)) {
	    if (argc == 1) {
		String varname = (String) st.nextElement();
		if (!_variables.containsKey(varname)) {
		    System.err.println("unknown variable.");
		}
		else {
		    _variables.remove(varname);
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
	if (argumentCount(partialCommand) >
	    ("".equals(lastWord) ? 1 : 2)) {
	    return null;
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
	if ("set".equals(cmd)) {
	    return "set [<varname> <value>]"; 
	}
	else if ("unset".equals(cmd)) {
	    return "unset <varname>";
	}
	return cmd;
    }

    public String getLongDescription(String cmd) { 
	String dsc = null;
	if ("set".equals(cmd)) {
	    dsc= "\twithout parameters, show all variable settings. With\n"
		+"\tparameters, set variable with name <varname> to <value>";
	}
	else if ("unset".equals(cmd)) {
	    dsc="\tunset the variable with name <varname>";
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
