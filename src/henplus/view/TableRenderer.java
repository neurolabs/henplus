/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: TableRenderer.java,v 1.7 2005-06-18 04:58:13 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.view;

import henplus.OutputDevice;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/**
 * document me.
 */
public class TableRenderer {

    private static final int MAX_CACHE_ELEMENTS = 500;

    private final List _cacheRows;
    private boolean _alreadyFlushed;
    private int _writtenRows;
    private final int _separatorWidth;

    private final boolean _enableHeader;
    private final boolean _enableFooter;

    protected final ColumnMetaData meta[];
    protected final OutputDevice out;
    protected final String colSeparator;

    public TableRenderer(final ColumnMetaData[] meta, final OutputDevice out,
            final String separator, final boolean enableHeader, final boolean enableFooter) {
        this.meta = meta;
        this.out = out;
        this._enableHeader = enableHeader;
        this._enableFooter = enableFooter;

        /*
         * we cache the rows in order to dynamically determine the output width
         * of each column.
         */
        this._cacheRows = new ArrayList(MAX_CACHE_ELEMENTS);
        this._alreadyFlushed = false;
        this._writtenRows = 0;
        this.colSeparator = " " + separator;
        this._separatorWidth = separator.length();
    }

    public TableRenderer(final ColumnMetaData[] meta, final OutputDevice out) {
        this(meta, out, "|", true, true);
    }

    public void addRow(final Column[] row) {
        updateColumnWidths(row);
        addRowToCache(row);
    }

    protected void addRowToCache(final Column[] row) {
        _cacheRows.add(row);
        if (_cacheRows.size() >= MAX_CACHE_ELEMENTS) {
            flush();
            _cacheRows.clear();
        }
    }

    /**
     * return the meta data that is used to display this table.
     */
    public ColumnMetaData[] getMetaData() {
        return meta;
    }

    /**
     * Overwrite this method if you need to handle customized columns.
     * 
     * @param row
     */
    protected void updateColumnWidths(final Column[] row) {
        for (int i = 0; i < meta.length; ++i) {
            row[i].setAutoWrap(meta[i].getAutoWrap());
            meta[i].updateWidth(row[i].getWidth());
        }
    }

    public void closeTable() {
        flush();
        if (_writtenRows > 0 && _enableFooter) {
            printHorizontalLine();
        }
    }

    /**
     * flush the cached rows.
     */
    public void flush() {
        if (!_alreadyFlushed) {
            if (_enableHeader) {
                printTableHeader();
            }
            _alreadyFlushed = true;
        }
        final Iterator rowIterator = _cacheRows.iterator();
        while (rowIterator.hasNext()) {
            final Column[] currentRow = (Column[]) rowIterator.next();
            boolean hasMoreLines;
            do {
                hasMoreLines = false;
                hasMoreLines = printColumns(currentRow, hasMoreLines);
                out.println();
            } while (hasMoreLines);
            ++_writtenRows;
        }
    }

    protected boolean printColumns(final Column[] currentRow, boolean hasMoreLines) {
        for (int i = 0; i < meta.length; ++i) {
            if (!meta[i].doDisplay()) {
                continue;
            }
            hasMoreLines = printColumn(currentRow[i], hasMoreLines, i);
        }
        return hasMoreLines;
    }

    protected boolean printColumn(final Column col, boolean hasMoreLines, final int i) {
        String txt;
        out.print(" ");
        txt = formatString(col.getNextLine(), ' ', meta[i].getWidth(), meta[i]
                                                                            .getAlignment());
        hasMoreLines |= col.hasNextLine();
        if (col.isNull()) {
            out.attributeGrey();
        }
        out.print(txt);
        if (col.isNull()) {
            out.attributeReset();
        }
        out.print(colSeparator);
        return hasMoreLines;
    }

    private void printHorizontalLine() {
        for (int i = 0; i < meta.length; ++i) {
            if (!meta[i].doDisplay()) {
                continue;
            }
            String txt;
            txt = formatString("", '-',
                    meta[i].getWidth() + _separatorWidth + 1,
                    ColumnMetaData.ALIGN_LEFT);
            out.print(txt);
            out.print("+");
        }
        out.println();
    }

    private void printTableHeader() {
        printHorizontalLine();
        for (int i = 0; i < meta.length; ++i) {
            if (!meta[i].doDisplay()) {
                continue;
            }
            String txt;
            txt = formatString(meta[i].getLabel(), ' ', meta[i].getWidth() + 1,
                    ColumnMetaData.ALIGN_CENTER);
            out.attributeBold();
            out.print(txt);
            out.attributeReset();
            out.print(colSeparator);
        }
        out.println();
        printHorizontalLine();
    }

    protected String formatString(String text, final char fillchar, int len,
            final int alignment) {
        // System.out.println("[formatString] len: " + len + ", text.length: " +
        // text.length());
        // text = "hi";
        final StringBuffer fillstr = new StringBuffer();

        if (len > 4000) {
            len = 4000;
        }

        if (text == null) {
            text = "[NULL]";
        }
        final int slen = text.length();

        if (alignment == ColumnMetaData.ALIGN_LEFT) {
            fillstr.append(text);
        }
        int fillNumber = len - slen;
        int boundary = 0;
        if (alignment == ColumnMetaData.ALIGN_CENTER) {
            boundary = fillNumber / 2;
        }
        while (fillNumber > boundary) {
            fillstr.append(fillchar);
            --fillNumber;
        }
        if (alignment != ColumnMetaData.ALIGN_LEFT) {
            fillstr.append(text);
        }
        while (fillNumber > 0) {
            fillstr.append(fillchar);
            --fillNumber;
        }
        return fillstr.toString();
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
