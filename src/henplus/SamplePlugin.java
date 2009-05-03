/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SamplePlugin.java,v 1.1 2002-05-06 06:57:56 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus;

/**
 * This is a Sample plugin. Its simple: just implement the Command interface and
 * a public default constructor. Thats it.
 */
public class SamplePlugin extends AbstractCommand {

    public SamplePlugin() { /* default constructor */
    }

    /**
     * returns the command-strings this plug-in can handle
     */
    public String[] getCommandList() {
        return new String[] { "sample-plugin", "do-something" };
    }

    public int execute(final SQLSession session, final String cmd, final String param) {
        System.err.println("This plugin does nothing.");
        return SUCCESS;
    }

    @Override
    public boolean requiresValidSession(final String cmd) {
        return false; // this plugin works always.
    }

    @Override
    public String getShortDescription() {
        return "sample plugin";
    }

    @Override
    public String getLongDescription(final String cmd) {
        return "\tThis  is an  example  for the  long  description  of the\n"
        + "\tsample plugin. Actually, this plugin does really nothing\n"
        + "\tbut  shows how simple  it is to  implement a plugin that\n"
        + "\tbehaves  like  a  normal  built-in command.  This one is\n"
        + "\tjust  simply  derived  from henplus.AbstractCommand  and\n"
        + "\toverrides some methods.\n" + "\tThats it.";
    }

    @Override
    public String getSynopsis(final String cmd) {
        return cmd;
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
