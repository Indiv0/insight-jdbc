package in.nikitapek.insightjdbc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class RealmProperties {
    private Properties properties = null;
    private final String fileLocation;

    private static final Map<String, String> defaultProperties = new HashMap<>();
    static {
        defaultProperties.put("dbName", "insight");
        defaultProperties.put("dbUsername", "insightuser");
        defaultProperties.put("dbPassword", "insight");
        defaultProperties.put("dbURL", "localhost");
        defaultProperties.put("dbPort", "3306");
    }

    public RealmProperties(String fileName) {
        this.fileLocation = getFileLocation(fileName);
        loadProperties();
    }

    public void loadProperties() {
        try {
            System.out.println("[insight-jdbc] Loading properties.");
            FileInputStream propFile = new FileInputStream(fileLocation);
            Properties properties = new Properties();
            properties.load(propFile);
            this.properties = properties;
        } catch (IOException e) {
            System.out.println("[insight-jdbc] Error loading properties");
            System.out.println("[insight-jdbc] Saving default properties");
            saveDefaultProperties();
        }

        System.out.println("[insight-jdbc] Properties loaded.");
    }

    private void saveDefaultProperties() {
        Properties properties = new Properties();

        for (Map.Entry<String, String> entry : defaultProperties.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue());
        }

        saveProperties(properties);

        this.properties = properties;
    }

    private void saveProperties(Properties properties) {
        try {
            properties.store(new FileOutputStream(fileLocation), null);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static String getFileLocation(String fileName) {
        String confDir = System.getProperty("insightweb.confdir", null);

        if (confDir != null) {
            File file1 = new File(confDir);
            File file2 = new File(file1, fileName);
            fileName = file2.getPath();
        }

        return fileName;
    }

    public String getDatabaseName() {
        return properties.getProperty("dbName");
    }

    public String getUsername() {
        return properties.getProperty("dbUsername");
    }

    public String getPassword() {
        return properties.getProperty("dbPassword");
    }

    public String getURL() {
        return properties.getProperty("dbURL");
    }

    public String getPort() {
        return properties.getProperty("dbPort");
    }
}
