/*
 * $Id$ (c)
 * Copyright 2009 freiheit.com technologies GmbH
 *
 * Created on 04.05.2009
 *
 * This file contains unpublished, proprietary trade secret information of
 * freiheit.com technologies GmbH. Use, transcription, duplication and
 * modification are strictly prohibited without prior written consent of
 * freiheit.com technologies GmbH.
 */

package henplus.util;

/**
 * @author ole
 *
 */
public final class StringUtil {

    /**
     * Hide the constructor, this is a class only with static methods,
     * no need for constructing.
     */
    private StringUtil() {
        // never called
    }

    public static boolean nullSafeEquals(String one, String two) {
        return ObjectUtil.nullSafeEquals(one, two);   
    }
    
    public static boolean nullSafeEquals(String one, String two, boolean ignoreCase) {
        if (ignoreCase) {
            if (one == null || two == null) {
                return (one == null && two == null); 
            } else {
                return StringUtil.nullSafeEquals(one.toLowerCase(), two.toLowerCase());
            }
        } else {
            return StringUtil.nullSafeEquals(one, two);
        }
    }
}
