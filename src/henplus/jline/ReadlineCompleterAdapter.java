package henplus.jline;

import java.util.List;

import jline.console.completer.Completer;

public class ReadlineCompleterAdapter implements Completer {

    private final String wordBreakCharacters;

    private final ReadlineCompleter readLineCompleter;

    public ReadlineCompleterAdapter(final String wordBreakCharacters, final ReadlineCompleter readlineCompleter) {
        this.wordBreakCharacters = wordBreakCharacters;
        this.readLineCompleter = readlineCompleter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int complete(final String buffer, final int cursor, final List candidates) {

        // need to find the last incomplete word
        int wordStart = buffer.length();
        while ((wordStart > 0) && (wordBreakCharacters.indexOf(buffer.charAt(wordStart - 1)) == -1)) {
            wordStart--;
        }

        final String lastWord = buffer.substring(wordStart, buffer.length());

        String nextCompletion = null;
        int state = 0;
        while ((nextCompletion = readLineCompleter.completer(lastWord, state)) != null) {
            candidates.add(nextCompletion);
            state++;
        }

        return wordStart;
    }
}
