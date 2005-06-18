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
    private final HenPlus          _henplus;

    public SessionPropertyCommand(HenPlus henplus) {
        _henplus = henplus;
    }
    
    protected String getSetCommand() { return "set-session-property"; }
    protected String getHelpHeader() { return "SQL-connection specific"; }

    protected PropertyRegistry getRegistry() {
        return _henplus.getCurrentSession().getPropertyRegistry();
    }

    public boolean requiresValidSession(String cmd) { 
        return true; 
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
