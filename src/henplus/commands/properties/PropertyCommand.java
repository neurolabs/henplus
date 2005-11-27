/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: PropertyCommand.java,v 1.4 2005-11-27 16:20:28 hzeller Exp $ 
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands.properties;

import henplus.HenPlus;
import henplus.PropertyRegistry;
import henplus.io.ConfigurationContainer;
import henplus.property.PropertyHolder;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Set global HenPlus properties.
 */
public class PropertyCommand extends AbstractPropertyCommand {
    private final static String SETTINGS_FILENAME = "properties";
    private final HenPlus          _henplus;
    private final PropertyRegistry _registry;
    private final ConfigurationContainer _config;
    
    public PropertyCommand(HenPlus henplus, PropertyRegistry registry) {
        _henplus = henplus;
        _registry = registry;
        _config = _henplus.createConfigurationContainer(SETTINGS_FILENAME);
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
        Map props = _config.readProperties();

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
        Map writeMap = new HashMap();
        Iterator propIt = (_registry.getPropertyMap()
                .entrySet().iterator());
        while (propIt.hasNext()) {
            Map.Entry entry = (Map.Entry) propIt.next();
            PropertyHolder holder = (PropertyHolder) entry.getValue();
            writeMap.put(entry.getKey(), holder.getValue());
        }
        _config.storeProperties(writeMap, true, "user properties");
    }
}

/*
 * Local variables:
 * c-basic-offset: 4
 * compile-command: "ant -emacs -find build.xml"
 * End:
 */
