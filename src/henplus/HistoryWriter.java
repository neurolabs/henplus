/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: HistoryWriter.java,v 1.2 2004-03-05 23:34:38 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

import org.gnu.readline.Readline;

/**
 * A utility class that writes the history. This especially handles 
 * multiline elements. This should be some Reader/Writer, that handles
 * reading/writing of escaped lines. For now, it is just a collection
 * of static methods. Quick hack to make storing of multiline statements
 * work..
 */
public class HistoryWriter {

    public static void writeReadlineHistory(String filename) 
	throws IOException {
	File f = new File(filename);
	PrintWriter w = new PrintWriter(new FileWriter(f));
	int len = Readline.getHistorySize();
	for (int i=0; i < len; ++i) {
	    String line = Readline.getHistoryLine(i);
	    if (line == null) continue;
	    line = escape(line);
	    w.println(line);
	}
	w.close();
    }

    public static void readReadlineHistory(String filename) 
	throws IOException {
	File f = new File(filename);
	Reader r = new BufferedReader(new FileReader(f));
	StringBuffer line = new StringBuffer();
	int c;
	do {
	    while ((c = r.read()) >= 0 && c != '\n') {
		char ch = (char) c;
		if (ch == '\\') {
		    line.append((char) r.read());
		}
		else {
		    line.append(ch);
		}
	    }
	    if (line.length() > 0) {
		Readline.addToHistory(line.toString());
		line.setLength(0);
	    }
	}
	while (c >= 0);
	r.close();
    }
    
    private static String escape(String s) {
	if (s.indexOf('\\') >= 0 || s.indexOf('\n') >= 0) {
	    StringBuffer out = new StringBuffer();
	    for (int i=0; i < s.length(); ++i) {
		char c = s.charAt(i);
		switch (c) {
		case '\\': out.append("\\\\"); break;
		case '\n': out.append("\\\n"); break;
		default: out.append(c);
		}
	    }
	    return out.toString();
	}
	else {
	    return s;
	}
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
