package uk.gov.homeoffice.unhcr.cases.tool.gui;

import org.apache.commons.io.FileUtils;
import uk.gov.homeoffice.unhcr.cases.tool.CaseFileValidator;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Properties;

public class ConfigProperties {

    public static final String AUTOCHECK_NEWER_VERSION = "version.auto.check";

    final static private int SYSTEM_UNKNOWN		= 0;
    final static private int SYSTEM_WINDOWS		= 1;
    final static private int SYSTEM_LINUX		= 2;
    final static private int SYSTEM_MACOS		= 3;
    final static private int SYSTEM_ANDROID		= 4;

    final static private String CONFIG_FILE = "UnhcrValidationTool" + File.separator + "config.properties";

    final static private Properties configProperties;

    final static private int systemId;
    static {
        final String os = System.getProperty("os.name").toLowerCase(Locale.US);
        if (os == null) {
            System.out.println("There is no os.name defined.");
            systemId = SYSTEM_UNKNOWN;
        } else if (os.indexOf("win") >= 0) {
            systemId = SYSTEM_WINDOWS;
        } else if (os.indexOf("linux") >= 0) {
            final String runtime = System.getProperty("java.runtime.name");
            if ((runtime != null) && (runtime.toLowerCase(Locale.US).indexOf("android") >= 0)) {
                systemId = SYSTEM_ANDROID;
            } else {
                systemId = SYSTEM_LINUX;
            }
        } else if (os.indexOf("mac os") >= 0) {
            // note: should Mac OS X, but we check just for Mac OS (in case PowerPC runs it?)
            systemId = SYSTEM_MACOS;
        } else {
            System.out.println("Unknown system [" + os + "]");
            systemId = SYSTEM_UNKNOWN;
        }
    }

    final static public boolean isWindows() {
        return systemId==SYSTEM_WINDOWS;
    }

    final static public boolean isLinux() {
        return systemId==SYSTEM_LINUX;
    }

    final static public boolean isMacOSX() {
        return systemId==SYSTEM_MACOS;
    }

    final static public boolean isAndroid() {
        return systemId==SYSTEM_ANDROID;
    }

    final static String getConfigFilePath() {
        if (isWindows()) {
            // Note: there are problems in Windows 7 and 8, as it returns
            // %USERPROFILE% in user.home

            // try to get from %appdata% at first
            String userAppDataFolder = System.getenv("appdata");
            if (userAppDataFolder == null) {
                // try user.home
                userAppDataFolder = System.getProperty("user.home");
            }

            // if no user home, then try current directory
            if (userAppDataFolder == null) {
                userAppDataFolder = "";
            }

            if (!userAppDataFolder.endsWith(File.separator)) {
                userAppDataFolder = userAppDataFolder + File.separator;
            }

            return userAppDataFolder + CONFIG_FILE;
        } else if (isLinux()) {

            // linux uses user.home and creates folder inside
            String userHomeFolder = System.getProperty("user.home");
            if (!userHomeFolder.endsWith(File.separator)) {
                userHomeFolder = userHomeFolder + File.separator;
            }
            return userHomeFolder + ".config" + File.separator + CONFIG_FILE;
        } else if (isMacOSX()) {
            // Mac OS X uses user.home/Library/Application Support/
            // /Users/<User-Name>/Library/Application Support/
            String userHomeFolder = System.getProperty("user.home");
            if (!userHomeFolder.endsWith(File.separator)) {
                userHomeFolder = userHomeFolder + File.separator;
            }
            return userHomeFolder + "Library" + File.separator + "Application Support" + File.separator + CONFIG_FILE;
        } else if (isAndroid()) {
            try {

                @SuppressWarnings("rawtypes") final Class clazz = Class.forName("android.os.Environment");

                @SuppressWarnings("unchecked") final Method method = clazz.getMethod("getExternalStorageDirectory", new Class[]{});
                final File file = (File) method.invoke(null);

                String externalStorageDirectoryPath = file.getAbsolutePath();
                if (!externalStorageDirectoryPath.endsWith(File.separator)) {
                    externalStorageDirectoryPath = externalStorageDirectoryPath + File.separator;
                }

                return externalStorageDirectoryPath + CONFIG_FILE;
            } catch (final Exception e) {
                throw new RuntimeException("Could not find external storage folder", e);
            }
        } else {
            // for all unknown systems use current directory
            return "";
        }
    }

    static {
        final File configPropertiesFile = getConfigPropertiesFile();
        try {
            configProperties    = loadConfigPropertiesFile(configPropertiesFile);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Config file %s cannot be loaded", configPropertiesFile.getAbsolutePath()), e);
        }
    }

    final static private File getConfigPropertiesFile() {
        return new File(getConfigFilePath());
    }

    final static public boolean getConfigPropertyAsBoolean(String propertyName) {
        return Boolean.parseBoolean(configProperties.getProperty(propertyName));
    }

    final static public void setConfigProperty(String propertyName, boolean propertyValue) throws IOException {
        configProperties.setProperty(propertyName, Boolean.toString(propertyValue));
        saveConfigPropertiesFile(getConfigPropertiesFile());
    }

    final static public void deleteConfigFile() {
        FileUtils.deleteQuietly(getConfigPropertiesFile());
    }

    final static private void saveConfigPropertiesFile(File configPropertiesFile) throws IOException {
        //create parent folders (if they don't exist)
        FileUtils.createParentDirectories(configPropertiesFile);
        try (OutputStream output = new FileOutputStream(configPropertiesFile)) {
            configProperties.store(output, CaseFileValidator.NAME_AND_VERSION);
        }
    }

    final static private Properties loadConfigPropertiesFile(File configPropertiesFile) throws IOException {
        if (configPropertiesFile.exists()) {
            try (InputStream input = new FileInputStream(configPropertiesFile)) {
                Properties properties = new Properties();
                properties.load(input);
                return properties;
            }
        } else {
            return new Properties();
        }
    }
}
