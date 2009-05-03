/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SessionPropertyCommand.java,v 1.2 2005-06-18 04:58:13 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands.properties;

import henplus.HenPlus;
import henplus.PropertyRegistry;

/**
 * handle session local properties.
 */
public class SessionPropertyCommand extends AbstractPropertyCommand {
    private final HenPlus _henplus;

    public SessionPropertyCommand(final HenPlus henplus) {
        _henplus = henplus;
    }

    @Override
    protected String getSetCommand() {
        return "set-session-property";
    }

    @Override
    protected String getHelpHeader() {
        return "SQL-connection specific";
    }

    @Override
    protected PropertyRegistry getRegistry() {
        return _henplus.getCurrentSession().getPropertyRegistry();
    }

    @Override
    public boolean requiresValidSession(final String cmd) {
        return true;
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
