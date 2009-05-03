/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 */
package henplus.view;

import henplus.OutputDevice;

/**
 * <p>
 * Title: ExtendedTableRenderer.
 * </p>
 * <p>
 * Description:<br>
 * Created on: 25.07.2003
 * </p>
 * 
 * @version $Id: ExtendedTableRenderer.java,v 1.5 2004-03-05 23:34:38 hzeller
 *          Exp $
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
public class ExtendedTableRenderer extends TableRenderer {

    public ExtendedTableRenderer(final ColumnMetaData[] meta, final OutputDevice out,
            final String separator, final boolean showHeader, final boolean showFooter) {
        super(meta, out, separator, showHeader, showFooter);
    }

    public ExtendedTableRenderer(final ColumnMetaData[] meta, final OutputDevice out) {
        this(meta, out, "|", true, true);
    }

    /**
     * Checks for each element in the array its type, so can handle
     * <code>ExtendedColumn</code> correctly.
     * 
     * @param row
     */
    @Override
    protected void updateColumnWidths(final Column[] row) {
        int metaIndex = 0;
        for (int i = 0; i < row.length; ++i) {
            if (row[i] instanceof ExtendedColumn
                    && ((ExtendedColumn) row[i]).getColspan() > 1) {
                final ExtendedColumn col = (ExtendedColumn) row[i];
                metaIndex = updateMetaWidth(metaIndex, col);

            } else {
                meta[i].updateWidth(row[i].getWidth());
                metaIndex++;
            }
        }
    }

    private int updateMetaWidth(int metaIndex, final ExtendedColumn col) {
        final int span = col.getColspan();
        // calculate the summarized width of concerned tables
        int sumWidth = 0;
        int participating = 0; // count the DISPLAYED columns
        for (int j = metaIndex; j < metaIndex + span; j++) {
            if (!meta[j].doDisplay()) {
                continue;
            }
            sumWidth += meta[j].getWidth();
            participating++;
        }
        // test if the spanning column's width is greater than the sum
        if (col.getWidth() > sumWidth) {
            // to each meta col add the same amount
            final int diff = (col.getWidth() - sumWidth) / participating;
            for (int j = metaIndex; j < metaIndex + span; j++) {
                if (!meta[j].doDisplay()) {
                    continue;
                }
                meta[j].updateWidth(meta[j].getWidth() + diff);
            }
        }
        // set the metaIndex
        metaIndex += span;
        return metaIndex;
    }

    /**
     * Overwrites the <code>TableRenderer</code>s implementation for special
     * handling of <code>ExtendedColumn</code>s.
     */
    @Override
    protected boolean printColumns(final Column[] currentRow, boolean hasMoreLines) {
        int metaIndex = 0;
        // iterate over the elements of the given row
        for (int i = 0; i < currentRow.length; ++i) {
            if (currentRow[i] instanceof ExtendedColumn) {
                final ExtendedColumn col = (ExtendedColumn) currentRow[i];
                hasMoreLines = printColumn(col, hasMoreLines, metaIndex);
                metaIndex += col.getColspan();
            } else {
                if (!meta[metaIndex].doDisplay()) {
                    continue;
                }
                hasMoreLines = printColumn(currentRow[i], hasMoreLines,
                        metaIndex);
                metaIndex++;
            }
        }
        return hasMoreLines;
    }

    protected boolean printColumn(final ExtendedColumn col, boolean hasMoreLines,
            final int metaIndex) {
        String txt;
        out.print(" ");
        // get summarized width of meta cols
        final int span = col.getColspan();
        int width = 0;
        if (span > 1) {
            for (int i = metaIndex; i < metaIndex + span; i++) {
                if (!meta[i].doDisplay()) {
                    continue;
                }
                width += meta[i].getWidth();
            }
            // for each column after the first one add spaces for the
            // surrounding spaces and the separator
            width += (span - 1) * 3;
        } else {
            width = meta[metaIndex].getWidth();
        }

        txt = formatString(col.getNextLine(), ' ', width, col.getAlignment());
        hasMoreLines |= col.hasNextLine();

        if (col.isBoldRequested()) {
            out.attributeBold();
        } else if (col.isNull()) {
            out.attributeGrey();
        }

        out.print(txt);

        if (col.isNull() || col.isBoldRequested()) {
            out.attributeReset();
        }

        out.print(colSeparator);
        return hasMoreLines;
    }
}
