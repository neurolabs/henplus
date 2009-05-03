/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: HistoryWriter.java,v 1.3 2005-11-27 16:20:27 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;

import org.gnu.readline.Readline;

/**
 * A utility class that writes the history. This especially handles multiline
 * elements. This should be some Reader/Writer, that handles reading/writing of
 * escaped lines. For now, it is just a collection of static methods. Quick hack
 * to make storing of multiline statements work..
 */
public class HistoryWriter {

    public static void writeReadlineHistory(final OutputStream out)
    throws IOException {
        final PrintWriter w = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
        final int len = Readline.getHistorySize();
        for (int i = 0; i < len; ++i) {
            String line = Readline.getHistoryLine(i);
            if (line == null) {
                continue;
            }
            line = escape(line);
            w.println(line);
        }
        w.close();
    }

    public static void readReadlineHistory(final InputStream in) throws IOException {
        // todo: check first utf-8, then default-encoding to be
        // backward-compatible.
        readReadlineHistory(new InputStreamReader(in, "UTF-8"));
    }

    private static void readReadlineHistory(final Reader in) throws IOException {
        final Reader r = new BufferedReader(in);
        final StringBuffer line = new StringBuffer();
        int c;
        do {
            while ((c = r.read()) >= 0 && c != '\n') {
                final char ch = (char) c;
                if (ch == '\\') {
                    line.append((char) r.read());
                } else {
                    line.append(ch);
                }
            }
            if (line.length() > 0) {
                Readline.addToHistory(line.toString());
                line.setLength(0);
            }
        } while (c >= 0);
        r.close();
    }

    private static String escape(final String s) {
        if (s.indexOf('\\') >= 0 || s.indexOf('\n') >= 0) {
            final StringBuffer out = new StringBuffer();
            for (int i = 0; i < s.length(); ++i) {
                final char c = s.charAt(i);
                switch (c) {
                case '\\':
                    out.append("\\\\");
                    break;
                case '\n':
                    out.append("\\\n");
                    break;
                default:
                    out.append(c);
                }
            }
            return out.toString();
        } else {
            return s;
        }
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
