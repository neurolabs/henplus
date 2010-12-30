package henplus.io;

import henplus.logging.Logger;

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
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Helper class to write the configuration. Focus is to avoid half-written configuration files if IO-Errors occur (full harddisk ..)
 * and to merge properties.
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
     * Execute the read action with the InputStream from the corresponding configuration file.
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
     * get the input stream for this configuration container. If no configuration file exists, 'null' is returned. Remember content
     * digest on close().
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
         * Write configuration. If any Exception is thrown, the original file is not overwritten.
         */
        void writeConfiguration(OutputStream out) throws Exception;
    }

    /**
     * Write configuration. The configuration is first written to a temporary file. Does not overwrite the original file if any
     * Exception occurs in the course of this or the resulting file is no different.
     */
    public void write(final WriteAction action) {
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("config-", ".tmp", _configFile.getParentFile());
            final MessageDigest outputDigest = MessageDigest.getInstance("MD5");
            final OutputStream out = new DigestOutputStream(new FileOutputStream(tmpFile), outputDigest);
            try {
                action.writeConfiguration(out);
            } finally {
                out.close();
            }
            if (_inputDigest == null || !_configFile.exists() || !MessageDigest.isEqual(_inputDigest, outputDigest.digest())) {
                Logger.debug("non equal.. write file '%s'", _configFile);
                tmpFile.renameTo(_configFile);
            }
        } catch (final Exception e) {
            Logger.error("Could not write config. Error occured: ", e);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
        }
    }

    public Map<String,String> readProperties() {
        return readProperties(null);
    }

    /**
     * convenience-method to read properties. If you handle simple properties within your command, then use this method so that
     * versioning and merging is handled.
     */
    public Map<String,String> readProperties(final Map<String,String> prefill) {
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
                Logger.error("Could not load properties: ", e);
            }
        }
        @SuppressWarnings("unchecked")
		final Map<String,String> props = (Hashtable<String,String>)/*(Hashtable<?,?>)(Properties)*/ _readProperties.clone();
        return props;
    }

    /**
     * convenience-method to write properties. Properties must have been read before.
     * 
     * @param allowMerge
     *            allow merging of properties that have been added by another instance of henplus.
     */
    @SuppressWarnings("unchecked")
	public void storeProperties(final Map<String,String> props, final boolean allowMerge, final String comment) {
        if (_readProperties == null) {
            throw new IllegalStateException("properties not read before");
        }

        /* merge if wanted */
        final Properties outputProperties = new Properties();
        if (allowMerge) {
            // all properties, that are not present compared to last read
            // should be removed after merge.
            final Set<String> locallyRemovedProperties = new HashSet<String>();
            locallyRemovedProperties.addAll((Set<String>)(Set<?>)_readProperties.keySet());
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

            for (String key : locallyRemovedProperties) {
                outputProperties.remove(key);
            }
        }

        outputProperties.putAll(props);

        if (outputProperties.equals(_readProperties)) {
            return;
        }

        write(new WriteAction() {

            @Override
            public void writeConfiguration(final OutputStream out) throws Exception {
                outputProperties.store(out, comment);
                out.close();
            }
        });
    }
}
