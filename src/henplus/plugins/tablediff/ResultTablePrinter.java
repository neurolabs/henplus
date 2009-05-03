/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 */
package henplus.plugins.tablediff;

import henplus.HenPlus;
import henplus.Command;
import henplus.sqlmodel.ColumnFkInfo;
import henplus.sqlmodel.ColumnPkInfo;
import henplus.view.Column;
import henplus.view.ColumnMetaData;
import henplus.view.ExtendedColumn;
import henplus.view.ExtendedTableRenderer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SortedSet;

/**
 * <p>
 * Title: ResultTablePrinter
 * </p>
 * <p>
 * Description:<br>
 * Created on: 24.07.2003
 * </p>
 * 
 * @version $Id: ResultTablePrinter.java,v 1.6 2005-06-18 04:58:13 hzeller Exp $
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
public final class ResultTablePrinter {

    private static final ColumnMetaData[] DESC_META;
    static {
        DESC_META = new ColumnMetaData[8];
        DESC_META[0] = new ColumnMetaData("status", ColumnMetaData.ALIGN_CENTER);
        DESC_META[1] = new ColumnMetaData("#", ColumnMetaData.ALIGN_RIGHT);
        DESC_META[2] = new ColumnMetaData("column");
        DESC_META[3] = new ColumnMetaData("type");
        DESC_META[4] = new ColumnMetaData("null");
        DESC_META[5] = new ColumnMetaData("default");
        DESC_META[6] = new ColumnMetaData("pk");
        DESC_META[7] = new ColumnMetaData("fk");
    }

    private static final String STAT_MODIFIED_ORG = "M-";
    private static final String STAT_MODIFIED_NEW = "M+";
    private static final String STAT_REMOVED = "-";
    private static final String STAT_ADDED = "+";
    private static final String YES = "YES";
    private static final String NO = "NO";

    public static int printResult(final TableDiffResult result) {
        /*
         * if all columns belong to the same table name, then don't report it. A
         * different table name may only occur in rare circumstance like object
         * oriented databases.
         */
        // boolean allSameTableName = true;
        /*
         * build up actual describe table.
         */
        final List<Column[]> rows = new ArrayList<Column[]>();
        if (result != null) {
            // first, print removed columns
            final SortedSet<henplus.sqlmodel.Column> removed = result.getRemovedColumns();
            if (removed != null) {
                // ExtendedColumn header = new ExtendedColumn("Removed Columns",
                // 8, ExtendedColumn.ALIGN_CENTER);
                // rows.add(header);
                appendLines(STAT_REMOVED, rows, removed);
            }
            // then, print added columns
            final SortedSet added = result.getAddedColumns();
            if (added != null) {
                appendLines(STAT_ADDED, rows, added);
            }
            // at last, print modified columns
            final LinkedHashMap<henplus.sqlmodel.Column, henplus.sqlmodel.Column> modified = result.getModifiedColumns();
            if (modified != null) {
                appendModified(rows, modified);
            }
        }

        /*
         * we render the table now, since we only know know, whether we will
         * show the first column or not.
         */
        final ExtendedTableRenderer table = new ExtendedTableRenderer(DESC_META,
                HenPlus.out());
        final Iterator it = rows.iterator();
        while (it.hasNext()) {
            table.addRow((Column[]) it.next());
        }
        table.closeTable();
        return Command.SUCCESS;
    }

    private static void appendLines(final String symbol, final List rows, final SortedSet rowSet) {
        final Iterator iter = rowSet.iterator();
        while (iter.hasNext()) {
            final henplus.sqlmodel.Column col = (henplus.sqlmodel.Column) iter.next();

            final Column[] row = new Column[8];
            row[0] = new Column(symbol);
            row[1] = new Column(col.getPosition());
            row[2] = new Column(col.getName());
            final String type = extractType(col);
            row[3] = new Column(type);
            row[4] = new Column(col.isNullable() ? YES : NO);

            final String defaultVal = col.getDefault();
            // oracle appends newline to default values for some reason.
            row[5] = new Column((defaultVal != null ? defaultVal.trim()
                    : null));

            // String pkdesc = (String)pks.get(colname);
            row[6] = new Column(getPkDesc(col));
            // String fkdesc = (String)fks.get(colname);
            row[7] = new Column(getFkDesc(col));
            rows.add(row);
        }
    }

    private static String extractType(final henplus.sqlmodel.Column col) {
        String type = col.getType();
        final int colSize = col.getSize();
        if (colSize > 0) {
            final StringBuffer sb = new StringBuffer(type);
            sb.append("(").append(colSize).append(")");
            type = sb.toString();
        }
        return type;
    }

    private static void appendModified(final List rows, 
            final LinkedHashMap<henplus.sqlmodel.Column, henplus.sqlmodel.Column> modified) {
        final Iterator<henplus.sqlmodel.Column> iter = modified.keySet().iterator();
        while (iter.hasNext()) {
            final henplus.sqlmodel.Column org = iter.next();
            final henplus.sqlmodel.Column mod = modified.get(org);

            final ExtendedColumn[] orgView = new ExtendedColumn[8];
            final ExtendedColumn[] modView = new ExtendedColumn[8];

            orgView[0] = new ExtendedColumn(STAT_MODIFIED_ORG, DESC_META[0]
                                                                         .getAlignment());
            modView[0] = new ExtendedColumn(STAT_MODIFIED_NEW, DESC_META[0]
                                                                         .getAlignment());

            // if this was modified it doesn't matter
            orgView[1] = new ExtendedColumn(org.getPosition(), DESC_META[1]
                                                                         .getAlignment());
            modView[1] = new ExtendedColumn(mod.getPosition(), DESC_META[1]
                                                                         .getAlignment());

            // this should not differ
            orgView[2] = new ExtendedColumn(org.getName(), DESC_META[2]
                                                                     .getAlignment());
            modView[2] = new ExtendedColumn(mod.getName(), DESC_META[2]
                                                                     .getAlignment());

            final String orgType = extractType(org);
            final String modType = extractType(mod);
            orgView[3] = new ExtendedColumn(orgType, DESC_META[3]
                                                               .getAlignment());
            modView[3] = new ExtendedColumn(modType, DESC_META[3]
                                                               .getAlignment());
            if (!modType.equals(orgType)) {
                markAsChanged(modView[3]);
            }

            orgView[4] = new ExtendedColumn(org.isNullable() ? YES : NO,
                    DESC_META[4].getAlignment());
            modView[4] = new ExtendedColumn(mod.isNullable() ? YES : NO,
                    DESC_META[4].getAlignment());
            if (org.isNullable() != mod.isNullable()) {
                markAsChanged(modView[4]);
            }

            // System.out.println("default: " + org.getDefault());
            final String orgDefaultVal = org.getDefault() != null ? org
                    .getDefault().trim() : null;
                    // oracle appends newline to default values for some reason.
                    orgView[5] = new ExtendedColumn(orgDefaultVal, DESC_META[5]
                                                                             .getAlignment());

                    final String modDefaultVal = mod.getDefault() != null ? mod
                            .getDefault().trim() : null;
                            modView[5] = new ExtendedColumn(modDefaultVal, DESC_META[5]
                                                                                     .getAlignment());
                            if (orgDefaultVal != null && !orgDefaultVal.equals(modDefaultVal)
                                    || orgDefaultVal == null && modDefaultVal != null) {
                                markAsChanged(modView[5]);
                            }

                            // primary key
                            final String pkDescOrg = getPkDesc(org);
                            final String pkDescMod = getPkDesc(mod);
                            orgView[6] = new ExtendedColumn(pkDescOrg, DESC_META[6]
                                                                                 .getAlignment());
                            modView[6] = new ExtendedColumn(pkDescMod, DESC_META[6]
                                                                                 .getAlignment());
                            // check if one of the cols has to be marked as changed
                            if (org.isPartOfPk() && !mod.isPartOfPk()) {
                                markAsChanged(orgView[6]);
                            } else if (!org.isPartOfPk() && mod.isPartOfPk()) {
                                markAsChanged(modView[6]);
                            } else if (org.isPartOfPk() && mod.isPartOfPk()) {
                                // compare values of pk names
                                if (org.getPkInfo().getPkName() != null
                                        && !org.getPkInfo().getPkName().equals(
                                                mod.getPkInfo().getPkName())) {
                                    markAsChanged(modView[6]);
                                }
                            }

                            // foreign key
                            final String fkDescOrg = getFkDesc(org);
                            final String fkDescMod = getFkDesc(mod);
                            orgView[7] = new ExtendedColumn(fkDescOrg, DESC_META[7]
                                                                                 .getAlignment());
                            modView[7] = new ExtendedColumn(fkDescMod, DESC_META[7]
                                                                                 .getAlignment());
                            // check if one of the cols has to be marked as changed
                            if (org.isForeignKey() && !mod.isForeignKey()) {
                                markAsChanged(orgView[7]);
                            } else if (!org.isForeignKey() && mod.isForeignKey()) {
                                markAsChanged(modView[7]);
                            } else if (org.isForeignKey() && mod.isForeignKey()) {
                                // compare values of pk names
                                if (!org.getFkInfo().equals(mod.getFkInfo())) {
                                    markAsChanged(modView[7]);
                                }
                            }

                            rows.add(orgView);
                            rows.add(modView);
        }
    }

    private static void markAsChanged(final ExtendedColumn col) {
        col.setBoldRequested(true);
    }

    private static String getPkDesc(final henplus.sqlmodel.Column col) {
        String pkDesc = "";

        if (col.isPartOfPk()) {
            final ColumnPkInfo pkInfo = col.getPkInfo();
            if (pkInfo.getColumnIndex() == 1) {
                pkDesc = pkInfo.getPkName() != null ? pkInfo.getPkName()
                        : "*";
            } else { // the pk index is greater than 1
                pkDesc = pkInfo.getPkName() != null ? pkInfo.getPkName()
                        : "*";
                pkDesc = new StringBuffer(pkDesc).append("{").append(
                        pkInfo.getColumnIndex()).append("}").toString();
            }
        }

        return pkDesc;
    }

    private static String getFkDesc(final henplus.sqlmodel.Column col) {
        String fkDesc = "";

        final ColumnFkInfo fkInfo = col.getFkInfo();
        if (fkInfo != null) {
            final StringBuffer sb = new StringBuffer();
            if (fkInfo.getFkName() != null) {
                sb.append(fkInfo.getFkName()).append("\n -> ");
            } else {
                sb.append(" -> ");
            }
            sb.append(fkInfo.getPkTable()).append("(").append(
                    fkInfo.getPkColumn()).append(")");
            fkDesc = sb.toString();
        }

        return fkDesc;
    }

}
