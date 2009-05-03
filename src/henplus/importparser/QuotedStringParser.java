/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.importparser;

public final class QuotedStringParser extends TypeParser {
    private final int _field;

    public QuotedStringParser(final int field) {
        _field = field;
    }

    /**
     * parse the value from the character buffer starting from the given offset
     * and with the given length. Store the result in the ValueRecipient.
     */
    @Override
    public void parse(final char[] buffer, int offset, int len,
            final ValueRecipient recipient) throws Exception {
        final char start = buffer[offset];
        final char end = buffer[offset + len - 1];
        if (len >= 2 && (start == '\'' || start == '"') && start == end) {
            offset += 1;
            len -= 2;
        }
        recipient.setString(_field, new String(buffer, offset, len));
    }
}
