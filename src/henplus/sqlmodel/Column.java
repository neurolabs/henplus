/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: Column.java,v 1.3 2004-03-07 14:22:02 hzeller Exp $
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
package henplus.sqlmodel;

/**
 * Represents the meta data for a telational table Column
 * 
 * @author Martin Grotzke
 */
public final class Column implements Comparable {
    private String _name;
    private int _position; // starting at 1
    private String _type;
    private int _size;
    private boolean _nullable;
    private String _default;
    private ColumnPkInfo _pkInfo;
    private ColumnFkInfo _fkInfo;

    public Column(final String name) {
        _name = name;
    }

    public String getName() {
        return _name;
    }

    public void setName(final String string) {
        _name = string;
    }

    public String getDefault() {
        return _default;
    }

    public String getType() {
        return _type;
    }

    /**
     * Set the default value for this Column.
     * 
     * @param defaultValue
     */
    public void setDefault(final String defaultValue) {
        _default = defaultValue;
    }

    public void setType(final String string) {
        _type = string;
    }

    public int getSize() {
        return _size;
    }

    public void setSize(final int i) {
        _size = i;
    }

    public boolean isNullable() {
        return _nullable;
    }

    public void setNullable(final boolean b) {
        _nullable = b;
    }

    public int getPosition() {
        return _position;
    }

    public void setPosition(final int i) {
        _position = i;
    }

    public boolean isPartOfPk() {
        return _pkInfo != null;
    }

    public ColumnPkInfo getPkInfo() {
        return _pkInfo;
    }

    public void setPkInfo(final ColumnPkInfo pkInfo) {
        _pkInfo = pkInfo;
    }

    public boolean isForeignKey() {
        return _fkInfo != null;
    }

    public ColumnFkInfo getFkInfo() {
        return _fkInfo;
    }

    public void setFkInfo(final ColumnFkInfo info) {
        _fkInfo = info;
    }

    /*
     * Compares both <code>Column</code>s according to their position.
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final Object o) {
        int result = 1;
        final Column other = (Column) o;
        if (other.getPosition() < _position) {
            result = -1;
        } else if (other.getPosition() == _position) {
            result = 0;
        }
        return result;
    }

    /**
     * 
     * @param o
     * @param colNameIgnoreCase
     *            specifies if column names shall be compared in a case
     *            insensitive way.
     * @return if the columns are equal
     */
    public boolean equals(final Object o, final boolean colNameIgnoreCase) {
        if (o instanceof Column) {
            final Column other = (Column) o;

            if (_size != other._size) {
                return false;
            }

            // ignore the position, it's not important
            /*
             * if (_position != other._position) return false;
             */

            if (_nullable != other._nullable) {
                return false;
            }

            if (_name == null && other._name != null
                    || _name != null && (colNameIgnoreCase
                            && !_name.equalsIgnoreCase(other._name) || !colNameIgnoreCase
                            && !_name.equals(other._name))) {
                return false;
            }

            if (_type == null && other._type != null
                    || _type != null && !_type.equals(other._type)) {
                return false;
            }

            if (_default == null && other._default != null
                    || _default != null && !_default.equals(other._default)) {
                return false;
            }

            if (_pkInfo == null && other._pkInfo != null
                    || _pkInfo != null && !_pkInfo.equals(other._pkInfo)) {
                return false;
            }

            if (_fkInfo == null && other._fkInfo != null
                    || _fkInfo != null && !_fkInfo.equals(other._fkInfo)) {
                return false;
            }

        }
        return true;
    }

}
