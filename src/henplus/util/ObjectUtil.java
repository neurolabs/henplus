/*
 * This is free software, licensed under the Gnu Public License (GPL) get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Ole Langbehn (neurolabs@web.de)
 */
package henplus.util;

/**
 * Collection of static helper methods for Object handling.
 * 
 * @author ole
 * 
 */
public final class ObjectUtil {

    /**
     * Hide the constructor, this is a class only with static methods, no need for constructing.
     */
    private ObjectUtil() {
        // never called
    }

    /**
     * @param that
     *            the object for which a hashcode should be returned
     * @return the hashcode, or 0 if null is passed
     */
    public static int nullSafeHashCode(final Object that) {
        if (that == null) {
            return 0;
        } else {
            return that.hashCode();
        }
    }

    public static boolean nullSafeEquals(final Object one, final Object two) {
        if (one == null || two == null) {
            return (one == null && two == null);
        } else {
            return one.equals(two);
        }
    }

}
