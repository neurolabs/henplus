/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SQLSession.java,v 1.6 2002-02-11 16:33:03 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import java.util.*;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.Properties;
import java.util.Enumeration;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineCompleter;
import org.gnu.readline.ReadlineLibrary;

import henplus.commands.*;

/**
 * document me.
 */
public class SQLSession {
    private long       _connectTime;
    private long       _statementCount;
    private String     _url;
    private String     _username;
    private String     _password;
    private Connection _conn;
    private boolean    _terminated = false;

    /**
     * creates a new SQL session. Open the database connection, initializes
     * the readline library
     */
    public SQLSession(String url, String user, String password)
	throws IllegalArgumentException, 
	       ClassNotFoundException, 
	       SQLException,
	       IOException 
    {
	_statementCount = 0;
	_conn = null;
	_url = url;
	_username = user;

	Driver driver = null;
	//System.err.println("connect to '" + url + "'");
	driver = DriverManager.getDriver(url);

	System.err.println ("HenPlus II connecting ");
	System.err.println(" url '" + url + '\'');
	System.err.println(" driver version " 
			 + driver.getMajorVersion()
			 + "."
			 + driver.getMinorVersion());
	connect();
	
	int transactionIsolation = Connection.TRANSACTION_NONE;
	DatabaseMetaData meta = _conn.getMetaData();
	System.err.println(" " + meta.getDatabaseProductName()
			   + " - " + meta.getDatabaseProductVersion());
	try {
	    if (meta.supportsTransactions()) {
		transactionIsolation = _conn.getTransactionIsolation();
	    }
	    else {
		System.err.println("no transactions.");
	    }
	    _conn.setAutoCommit(false);
	}
	catch (SQLException ignore_me) {
	}

	printTransactionIsolation(meta,Connection.TRANSACTION_NONE, 
				  "No Transaction", transactionIsolation);
	printTransactionIsolation(meta, 
				  Connection.TRANSACTION_READ_UNCOMMITTED,
				  "read uncommitted", transactionIsolation);
	printTransactionIsolation(meta, Connection.TRANSACTION_READ_COMMITTED,
				  "read committed", transactionIsolation);
	printTransactionIsolation(meta, Connection.TRANSACTION_REPEATABLE_READ,
				  "repeatable read", transactionIsolation);
	printTransactionIsolation(meta, Connection.TRANSACTION_SERIALIZABLE, 
				  "serializable", transactionIsolation);
    }
    
    private void printTransactionIsolation(DatabaseMetaData meta,
			int iLevel, String descript, int current) 
	throws SQLException {
	if (meta.supportsTransactionIsolationLevel(iLevel)) {
	    System.err.println(" " + descript
			       + ((current == iLevel) ? " *" : " "));
	}
    }

    public String getURL() {
	return _url;
    }

    public void connect() throws SQLException, IOException {
	boolean authRequired = false;
	// try to connect directly with the url.
	if (_username == null || _password == null) {
	    try {
		_conn = DriverManager.getConnection(_url);
	    }
	    catch (SQLException e) {
		authRequired = true;
	    }
	}
	
	// read username, password
	if (authRequired) {
	    System.err.println("============ authorization required ===");
	    BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
	    System.err.print("Username: ");
	    _username = input.readLine();
	    System.err.print("Password: ");
	    _password = input.readLine();
	}
	
	_conn = DriverManager.getConnection(_url, _username, _password);
	_connectTime = System.currentTimeMillis();
    }
    
    /**
     * return username, if known.
     */
    public String getUsername() {
	return _username;
    }

    public long getUptime() {
	return System.currentTimeMillis() - _connectTime;
    }
    public long getStatementCount() {
	return _statementCount;
    }
    
    public void close() {
	try {
	    getConnection().close();
	}
	catch (Exception e) {
	    System.err.println(e); // don't care
	}
    }

    /**
     * returns the current connection of this session.
     */
    public Connection getConnection() { return _conn; }

    public Statement createStatement() {
	Statement result = null;
	for (int retries=2; retries > 0; --retries) {
	    try {
		result = _conn.createStatement();
		++_statementCount;
		break;
	    }
	    catch (Exception e) {
		System.err.println("connection failure. Try to reconnect.");
		try { connect(); } catch (Exception e1) {}
	    }
	}
	return result;
    }

    /**
     * returns the command dispatcher.
     */
    //public CommandDispatcher getDispatcher() { return dispatcher; }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */

