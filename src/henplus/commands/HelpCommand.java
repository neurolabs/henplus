/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import java.util.StringTokenizer;
import java.util.Iterator;

import henplus.HenPlus;
import henplus.SQLSession;
import henplus.CommandDispatcher;
import henplus.Command;
import henplus.AbstractCommand;
import henplus.view.util.SortedMatchIterator;

/**
 * document me.
 */
public class HelpCommand extends AbstractCommand {
    static final int INDENT = 42;

    /**
     * returns the command-string this command can handle.
     */
    public String[] getCommandList() {
        return new String[] { "help", "?" };
    }

    @Override
    public boolean requiresValidSession(final String cmd) {
        return false;
    }

    /**
     * Returns a list of strings that are possible at this stage.
     */
    @Override
    public Iterator complete(final CommandDispatcher disp,
            final String partialCommand, final String lastWord) {
        // if we already have one arguemnt and try to expand the next: no.
        final int argc = argumentCount(partialCommand);
        if (argc > 2 || argc == 2 && lastWord.length() == 0) {
            return null;
        }

        final Iterator it = disp.getRegisteredCommandNames(lastWord);
        return new SortedMatchIterator(lastWord, it) {
            @Override
            protected boolean exclude(final String cmdName) {
                final Command cmd = disp.getCommandFrom(cmdName);
                return cmd.getLongDescription(cmdName) == null;
            }
        };
    }

    /**
     * execute the command given.
     */
    public int execute(final SQLSession session, final String cmdstr, String param) {
        final StringTokenizer st = new StringTokenizer(param);
        if (st.countTokens() > 1) {
            return SYNTAX_ERROR;
        }
        param = st.hasMoreElements() ? (String) st.nextElement() : null;
        /*
         * nothing given: provide generic help.
         */
        if (param == null) {
            final Iterator it = HenPlus.getInstance().getDispatcher()
            .getRegisteredCommands();
            while (it.hasNext()) {
                final Command cmd = (Command) it.next();
                final String description = cmd.getShortDescription();
                if (description == null) {
                    continue;
                }

                final StringBuffer cmdPrint = new StringBuffer(" ");
                final String[] cmds = cmd.getCommandList();
                final String firstSynopsis = cmd.getSynopsis(cmds[0]);
                /*
                 * either print a list of known commands or the complete
                 * synopsis, if there is only one command.
                 */
                if (cmds.length > 1 || firstSynopsis == null) {
                    for (int i = 0; i < cmds.length; ++i) {
                        if (i != 0) {
                            cmdPrint.append(" | ");
                        }
                        cmdPrint.append(cmds[i]);
                    }
                } else {
                    cmdPrint
                    .append(firstSynopsis.length() < INDENT ? firstSynopsis
                            : cmds[0]);
                }
                HenPlus.msg().print(cmdPrint.toString());
                for (int i = cmdPrint.length(); i < INDENT; ++i) {
                    HenPlus.msg().print(" ");
                }
                HenPlus.msg().print(": ");
                HenPlus.msg().println(description);
            }
            HenPlus.msg().println(
            "Full documentation at http://henplus.sf.net/");
            HenPlus.msg().println(
                    "config read from ["
                    + HenPlus.getInstance()
                    .getConfigurationDirectoryInfo() + "]");
        } else {
            final CommandDispatcher disp = HenPlus.getInstance().getDispatcher();
            final String cmdString = disp.getCommandNameFrom(param);
            final Command c = disp.getCommandFrom(param);
            if (c == null) {
                HenPlus.msg().println("Help: unknown command '" + param + "'");
                return EXEC_FAILED;
            }
            printDescription(cmdString, c);
        }
        return SUCCESS;
    }

    private void printDescription(final String cmdStr, final Command c) {
        String desc = c.getLongDescription(cmdStr);
        if (desc == null) {
            if (c.getShortDescription() != null) {
                desc = "\t[short description]: " + c.getShortDescription();
            }
        }
        final String synopsis = c.getSynopsis(cmdStr);

        if (synopsis != null) {
            HenPlus.msg().attributeBold();
            HenPlus.msg().println("SYNOPSIS");
            HenPlus.msg().attributeReset();
            HenPlus.msg().println("\t" + synopsis);
            HenPlus.msg().println();
        }
        if (desc != null) {
            HenPlus.msg().attributeBold();
            HenPlus.msg().println("DESCRIPTION");
            HenPlus.msg().attributeReset();
            HenPlus.msg().println(desc);
            if (c.requiresValidSession(cmdStr)) {
                HenPlus.msg().println("\tRequires valid session.");
            }
        }
        if (desc == null && synopsis == null) {
            HenPlus.msg().println("no detailed help for '" + cmdStr + "'");
        }
    }

    /**
     * return a descriptive string.
     */
    @Override
    public String getShortDescription() {
        return "provides help for commands";
    }

    @Override
    public String getSynopsis(final String cmd) {
        return cmd + " [command]";
    }

    @Override
    public String getLongDescription(final String cmd) {
        String dsc;
        dsc = "\tProvides help for the given command.   If invoked without a\n"
            + "\tcommand name as parameter, a list of all available commands\n"
            + "\tis shown.";
        return dsc;
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
