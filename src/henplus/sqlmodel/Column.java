/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: Column.java,v 1.2 2004-01-27 18:16:33 hzeller Exp $ 
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
package henplus.sqlmodel;


public final class Column implements Comparable {
     
     private String _name;
     private int _position; // starting at 1
     private String _type;
     private int _size;
     private boolean _nullable;
     private String _default;
     private ColumnPkInfo _pkInfo;
     private ColumnFkInfo _fkInfo;
     
     public Column (String name) {
         _name = name;
     }

    /**
     * @return
     */
    public String getName() {
        return _name;
    }

    /**
     * @param string
     */
    public void setName(String string) {
        _name = string;
    }

    /**
     * @return
     */
    public String getDefault() {
        return _default;
    }

    /**
     * @return
     */
    public String getType() {
        return _type;
    }

    /**
     * @param string
     */
    public void setDefault(String string) {
        _default = string;
    }

    /**
     * @param string
     */
    public void setType(String string) {
        _type = string;
    }

    /**
     * @return
     */
    public int getSize() {
        return _size;
    }

    /**
     * @param i
     */
    public void setSize(int i) {
        _size = i;
    }

    /**
     * @return
     */
    public boolean isNullable() {
        return _nullable;
    }

    /**
     * @param b
     */
    public void setNullable(boolean b) {
        _nullable = b;
    }

    /**
     * @return
     */
    public int getPosition() {
        return _position;
    }

    /**
     * @param i
     */
    public void setPosition(int i) {
        _position = i;
    }

    /**
     * @return
     */
    public boolean isPartOfPk() {
        return _pkInfo != null;
    }
    
    public ColumnPkInfo getPkInfo() {
        return _pkInfo;
    }
    
    public void setPkInfo(ColumnPkInfo pkInfo) {
        _pkInfo = pkInfo;
    }
    
    public boolean isForeignKey() {
        return _fkInfo != null;
    }

    /**
     * @return
     */
    public ColumnFkInfo getFkInfo() {
        return _fkInfo;
    }

    /**
     * @param info
     */
    public void setFkInfo(ColumnFkInfo info) {
        _fkInfo = info;
    }

    /* 
     * Compares both <code>Column</code>s according to their position.
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        int result = 1;
        Column other = (Column)o;
        if ( other.getPosition() < _position )
            result = -1;
        else if ( other.getPosition() == _position )
            result = 0;
        return result;
    }
    
    /**
     * 
     * @param o
     * @param colNameIgnoreCase  specifies if column names shall be compared in a case insensitive way.
     * @return
     */
    public boolean equals(Object o, boolean colNameIgnoreCase) {
        if (o instanceof Column) {
            Column other = (Column)o;
            
            if (_size != other._size)
                return false;
            
            // ignore the position, it's not important
            /*
            if (_position != other._position)
                return false;
            */
                
            if (_nullable != other._nullable)
                return false;
            
            if ( ( _name == null && other._name != null )
               || ( _name != null 
                    && ( colNameIgnoreCase && !_name.equalsIgnoreCase(other._name)
                            || !colNameIgnoreCase && !_name.equals(other._name)
                          )
                  )
               )
               return false;
            
            if ( ( _type == null && other._type !=null )
               || ( _type != null && !_type.equals(other._type) ) )
              return false;
              
            if ( ( _default == null && other._default !=null )
               || ( _default != null && !_default.equals(other._default) ) )
              return false;
              
            if ( ( _pkInfo == null && other._pkInfo !=null )
               || ( _pkInfo != null && !_pkInfo.equals(other._pkInfo) ) )
              return false;
              
            if ( ( _fkInfo == null && other._fkInfo !=null )
               || ( _fkInfo != null && !_fkInfo.equals(other._fkInfo) ) )
              return false;
              
        }
        return true;
    }

}
