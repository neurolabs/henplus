/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: SessionPropertyCommand.java,v 1.1 2004-02-01 14:12:52 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands.properties;

import henplus.HenPlus;
import henplus.PropertyRegistry;
import henplus.SQLSession;
import henplus.property.PropertyHolder;

import java.util.Properties;
import java.util.Iterator;
import java.util.Map;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

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
