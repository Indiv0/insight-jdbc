package in.nikitapek.insightjdbc;

import org.apache.catalina.realm.JDBCRealm;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.Principal;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class InsightRealm extends JDBCRealm {
    private static final String databaseCreationQuery =
        "CREATE DATABASE ?;";

    private static final String usersTableCreationQuery =
        "CREATE TABLE `tomcat_users` (\n" +
        "    `user_name` varchar(20) NOT NULL PRIMARY KEY,\n" +
        "    `password` varchar(32) NOT NULL\n" +
        ");";
    private static final String insertDefaultUsersQuery =
        "INSERT INTO `tomcat_users` (`user_name`, `password`) VALUES ('admin', '21232f297a57a5a743894a0e4a801fc3');";
    private static final String rolesTableCreationQuery =
        "CREATE TABLE `tomcat_roles` (\n" +
        "   `role_name` varchar(20) NOT NULL PRIMARY KEY\n" +
        ");";
    private static final String insertDefaultRolesQuery =
        "INSERT INTO `tomcat_roles` (`role_name`) VALUES ('insight-user');";
    private static final String usersRolesTableCreationQuery = 
        "CREATE TABLE IF NOT EXISTS `tomcat_users_roles` (\n" +
        "    `user_name` varchar(20) NOT NULL,\n" +
        "    `role_name` varchar(20) NOT NULL,\n" +
        "    PRIMARY KEY (`user_name`,`role_name`),\n" +
        "    CONSTRAINT `tomcat_users_roles_foreign_key_1` FOREIGN KEY (`user_name`) REFERENCES `tomcat_users` (`user_name`),\n" +
        "    CONSTRAINT `tomcat_users_roles_foreign_key_2` FOREIGN KEY (`role_name`) REFERENCES `tomcat_roles` (`role_name`)\n" +
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
    private static final String insertDefaultUsersRolesQuery =
         "INSERT INTO `tomcat_users_roles` (`user_name`, `role_name`) VALUES ('admin', 'insight-user');";

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
        setProperties();

        ensureAuthenticationDatabaseExists();
        ensureTableExists("tomcat_users", usersTableCreationQuery, insertDefaultUsersQuery);
        ensureTableExists("tomcat_roles", rolesTableCreationQuery, insertDefaultRolesQuery);
        ensureTableExists("tomcat_users_roles", usersRolesTableCreationQuery, insertDefaultUsersRolesQuery);
    }

    @Override
    protected Connection open() throws SQLException {
        if (properties == null) {
            loadProperties();
            setProperties();
        }

        return super.open();
    }

    private void ensureAuthenticationDatabaseExists() {
        String configuredDatabaseName = properties.getProperty("dbName");

        if (databaseExists()) {
            return;
        }

        try {
            PreparedStatement preparedStatement = open().prepareStatement(databaseCreationQuery);
            preparedStatement.setString(1, configuredDatabaseName);
            //preparedStatement.setString(2, configuredDatabaseName);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to create authentication database.");
        }
    }

    private boolean databaseExists() {
        String configuredDatabaseName = properties.getProperty("dbName");
        boolean databaseExists = false;

        try {
            ResultSet resultSet = open().getMetaData().getCatalogs();

            while (resultSet.next()) {
                String databaseName = resultSet.getString(1);

                if (configuredDatabaseName.equals(resultSet.getString(1))) {
                    databaseExists = true;
                }
            }

            resultSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to check if database '" + configuredDatabaseName + "' exists.");
        }

        return databaseExists;
    }

    private boolean tableExists(String tableName) {
        boolean tableExists = false;

        if (!databaseExists()) {
            return false;
        }

        try {
            DatabaseMetaData meta = open().getMetaData();
            ResultSet resultSet = meta.getTables(null, null, null, new String[] {"TABLE"});

            while (resultSet.next()) {
                if (tableName.equals(resultSet.getString("TABLE_NAME"))) {
                    tableExists = true;
                }
            }

            resultSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to check if table '" + tableName + "' exists.");
        }

        return tableExists;
    }

    private void ensureTableExists(String tableName, String tableCreationQuery, String defaultValuesQuery) {
        if (tableExists(tableName)) {
            return;
        }

        try {
            //PreparedStatement preparedStatement = open().prepareStatement(tableCreationQuery);
            //preparedStatement.setString(1, configuredDatabaseName);
            //preparedStatement.executeUpdate(tableCreationQuery);
            Connection connection = open();
            connection.createStatement().executeUpdate(tableCreationQuery);
            connection.createStatement().executeUpdate(defaultValuesQuery);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to create authentication table '" + tableName + "'.");
        }
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
        String fileLocation = "insightweb-auth.properties";

        if (confDir != null) {
            File file1 = new File(confDir);
            File file2 = new File(file1, fileLocation);
            fileLocation = file2.getPath();
        }

        return fileLocation;
    }
}
