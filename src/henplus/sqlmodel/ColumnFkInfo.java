/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 */
package henplus.sqlmodel;

import henplus.util.ObjectUtil;

/**
 * <p>
 * Title: ColumnFkInfo
 * </p>
 * <p>
 * Description:<br>
 * Created on: 01.08.2003
 * </p>
 * 
 * @version $Id: ColumnFkInfo.java,v 1.4 2004-09-22 11:49:32 magrokosmos Exp $
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
public final class ColumnFkInfo {

    private final String _fkName;
    private final String _pkTable;
    private final String _pkColumn;

    public ColumnFkInfo(final String fkName, final String pkTable, final String pkColumn) {
        _fkName = fkName;
        _pkTable = pkTable;
        _pkColumn = pkColumn;
    }

    /**
     * @return the name of the foreign key.
     */
    public String getFkName() {
        return _fkName;
    }

    @Override
    public boolean equals(final Object other) {
        if (other != null && other instanceof ColumnFkInfo) {
            final ColumnFkInfo o = (ColumnFkInfo) other;
            if (_fkName != null && !_fkName.equals(o.getFkName())
                    || _fkName == null && o.getFkName() != null) {
                return false;
            } else if (_pkTable != null && !_pkTable.equals(o.getPkTable())
                    || _pkTable == null && o.getPkTable() != null) {
                return false;
            } else if (_pkColumn != null && !_pkColumn.equals(o.getPkColumn())
                    || _pkColumn == null && o.getPkColumn() != null) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ObjectUtil.nullSafeHashCode(_fkName)
        ^ ObjectUtil.nullSafeHashCode(_pkTable)
        ^ ObjectUtil.nullSafeHashCode(_pkColumn);
    }

    /**
     * @return the primary key colum name (should this return a Column?)
     */
    public String getPkColumn() {
        return _pkColumn;
    }

    /**
     * @return the name of the primary key table (should this return a Table?)
     */
    public String getPkTable() {
        return _pkTable;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ColumnFkInfo [");
        sb.append("fkName: ").append(_fkName);
        sb.append(", pkTable: ").append(_pkTable);
        sb.append(", pkColumn: ").append(_pkColumn);
        sb.append("]");
        return sb.toString();
    }

}
