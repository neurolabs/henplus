/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.importparser;

import java.util.Calendar;

/**
 * A Recipient of a value parsed by the TypeParser.
 */
public interface ValueRecipient {
    void setLong(int fieldNumber, long value) throws Exception;
    void setString(int fieldNumber, String value) throws Exception;
    void setDate(int fieldNumber, Calendar cal) throws Exception;

    void finishRow() throws Exception;
}
