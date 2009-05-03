/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import java.util.Iterator;

/**
 * Utility class to split Commands into tokens.
 */
public class CommandTokenizer implements Iterator {
    private final char[] _toTokenize;
    private final char[] _separatorBegins;
    private final char[] _separatorEnds;

    private int _pos;
    private boolean _tokenFinished;
    private String _nextToken;

    /**
     * Tokenizes a command string. Strings are separated at any whitespace
     * character if not within nested element. Handles nesting with the given
     * separatorPairs; separator pairs have to be given always in pairs, even if
     * the opening and closing element is the same. Example could be <b>
     * <code>""()[]{}</code></b>. These separator pairs handle strings and
     * elements in all kinds of parentheses.
     */
    public CommandTokenizer(final String cmd, final String separatorPairs) {
        _toTokenize = new char[cmd.length()];
        cmd.getChars(0, cmd.length(), _toTokenize, 0);
        final int sepLen = separatorPairs.length();
        if (sepLen % 2 != 0) {
            throw new IllegalArgumentException("invalid numbers of pairs");
        }
        _separatorBegins = new char[sepLen / 2];
        _separatorEnds = new char[sepLen / 2];
        for (int i = 0; i < sepLen; i += 2) {
            _separatorBegins[i / 2] = separatorPairs.charAt(i);
            _separatorEnds[i / 2] = separatorPairs.charAt(i + 1);
        }
        _pos = 0;
    }

    // -- java.util.Iterator interface implementation
    public boolean hasNext() {
        while (_pos < _toTokenize.length
                && Character.isWhitespace(_toTokenize[_pos])) {
            _pos++;
        }
        if (_pos >= _toTokenize.length) {
            return false;
        }
        final int startToken = _pos;
        final int expectedEndToken = findEndToken(_toTokenize[_pos]);
        if (expectedEndToken < 0) {
            while (_pos < _toTokenize.length
                    && !Character.isWhitespace(_toTokenize[_pos])
                    && !isSpecialSeparator(_toTokenize[_pos])) {
                _pos++;
            }
            _tokenFinished = _pos < _toTokenize.length;
        } else {
            final char endTok = (char) expectedEndToken;
            ++_pos;
            while (_pos < _toTokenize.length && endTok != _toTokenize[_pos]) {
                _pos++;
            }
            if (_pos < _toTokenize.length && endTok == _toTokenize[_pos]) {
                _pos++; // include the close token character
                _tokenFinished = true;
            } else {
                _tokenFinished = false;
            }
        }
        _nextToken = new String(_toTokenize, startToken, _pos - startToken);
        return true;
    }

    private boolean isSpecialSeparator(final char c) {
        for (int i = 0; i < _separatorBegins.length; ++i) {
            if (_separatorBegins[i] == c) {
                return true;
            }
        }
        return false;
    }

    /**
     * tries to determine appropriate end token if this is a startToken; returns
     * -1 otherwise.
     */
    private int findEndToken(final char tokenStart) {
        int i = 0;
        for (/* */; i < _separatorBegins.length; ++i) {
            if (_separatorBegins[i] == tokenStart) {
                return _separatorEnds[i];
            }
        }
        return -1;
    }

    public String nextToken() {
        return _nextToken;
    }

    public Object next() {
        return _nextToken;
    }

    /**
     * returns, wether the current token is finished. An token is unfinished if
     * - it is a nested token that has not seen its closing element till the end
     * of the string. - it is a normal token that is not followed by a
     * whitespace
     */
    public boolean isCurrentTokenFinished() {
        return _tokenFinished;
    }

    public void remove() {
        throw new UnsupportedOperationException("no!");
    }

    public static void main(final String argv[]) {
        final CommandTokenizer cmdTok = new CommandTokenizer(argv[0], argv[1]);
        while (cmdTok.hasNext()) {
            final String tok = cmdTok.nextToken();
            System.out.println("token: '" + tok + "'; complete="
                    + cmdTok.isCurrentTokenFinished());
        }
    }
}
