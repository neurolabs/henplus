/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.SQLSession;
import henplus.AbstractCommand;
import java.util.StringTokenizer;
import java.io.PrintStream;
import java.sql.*;

/**
 * document me.
 */
public class DescribeCommand extends AbstractCommand {
    static final boolean LEFT        = true;
    static final boolean RIGHT       = false;

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
	return new String[] { "describe" }; 
    }

    /**
     * execute the command given.
     */
    public int execute(SQLSession session, String command) {
	StringTokenizer st = new StringTokenizer(command);
	String cmd = (String) st.nextElement();
	int argc = st.countTokens();
	if (argc != 1) {
	    return SYNTAX_ERROR;
	}
	try {
	    describeTable(System.out, "Table", (String) st.nextElement(),
			  session.getUsername(), session.getConnection());
	}
	catch (SQLException e) {
	    System.err.println(e.getMessage());
	    return EXEC_FAILED;
	}
	return SUCCESS;
    }

    private void describeTable (PrintStream out, 
				String objectType, String tabname, 
				String owner,
				Connection conn) 
	throws SQLException {
	Statement stmt = conn.createStatement(); // use SQLSession.createSt.()
	ResultSet rset = null;
	String ownerSelect;
	// oracle: always uppercase
	tabname = tabname.toUpperCase();
	if (owner != null) {
	    owner = owner.toUpperCase();
	    ownerSelect = " and owner='" + owner + "'";
	}
	else {
	    ownerSelect = "";
	}

	/*
	 * describe the tables tablespace
	 */
	if ("Table".equals(objectType)) {
	    rset = stmt.executeQuery ("select tablespace_name from all_tables "
				      + "where table_name='" + tabname + "'"
				      +  ownerSelect);
	    if (rset.next()) {
		out.println ("in tablespace " + rset.getString(1));
		rset.close();
	    }
	}
    
	/*
	 * Describe the columns
	 */
	rset = stmt.executeQuery ("select max(length(COLUMN_NAME)) from all_tab_columns "
				  + "where table_name='" + tabname + "'"
				  + ownerSelect);
	if (!rset.next()) {
	    throw new SQLException ("Unable to read column description");
	}
	int breit = (int) rset.getLong (1);
	rset = stmt.executeQuery ("select column_name,data_type,"+
				  "data_length,data_precision,nullable"+
				  " from all_tab_columns"+
				  " where table_name ='" + tabname + "'"
				  + ownerSelect);
	Statement constrStmt = conn.createStatement();
	ResultSet constrRset = null;
    
	while (rset.next()) {
	    out.print ("\t");
	    String Column = rset.getString(1);
	    formatString (Column, ' ', breit + 2, LEFT);
	    String type = rset.getString (2);
	    if (type.equals("NUMBER")) {
		type = type + "(" + rset.getString(4) + ")";
	    }
	    else if (type.equals("CHAR") || type.startsWith ("VARCHAR")) {
		type = type + "(" + rset.getString(3) + ")";
	    }
	    formatString (type, ' ', 15, LEFT);
	    if ((rset.getString(5)).equals("N"))
		out.print ("NOT NULL");
	    else
		out.print ("        ");
	
	    if ("Table".equals(objectType)) {
		/*
		 * Leider kann man die all_cons_columns
		 * nicht mit der all_tab_colmuns zusammenjoinen
		 */
		constrRset = constrStmt
		    .executeQuery ("select all_constraints.constraint_name,"+
				   " constraint_type,r_constraint_name,"+
				   " delete_rule,status" +
				   " from all_constraints,all_cons_columns"+
				   " where all_cons_columns.table_name='" + tabname + "'" +
				   "   and column_name='" + Column + "'"+
				   "   and all_constraints.table_name=all_cons_columns.table_name"+
				   "   and all_constraints.owner=all_cons_columns.owner"+
				   "   and all_constraints.constraint_name=all_cons_columns.constraint_name"+
				   "   and constraint_type != 'C'");
		int consNum = 0;
		while (constrRset.next()) {
		    if (consNum > 0) {
			out.print ("\n");
			formatString (" ", ' ', breit + 15 + 8 + 10, LEFT);
		    }
		    out.print (" constraint " + constrRset.getString(1));
		    out.print (" ");
		    if ("P".equals(constrRset.getString(2)))
			out.print ("PRIMARY KEY");
		    else if ("R".equals(constrRset.getString(2))) {
			out.print ("\n");
			formatString (" ", ' ', breit + 2 + 15 + 8 + 11, LEFT);
			out.print ("references KEY (" + constrRset.getString(3) + ")");
			out.print ("\n");
			formatString (" ", ' ', breit + 2 + 15 + 8 + 11, LEFT);
			out.print ("on delete " + constrRset.getString(4));
		    }
		    if ("DISABLED".equals(constrRset.getString(5))) {
			out.print ("\n");
			formatString (" ", ' ', breit + 2 + 15 + 8 + 11, LEFT);
			out.print ("[constraint disabled]");
		    }
		    consNum++;
		}
		constrRset.close();
	    }
	    out.println ();
	}
	constrStmt.close();
	/*
	 * describe the view (if it is a view ..)
	 */
	if ("View".equals(objectType)) {
	    Statement ViewStmt = conn.createStatement ();
	    rset = ViewStmt.executeQuery ("select text from all_views "+
					  "where view_name='" + tabname + "'"
					  + ownerSelect);
	    if (rset.next()) {
		out.println ("----------------\nVIEW definition:\n " + rset.getString(1));
	    }
	    rset.close();
	    ViewStmt.close();
	}
    }

  private void  formatString (String out, char fillchar, 
			      int len, boolean alignment) {
	StringBuffer fillstr = new StringBuffer();
	
	if (len > 4000)
	    len = 4000;
	
	if (out == null)
	    out = "[NULL]";
	int slen = out.length();
	
	for (int i = slen+1 ; i <= len ; i++)
	    fillstr.append (fillchar);
	
	if (alignment == LEFT) {
	    System.out.print (out);
	}
	System.out.print (fillstr.toString());
	if (alignment == RIGHT) {
	    System.out.print (out);
	}
    }
    
    /**
     * return a descriptive string.
     */
    public String  getShortDescription() { 
	return "describe a database object";
    }
    
    public String getSynopsis(String cmd) {
	return "describe <table|view|index>";
    }

    public String getLongDescription(String cmd) {
	String dsc;
	dsc="\tDescribe the meta information of the named user object.\n" +
	    "\tA table,  for instance,  is described as a CREATE TABLE\n" +
	    "\tstatement.";
	return dsc;
    }

}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
