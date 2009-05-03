/*
 * This is free software, licensed under the Gnu Public License (GPL)
 * get a copy from <http://www.gnu.org/licenses/gpl.html>
 * $Id: DriverCommand.java,v 1.15 2008-10-19 08:53:25 hzeller Exp $
 * author: Henner Zeller <H.Zeller@acm.org>
 */
package henplus.commands;

import henplus.AbstractCommand;
import henplus.CommandDispatcher;
import henplus.HenPlus;
import henplus.SQLSession;
import henplus.io.ConfigurationContainer;
import henplus.view.Column;
import henplus.view.ColumnMetaData;
import henplus.view.TableRenderer;
import henplus.view.util.SortedMatchIterator;

import java.sql.Driver;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * document me.
 */
public final class DriverCommand extends AbstractCommand {
    private static final boolean VERBOSE = HenPlus.VERBOSE;
    private static final String[][] KNOWN_DRIVERS = {
        { "Oracle", "oracle.jdbc.driver.OracleDriver",
        "jdbc:oracle:thin:@localhost:1521:ORCL" },
        { "DB2", "COM.ibm.db2.jdbc.net.DB2Driver",
        "jdbc:db2://localhost:6789/foobar" },
        { "Postgres", "org.postgresql.Driver",
        "jdbc:postgresql://localhost/foobar" },
        { "SAP-DB", "com.sap.dbtech.jdbc.DriverSapDB",
        "jdbc:sapdb://localhost/foobar" },
        { "MySQL", "org.gjt.mm.mysql.Driver",
        "jdbc:mysql://localhost/foobar" },
        { "Adabas", "de.sag.jdbc.adabasd.ADriver",
        "jdbc:adabasd://localhost:7200/work" } };

    private static final String DRIVERS_FILENAME = "drivers";
    private static final ColumnMetaData[] DRV_META;
    static {
        DRV_META = new ColumnMetaData[4];
        DRV_META[0] = new ColumnMetaData("for");
        DRV_META[1] = new ColumnMetaData("driver class");
        DRV_META[2] = new ColumnMetaData("Version");
        DRV_META[3] = new ColumnMetaData("sample url");
    }

    private static final class DriverDescription {
        private final String _className;
        private final String _sampleURL;

        private String _version; // known after loading.
        private boolean _loaded;

        public DriverDescription(final String cn, final String surl) {
            _className = cn;
            _sampleURL = surl;
            load();
        }

        public String getClassName() {
            return _className;
        }

        public String getSampleURL() {
            return _sampleURL;
        }

        public String getVersion() {
            return _version;
        }

        public boolean isLoaded() {
            return _loaded;
        }

        public boolean load() {
            try {
                if (VERBOSE) {
                    HenPlus.msg().print("loading .. '" + _className + "'");
                }
                final Class<?> cls = Class.forName(_className);
                if (VERBOSE) {
                    HenPlus.msg().println(" done.");
                }
                try {
                    final Driver driver = (Driver) cls.newInstance();
                    _version = driver.getMajorVersion() + "." + driver
                            .getMinorVersion();
                } catch (final Throwable t) {
                    // ign.
                }
                _loaded = true;
            } catch (final Throwable t) {
                if (VERBOSE) {
                    HenPlus.msg().println(" failed: " + t.getMessage());
                }
                _loaded = false;
            }
            return _loaded;
        }
    }

    private final SortedMap<String,DriverDescription> _drivers;
    private final ConfigurationContainer _config;

    /**
     * returns the command-strings this command can handle.
     */
    public String[] getCommandList() {
        return new String[] { "list-drivers", "register", "unregister" };
    }

    public DriverCommand(final HenPlus henplus) {
        _drivers = new TreeMap();
        _config = henplus.createConfigurationContainer(DRIVERS_FILENAME);
        final Map props = _config.readProperties();
        final Iterator propNames = props.keySet().iterator();
        while (propNames.hasNext()) {
            final String name = (String) propNames.next();
            if (name.startsWith("driver.") && name.endsWith(".class")) {
                final String databaseName = name.substring("driver.".length(), name
                        .length()
                        - ".class".length());
                final String exampleName = "driver." + databaseName + ".example";
                DriverDescription desc;

                desc = new DriverDescription((String) props.get(name),
                        (String) props.get(exampleName));
                _drivers.put(databaseName, desc);
            }
        }
        if (_drivers.size() == 0) {
            for (int i = 0; i < KNOWN_DRIVERS.length; ++i) {
                final String[] row = KNOWN_DRIVERS[i];
                _drivers.put(row[0], new DriverDescription(row[1], row[2]));
            }
        }
    }

    @Override
    public boolean requiresValidSession(final String cmd) {
        return false;
    }

    /**
     * execute the command given.
     */
    public int execute(final SQLSession currentSession, final String cmd, final String param) {
        final StringTokenizer st = new StringTokenizer(param);
        final int argc = st.countTokens();

        if ("list-drivers".equals(cmd)) {
            if (argc == 0) {
                HenPlus
                .msg()
                .println(
                        "loaded drivers are marked with '*' (otherwise not found in CLASSPATH)");
                DRV_META[0].resetWidth();
                DRV_META[1].resetWidth();
                DRV_META[2].resetWidth();
                DRV_META[3].resetWidth();
                final TableRenderer table = new TableRenderer(DRV_META, HenPlus.out());
                final Iterator vars = _drivers.entrySet().iterator();
                while (vars.hasNext()) {
                    final Map.Entry entry = (Map.Entry) vars.next();
                    final Column[] row = new Column[4];
                    final DriverDescription desc = (DriverDescription) entry
                    .getValue();
                    final String dbName = (String) entry.getKey();
                    row[0] = new Column((desc.isLoaded() ? "* " : "  ")
                            + dbName);
                    row[1] = new Column(desc.getClassName());
                    row[2] = new Column(desc.getVersion());
                    row[3] = new Column(desc.getSampleURL());
                    table.addRow(row);
                }
                table.closeTable();
                return SUCCESS;
            } else {
                return SYNTAX_ERROR;
            }
        } else if ("register".equals(cmd)) {
            if (argc < 2 || argc > 3) {
                return SYNTAX_ERROR;
            }
            final String shortname = (String) st.nextElement();
            final String driverClass = (String) st.nextElement();
            String sampleURL = null;
            if (argc >= 3) {
                sampleURL = (String) st.nextElement();
            }
            DriverDescription drv;
            drv = new DriverDescription(driverClass, sampleURL);
            if (!drv.isLoaded()) {
                HenPlus.msg().println(
                        "cannot load driver class '" + driverClass + "'");
                return EXEC_FAILED;
            } else {
                _drivers.put(shortname, drv);
            }
        } else if ("unregister".equals(cmd)) {
            if (argc != 1) {
                return SYNTAX_ERROR;
            }
            final String shortname = (String) st.nextElement();
            if (!_drivers.containsKey(shortname)) {
                HenPlus.msg().println("unknown driver for '" + shortname + "'");
                return EXEC_FAILED;
            } else {
                _drivers.remove(shortname);
            }
        }
        return SUCCESS;
    }

    @Override
    public Iterator complete(final CommandDispatcher disp, final String partialCommand,
            final String lastWord) {
        final StringTokenizer st = new StringTokenizer(partialCommand);
        final String cmd = (String) st.nextElement();
        final int argc = st.countTokens();
        // list-drivers gets no names.
        if ("list-drivers".equals(cmd)) {
            return null;
        }
        // do not complete beyond first word.
        if (argc > ("".equals(lastWord) ? 0 : 1)) {
            return null;
        }
        return new SortedMatchIterator(lastWord, _drivers);
    }

    @Override
    public void shutdown() {
        final Map result = new HashMap();
        final Iterator drvs = _drivers.entrySet().iterator();
        while (drvs.hasNext()) {
            final Map.Entry entry = (Map.Entry) drvs.next();
            final String shortName = (String) entry.getKey();
            final DriverDescription desc = (DriverDescription) entry.getValue();
            result.put("driver." + shortName + ".class", desc.getClassName());
            result.put("driver." + shortName + ".example", desc.getSampleURL());
        }
        _config.storeProperties(result, true, "JDBC drivers");
    }

    /**
     * return a descriptive string.
     */
    @Override
    public String getShortDescription() {
        return "handle JDBC drivers";
    }

    @Override
    public String getSynopsis(final String cmd) {
        if ("unregister".equals(cmd)) {
            return cmd + " <shortname>";
        } else if ("register".equals(cmd)) {
            return cmd + " <shortname> <driver-class> [sample-url]";
        }
        return cmd;
    }

    @Override
    public String getLongDescription(final String cmd) {
        String dsc = null;
        if ("register".equals(cmd)) {
            dsc = "\tRegister a  new driver.   Basically this  adds the  JDBC\n"
                + "\tdriver represented by its driver class. You have to give\n"
                + "\ta short name,  that is used in the user interface.   You\n"
                + "\tmight  give  a  sample  JDBC-URL  that is shown with the\n"
                + "\tlist-drivers command.   This  command tries  to load the\n"
                + "\tdriver from the CLASSPATH;  if it is not found,  then it\n"
                + "\tis not added to the list of usable drivers.";
        } else if ("unregister".equals(cmd)) {
            dsc = "\tUnregister the driver with the given shortname. There\n"
                + "\tis a command line completion for the shortname, but you\n"
                + "\tcan list them as well with the list-drivers command.";
        } else if ("list-drivers".equals(cmd)) {
            dsc = "\tList the drivers that are registered. The drivers, that\n"
                + "\tare actually loaded have a little star (*) in the first\n"
                + "\tcolumn. If it is not loaded, than you have to augment your\n"
                + "\tCLASSPATH in order to be able to connect to some database\n"
                + "\tof that kind.";
        }
        return dsc;
    }
}

/*
 * Local variables: c-basic-offset: 4 compile-command:
 * "ant -emacs -find build.xml" End:
 */
