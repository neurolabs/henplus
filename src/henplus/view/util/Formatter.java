/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * @version $Id: Formatter.java,v 1.1 2004-03-23 11:06:40 magrokosmos Exp $ 
 * @author <a href="mailto:martin.grotzke@javakaffee.de">Martin Grotzke</a>
 */
package henplus.view.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Knows how to format various things, like number, dates etc.<br>
 * Created on: Mar 23, 2004<br>
 */
public final class Formatter {

    private static final NumberFormat NUMBER_FORMAT =
        NumberFormat.getNumberInstance(Locale.GERMANY);

    private Formatter() {
    }

    /**
     * Formats a double to a String with number-format.
     *
     * @param amount        the <code>double</code> to be formatted
     * @param precision an <code>int</code>. which precision should be used?
     * @return a <code>String</code>
     */
    public static String formatNumber(double amount, int precision) {
        if (amount == 0.0)
            return null;
        NUMBER_FORMAT.setMinimumFractionDigits(precision);
        NUMBER_FORMAT.setMaximumFractionDigits(precision);
        return NUMBER_FORMAT.format(amount);
    }

}
