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
    public static InsightRealm realm;

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
        super();

        if (realm == null) {
            realm = this;
        }
    }

    @Override
    protected Connection open() throws SQLException {
        if (properties == null) {
            try {
                System.out.println("[insight-jdbc] Loading properties.");
                properties = loadProperties();
            } catch (IOException e) {
                System.out.println("[insight-jdbc] Error loading properties");
            }

            System.out.println("[insight-jdbc] Saving default properties");
            properties = saveDefaultProperties();

            setConnectionName(properties.getProperty("dbUsername"));
            setConnectionPassword(properties.getProperty("dbPassword"));
            setConnectionURL("jdbc:mysql://" + properties.getProperty("dbURL") + ":" + properties.getProperty("dbPort") + "/" + properties.getProperty("dbName"));
            setDriverName("org.mariadb.jdbc.Driver");
        }

        return super.open();
    }

    public void setDatabaseProperties(String username, String password, String url, String port, String databaseName) {
        properties.setProperty("dbUsername", username);
        properties.setProperty("dbPassword", password);
        properties.setProperty("dbUrl", url);
        properties.setProperty("dbPort", port);
        properties.setProperty("dbName", databaseName);
        saveProperties(properties);
    }

    private Properties loadProperties() throws IOException {
        FileInputStream propFile = new FileInputStream(fileLocation);

        Properties p = new Properties();
        p.load(propFile);

        return p;
    }

    private Properties saveDefaultProperties() {
        Properties properties = new Properties();

        for (Map.Entry<String, String> entry : defaultProperties.entrySet()) {
            properties.setProperty(entry.getKey(), entry.getValue());
        }

        saveProperties(properties);

        return properties;
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

    public boolean userExists(String username) {
        return getPassword(username) == null;
    }

    @Override
    public String getPassword(String username) {
        return super.getPassword(username);
    }
}
