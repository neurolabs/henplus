/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */

import java.util.Properties;

public class HenPlus {
    public final static void main(String argv[]) throws Exception {
	Properties properties = new Properties();
	SQLSession session;
	properties.setProperty("driver.Oracle.class", 
			       "oracle.jdbc.driver.OracleDriver");
	properties.setProperty("driver.DB2.class",
			       "COM.ibm.db2.jdbc.net.DB2Driver");
	String cpy;
	cpy = 
"-------------------------------------------------------------------------\n"
+" HenPlus II 0.1 Copyright(C) 1997, 2001 Henner Zeller <H.Zeller@acm.org>\n"
+" HenPlus is provided AS IS and comes with ABSOLUTELY NO WARRANTY\n"
+" This is free software, and you are welcome to redistribute it under the\n"
+" conditions of the GNU Public License <http://www.gnu.org/>\n"
+"-------------------------------------------------------------------------\n";
	System.err.println(cpy);
	try {
	    session = new SQLSession(properties, argv);
	}
	catch (Exception e) {
	    e.printStackTrace();
	    return;
	}
	session.run();
    }
}
