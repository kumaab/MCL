
package org.mcl.config;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.mcl.utils.Constants;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

public class Config {

    private static final Logger LOG     = LogManager.getLogger(Config.class);

    public boolean isDir(String path){
        File f = new File(path);
        return f.isDirectory();
    }

    public Properties readProperties() {
        return readProperties(Constants.propertiesFile);
    }

    public Properties readProperties(String fileName) {
        Properties  ret     = null;
        InputStream inStr   = null;
        URL fileURL         = null;
        File f              = new File(fileName);

        if (f.exists() && f.isFile() && f.canRead()) {
            try {
                inStr   = new FileInputStream(f);
                fileURL = f.toURI().toURL();
            } catch (FileNotFoundException exception) {
                LOG.error("Error processing input file:" + fileName + " or no privilege for reading file " + fileName, exception);
            } catch (MalformedURLException malformedException) {
                LOG.error("Error processing input file:" + fileName + " cannot be converted to URL " + fileName, malformedException);
            }
        } else {
            fileURL = Config.class.getResource(fileName);

            if (fileURL == null) {
                fileURL = ClassLoader.getSystemClassLoader().getResource(fileName);
            }
        }

        if (fileURL != null) {
            try {
                inStr = fileURL.openStream();
                Properties prop = new Properties();
                prop.load(inStr);
                ret = prop;
            } catch (Exception excp) {
                LOG.error("failed to load properties from file '" + fileName + "'", excp);
            } finally {
                if (inStr != null) {
                    try {
                        inStr.close();
                    } catch (Exception excp) {
                        // ignore
                    }
                }
            }
        }
        return ret;
    }
}
