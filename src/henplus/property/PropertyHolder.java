/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: PropertyHolder.java,v 1.3 2003-05-01 23:21:17 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.property;

import java.util.Iterator;

/**
 * A Property is something that has a value and is bound to
 * some name. The binding to a name is done elsewhere, the
 * PropertyHolder holds the value and informs a callback method
 * on change. It provides a simple way of completing values, if
 * possible to aid the shell. The PropertyHolder is abstract, since it
 * needs to be overwritten to get informed on changes of its value. Since
 * a property is always <em>special</em> in a sense that changing the
 * property does change some internal state, possibly by calling several
 * methods, code is always executed on change.
 */
public abstract class PropertyHolder {
    protected String _propertyValue;
    
    /**
     * construct a PropertyHolder with an empty value.
     */
    protected PropertyHolder() {
        this(null);
    }
    
    protected PropertyHolder(String initialValue) {
        _propertyValue = initialValue;
    }

    /**
     * set the new value of this property. If changing the property
     * does not work for e.g. a constraint propblem, then this method will
     * throw an Exception and the property is <em>not</em> set.
     * Also, after calling setValue(), the internal value of the property
     * might not exactly the value given, but some canonicalized form
     * returned by the {@link propertyChanged(String)} listener method.
     *
     * @param newValue the new value to be set.
     */
    public void setValue(String newValue) throws Exception {
        _propertyValue = propertyChanged(newValue);
    }

    /**
     * The canonicalized value of the value of this Property.
     */
    public String getValue() {
        return _propertyValue;
    }

    public abstract String getDefaultValue();

    /**
     * is called, when the property changes. This method
     * is supposed to do whatever is needed on change of the
     * property.
     * It returns a canonicalized version of the new value,
     * or the value itself, if it is cool with it. If the value
     * is not of the expected range, then this method must
     * throw an Exception.
     *
     * @param newValue a new value of the property. The old value
     *        is still accessible with the {@link getValue()}
     *        method.
     * @return the canonicalized value. e.g. for a Property taking
     *         boolean values, it returns all '1', '0', 'on', 'off' as
     *         'true', 'false'.
     */
    protected abstract String propertyChanged(String newValue) 
        throws Exception;

    /**
     * given a partial value of a to-be-set value, this will return
     * an iterator of possible values possible at that point or 'null'
     * if no such completion can take place.
     *
     * @param partialValue a partial given value
     * @return an Iterator of values that all start with the given String or
     *         <code>null</code> if no such completion exists.
     */
    public Iterator completeValue(String partialValue) {
        return null;
    }

    //-- something for the build-in help
    /**
     * return a short string describing the purpose of this property
     * Should contain no newline, no leading spaces and should not be
     * longer than 40 characters.
     */
    public String getShortDescription() {
        return null;
    }

    /**
     * returns a longer string describing this property. This should return
     * a String describing details of the given command. This String should
     * start with a TAB-character in each new line (the first line is a
     * new line). The last line should not end with newline. Should fit on a
     * 80 character width terminal.
     */
    public String getLongDescription() {
        return null;
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
