/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SQLSession.java,v 1.4 2002-01-28 11:32:00 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
import java.util.*;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.EOFException;
import java.io.IOException;
import java.util.Properties;
import java.util.Enumeration;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.DatabaseMetaData;

import org.gnu.readline.Readline;
import org.gnu.readline.ReadlineCompleter;
import org.gnu.readline.ReadlineLibrary;

import commands.*;

/**
 * document me.
 */
public class SQLSession {
    private long connectTime;
    private long statementCount;
    private String url;
    private String username;
    private Connection conn;
    private boolean terminated = false;

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
	statementCount = 0;
	conn = null;
	this.url = url;
	this.username = user;
	boolean authRequired = false;
	Driver driver = null;
	//System.err.println("connect to '" + url + "'");
	driver = DriverManager.getDriver(url);

	System.err.println ("HenPlus II connecting ");
	System.err.println(" url '" + url + '\'');
	System.err.println(" driver version " 
			 + driver.getMajorVersion()
			 + "."
			 + driver.getMinorVersion());
	// try to connect directly with the url.
	if (username == null || password == null) {
	    try {
		conn = DriverManager.getConnection(url);
	    }
	    catch (SQLException e) {
		authRequired = true;
	    }
	}
	
	if (conn == null) {
	    // read username, password
	    if (authRequired) {
		System.err.println("============ authorization required ===");
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		System.err.print("Username: ");
		username = input.readLine();
		System.err.print("Password: ");
		password = input.readLine();
	    }
	    
	    conn = DriverManager.getConnection(url, username, password);
	}
	connectTime = System.currentTimeMillis();
	
	int transactionIsolation = Connection.TRANSACTION_NONE;
	DatabaseMetaData meta = conn.getMetaData();
	System.err.println(" " + meta.getDatabaseProductName()
			   + " - " + meta.getDatabaseProductVersion());
	try {
	    if (meta.supportsTransactions()) {
		transactionIsolation = conn.getTransactionIsolation();
	    }
	    else {
		System.err.println("no transactions.");
	    }
	    conn.setAutoCommit(false);
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
	return url;
    }

    /**
     * return username, if known.
     */
    public String getUsername() {
	return username;
    }

    public long getUptime() {
	return System.currentTimeMillis() - connectTime;
    }
    public long getStatementCount() {
	return statementCount;
    }
    
    public void close() {
	try {
	    getConnection().close();
	}
	catch (SQLException e) {
	    System.err.println(e); // don't care
	}
    }

    /**
     * returns the current connection of this session.
     */
    public Connection getConnection() { return conn; }

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

