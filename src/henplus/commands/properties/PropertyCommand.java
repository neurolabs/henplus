/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: PropertyCommand.java,v 1.1 2004-02-01 14:12:52 hzeller Exp $ 
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
 * Set global HenPlus properties.
 */
public class PropertyCommand extends AbstractPropertyCommand {
    private final static String SETTINGS_FILENAME = "properties";
    private final HenPlus          _henplus;
    private final PropertyRegistry _registry;

    public PropertyCommand(HenPlus henplus, PropertyRegistry registry) {
        _henplus = henplus;
        _registry = registry;
    }
    
    protected String getSetCommand() { return "set-property"; }
    protected String getHelpHeader() { return "global HenPlus"; }

    protected PropertyRegistry getRegistry() {
        return _registry;
    }

    public boolean requiresValidSession(String cmd) { 
        return false; 
    }

    public void load() {
        Properties props = new Properties();
	try {
	    File settingsFile = new File(_henplus.getConfigDir(),
					 SETTINGS_FILENAME);
	    InputStream stream = new FileInputStream(settingsFile);
	    props.load(stream);
	}
	catch (IOException dont_care) {}

        Iterator it = props.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            try {
                _registry.setProperty((String) entry.getKey(),
                                      (String) entry.getValue());
            }
            catch (Exception e) {
                // don't care. silently ignore errornous entries.
            }
        }
    }

    public void shutdown() {
	try {
	    File settingsFile = new File(_henplus.getConfigDir(),
					 SETTINGS_FILENAME);
	    OutputStream stream = new FileOutputStream(settingsFile);
	    Properties p = new Properties();
           
            Iterator propIt = (_registry.getPropertyMap()
                               .entrySet().iterator());
            while (propIt.hasNext()) {
                Map.Entry entry = (Map.Entry) propIt.next();
                PropertyHolder holder = (PropertyHolder) entry.getValue();
                p.put((String) entry.getKey(), holder.getValue());
            }
	    p.store(stream, "user properties");
	}
	catch (IOException dont_care) {}
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
