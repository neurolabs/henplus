/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: BooleanPropertyHolder.java,v 1.1 2003-05-01 16:50:45 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.property;

/**
 * A boolean property.
 */
public abstract class BooleanPropertyHolder extends EnumeratedPropertyHolder {
    private final static String[] BOOL_VALUES = { "0", "off", "false",
                                                  "1", "on",  "true" };

    public BooleanPropertyHolder() {
        super(BOOL_VALUES);
    }

    public BooleanPropertyHolder(boolean initialValue) {
        this();
        _propertyValue = initialValue ? "true" : "false";
    }

    protected void enumeratedPropertyChanged(int index, String value) {
        /*
         * the upper part of the array contains the 'true' values.
         */
        booleanPropertyChanged(index >= (BOOL_VALUES.length / 2));
    }

    /**
     * to be overridden to get informed of the boolean change
     */
    public abstract void booleanPropertyChanged(boolean val);
}
/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
