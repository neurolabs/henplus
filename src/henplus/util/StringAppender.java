/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 */
package henplus.util;

/**
 * FIXME: removeme.
 *   
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 * @version $Id: StringAppender.java,v 1.3 2004-05-31 10:48:22 hzeller Exp $
 */
public final class StringAppender {
    
    private static StringBuffer _sb;
    private static StringAppender _instance;
    
    private StringAppender() {
        _sb = new StringBuffer();
    }
    
    public static final StringAppender getInstance() {
        if ( _instance == null )
            _instance = new StringAppender();
        return _instance;
    }
    
    public static StringAppender start( String value ) {
        if ( _instance == null )
            _instance = new StringAppender();
        _sb.append( value );
        return _instance;
    }
    
    public StringAppender append( String value ) {
        _sb.append( value );
        return this;
    }
    
    public StringAppender append( int value ) {
        _sb.append( value );
        return this;
    }
    
    public String toString() {
        String result = _sb.toString();
        _sb.delete( 0, _sb.length() );
        return result;
    }

}
