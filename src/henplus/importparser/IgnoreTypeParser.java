/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.importparser;

/**
 * A TypeParser, that ignores the value and does nothing.
 */
public class IgnoreTypeParser extends TypeParser {
    @Override
    public void parse(final char[] buffer, final int offset, final int len,
            final ValueRecipient recipient) throws Exception {
        return;
    }
}
