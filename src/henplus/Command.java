/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: Command.java,v 1.5 2002-02-26 21:15:17 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

import java.util.Iterator;

/**
 * interface to be implemented for user level commands. The CommandDispatcher
 * and the HelpCommand operate on this interface.
 */
public interface Command {
    public final static int SUCCESS      = 0;
    public final static int SYNTAX_ERROR = 1;
    public final static int EXEC_FAILED  = 2;

    /**
     * returns the prefices of all command-strings this command can handle.
     * The special prefix is the empty string that matches anything that is
     * not handled by all other commands. It is used in the SQLCommand.
     */
    String[] getCommandList();
    
    /**
     * returns 'false', if the commands supported by this Commands should
     * not be part of the toplevel command completion. So if the user
     * presses TAB on an empty string to get the full list of possible
     * commands, this command should not show up.
     */
    boolean participateInCommandCompletion();

    /**
     * execute the command given. The command is given completely without
     * the final delimiter (which would be newline or semicolon). Before
     * this method is called, the CommandDispatcher checks with the 
     * {@link #isComplete(String)} method, if this command is complete.
     *
     * @param session the SQLsession this command is executed from.
     * @param command the command as string.
     * @param parameters the rest parameters following the command.
     * @return one of SUCCESS, SYNTAX_ERROR, EXEC_FAILED to indicate
     *         the exit status of this command. On SYNTAX_ERROR, the
     *         CommandDispatcher displays a synopsis if possible.
     */
    int execute(SQLSession session, String command, String parameters);
    
    /**
     * Returns a list of strings that are possible at this stage. Used
     * for the readline-completion in interactive mode.
     */
    Iterator complete(CommandDispatcher disp, String partialCommand, 
		      String lastWord);
    
    /**
     * returns, whether the command is complete.
     *
     * <p>This method is called, whenever the input reads a newline or
     * a semicolon to decide if this separator is to separate different
     * commands or if it is part of the command itself.
     *
     * <p>The delimiter (newline or semicolon) is contained (at the end)
     * in the String given. This
     * method returns <code>false</code>, if the delimiter is part of the 
     * command and will not be regarded as delimiter between commands.
     *
     * <p>This method will return true for most simple commands like
     * 'help'. For commands that have a more complicated syntax, this
     * might not be true.
     * <ul>
     *  <li>'select * from foobar' is not complete, since we can
     *      expect a where clause. If it has a semicolon at the end, we
     *      know, that is is complete. So newline is <em>not</em> a delimiter
     *      while ';' is (return command.endsWith(";")). Of course, the command
     *      has to take care of string-constants as well (consider a semicolon
     *      or newline within a string constant)
     *  <li>definitions of stored procedures are even more complicated: it
     *      depends on the syntax whether a semicolon is part of the
     *      command or can be regarded as delimiter. Here, neither ';' nor
     *      newline can be regarded as delimiter per-se. Only the Command
     *      implementation can decide upon this.
     * </ul>
     * Note, this method should only apply a very lazy syntax check so it does
     * not get confused unecessarily..
     */
    boolean isComplete(String command);

    /**
     * returns true, if this command requires a valid SQLSession.
     */
    boolean requiresValidSession(String cmd);

    /**
     * shutdown this command. This is called on exit of the CommandDispatcher
     * and allows you to do some cleanup (close connections, flush files..)
     */
    void shutdown();

    /**
     * return a short string describing the purpose of the commands
     * handled by this Command-implementation. This is the string listed
     * in the bare 'help' overview (like 
     * <code>'describe a database object'</code>)
     * Should contain no newline, no leading spaces.
     */
    String getShortDescription();

    /**
     * retuns a synopsis-string. The synopsis string returned should follow
     * the following conventions:
     * <ul>
     *  <li>expected parameters are described with angle brackets like in
     *      <code>export-xml &lt;table&gt; &lt;filename&gt;</li>
     *  <li>optional parameters are described with square brackets like in
     *      <code>help [command]</code></li>
     * </ul>
     * <p>Should contain no newline, no leading spaces.
     *
     * @param cmd the command the synopsis is for. This is one of the possible
     *            commands returned by getCommandList().
     */
    String getSynopsis(String cmd);

    /**
     * returns a longer string describing this action. This should return
     * a String describing details of the given command. This String should
     * start with a TAB-character in each new line (the first line is a
     * new line). The last line should not end with newline.
     *
     * @param cmd The command the long description is asked for. This
     *            is one of the possible commands returned by getCommandList().
     */
    String getLongDescription(String cmd);
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
