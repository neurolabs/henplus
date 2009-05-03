package henplus.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to write the configuration. Focus is to avoid half-written
 * configuration files if IO-Errors occur (full harddisk ..) and to merge
 * properties.
 * 
 * @author hzeller
 * @version $Revision: 1.1 $
 */
public final class ConfigurationContainer {
    /** configuration file name. */
    private final File _configFile;

    /** file content digest on last read. */
    private byte[] _inputDigest;

    /** properties read initially. */
    private Properties _readProperties;

    public ConfigurationContainer(final File file) {
        _configFile = file.getAbsoluteFile();
    }

    public interface ReadAction {
        void readConfiguration(InputStream in) throws Exception;
    }

    /**
     * Execute the read action with the InputStream from the corresponding
     * configuration file.
     */
    public void read(final ReadAction action) {
        try {
            final InputStream input = getInput();
            try {
                action.readConfiguration(input);
            } finally {
                if (input != null) {
                    input.close();
                }
            }
        } catch (final Exception e) {
        }
    }

    /**
     * get the input stream for this configuration container. If no
     * configuration file exists, 'null' is returned. Remember content digest on
     * close().
     */
    private InputStream getInput() {
        if (!_configFile.canRead()) {
            return null;
        }
        try {
            final InputStream in = new FileInputStream(_configFile);
            final MessageDigest inputDigest = MessageDigest.getInstance("MD5");
            return new DigestInputStream(in, inputDigest) {
                boolean isClosed = false;

                @Override
                public void close() throws IOException {
                    if (!isClosed) {
                        super.close();
                        _inputDigest = inputDigest.digest();
                    }
                    isClosed = true;
                }
            };
        } catch (final Exception e) {
            return null; // no input.
        }
    }

    public interface WriteAction {
        /**
         * Write configuration. If any Exception is thrown, the original file is
         * not overwritten.
         */
        void writeConfiguration(OutputStream out) throws Exception;
    }

    /**
     * Write configuration. The configuration is first written to a temporary
     * file. Does not overwrite the original file if any Exception occurs in the
     * course of this or the resulting file is no different.
     */
    public void write(final WriteAction action) {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("config-", ".tmp", _configFile
                    .getParentFile());
            final MessageDigest outputDigest = MessageDigest.getInstance("MD5");
            final OutputStream out = new DigestOutputStream(new FileOutputStream(
                    tmpFile), outputDigest);
            try {
                action.writeConfiguration(out);
            } finally {
                out.close();
            }
            if (_inputDigest == null
                    || !_configFile.exists()
                    || !MessageDigest.isEqual(_inputDigest, outputDigest
                            .digest())) {
                // System.err.println("non equal.. write file " + _configFile);
                tmpFile.renameTo(_configFile);
            }
        } catch (final Exception e) {
            System.err.println("do not write config. Error occured: " + e);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    public Map readProperties() {
        return readProperties(null);
    }

    /**
     * convenience-method to read properties. If you handle simple properties
     * within your command, then use this method so that versioning and merging
     * is handled.
     */
    public Map readProperties(final Map prefill) {
        _readProperties = new Properties();
        if (prefill != null) {
            _readProperties.putAll(prefill);
        }
        final InputStream input = getInput();
        if (input != null) {
            try {
                _readProperties.load(input);
                input.close();
            } catch (final Exception e) {
                System.err.println(e); // can't help.
            }
        }
        final Map props = (Properties) _readProperties.clone();
        return props;
    }

    /**
     * convenience-method to write properties. Properties must have been read
     * before.
     * 
     * @param allowMerge
     *            allow merging of properties that have been added by another
     *            instance of henplus.
     */
    public void storeProperties(final Map props, final boolean allowMerge,
            final String comment) {
        if (_readProperties == null) {
            throw new IllegalStateException("properties not read before");
        }

        /* merge if wanted */
        final Properties outputProperties = new Properties();
        if (allowMerge) {
            // all properties, that are not present compared to last read
            // should be removed after merge.
            final Set locallyRemovedProperties = new HashSet();
            locallyRemovedProperties.addAll(_readProperties.keySet());
            locallyRemovedProperties.removeAll(props.keySet());

            final InputStream input = getInput();
            if (input != null) {
                try {
                    outputProperties.load(input);
                    input.close();
                } catch (final Exception e) {
                    // can't help.
                }
            }

            final Iterator it = locallyRemovedProperties.iterator();
            while (it.hasNext()) {
                final String key = (String) it.next();
                outputProperties.remove(key);
            }
        }

        outputProperties.putAll(props);

        if (outputProperties.equals(_readProperties)) {
            // System.err.println("equal properties. Do nothing " +
            // _configFile);
            return;
        }

        write(new WriteAction() {
            public void writeConfiguration(final OutputStream out) throws Exception {
                outputProperties.store(out, comment);
                out.close();
            }
        });
    }
}
