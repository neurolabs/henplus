/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.importparser;

/**
 * A parser for a specific type. Different implementations
 */
public final class StringParser extends TypeParser {
    private final int _field;
    
    public StringParser(int field) {
        _field = field;
    }

    /**
     * parse the value from the character buffer starting from the given
     * offset and with the given length. Store the result in the 
     * ValueRecipient.
     */
    public void parse(char[] buffer, int offset, int len,
                      ValueRecipient recipient) throws Exception {
        recipient.setString(_field, new String(buffer, offset, len));
    }
}
