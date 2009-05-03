/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.importparser;

import java.io.Reader;
import java.util.Calendar;

/**
 * A Parser for a
 */
public class ImportParser {
    private static final int INIT_SIZE = 8192;

    private final TypeParser[] _parsers;
    private final char[] _colDelim;
    private final char[] _rowDelim;

    public ImportParser(final TypeParser[] parsers, final String colDelim, final String rowDelim) {
        _parsers = parsers;
        _colDelim = new char[colDelim.length()];
        colDelim.getChars(0, colDelim.length(), _colDelim, 0);
        _rowDelim = new char[rowDelim.length()];
        rowDelim.getChars(0, rowDelim.length(), _rowDelim, 0);
    }

    // fixme: build in read-ahead in case colDelim and rowDelim have the
    // same prefix..build fast state machine
    // allows for multiple ways to delimit rows and columns
    public void parse(final Reader input, final ValueRecipient recipient) throws Exception {
        // local variable access is faster
        final char[] colPattern = _colDelim;
        int colPatternPos = 0;

        final char[] rowPattern = _rowDelim;
        int rowPatternPos = 0;

        char[] buffer = new char[INIT_SIZE];
        int fieldStart = 0;
        int pos = 0;
        int currentColumn = 0;
        int currentRow = 1;

        for (;;) {
            if (buffer.length - pos == 0) { // need to adjust buffer
                if (fieldStart > 0) { // remove unneded stuff in front
                    System.arraycopy(buffer, fieldStart, buffer, 0,
                            buffer.length - fieldStart);
                    // System.out.println("**shift buffer from " + fieldStart);
                    pos -= fieldStart;
                    fieldStart = 0;
                } else { // fieldStart is already at 0, so increase size
                    final char[] newBuffer = new char[buffer.length * 2];
                    System.arraycopy(buffer, fieldStart, newBuffer, 0,
                            buffer.length - fieldStart);
                    buffer = newBuffer;
                    // System.out.println("**larger buffer..");
                }
            }
            final int bytesRead = input.read(buffer, pos, buffer.length - pos);
            if (bytesRead < 0) {
                break; // EOF
            }

            final int bufferEnd = pos + bytesRead;
            while (pos < bufferEnd) {
                final char c = buffer[pos++];

                // column pattern matches ?
                if (colPattern[colPatternPos] == c) {
                    colPatternPos++;
                    if (colPatternPos >= colPattern.length) { // match!
                        if (currentColumn < _parsers.length) {
                            final TypeParser colParser = _parsers[currentColumn];
                            if (colParser != null) {
                                colParser.parse(buffer, fieldStart, pos
                                        - fieldStart - colPattern.length,
                                        recipient);
                            }
                        }
                        colPatternPos = 0;
                        fieldStart = pos;
                        currentColumn++;
                    }
                } else {
                    colPatternPos = 0; // no match. restart pattern..
                }

                // row pattern matches ?
                if (rowPattern[rowPatternPos] == c) {
                    rowPatternPos++;
                    if (rowPatternPos >= rowPattern.length) { // match!
                        if (currentColumn < _parsers.length - 1) {
                            System.err
                            .println("less columns than expected in row "
                                    + currentRow
                                    + ": expected "
                                    + _parsers.length
                                    + " but got "
                                    + (currentColumn + 1));
                        }
                        if (currentColumn < _parsers.length) {
                            final TypeParser colParser = _parsers[currentColumn];
                            if (colParser != null) {
                                colParser.parse(buffer, fieldStart, pos
                                        - fieldStart - rowPattern.length,
                                        recipient);
                            }
                        }
                        if (recipient.finishRow()) {
                            return;
                        }
                        fieldStart = pos;
                        rowPatternPos = 0;
                        currentColumn = 0;
                        currentRow++;
                    }
                } else {
                    rowPatternPos = 0; // no match. restart pattern..
                }
            }
        }
    }

    static int count = 0;

    public static void main(final String argv[]) throws Exception {
        final Reader r = new java.io.FileReader(argv[0]);
        final int cols = Integer.parseInt(argv[1]);
        final TypeParser[] parsers = new TypeParser[cols];
        for (int i = 0; i < cols; ++i) {
            parsers[i] = new StringParser(i + 1);
        }
        final ValueRecipient v = new ValueRecipient() {
            public void setLong(final int fieldNumber, final long value) {
            }

            public void setString(final int fieldNumber, final String value) {
                System.out.println("'" + value + "'");
            }

            public void setDate(final int fieldNumber, final Calendar cal) {
            }

            public boolean finishRow() {
                System.out.println(">>row done..<<");
                count++;
                return false;
            }
        };
        final ImportParser parser = new ImportParser(parsers, "\",\"", "\n\"");
        parser.parse(r, v);
        System.err.println("COUNT: " + count);
    }
}
