/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: AboutCommand.java,v 1.13 2008-10-19 08:53:25 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.AbstractCommand;
import henplus.HenPlus;
import henplus.SQLSession;
import henplus.Version;

/**
 * document me.
 */
public class AboutCommand extends AbstractCommand {
    static final String LICENSE = "GNU Public License <http://www.gnu.org/licenses/gpl2.txt>";

    static final String ABOUT = new StringBuilder()
    .append(
            "----------------------------------------------------------------------------\n")
            .append(" HenPlus II ")
            .append(Version.getVersion())
            .append(" \"")
            .append(Version.getVersionTitle())
            .append("\"\n")
            .append(
            " Copyright(C) 1997..2009 Henner Zeller <H.Zeller@acm.org>\n")
            .append(
            " HenPlus is provided AS IS and comes with ABSOLUTELY NO WARRANTY\n")
            .append(
            " This is free software, and you are welcome to redistribute it under the\n")
            .append(" conditions of the " + LICENSE + "\n")
            .append(
            "----------------------------------------------------------------------------\n")
            .toString();

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
        return new String[] { "about", "version", "license" };
    }

    public AboutCommand(final boolean quiet) {
        if (!quiet) {
            System.err.print(ABOUT);
        }
    }

    /**
     * execute the command given.
     */
    public int execute(final SQLSession session, final String cmd, final String param) {
        if ("about".equals(cmd)) {
            HenPlus.msg().print(ABOUT);
        } else if ("version".equals(cmd)) {
            HenPlus.msg().println(
                    Version.getVersion() + " / compiled "
                    + Version.getCompileTime());
        } else if ("license".equals(cmd)) {
            HenPlus.msg().println(LICENSE);
        }
        return SUCCESS;
    }

    @Override
    public boolean requiresValidSession(final String cmd) {
        return false;
    }

    /**
     * return a descriptive string.
     */
    @Override
    public String getShortDescription() {
        return "about HenPlus";
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
