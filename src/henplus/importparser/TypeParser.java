/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.importparser;

/**
 * A parser for a specific type. The TypeParser knows from the value or its
 * configuration, into which field the parsed value should be stored.
 */
public abstract class TypeParser {
    /**
     * parse the value from the character buffer starting from the given
     * offset and with the given length. Store the result in the 
     * ValueRecipient.
     */
    public abstract void parse(char[] buffer, int offset, int len,
                               ValueRecipient recipient) throws Exception;
}
