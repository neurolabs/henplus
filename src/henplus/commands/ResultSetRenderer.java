/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: ResultSetRenderer.java,v 1.22 2005-06-18 04:58:13 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.HenPlus;
import henplus.Interruptable;
import henplus.OutputDevice;
import henplus.view.Column;
import henplus.view.ColumnMetaData;
import henplus.view.TableRenderer;

import java.io.Reader;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

/**
 * document me.
 */
public class ResultSetRenderer implements Interruptable {
    private final ResultSet _rset;
    private final ResultSetMetaData _meta;
    private final TableRenderer _table;
    private final int _columns;
    private final int[] _showColumns;

    private boolean _beyondLimit;
    private long _firstRowTime;
    private final long _clobLimit = 8192;
    private final int _rowLimit;
    private volatile boolean _running;

    public ResultSetRenderer(final ResultSet rset, final String columnDelimiter,
            final boolean enableHeader, final boolean enableFooter, final int limit,
            final OutputDevice out, final int[] show) throws SQLException {
        this._rset = rset;
        _beyondLimit = false;
        _firstRowTime = -1;
        _showColumns = show;
        _rowLimit = limit;
        _meta = rset.getMetaData();
        _columns = show != null ? show.length : _meta.getColumnCount();
        _table = new TableRenderer(getDisplayMeta(_meta), out, columnDelimiter,
                enableHeader, enableFooter);
    }

    public ResultSetRenderer(final ResultSet rset, final String columnDelimiter,
            final boolean enableHeader, final boolean enableFooter, final int limit,
            final OutputDevice out) throws SQLException {
        this(rset, columnDelimiter, enableHeader, enableFooter, limit, out,
                null);
    }

    // Interruptable interface.
    public synchronized void interrupt() {
        _running = false;
    }

    public ColumnMetaData[] getDisplayMetaData() {
        return _table.getMetaData();
    }

    private String readClob(final Clob c) throws SQLException {
        if (c == null) {
            return null;
        }
        final StringBuffer result = new StringBuffer();
        long restLimit = _clobLimit;
        try {
            final Reader in = c.getCharacterStream();
            final char buf[] = new char[4096];
            int r;

            while (restLimit > 0
                    && (r = in.read(buf, 0, (int) Math.min(buf.length,
                            restLimit))) > 0) {
                result.append(buf, 0, r);
                restLimit -= r;
            }
        } catch (final Exception e) {
            HenPlus.msg().println(e.toString());
        }
        if (restLimit == 0) {
            result.append("...");
        }
        return result.toString();
    }

    public int execute() throws SQLException {
        int rows = 0;

        _running = true;
        try {
            while (_running && _rset.next()) {
                final Column[] currentRow = new Column[_columns];
                for (int i = 0; i < _columns; ++i) {
                    final int col = _showColumns != null ? _showColumns[i] : i + 1;
                    String colString;
                    if (_meta.getColumnType(col) == Types.CLOB) {
                        colString = readClob(_rset.getClob(col));
                    } else {
                        colString = _rset.getString(col);
                    }
                    final Column thisCol = new Column(colString);
                    currentRow[i] = thisCol;
                }
                if (_firstRowTime < 0) {
                    // read first row completely.
                    _firstRowTime = System.currentTimeMillis();
                }
                _table.addRow(currentRow);
                ++rows;
                if (rows >= _rowLimit) {
                    _beyondLimit = true;
                    break;
                }
            }

            _table.closeTable();
            if (!_running) {
                try {
                    _rset.getStatement().cancel();
                } catch (final Exception e) {
                    HenPlus.msg().println(
                            "cancel statement failed: " + e.getMessage());
                }
            }
        } finally {
            _rset.close();
        }
        return rows;
    }

    public boolean limitReached() {
        return _beyondLimit;
    }

    public long getFirstRowTime() {
        return _firstRowTime;
    }

    /**
     * determine meta data necesary for display.
     */
    private ColumnMetaData[] getDisplayMeta(final ResultSetMetaData m)
    throws SQLException {
        final ColumnMetaData result[] = new ColumnMetaData[_columns];

        for (int i = 0; i < result.length; ++i) {
            final int col = _showColumns != null ? _showColumns[i] : i + 1;
            int alignment = ColumnMetaData.ALIGN_LEFT;
            final String columnLabel = m.getColumnLabel(col);
            /*
             * int width = Math.max(m.getColumnDisplaySize(i),
             * columnLabel.length());
             */
            switch (m.getColumnType(col)) {
            case Types.NUMERIC:
            case Types.INTEGER:
            case Types.REAL:
            case Types.SMALLINT:
            case Types.TINYINT:
                alignment = ColumnMetaData.ALIGN_RIGHT;
                break;
            }
            result[i] = new ColumnMetaData(columnLabel, alignment);
        }
        return result;
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
