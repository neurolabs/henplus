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
    public void parse(char[] buffer, int offset, int len,
                      ValueRecipient recipient) throws Exception {
        return;
    }
}
