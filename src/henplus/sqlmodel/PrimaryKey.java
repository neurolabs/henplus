/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: PrimaryKey.java,v 1.3 2004-03-07 14:22:03 hzeller Exp $
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
package henplus.sqlmodel;

import henplus.util.ObjectUtil;

import java.util.HashMap;
import java.util.Map;

public final class PrimaryKey {

    public static final int INVALID_INDEX = -1;

    private String _name;
    private final Map <String, ColumnPkInfo> _columns; // column name -> pk info
    // specific to column

    public PrimaryKey() {
        _columns = new HashMap<String, ColumnPkInfo>();
    }

    public void addColumn(final String columnName, final String columnPkName,
            final int columnPkIndex) {
        _columns.put(columnName, new ColumnPkInfo(columnPkName, columnPkIndex));
    }

    public boolean columnParticipates(final String column) {
        return _columns.containsKey(column);
    }

    /*
     * public int getColumnIndex(String column) { int result = INVALID_INDEX;
     * ColumnPkInfo info = (ColumnPkInfo)_columns.get(column); if (info != null)
     * result = info.getColumnIndex(); return result; }
     */

    public ColumnPkInfo getColumnPkInfo(final String column) {
        return (ColumnPkInfo) _columns.get(column);
    }

    public Map getColumns() {
        return _columns;
    }

    public String getName() {
        return _name;
    }

    public void setName(final String string) {
        _name = string;
    }

    @Override
    public boolean equals(final Object other) {
        if (other != null && other instanceof PrimaryKey) {
            final PrimaryKey o = (PrimaryKey) other;
            if (_name != null && !_name.equals(o.getName()) || _name == null
                    && o.getName() != null) {
                return false;
            }

            if (_columns != null && !_columns.equals(o.getColumns())
                    || _columns == null && o.getColumns() != null) {
                return false;
            }

            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return ObjectUtil.nullSafeHashCode(_name)
        ^ ObjectUtil.nullSafeHashCode(_columns);
    }

}
