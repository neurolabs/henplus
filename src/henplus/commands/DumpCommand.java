/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: DumpCommand.java,v 1.17 2003-05-07 11:22:09 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import java.util.*;
import java.sql.*;
import java.io.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import henplus.Interruptable;
import henplus.SigIntHandler;
import henplus.Version;
import henplus.SQLSession;
import henplus.AbstractCommand;
import henplus.CommandDispatcher;
import henplus.util.NameCompleter;

/**
 * Dump out and read that dump of a table; database-independently.
 * This reads directly from the stream, so only needs not much
 * memory, no matter the size of the file.
 ---------------------------
          (tabledump 'student'
            (dump-version 1 1)
            (henplus-version 0.3.3)
            (database-info 'MySQL - 3.23.47')
            (meta ('name',   'sex',    'student_id')
                  ('STRING', 'STRING', 'INTEGER'   ))
            (data ('Megan','F',1)
                  ('Joseph','M',2)
                  ('Kyle','M',3)
                  ('Mac Donald\'s','M',44))
            (rows 4))
 ---------------------------
 *
 * QUICK AND DIRTY HACK .. NOT YET NICE. Too long.
 *
 * @author Henner Zeller
 */
public class DumpCommand 
    extends AbstractCommand
    implements Interruptable 
{
    private final String FILE_ENCODING = "UTF-8";
    private final static int DUMP_VERSION = 1;
    private final static int PROGRESS_WIDTH = 65;
    private final static String NULL_STR = "NULL";
    private final static Map JDBCTYPE2TYPENAME = new HashMap();
    
    // differentiated types by dump
    private final static String TYPES[] = new String [ 9 ];
    private final static int HP_STRING    = 0;
    private final static int HP_INTEGER   = 1;
    private final static int HP_NUMERIC   = 2;
    private final static int HP_DOUBLE    = 3;
    private final static int HP_DATE      = 4;
    private final static int HP_TIME      = 5;
    private final static int HP_TIMESTAMP = 6;
    private final static int HP_BLOB      = 7;
    private final static int HP_CLOB      = 8;

    static {
	TYPES[ HP_STRING ]   = "STRING";
	TYPES[ HP_INTEGER ]  = "INTEGER";
	TYPES[ HP_NUMERIC ]  = "NUMERIC";
	TYPES[ HP_DOUBLE ]   = "DOUBLE";
	TYPES[ HP_DATE ]     = "DATE";
	TYPES[ HP_TIME ]     = "TIME";
	TYPES[ HP_TIMESTAMP ]= "TIMESTAMP";
	TYPES[ HP_BLOB ]     = "BLOB";
	TYPES[ HP_CLOB ]     = "CLOB";

	JDBCTYPE2TYPENAME.put(new Integer(Types.CHAR),    TYPES[ HP_STRING ]);
	JDBCTYPE2TYPENAME.put(new Integer(Types.VARCHAR), TYPES[ HP_STRING ]);

	// proprietary oracle types. Is there a better way than this ?
	JDBCTYPE2TYPENAME.put(new Integer(1111),          TYPES[ HP_DOUBLE ]);
	JDBCTYPE2TYPENAME.put(new Integer(-4),            TYPES[ HP_BLOB ]);
	JDBCTYPE2TYPENAME.put(new Integer(-1),            TYPES[ HP_CLOB ]);

	// not supported yet.
	JDBCTYPE2TYPENAME.put(new Integer(Types.BLOB),    TYPES[ HP_BLOB ]);
	JDBCTYPE2TYPENAME.put(new Integer(Types.CLOB),    TYPES[ HP_CLOB ]);
	
	// generic float.
	JDBCTYPE2TYPENAME.put(new Integer(Types.DOUBLE),  TYPES[ HP_DOUBLE ]);
	JDBCTYPE2TYPENAME.put(new Integer(Types.FLOAT),   TYPES[ HP_DOUBLE ]);

	// generic numeric. could be integer or double
	JDBCTYPE2TYPENAME.put(new Integer(Types.NUMERIC), TYPES[ HP_NUMERIC ]);
	JDBCTYPE2TYPENAME.put(new Integer(Types.DECIMAL), TYPES[ HP_NUMERIC ]);
	
	// generic integer.
	JDBCTYPE2TYPENAME.put(new Integer(Types.INTEGER), TYPES[ HP_INTEGER ]);
	JDBCTYPE2TYPENAME.put(new Integer(Types.SMALLINT),TYPES[ HP_INTEGER ]);
	JDBCTYPE2TYPENAME.put(new Integer(Types.TINYINT), TYPES[ HP_INTEGER ]);

	JDBCTYPE2TYPENAME.put(new Integer(Types.DATE),     TYPES[ HP_DATE ]);
	JDBCTYPE2TYPENAME.put(new Integer(Types.TIME),     TYPES[ HP_TIME ]);
	JDBCTYPE2TYPENAME.put(new Integer(Types.TIMESTAMP),TYPES[ HP_TIMESTAMP ]);
    }

    private final ListUserObjectsCommand _tableCompleter;
    private final LoadCommand _fileOpener;
    private volatile boolean _running;

    public DumpCommand(ListUserObjectsCommand tc, LoadCommand lc) {
	_tableCompleter = tc;
        _fileOpener = lc;
	_running = false;
    }

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] {
	    "dump-out", "dump-in", "verify-dump", "dump-conditional"
	};
    }

    /**
     * verify works without session.
     */
    public boolean requiresValidSession(String cmd) { return false; }

    /**
     * dump-in and verify-dump is complete as single-liner.
     * dump-out and dump-conditional needs a semicolon.
     */
    public boolean isComplete(String command) {
        if (command.startsWith("dump-in") || command.startsWith("verify-dump"))
            return true;
        return command.endsWith(";");
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String cmd, String param) {
	//final String FILE_ENCODING = System.getProperty("file.encoding");
	StringTokenizer st = new StringTokenizer(param);
	int argc = st.countTokens();

	if ("dump-conditional".equals(cmd)) {
	    if (session == null) {
		System.err.println("not connected.");
		return EXEC_FAILED;
	    }
	    if ((argc < 2)) return SYNTAX_ERROR;
	    String fileName = (String) st.nextElement();
	    String tabName  = (String) st.nextElement();
	    String whereClause = null;
	    if (argc >= 3) {
		whereClause = (String) st.nextToken("\n"); // till EOL
		whereClause = whereClause.trim();
		if (whereClause.toUpperCase().startsWith("WHERE")) {
		    whereClause = whereClause.substring(5);
		    whereClause = whereClause.trim();
		}
	    }
	    PrintStream out = null;
	    try {
		out = openOutputStream(fileName, FILE_ENCODING);
		int result = dumpTable(session, tabName, whereClause, out,
				       FILE_ENCODING);
		return result;
	    }
	    catch (Exception e) {
		System.err.println("failed: " + e.getMessage());
		return EXEC_FAILED;
	    }
	    finally {
		if (out != null) out.close(); 
	    }
	}
	
	else if ("dump-out".equals(cmd)) {
	    if (session == null) {
		System.err.println("not connected.");
		return EXEC_FAILED;
	    }
	    if ((argc < 2)) return SYNTAX_ERROR;
	    String fileName = (String) st.nextElement();
	    PrintStream out = null;
	    try {
		out = openOutputStream(fileName, FILE_ENCODING);
		while (st.hasMoreElements()) {
		    String tabName  = (String) st.nextElement();
		    int result = dumpTable(session, tabName, null, out,
					   FILE_ENCODING);
		    if (result != SUCCESS) {
			return result;
		    }
		}
		return SUCCESS;
	    }
	    catch (Exception e) {
		System.err.println("failed: " + e.getMessage());
		return EXEC_FAILED;
	    }
	    finally {
		if (out != null) out.close(); 
	    }
	}

	else if ("dump-in".equals(cmd)) {
	    if (session == null) {
		System.err.println("not connected. Only verify-dump possible.");
		return EXEC_FAILED;
	    }
	    if (argc < 1 || argc > 2) return SYNTAX_ERROR;
	    String fileName = (String) st.nextElement();
	    int commitPoint = -1;
	    if (argc == 2) {
		try {
		    String val = (String) st.nextElement();
		    commitPoint = Integer.valueOf(val).intValue();
		}
		catch (NumberFormatException e) {
		    System.err.println("commit point number expected: " + e);
		    return SYNTAX_ERROR;
		}
	    }
            return retryReadDump(fileName, session, commitPoint);
	}
	
	else if ("verify-dump".equals(cmd)) {
	    if (argc != 1) return SYNTAX_ERROR;
	    String fileName = (String) st.nextElement();
            return retryReadDump(fileName, null, -1);
	}
	return SYNTAX_ERROR;
    }

    /**
     * reads a dump and does a retry if the file encoding does
     * not match.
     */
    private int retryReadDump(String fileName, SQLSession session, int commitPoint) {
        LineNumberReader in = null;
        boolean hot = (session != null);
        try {
            SigIntHandler.getInstance().pushInterruptable(this);
            String fileEncoding = FILE_ENCODING;
            boolean retryPossible = true;
            do {
                try {
                    in = openInputReader(fileName, fileEncoding);
                    while (skipWhite(in)) {
                        int result = readTableDump(in, fileEncoding,
                                                   session, hot, commitPoint);
                        retryPossible = false;
                        if (!_running) {
                            System.err.print("interrupted.");
                            return result;
                        }
                        if (result != SUCCESS) {
                            return result;
                        }
                    }
                }
                catch (EncodingMismatchException e) {
                    // did we already retry with another encoding?
                    if (!fileEncoding.equals(FILE_ENCODING)) {
                        throw new Exception("got file encoding problem twice");
                    }
                    fileEncoding = e.getEncoding();
                    System.err.println("got a different encoding; retry with " + fileEncoding);
                }
            }
            while (retryPossible);
            return SUCCESS;
        }
        catch (Exception e) {
            System.err.println("failed: " + e.getMessage());
            return EXEC_FAILED;
        }
        finally {
            try { 
                if (in != null) in.close(); 
            } 
            catch (IOException e) {
                System.err.println("closing file failed.");
            }
        }
    }

    private PrintStream openOutputStream(String fileName, 
					 String encoding)
	throws IOException {
        File f = _fileOpener.openFile(fileName);
	OutputStream outStream = new FileOutputStream(f);
	if (fileName.endsWith(".gz")) {
	    outStream = new GZIPOutputStream(outStream, 4096);
	}
	return new PrintStream(outStream, false, encoding);
    }

    private LineNumberReader openInputReader(String fileName, 
					     String fileEncoding) 
	throws IOException {
        File f = _fileOpener.openFile(fileName);
	InputStream inStream = new FileInputStream(f);
	if (fileName.endsWith(".gz")) {
	    inStream = new GZIPInputStream(inStream);
	}
	Reader fileIn = new InputStreamReader(inStream, fileEncoding);
	return new LineNumberReader(fileIn);
    }

    // to make the field-name and field-type nicely aligned
    private void printWidth(PrintStream out, String s, int width,
			    boolean comma) 
    {
	out.print("'");
	out.print(s);
	out.print("'");
	if (comma) out.print(", ");
	for (int i = s.length(); i < width; ++i) {
	    out.print(' ');
        }
    }

    private int dumpTable(SQLSession session, String tabName, 
			  String whereClause,
			  PrintStream dumpOut, String fileEncoding)
	throws Exception {

	// asking for meta data is only possible with the correct
	// table name.
	boolean correctName = true;
	if (tabName.startsWith("\"")) {
	    tabName = stripQuotes(tabName);
	    correctName = false;
	}
	if (correctName) {
	    String alternative = _tableCompleter.correctTableName(tabName);
	    if (alternative != null && !alternative.equals(tabName)) {
		tabName = alternative;
		System.out.println("dumping table: '" + tabName 
				   + "' (corrected name)");
	    }
	}

	List metaList = new ArrayList();
	Connection conn = session.getConnection();
	ResultSet rset = null;
	Statement stmt = null;
	try {
            /*
             * if the same column is in more than one schema defined, then
             * oracle seems to write them out twice..
             */
            Set doubleCheck = new HashSet();
	    DatabaseMetaData meta = conn.getMetaData();
	    rset = meta.getColumns(conn.getCatalog(), null, tabName, null);
	    while (rset.next()) {
                String columnName = rset.getString(4);
                if (doubleCheck.contains(columnName))
                    continue;
                doubleCheck.add(columnName);
		metaList.add(new MetaProperty(columnName, rset.getInt(5)));
	    }
	}
	finally {
	    if (rset != null) {
		try { rset.close(); } catch (Exception e) {}
	    }
	}
	MetaProperty[] metaProperty = (MetaProperty[]) metaList.toArray(new MetaProperty[metaList.size()]);
	if (metaList.size() == 0) {
	    System.err.println("No fields in table '" + tabName 
			       + "' found.");
	    return EXEC_FAILED;
	}
	
	dumpOut.println("(tabledump '" + tabName + "'");
	dumpOut.println("  (file-encoding '" + fileEncoding + "')");
	dumpOut.println("  (dump-version " + DUMP_VERSION + " " 
			+ DUMP_VERSION + ")");
	if (whereClause != null) {
	    dumpOut.print("  (where-clause ");
	    quoteString(dumpOut, whereClause);
	    dumpOut.println(")");
	}
	dumpOut.println("  (henplus-version '" + Version.getVersion() 
			+ "')");
	dumpOut.println("  (time '" + new Timestamp(System.currentTimeMillis())
			+ "')");
	dumpOut.print("  (database-info ");
	quoteString(dumpOut, session.getDatabaseInfo());
	dumpOut.println(")");
	dumpOut.print("  (meta (");
	Iterator it = metaList.iterator();
	while (it.hasNext()) {
	    MetaProperty p = (MetaProperty) it.next();
	    printWidth(dumpOut, p.fieldName, p.renderWidth(),
		       it.hasNext());
	}
	dumpOut.println(")");
	dumpOut.print("\t(");
	it = metaList.iterator();
	while (it.hasNext()) {
	    MetaProperty p = (MetaProperty) it.next();
	    printWidth(dumpOut, p.typeName, p.renderWidth(),
		       it.hasNext());
	}
	dumpOut.println("))");
	
	long expectedRows = -1;
	try {
	    stmt = session.createStatement();
	    StringBuffer countStmt = new StringBuffer("SELECT count(*) from ");
	    countStmt.append(tabName);
	    if (whereClause != null) {
		countStmt.append(" WHERE ");
		countStmt.append(whereClause);
	    }
	    rset = stmt.executeQuery(countStmt.toString());
	    rset.next();
	    expectedRows = rset.getLong(1);
	}
	catch (Exception e) { /* ignore - not important */ }
	finally {
	    if (rset != null) {
		try { rset.close(); } catch (Exception e) {}
	    }
	    if (stmt != null) {
		try { stmt.close(); } catch (Exception e) {}
	    }
	}

	StringBuffer selectStmt = new StringBuffer("SELECT ");
	it = metaList.iterator();
	while (it.hasNext()) {
	    MetaProperty p = (MetaProperty) it.next();
	    selectStmt.append(p.fieldName);
	    if (it.hasNext()) selectStmt.append(", ");
	}
	selectStmt.append(" FROM ").append(tabName);
	if (whereClause != null) {
	    selectStmt.append(" WHERE ").append(whereClause);
	}
	//System.err.println(selectStmt.toString());

	dumpOut.print("  (data ");
	stmt = null;
	long startTime = System.currentTimeMillis();
	try {
	    long rows = 0;
	    long progressDots = 0;
	    stmt = session.createStatement();
	    rset = stmt.executeQuery(selectStmt.toString());
	    boolean isFirst = true;
	    while (rset.next()) {
		++rows;
		if (expectedRows > 0  && rows <= expectedRows) {
		    long newDots = (PROGRESS_WIDTH * rows) / expectedRows;
		    if (newDots > progressDots) {
			while (progressDots <= newDots) {
			    System.err.print(".");
			    ++progressDots;
			}
			System.err.flush();
		    }
		}
		if (!isFirst) dumpOut.print("\n\t");
		isFirst = false;
		dumpOut.print("(");
		    
		for (int i=0; i < metaProperty.length; ++i) {
		    final int col = i+1;
		    final int thisType = metaProperty[i].getType();
		    switch (thisType) {
		    case HP_INTEGER:
		    case HP_NUMERIC:
		    case HP_DOUBLE: {
			String val = rset.getString(col);
			if (rset.wasNull()) 
			    dumpOut.print( NULL_STR );
			else
			    dumpOut.print(val);
			break;
		    }

		    case HP_TIMESTAMP: {
			Timestamp val = rset.getTimestamp(col);
			if (rset.wasNull())
			    dumpOut.print( NULL_STR );
			else {
			    quoteString(dumpOut, val.toString());
			}
			break;
		    }

		    case HP_TIME: {
			Time val = rset.getTime(col);
			if (rset.wasNull())
			    dumpOut.print( NULL_STR );
			else {
			    quoteString(dumpOut, val.toString());
			}
			break;
		    }

		    case HP_DATE: {
			java.sql.Date val = rset.getDate(col);
			if (rset.wasNull())
			    dumpOut.print( NULL_STR );
			else {
			    quoteString(dumpOut, val.toString());
			}
			break;
		    }

		    case HP_STRING: {
			String val = rset.getString(col);
			if (rset.wasNull())
			    dumpOut.print( NULL_STR );
			else {
			    quoteString(dumpOut, val);
			}
			break;
		    }
			
		    default:
			throw new IllegalArgumentException("type " + 
							   TYPES[thisType]
							   + " not supported yet");
		    }
		    if (metaProperty.length > col) 
			dumpOut.print(",");
		    else
			dumpOut.print(")");
		}
	    }
	    dumpOut.println(")");
	    dumpOut.println("  (rows " + rows + "))\n");

	    System.err.print("(" + rows + " rows)\n");
	    long execTime = System.currentTimeMillis()-startTime;
	    TimeRenderer.printTime(execTime, System.err);
	    System.err.print(" total; ");
	    TimeRenderer.printFraction(execTime, rows, System.err);
	    System.err.println(" / row");
	    if (expectedRows >= 0 && rows != expectedRows) {
		System.err.println("\nWarning: 'select count(*)' in the"
				   + " beginning resulted in " + expectedRows
				   + " but the dump exported " + rows 
				   + " rows");
	    }
	}
	catch (Exception e) {
	    System.err.println(selectStmt.toString());
	    throw e; // handle later.
	}
	finally {
	    if (rset != null) {
		try { rset.close(); } catch (Exception e) {}
	    }
	    if (stmt != null) {
		try { stmt.close(); } catch (Exception e) {}
	    }
	}
	return SUCCESS;
    }
    
    private Number readNumber(LineNumberReader in) throws IOException {
	String token = readToken(in);
	// separated sign.
	if (token.length() == 1 
	    && (token.equals("+") || token.equals("-"))) {
	    token += readToken(in);
	}
	if (token.equals( NULL_STR )) return null;
	try {
	    if (token.indexOf('.') > 0) {
		return Double.valueOf(token);
	    }
	    if (token.length() < 10) {
		return Integer.valueOf(token);
	    }
	    else {
		return Long.valueOf(token);
	    }
	}
	catch (NumberFormatException e) {
	    raiseException(in,
			   "Number format " + token + ": " + e.getMessage());
	}
	return null;
    }

    private int readTableDump(LineNumberReader reader, String fileEncoding,
			      SQLSession session, boolean hot,
			      int commitPoint)
	throws IOException, SQLException, InterruptedException {
	MetaProperty[] metaProperty = null;
	String tableName = null;
	int    dumpVersion = -1;
	int    compatibleVersion = -1;
	String henplusVersion = null;
	String databaseInfo = null;
	String dumpTime = null;
	String whereClause = null;
	int c;
	char ch;
	String token;
	long importedRows = -1;
	long expectedRows = -1;
	long problemRows  = -1;
	Connection conn = null;
	PreparedStatement stmt = null;

	expect(reader, '(');
	token = readToken(reader);
	if (!"tabledump".equals(token)) raiseException(reader, 
						       "'tabledump' expected");
	tableName = readString(reader);
	long startTime = System.currentTimeMillis();
	_running = true; // interruptable
	while (_running) {
	    skipWhite(reader);
	    int rawChar = reader.read();
	    if (rawChar == -1) return SUCCESS; // EOF reached.
	    char inCh = (char) rawChar;
	    if (inCh == ')') break;
	    if (inCh != '(') {
		raiseException(reader, "'(' or ')' expected");
	    }
	    token = readToken(reader);

	    if ("dump-version".equals(token)) {
		token = readToken(reader);
		try {
		    dumpVersion = Integer.valueOf(token).intValue();
		}
		catch (Exception e) {
		    raiseException(reader, "expected dump version number");
		}
		token = readToken(reader);
		try {
		    compatibleVersion = Integer.valueOf(token).intValue();
		}
		catch (Exception e) {
		    raiseException(reader, "expected compatible version number");
		}
		checkSupported(compatibleVersion);
		expect(reader, ')');
	    }

	    else if ("file-encoding".equals(token)) {
		token = readString(reader);
		if (!token.equals(fileEncoding)) {
                    throw new EncodingMismatchException(token);
		}
		expect(reader, ')');
	    }

	    else if ("henplus-version".equals(token)) {
		token = readString(reader);
		henplusVersion = token;
		expect(reader, ')');
	    }

	    else if ("rows".equals(token)) {
		token = readToken(reader);
		expectedRows = Integer.valueOf(token).intValue();
		expect(reader, ')');
	    }

	    else if ("database-info".equals(token)) {
		databaseInfo = readString(reader);
		expect(reader, ')');
	    }

	    else if ("where-clause".equals(token)) {
		whereClause = readString(reader);
		expect(reader, ')');
	    }

	    else if ("time".equals(token)) {
		dumpTime = readString(reader);
		expect(reader, ')');
	    }

	    else if ("meta".equals(token)) {
		if (dumpVersion < 0 || compatibleVersion < 0) {
		    raiseException(reader, "cannot read meta data without dump-version information");
		}
		metaProperty = parseMetaData(reader);
	    }

	    else if ("data".equals(token)) {
		if (metaProperty == null) {
		    raiseException(reader, "no meta-data available");
		}
		if (tableName == null) {
		    raiseException(reader, "no table name known");
		}
		if (hot) {
		    StringBuffer prep = new StringBuffer("INSERT INTO ");
		    prep.append(tableName);
		    prep.append(" (");
		    for (int i = 0; i < metaProperty.length; ++i) {
			prep.append(metaProperty[i].fieldName);
			if (i+1 < metaProperty.length) prep.append(",");
		    }
		    prep.append(") VALUES (");
		    for (int i = 0; i < metaProperty.length; ++i) {
			prep.append("?");
			if (i+1 < metaProperty.length) prep.append(",");
		    }
		    prep.append(")");
		    //System.err.println(prep.toString());
		    conn = session.getConnection();
		    stmt = conn.prepareStatement(prep.toString());
		}

		System.err.println((hot ? "importing" : "verifying")
				   + " table dump created with HenPlus "
				   + henplusVersion 
				   + "\nfor table           : " + tableName
				   + "\nfrom database       : " + databaseInfo
				   + "\nat                  : " + dumpTime
				   + "\ndump format version : " + dumpVersion);
		if (whereClause != null) {
		    System.err.println("projection          : " + whereClause);
		}

		System.err.print("reading rows..");
		System.err.flush();
		boolean rowBefore = false;
		importedRows = 0;
		problemRows = 0;
		_running = true;
		while (_running) {
		    skipWhite(reader);
		    inCh = (char) reader.read();
		    if (inCh == ')') break;
		    if (inCh != '(') {
			raiseException(reader, "'(' or ')' expected");
		    }
		    // we are now at the beginning of the row.
		    ++importedRows;
		    for (int i=0; i < metaProperty.length; ++i) {
			final int col = i+1;
			final int type = metaProperty[i].type;
			switch (type) {
			case HP_NUMERIC:
			case HP_DOUBLE:
			case HP_INTEGER: {
			    Number number = readNumber(reader);
			    if (stmt != null) {
				if (number == null) {
				    if (type == HP_NUMERIC) {
					stmt.setNull(col, Types.NUMERIC);
				    }
				    else if (type == HP_INTEGER) {
					stmt.setNull(col, Types.INTEGER);
				    }
				    else if (type == HP_DOUBLE) {
					stmt.setNull(col, Types.DOUBLE);
				    }
				}
				else {
				    if (number instanceof Integer) {
					stmt.setInt(col, number.intValue());
				    }
				    else if (number instanceof Long) {
					stmt.setLong(col, number.longValue());
				    }
				    else if (number instanceof Double) {
					stmt.setDouble(col,
						       number.doubleValue());
				    }
				}
			    }
			    break;
			}

			case HP_TIMESTAMP: {
			    String val = readString(reader);
			    if (stmt != null) {
				if (val == null) {
				    stmt.setTimestamp(col, null);
				}
				else {
				    stmt.setTimestamp(col,
						      Timestamp.valueOf(val));
				}
			    }
			    break;
			}
			    
			case HP_TIME: {
			    String val = readString(reader);
			    if (stmt != null) {
				if (val == null) {
				    stmt.setTime(col, null);
				}
				else {
				    stmt.setTime(col, Time.valueOf(val));
				}
			    }
			    break;
			}

			case HP_DATE: {
			    String val = readString(reader);
			    if (stmt != null) {
				if (val == null) {
				    stmt.setDate(col, null);
				}
				else {
				    stmt.setDate(col, 
						 java.sql.Date.valueOf(val));
				}
			    }
			    break;
			}
			    
			case HP_STRING: {
			    String val = readString(reader);
			    if (stmt != null) {
				stmt.setString(col, val);
			    }
			    break;
			}
			    
			default:
			    throw new IllegalArgumentException("type " + 
							       TYPES[metaProperty[i].type]
							       + " not supported yet");
			}
			expect(reader, (i+1 < metaProperty.length) ?',':')');
		    }
		    rowBefore = true;
		    try {
			if (stmt != null) stmt.execute();
		    }
		    catch (SQLException e) {
			String msg = e.getMessage();
			// oracle adds CR for some reason.
			if (msg != null) msg = msg.trim();
                        reportProblem(msg);
			++problemRows;
		    }
		    
		    // commit every once in a while.
		    if (hot 
			&& (commitPoint >= 0) 
			&& importedRows % commitPoint == 0) {
			conn.commit();
		    }
		}
	    }
	    
	    else {
		System.err.println("ignoring unknown token " + token);
		dumpTime = readString(reader);
		expect(reader, ')');
	    }
	}

	// return final count.
        finishProblemReports();
        
	// final commit, if commitPoints are enabled.
	if (hot && commitPoint >= 0) {
	    conn.commit();
	}

        // we're done.
        if (stmt != null) {
            try {
                stmt.close();
            }
            catch (Exception e) {
            }
        }

	if (expectedRows >= 0 && expectedRows != importedRows) {
	    System.err.println("WARNING: expected " + expectedRows 
			       + " but got " + importedRows + " rows");
	}
	else {
	    System.err.print("ok. ");
	}
	System.err.print("(" + importedRows + " rows total");
	if (hot) System.err.print(" / " + problemRows + " with errors");
	System.err.print("; ");
	long execTime = System.currentTimeMillis()-startTime;
	TimeRenderer.printTime(execTime, System.err);
	System.err.print(" total; ");
	TimeRenderer.printFraction(execTime, importedRows, System.err);
	System.err.println(" / row)");
	return SUCCESS;
    }

    public MetaProperty[] parseMetaData(LineNumberReader in) throws IOException {
	List metaList = new ArrayList();
	expect(in, '(');
	for (;;) {
	    String colName = readString(in);
	    metaList.add(new MetaProperty(colName));
	    skipWhite(in);
	    char inCh = (char) in.read();
	    if (inCh == ')') break;
	    if (inCh != ',') {
		raiseException(in, "',' or ')' expected");
	    }
	}
	expect(in, '(');
	MetaProperty[] result = (MetaProperty[]) metaList.toArray(new MetaProperty[metaList.size()]);
	for (int i = 0; i < result.length; ++i) {
	    String typeName = readString(in);
	    result[i].setTypeName(typeName);
	    expect(in, (i+1 < result.length) ? ',' : ')');
	}
	expect(in, ')');
	return result;
    }

    String lastProblem = null;
    long problemCount = 0;
    private void reportProblem(String msg) {
        if (msg == null) return;
        if (msg.equals(lastProblem)) {
            ++problemCount;
        }
        else {
            finishProblemReports();
            problemCount = 1;
            System.err.print("Problem: " + msg);
            lastProblem = msg;
        }
    }

    private void finishProblemReports() {
        if (problemCount > 1) {
            System.err.print("   (" + problemCount + " times)");
        }
        if (problemCount > 0) {
            System.err.println();
        }
        lastProblem = null;
        problemCount = 0;
    }

    public void checkSupported(int version) throws IllegalArgumentException {
	if (version <= 0 || version > DUMP_VERSION) {
	    throw new IllegalArgumentException("incompatible dump-version");
	}
    }

    public void expect(LineNumberReader in, char ch) throws IOException {
	skipWhite(in);
	char inCh = (char) in.read();
	if (ch != inCh) raiseException(in, "'" + ch + "' expected");
    }

    private void quoteString(PrintStream out, String in) {
	StringBuffer buf = new StringBuffer();
	buf.append("'");
	int len = in.length();
	for (int i=0; i < len; ++i) {
	    char c = in.charAt(i);
	    if (c == '\'' || c == '\\') {
		buf.append("\\");
	    }
	    buf.append(c);
	}
	buf.append("'");
	out.print(buf.toString());
    }

    /**
     * skip whitespace. return false, if EOF reached.
     */
    private boolean skipWhite(Reader in) throws IOException {
	in.mark(1);
	int c;
	while ((c = in.read()) > 0) {
	    if (!Character.isWhitespace((char)c)) {
		in.reset();
		return true;
	    }
	    in.mark(1);
	}
	return false;
    }

    private String readToken(LineNumberReader in) throws IOException {
	skipWhite(in);
	StringBuffer token = new StringBuffer();
	in.mark(1);
	int c;
	while ((c = in.read()) > 0) {
	    char ch = (char) c;
	    if (Character.isWhitespace(ch)
		|| ch == ';' || ch == ',' 
		|| ch == '(' || ch == ')') {
		in.reset();
		break;
	    }
	    token.append(ch);
	    in.mark(1);
	}
	return token.toString();
    }

    /**
     * read a string. This is either NULL without quotes or a quoted
     * string.
     */
    private String readString(LineNumberReader in) throws IOException {
	int nullParseState = 0;
	int c;
	while ((c = in.read()) > 0) {
	    char ch = (char) c;
	    // unless we already parse the NULL string, skip whitespaces.
	    if (nullParseState == 0 && Character.isWhitespace(ch)) continue;
	    if (ch == '\'') break; // -> opening string.
	    if (Character.toUpperCase(ch) == NULL_STR.charAt(nullParseState)) {
		++nullParseState;
		if (nullParseState == NULL_STR.length()) return null;
		continue;
	    }
	    raiseException(in, "unecpected character '" + ch + "'");
	}
	
	// ok, we found an opening quote.
	StringBuffer result = new StringBuffer();
	while ((c = in.read()) > 0) {
	    if (c == '\\') {
		c = in.read();
		if (c < 0) {
		    raiseException(in, "excpected character after backslash escape");
		}
		result.append((char) c);
		continue;
	    }
	    char ch = (char) c;
	    if (ch == '\'') break; // End Of String.
	    result.append((char) c);
	}
	return result.toString();
    }

    /**
     * convenience method to throw Exceptions containing the line
     * number
     */
    private void raiseException(LineNumberReader in, String msg) 
	throws IOException {
	throw new IOException ("line " + (in.getLineNumber()+1)
			       + ": " + msg);
    }
    
    //-- Interruptable interface
    public synchronized void interrupt() {
	_running = false;
    }

    /**
     * complete the table name.
     */
    public Iterator complete(CommandDispatcher disp,
			     String partialCommand, String lastWord) 
    {
	StringTokenizer st = new StringTokenizer(partialCommand);
	String cmd = (String) st.nextElement();
	int argc = st.countTokens();
	if (lastWord.length() > 0) {
	    argc--;
	}
	
	if ("dump-conditional".equals(cmd)) {
	    if (argc == 0) {
		return new FileCompletionIterator(partialCommand, lastWord);
	    }
	    else if (argc == 1) {
		if (lastWord.startsWith("\"")) {
		    lastWord = lastWord.substring(1);
		}
		return _tableCompleter.completeTableName(lastWord);
	    }
	    else if (argc > 1) {
		st.nextElement(); // discard filename.
		String table = (String) st.nextElement();
		Collection columns = _tableCompleter.columnsFor(table);
		NameCompleter compl = new NameCompleter(columns);
		return compl.getAlternatives(lastWord);
	    }
	}
	else if ("dump-out".equals(cmd)) {
	    // this is true for dump-out und verify-dump
	    if (argc == 0) {
		return new FileCompletionIterator(partialCommand, lastWord);
	    }
	    if (argc > 0) {
		if (lastWord.startsWith("\"")) {
		    lastWord = lastWord.substring(1);
		}
		final HashSet  alreadyGiven = new HashSet();
		/*
		 * do not complete the tables we already gave on the
		 * commandline.
		 */
		while (st.hasMoreElements()) {
		    alreadyGiven.add((String) st.nextElement());
		}
		final Iterator it = _tableCompleter.completeTableName(lastWord);
		return new Iterator() {
			String table = null;
			public boolean hasNext() {
			    while (it.hasNext()) {
				table = (String) it.next();
				if (alreadyGiven.contains(table)) {
				    continue;
				}
				return true;
			    }
			    return false;
			}
			public Object  next() { return table; }
			public void remove() { 
			    throw new UnsupportedOperationException("no!");
			}
		    };
	    }
	}
	else {
	    if (argc == 0) {
		return new FileCompletionIterator(partialCommand, lastWord);
	    }
	}
	return null;
    }

    private String stripQuotes(String value) {
	if (value.startsWith("\"") && value.endsWith("\"")) {
	    value = value.substring(1, value.length()-1);
	}
	return value;
    }

    /**
     * return a descriptive string.
     */
    public String  getShortDescription() {
	return "handle table dumps";
    }

    public String getSynopsis(String cmd) {
	if ("dump-out".equals(cmd)) {
	    return cmd + " <filename> <tablename> [<tablename> ..]";
	}
	if ("dump-conditional".equals(cmd)) {
	    return cmd + " <filename> <tablename> [<where-clause>]";
	}
	else if ("dump-in".equals(cmd)) {
	    return cmd + " <filename> [<commitpoints>]";
	}
	else if ("verify-dump".equals(cmd)) {
	    return cmd + " <filename>";
	}
	return cmd;
    }

    public String getLongDescription(String cmd) {
	String dsc = null;
	if ("dump-out".equals(cmd)) {
	    dsc= "\tDump out the contents of the table(s) given to the file\n"
		+"\twith the given name. If the filename ends with '.gz', the\n"
		+"\tcontent is gzip'ed automatically .. that saves space.\n\n"
		+"\tThe dump-format allows to read in the data back into\n"
		+"\tthe database ('dump-in' command). And unlike pure SQL-insert\n"
		+"\tstatements, this works even across databases.\n"
		+"\tSo you can make a dump of your MySQL database and read it\n"
		+"\tback into Oracle, for instance. To achive this database\n"
		+"\tindependence, the data is stored in a canonical form\n"
		+"\t(e.g. the Time/Date). The file itself is human readable\n"
		+"\tand uses less space than simple SQL-inserts:\n"
		+"\t----------------\n"
		+"\t  (tabledump 'student'\n"
		+"\t    (dump-version 1 1)\n"
		+"\t    (henplus-version 0.3.3)\n"
		+"\t    (database-info 'MySQL - 3.23.47')\n"
		+"\t    (meta ('name',   'sex',    'student_id')\n"
		+"\t          ('STRING', 'STRING', 'INTEGER'   ))\n"
		+"\t    (data ('Megan','F',1)\n"
		+"\t          ('Joseph','M',2)\n"
		+"\t          ('Kyle','M',3)\n"
		+"\t          ('Mac Donald\\'s','M',4))\n"
		+"\t    (rows 4))\n"
		+"\t----------------\n\n"
		+"\tTODOs\n"
		+"\tThis format contains only the data, no\n"
		+"\tcanonical 'create table' statement - so the table must\n"
		+"\talready exist at import time. Both these features will\n"
		+"\tbe in later versions of HenPlus.";
	}

	else if ("dump-conditional".equals(cmd)) {
	    dsc= "\tLike dump-out, but dump only the rows of a single table\n"
		+"\tthat match the where clause.";
	}
	
	else if ("dump-in".equals(cmd)) {
	    dsc= "\tRead back in the data that has been dumped out with the\n"
		+"\t'dump-out' command. If the filename ends with '.gz',\n"
		+"\tthen the content is assumed to be gzipped and is\n"
		+"\tunpacked on the fly. The 'dump-in' command fills\n"
		+"\texisting tables, it does not create missing ones!\n\n"
                +"\tExisting content ist not deleted before, dump-in just\n"
                +"\tinserts all data found in the dump.\n\n"
		+"\tInternally, the import uses a prepared statement that is\n"
		+"\tfed with the typed data according to the meta data (see\n"
		+"\tdump-out for the file format). This evens out differences\n"
		+"\tbetween databases and of course enhances speed compared\n"
		+"\tto non-prepared statements.\n\n"
		+"\tThe import is done in the current transaction, unless\n"
		+"\tyou specify the commitpoints. The commitpoints specify\n"
		+"\tthe number of inserts, that are executed before an commit\n"
		+"\tis done. For a large amount of data this option is\n"
		+"\tnecessary, since otherwise your rollback-segments\n"
		+"\tmight get a problem ;-)";
	}

	else if ("verify-dump".equals(cmd)) {
	    dsc= "\tLike dump-in, but a 'dry run'. Won't change anything\n"
		+"\tbut parses the whole file to determine whether it has\n"
		+"\tsyntax errors or is damaged. Any syntax errors are\n"
		+"\treported as it were a 'dump-in'. Problems that might\n"
		+"\toccur in a 'real' import in the database (that might\n"
		+"\tdetect, that the import would create duplicate keys for\n"
		+"\tinstance) can not be determined, of course.";
	}
	return dsc;
    }

    private static class MetaProperty {
	public final String fieldName;
	public int type;
	public String typeName;
	
	public MetaProperty(String fieldName) {
	    this.fieldName = fieldName;
	}
	public MetaProperty(String fieldName, int jdbcType) {
	    this.fieldName = fieldName;
	    this.typeName = (String) JDBCTYPE2TYPENAME.get(new Integer(jdbcType));
	    if (this.typeName == null) {
		throw new IllegalArgumentException("cannot handle type '"
						   + type + "'");
	    }
	    this.type = findType(typeName);
	}
	public void setTypeName(String typeName) {
	    this.type = findType(typeName);
	}
	
	/**
	 * find the type in the array. uses linear search, but this is
	 * only a small list.
	 */
	private int findType(String typeName) {
	    if (typeName == null) {
		throw new IllegalArgumentException("empty type ?");
	    }
	    typeName = typeName.toUpperCase();
	    for (int i=0; i < TYPES.length; ++i) {
		if (TYPES[i].equals(typeName))
		    return i;
	    }
	    throw new IllegalArgumentException("invalid type " + typeName);
	}

	public int getType() { return type; }
	public int renderWidth() {
	    return Math.max(typeName.length(), fieldName.length());
	}
    }

    private static class EncodingMismatchException extends IOException {
        private final String _encoding;
        public EncodingMismatchException(String encoding) {
            super("file encoding Mismatch Exception; got " + encoding);
            _encoding = encoding;
        }
        public String getEncoding() { return _encoding; }
    }

    // reading BLOBs.
    //private static class Base64InputStream extends InputStream { }

    // reading CLOBs
    //private static class Base64Reader extends Reader { }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
