/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package commands;

import SQLSession;
import AbstractCommand;

import java.text.DecimalFormat;

import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Types;

/**
 * document me.
 */
public class SQLCommand extends AbstractCommand {
    /** Automat **/
    final static byte START           = 1;  // statement == start
    final static byte STATEMENT       = 1;
    final static byte START_COMMENT   = 2;
    final static byte COMMENT         = 3;
    final static byte END_COMMENT     = 4;
    final static byte START_ANSI      = 5;
    final static byte ENDLINE_COMMENT = 6;
    final static byte STRING          = 7;
    final static byte SQLSTRING       = 8;
    final static byte END             = 10;
    
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "select", "insert", "update",
	    "create", "alter", "drop",
	    "commit", "rollback",
	    "" // any.
	};
    }

    /**
     * don't show the number of commands available in the toplevel
     * command list ..
     */
    public boolean participateInCommandCompletion() { return false; }

    /**
     * complicated SQL statements are only complete with
     * semicolon. Simple commands may have no semicolon (like
     * 'commit' and 'rollback').
     */
    public boolean isComplete(String command) {
	command = command.trim();
	if (command.startsWith("commit")
	    || command.startsWith("rollback"))
	    return true;
	// this will be wrong if we support stored procedures.
	if (command.trim().endsWith(";")) {
	    StringBuffer raw      = new StringBuffer(command);
	    StringBuffer parsedSQL= new StringBuffer();
	    return (parseSQL(START, raw, parsedSQL) == END);
	}
	return false;
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String command) {
	Statement stmt = null;
	ResultSet rset = null;
	try {
	    long startTime = System.currentTimeMillis();
	    long execTime = -1;
	    if (command.startsWith("commit")) {
		session.getConnection().commit();
	    }
	    else if (command.startsWith("rollback")) {
		session.getConnection().rollback();
	    }
	    else {
		StringBuffer raw      = new StringBuffer(command);
		StringBuffer parsedSQL= new StringBuffer();
		parseSQL(START, raw, parsedSQL);
		command = parsedSQL.toString();

		stmt = session.getConnection().createStatement();
		if (stmt.execute(command)) {
		    ResultSetRenderer renderer;
		    renderer = new ResultSetRenderer(stmt.getResultSet());
		    int rows = renderer.writeTo(System.out);
		    System.err.print(rows + " row" + ((rows!=1)?"s":"")
				     + " in result");
		}
		else {
		    int updateCount = stmt.getUpdateCount();
		    if (updateCount >= 0) {
			System.err.print("affected "+updateCount+" rows");
		    }
		    else {
			System.err.print("ok.");
		    }
		}
		execTime = System.currentTimeMillis() - startTime;
		System.err.print(" (");
		printTime(execTime);
		System.err.println(")");
	    }
	    return SUCCESS;
	}
	catch (SQLException e) {
	    System.err.println(e.getMessage());
	    return EXEC_FAILED;
	}
	finally {
	    try { if (rset != null) rset.close(); } catch (Exception e) {}
	    try { if (stmt != null) stmt.close(); } catch (Exception e) {}
	}
    }

    /*
     * Damit der ankommende String richtig dem JDBC übergeben werden kann, 
     * sollten zunächst mal die Kommentare entfernt werden (kann z.B. 
     * das Ora-JDBC nicht immer mit diesen an allen Stellen richtig 
     * umgehen), sowie das ';' als Ende-Zeichen ausfindig gemacht 
     * werden (natürlich nicht in normalen Strings).
     *
     * Das erledigt diese Funktion parseString().
     *
     * Die Funktion bekommt einen Startzustand übergeben (bei neuem
     * parsen START), den input-Puffer, von dem die bereits gelesenen 
     * und verarbeiteten Zeichen entfernt werden.
     * An den 'parsed'-Buffer werden die gültigen Statement-Zeichen
     * angehängt.
     *
     * Wird ein Output-Stream angegeben, so wird der gelesene String
     * in Syntax-entsprechendem font-face ausgegeben (statement: normal,
     * Bold für Kommentar ..)
     *
     * Der Rückgabewert ist der Status des Parsers nach dem Abarbeiten
     * des übergebenen Input-Strings .. Wurde ein Statement vollständig
     * erkannt, so bricht der Parser ab und gibt 'END' zurück.
     * War der Input-String vor dem Erkennen eines vollständigen Statements
     * leer, so wird der aktuelle Status zurückgegeben, der dem 
     * Parser nach dem Auffüllen desselben wieder übergeben werden
     * muß ..
     */
    private byte parseSQL (byte startState, 
			   StringBuffer input,StringBuffer parsed) {
	int pos = 0;
	char current;
	byte state = startState;
	byte oldstate = -1;
	
	while (state != END && pos < input.length()) {
	    current = input.charAt(pos);
	    //			System.out.print ("Pos: " + pos + "\t");
	    switch (state) {
	    case STATEMENT : 
		if (current == ';')  state = END;
		if (current == '/')  state = START_COMMENT;
		if (current == '"')  state = STRING;
		if (current == '\'') state = SQLSTRING;
		if (current == '-')  state = START_ANSI;
		break;
	    case START_COMMENT:
		if (current == '*') state = COMMENT;
		else if (current == '/') state = ENDLINE_COMMENT;
		else { parsed.append ('/'); state = STATEMENT; }
		break;
	    case COMMENT:
		if (current == '*') state = END_COMMENT;
		break;
	    case END_COMMENT:
		if (current == '/') state = STATEMENT;
		else if (current == '*') state = END_COMMENT;
		else state = COMMENT;
		break;
	    case START_ANSI:
		if (current == '-') state = ENDLINE_COMMENT;
		else { parsed.append('-'); state = STATEMENT; }
		break;
	    case ENDLINE_COMMENT:
		if (current == '\n') state = STATEMENT;
		if (current == '\r') state = STATEMENT;
		break;
	    case STRING:
	    case SQLSTRING:
		if (current == '"' && state == STRING) 
		    state=STATEMENT;
		if (current == '\'' && state == SQLSTRING) 
		    state=STATEMENT;
		//				if (current == '\\') pos++;
		break;
	    }
	    
	    /* append to parsed; ignore comments */
	    if ((state == STATEMENT && oldstate != END_COMMENT) ||
		state == STRING ||
		state == SQLSTRING) 
		parsed.append(current); // kein ';' ans Ende
	    
	    oldstate = state;
	    pos++;
	}
	/*
	StringBuffer rest = new StringBuffer();
	// skip leading whitespaces of next statement ..
	while (pos < input.length() && isWhite (input.charAt(pos)))
	    pos++;
	while (pos < input.length()) { 
	    rest.append(input.charAt(pos)); 
	    pos++; 
	}
	input.setLength(0);
	input.append(rest);
	*/
	return state;
    }
    
    private void printTime(long execTime) {
	if (execTime > 60000) {
	    System.err.print(execTime/60000);
	    System.err.print(":");
	    execTime %= 60000;
	    if (execTime < 10000)
		System.err.print("0");
	}
	if (execTime >= 1000) {
	    System.err.print(execTime / 1000);
	    System.err.print(".");
	    execTime %= 1000;
	    if (execTime < 100) System.err.print("0");
	    if (execTime < 10)  System.err.print("0");
	    System.err.print(execTime);
	    System.err.print(" ");
	}
	else {
	    System.err.print(execTime + " m");
	}
	System.err.print("sec");
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
