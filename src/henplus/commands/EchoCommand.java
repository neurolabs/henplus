/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: EchoCommand.java,v 1.7 2004-01-28 09:25:48 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.HenPlus;
import henplus.SQLSession;
import henplus.AbstractCommand;

/**
 * document me.
 */
public final class EchoCommand extends AbstractCommand {
    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
        return new String[] { "echo", "prompt" };
    }

    @Override
    public boolean requiresValidSession(final String cmd) {
        return false;
    }

    /**
     * execute the command given.
     */
    public int execute(final SQLSession currentSession, final String cmd, final String param) {
        final String outStr = param.trim();
        HenPlus.out().println(stripQuotes(outStr));
        return SUCCESS;
    }

    private String stripQuotes(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        } else if (value.startsWith("\'") && value.endsWith("\'")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }

    /**
     * return a descriptive string.
     */
    @Override
    public String getShortDescription() {
        return "echo argument";
    }

    @Override
    public String getSynopsis(final String cmd) {
        return cmd + " <whatever>";
    }

    @Override
    public String getLongDescription(final String cmd) {
        String dsc;
        dsc = "\tjust echo the string given.";
        return dsc;
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
