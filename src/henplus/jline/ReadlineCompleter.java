package henplus.jline;

/**
 * Standard java Readline interface, to be used for compatibility
 * 
 * @author mofleury
 * 
 */
public interface ReadlineCompleter {

	public String completer(String text, int state);
}
