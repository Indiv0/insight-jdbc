package in.nikitapek.insightjdbc;

import org.apache.catalina.realm.JDBCRealm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class InsightRealm extends JDBCRealm {
    private static Map<String, String> defaultProperties = new HashMap<>();
    private String fileLocation = getFileLocation();

    static {
        defaultProperties.put("dbName", "insight");
        defaultProperties.put("dbUsername", "insightuser");
        defaultProperties.put("dbPassword", "insight");
        defaultProperties.put("dbURL", "localhost");
        defaultProperties.put("dbPort", "3306");
    }

    private Properties properties = null;

    public InsightRealm() {
        loadProperties();
    }

    @Override
    protected Connection open() throws SQLException {
        if (properties == null) {
            loadProperties();
        }

        setProperties();

        return super.open();
    }

    public void setDatabaseProperties(String username, String password, String url, String port, String databaseName) {
        properties.setProperty("dbUsername", username);
        properties.setProperty("dbPassword", password);
        properties.setProperty("dbUrl", url);
        properties.setProperty("dbPort", port);
        properties.setProperty("dbName", databaseName);
        setProperties();
        saveProperties(properties);
    }

    private void setProperties() {
        setConnectionName(properties.getProperty("dbUsername"));
        setConnectionPassword(properties.getProperty("dbPassword"));
        setConnectionURL("jdbc:mysql://" + properties.getProperty("dbURL") + ":" + properties.getProperty("dbPort") + "/" + properties.getProperty("dbName"));
        setDriverName("org.mariadb.jdbc.Driver");

        System.out.println("[insight-jdbc] Properties set:");
        System.out.println("\t" + getConnectionName());
        System.out.println("\t" + getConnectionURL());
        System.out.println("\t" + getDriverName());
    }

    private void loadProperties() {
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

    private static String getFileLocation() {
        String confDir = System.getProperty("insightweb.confdir", null);
        String fileLocation = "insightweb.properties";

        if (confDir != null) {
            File file1 = new File(confDir);
            File file2 = new File(file1, fileLocation);
            fileLocation = file2.getPath();
        }

        return fileLocation;
    }
}
