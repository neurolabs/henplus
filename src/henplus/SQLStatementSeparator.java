/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SQLStatementSeparator.java,v 1.19 2004-06-07 08:31:56 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import java.util.Stack;

import henplus.property.PropertyHolder;
import henplus.property.BooleanPropertyHolder;

/**
 * Simple parser that separates SQLStatements.
 * Example.
 * <pre>
 *-----------------------
 statementSeparator.append("select * from foo; echo $foobar \n");
 while (statementSeparator.hasNext()) {
      String stmt = statementSeparator.next();
      if (stmt.startsWith("echo")) {
          // is ok, this command works always without ';'
	  statementSeparator.consumed();
	  System.err.println(stmt.substring("echo ".length());
      }
      else {  // SQL-command. we require a semicolon at the end.
         if (!stmt.charAt(stmt.length()-1) == ';') {
	    statementSeparator.cont(); // continue.
         }
         else {
            statementSeparator.consumed();
	    SQLExecute(stmt);
         }
      }
 }
 *----------------------- 
 *</pre>
 *
 * FIXME: this is a bit rough and accummulated some ideas. Should be 
 * cleaned up.
 *
 * @author Henner Zeller <H.Zeller@acm.org>
 */
public class SQLStatementSeparator {
    private static final byte NEW_STATEMENT   = 0;
    private static final byte START           = 1;  // statement == start
    private static final byte STATEMENT       = 1;
    private static final byte START_COMMENT   = 3;
    private static final byte COMMENT         = 4;
    private static final byte PRE_END_COMMENT = 5;
    private static final byte START_ANSI      = 6;
    private static final byte ENDLINE_COMMENT = 7;
    private static final byte STRING          = 8;
    private static final byte STRING_QUOTE    = 9;
    private static final byte SQLSTRING       = 10;
    private static final byte SQLSTRING_QUOTE = 11;
    private static final byte STATEMENT_QUOTE = 12;  // backslash in statement
    private static final byte FIRST_SEMICOLON_ON_LINE_SEEN  = 13;
    private static final byte POTENTIAL_END_FOUND = 14;

    private static class ParseState {
	private byte         _state;
	private StringBuffer _inputBuffer;
	private StringBuffer _commandBuffer;
	/*
	 * instead of adding new states, we store the
	 * fact, that the last 'potential_end_found' was
	 * a newline here.
	 */
	private boolean      _eolineSeen;

	public ParseState() {
	    _eolineSeen = true; // we start with a new line.
	    _state = NEW_STATEMENT;
	    _inputBuffer = new StringBuffer();
	    _commandBuffer = new StringBuffer();
	}
	
	public byte getState() { return _state; }
	public void setState(byte s) { _state = s; }
	public boolean hasNewlineSeen() { return _eolineSeen; }
	public void setNewlineSeen(boolean n) { _eolineSeen = n; }
	public StringBuffer getInputBuffer() { return _inputBuffer; }
	public StringBuffer getCommandBuffer() { return _commandBuffer; }
    };

    private boolean      _removeComments;
    private ParseState   _currentState;
    private Stack        _stateStack;

    public SQLStatementSeparator() {
	_currentState = new ParseState();
	_stateStack = new Stack();
        _removeComments = true;
    }

    /**
     * push the current state and start with a clean one. Use to parse
     * other files (like includes), and continue then with the old
     * state.
     * like 
     * load foobar.sql ; select * from foobar
     */
    public void push() {
	_stateStack.push(_currentState);
	_currentState = new ParseState();
    }

    public void pop() {
	_currentState = (ParseState) _stateStack.pop();
    }

    /**
     * add a new line including the '\n' to the input buffer.
     */
    public void append(String s) {
	_currentState.getInputBuffer().append(s);
    }
    
    /**
     * discard any input.
     */
    public void discard() {
	_currentState.getInputBuffer().setLength(0);
	_currentState.getCommandBuffer().setLength(0);
	_currentState.setState( NEW_STATEMENT );
    }

    /**
     * after having called next(), call cont(), if you are not yet
     * pleased with the result; the parser should read to the next
     * possible end.
     */
    public void cont() {
	_currentState.setState( START );
    }

    /**
     * after having called next() and you were pleased with the result
     * call this method to state, that you consumed it.
     */
    public void consumed() {
	_currentState.setState( NEW_STATEMENT );
    }

    /**
     * returns true, if the parser can find a complete command that either
     * ends with newline or with ';'
     */
    public boolean hasNext() throws IllegalStateException {
	if (_currentState.getState() == POTENTIAL_END_FOUND)
	    throw new IllegalStateException ("call cont() or consumed() before hasNext()");
	if (_currentState.getInputBuffer().length() == 0)
	    return false;
	parsePartialInput();
	return (_currentState.getState() == POTENTIAL_END_FOUND);
    }

    /**
     * returns the next command; requires to call hasNext() before.
     */
    public String next() throws IllegalStateException {
	if (_currentState.getState() != POTENTIAL_END_FOUND)
	    throw new IllegalStateException ("next() called without hasNext()");
	return _currentState.getCommandBuffer().toString();
    }

    /**
     * returns a property holder for the remove comments property.
     */
    public PropertyHolder getRemoveCommentsProperty() {
        return new RemoveCommentsProperty();
    }

    /**
     * set, whether comments should be removed.
     */
    public void removeComments(boolean b) {
        _removeComments = b;
    }

    /**
     * parse partial input and set state to POTENTIAL_END_FOUND if we
     * either reached end-of-line or a semicolon.
     */
    private void parsePartialInput () {
	int pos = 0;
	char current;
	byte oldstate = -1;

	// local variables: faster access.
	byte state = _currentState.getState();
	boolean lastEoline = _currentState.hasNewlineSeen();

	final StringBuffer input  = _currentState.getInputBuffer();
	final StringBuffer parsed = _currentState.getCommandBuffer();

	if (state == NEW_STATEMENT) {
	    parsed.setLength(0);
	    /* skip leading whitespaces of next statement .. */
	    while (pos < input.length()
		   && Character.isWhitespace (input.charAt(pos))) {
                _currentState.setNewlineSeen( input.charAt(pos) == '\n' );
		++pos;
	    }
	    input.delete(0, pos);
	    pos = 0;
	}
	
	if (input.length() == 0) {
	    state = POTENTIAL_END_FOUND;
	}

        //System.err.println("Startstate: " + state + "; LEOL: " + lastEoline);

	while (state != POTENTIAL_END_FOUND && pos < input.length()) {
	    boolean vetoAppend = false;
	    boolean reIterate;
	    current = input.charAt(pos);
	    if (current == '\r') {
		current = '\n'; // canonicalize.
	    }

            if (current == '\n') {
                _currentState.setNewlineSeen( true );
            }

	    //System.out.print ("Pos: " + pos + "\t");
	    do {
		reIterate = false;
		switch (state) {
		case NEW_STATEMENT:
		    //case START: START == STATEMENT.
		case STATEMENT :
		    if (current == '\n') {
			state = POTENTIAL_END_FOUND;
			_currentState.setNewlineSeen(true);
		    }

                    /*
                     * special handling of the 'first-two-semicolons-after
                     * a-newline-comment'.
                     */
		    else if (_removeComments && lastEoline && current== ';' ) {
			state = FIRST_SEMICOLON_ON_LINE_SEEN;
		    }
		    else if (!lastEoline && current==';') {
			_currentState.setNewlineSeen(false);
			state = POTENTIAL_END_FOUND;
		    }
		    else if (_removeComments && current == '/') {
                        state = START_COMMENT;
                    }

                    /*
                     * only if '#' this is the first character, make it
                     * a comment..
                     */
		    else if (_removeComments && lastEoline && current == '#') {
                        state = ENDLINE_COMMENT;
                    }
		    else if (current == '"')  state = STRING;
		    else if (current == '\'') state = SQLSTRING;
		    else if (current == '-')  state = START_ANSI;
		    else if (current == '\\') state = STATEMENT_QUOTE;
		    break;
		case STATEMENT_QUOTE:
		    state = STATEMENT;
		    break;
		case FIRST_SEMICOLON_ON_LINE_SEEN:
		    if (current == ';') state = ENDLINE_COMMENT;
		    else {
			state = POTENTIAL_END_FOUND;
			current = ';';
			/*
			 * we've read too much. Reset position.
			 */
			--pos;
		    }
		    break;
		case START_COMMENT:
		    if (current == '*')         state = COMMENT;
		    /*
		     * Endline comment in the style '// comment' is not a
		     * good idea, since many JDBC-urls contain the '//' as
		     * part of the URL .. and this should _not_ be regarded as
		     * commend of course.
		     */
		    //else if (current == '/')    state = ENDLINE_COMMENT;
		    else { 
			parsed.append ('/'); 
			state = STATEMENT; 
			reIterate = true;
		    }
		    break;
		case COMMENT:
		    if (current == '*') state = PRE_END_COMMENT;
		    break;
		case PRE_END_COMMENT:
		    if (current == '/')      state = STATEMENT;
		    else if (current == '*') state = PRE_END_COMMENT;
		    else state = COMMENT;
		    break;
		case START_ANSI:
		    if (current == '-')        state = ENDLINE_COMMENT;
		    else { 
			parsed.append('-'); 
			state = STATEMENT; 
			reIterate = true;
		    }
		    break;
		case ENDLINE_COMMENT:
		    if (current == '\n')      state = POTENTIAL_END_FOUND;
		    break;
		case STRING:     
		    if (current == '\\') state = STRING_QUOTE;
		    else if (current == '"') state = STATEMENT;
		    break;
		case SQLSTRING:
		    if (current == '\\') state = SQLSTRING_QUOTE;
		    if (current == '\'') state = STATEMENT;
		    break;
		case STRING_QUOTE:
		    vetoAppend = (current == '\n');
                    if (current == 'n') current = '\n';
                    else if (current == 'r') current = '\r';
                    else if (current == 't') current = '\t';
                    else if (current != '\n' && current != '"') {
                        // if we do not recognize the escape sequence,
                        // pass it through.
                        parsed.append("\\");
                    }
		    state = STRING;
		    break;
		case SQLSTRING_QUOTE:
                    // convert a "\'" to a correct SQL-Quote "''"
		    if (current == '\'') parsed.append("'");
                    else if (current == 'n') current = '\n';
                    else if (current == 'r') current = '\r';
                    else if (current == 't') current = '\t';
                    else if (current != '\n') {
                        // if we do not recognize the escape sequence,
                        // pass it through.
                        parsed.append("\\");
                    }
		    vetoAppend = (current == '\n');
		    state = SQLSTRING;
		    break;
		}
	    }
	    while (reIterate);

	    /* append to parsed; ignore comments */
	    if (!vetoAppend
		&& ((state == STATEMENT && oldstate != PRE_END_COMMENT)
		    || state == NEW_STATEMENT
		    || state == STATEMENT_QUOTE
		    || state == STRING
		    || state == SQLSTRING
		    || state == POTENTIAL_END_FOUND)) {
		parsed.append(current);
	    }
	    
	    oldstate = state;
	    pos++;
	    /*
	     * we maintain the state of 'just seen newline' as long
	     * as we only skip whitespaces..
	     */
	    lastEoline &= Character.isWhitespace(current);
	}
	// we reached: POTENTIAL_END_FOUND. Store the rest, that
	// has not been parsed in the input-buffer.
	input.delete(0, pos);
	_currentState.setState(state);
    }

    private class RemoveCommentsProperty extends BooleanPropertyHolder {

        public RemoveCommentsProperty() {
            super(SQLStatementSeparator.this._removeComments);
        }

        public void booleanPropertyChanged(boolean value){
            removeComments( value );
        }
        
        public String getDefaultValue() {
            return "on";
        }

        /**
         * return a short descriptive string.
         */
        public String getShortDescription() {
            return "switches the removal of SQL-comments";
        }
        
        public String getLongDescription() {
            String dsc;
            dsc= "\tSwitch the behaviour to remove all comments\n"
                +"\tfound in the string sent to the database. Some databases\n"
                +"\tcan not handle comments in JDBC-Strings.\n\nValues\n"
                
                +"\ttrue\n"
                +"\t\tDEFAULT. Remove all SQL92 comments found in the given\n"
                +"\t\tSQL Strings before sending them to the database.\n\n"
                
                +"\tfalse\n"
                +"\t\tSwitch off the default behaviour to remove all\n"
                +"\t\tcomments found in the string sent to the database.\n"
                +"\t\tUsually, this is not necessary, but there are\n"
                +"\t\tconditions where comments actually convey a meaning\n"
                +"\t\tto the database. For instance hinting in oracle works\n"
                +"\t\twith comments, like\n"
                +"\t\t   select /*+ index(foo,foo_fk_idx) */ ....\n"
                +"\t\t..so removing of comments should be off in this case";
            return dsc;
        }
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
