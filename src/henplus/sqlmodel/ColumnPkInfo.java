/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 */
package henplus.sqlmodel;

/**
 * <p>Title: ColumnPkInfo</p>
 * <p>Description:<br>
 * Created on: 30.07.2003</p>
 * @version $Id: ColumnPkInfo.java,v 1.3 2004-03-07 14:22:03 hzeller Exp $ 
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
public final class ColumnPkInfo {
        
    private final String _pkName;
    private final int _columnIndex;
        
    public ColumnPkInfo(String pkName, int columnIndex) {
        _pkName = pkName;
        _columnIndex = columnIndex;
    }
        
    public int getColumnIndex() {
        return _columnIndex;
    }

    public String getPkName() {
        return _pkName;
    }
    
    public boolean equals (Object other) {
        boolean result = false;
        if (other != null && other instanceof ColumnPkInfo) {
            ColumnPkInfo o = (ColumnPkInfo)other;
            if ( _pkName != null && _pkName.equals(o.getPkName())
                 || _pkName == null && o.getPkName() == null ) {
                result = true;
            }
        }
        return result;
    }

}
