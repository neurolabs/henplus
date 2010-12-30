/*
 * This is free software, licensed under the Gnu Public License (GPL) get a copy from <http://www.gnu.org/licenses/gpl.html> $Id:
 * PropertyCommand.java,v 1.4 2005-11-27 16:20:28 hzeller Exp $ author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands.properties;

import henplus.HenPlus;
import henplus.PropertyRegistry;
import henplus.io.ConfigurationContainer;
import henplus.property.PropertyHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Set global HenPlus properties.
 */
public class PropertyCommand extends AbstractPropertyCommand {

    private static final String SETTINGS_FILENAME = "properties";
    private final HenPlus _henplus;
    private final PropertyRegistry _registry;
    private final ConfigurationContainer _config;

    public PropertyCommand(final HenPlus henplus, final PropertyRegistry registry) {
        _henplus = henplus;
        _registry = registry;
        _config = _henplus.createConfigurationContainer(SETTINGS_FILENAME);
    }

    @Override
    protected String getSetCommand() {
        return "set-property";
    }

    @Override
    protected String getHelpHeader() {
        return "global HenPlus";
    }

    @Override
    protected PropertyRegistry getRegistry() {
        return _registry;
    }

    @Override
    public boolean requiresValidSession(final String cmd) {
        return false;
    }

    public void load() {
        final Map<String,String> props = _config.readProperties();
        
        for (Entry<String,String> entry : props.entrySet()) {
            try {
                _registry.setProperty(entry.getKey(), entry.getValue());
            } catch (final Exception e) {
                // don't care. silently ignore errornous entries.
            }
        }
    }

    @Override
    public void shutdown() {
        final Map<String,String> writeMap = new HashMap<String,String>();
        for (Map.Entry<String, PropertyHolder> entry : _registry.getPropertyMap().entrySet()) {
            final PropertyHolder holder = entry.getValue();
            writeMap.put(entry.getKey(), holder.getValue());
        }
        _config.storeProperties(writeMap, true, "user properties");
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
