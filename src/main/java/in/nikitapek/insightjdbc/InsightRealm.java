package in.nikitapek.insightjdbc;

import org.apache.catalina.realm.JDBCRealm;

import java.sql.*;

public class InsightRealm extends JDBCRealm {
    private static final String DATABASE_CREATION_QUERY =
        "CREATE DATABASE ?;";

    private static final String USERS_TABLE_CREATION_QUERY =
        "CREATE TABLE `tomcat_users` (\n" +
        "    `user_name` varchar(20) NOT NULL PRIMARY KEY,\n" +
        "    `password` varchar(32) NOT NULL\n" +
        ");";
    private static final String INSERT_DEFAULT_USERS_QUERY =
        "INSERT INTO `tomcat_users` (`user_name`, `password`) VALUES (?, ?);";
    private static final String ROLES_TABLE_CREATION_QUERY =
        "CREATE TABLE `tomcat_roles` (\n" +
        "   `role_name` varchar(20) NOT NULL PRIMARY KEY\n" +
        ");";
    private static final String INSERT_DEFAULT_ROLES_QUERY =
        "INSERT INTO `tomcat_roles` (`role_name`) VALUES (?);";
    private static final String USERS_ROLES_TABLE_CREATION_QUERY =
        "CREATE TABLE IF NOT EXISTS `tomcat_users_roles` (\n" +
        "    `user_name` varchar(20) NOT NULL,\n" +
        "    `role_name` varchar(20) NOT NULL,\n" +
        "    PRIMARY KEY (`user_name`,`role_name`),\n" +
        "    CONSTRAINT `tomcat_users_roles_foreign_key_1` FOREIGN KEY (`user_name`) REFERENCES `tomcat_users` (`user_name`),\n" +
        "    CONSTRAINT `tomcat_users_roles_foreign_key_2` FOREIGN KEY (`role_name`) REFERENCES `tomcat_roles` (`role_name`)\n" +
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8;";
    private static final String INSERT_DEFAULT_USERS_ROLES_QUERY =
         "INSERT INTO `tomcat_users_roles` (`user_name`, `role_name`) VALUES (?, ?);";

    private RealmProperties properties = new RealmProperties("insightweb-auth.properties");
    private boolean propertiesSet = false;

    public InsightRealm() {
        setProperties();
        ensureAuthenticationDatabaseExists();
        ensureTableExists("tomcat_users", USERS_TABLE_CREATION_QUERY);
        try {
            Connection connection = open();
            PreparedStatement preparedStatement = connection.prepareStatement(INSERT_DEFAULT_USERS_QUERY);
            preparedStatement.setString(1, "admin");
            preparedStatement.setString(2, "21232f297a57a5a743894a0e4a801fc3");
            preparedStatement.executeUpdate();
            preparedStatement.setString(1, "user");
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to insert default users.");
        }

        ensureTableExists("tomcat_roles", ROLES_TABLE_CREATION_QUERY);
        try {
            Connection connection = open();
            PreparedStatement preparedStatement = connection.prepareStatement(INSERT_DEFAULT_ROLES_QUERY);
            preparedStatement.setString(1, "insight-admin");
            preparedStatement.executeUpdate();
            preparedStatement.setString(1, "insight-user");
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to insert default roles.");
        }

        ensureTableExists("tomcat_users_roles", USERS_ROLES_TABLE_CREATION_QUERY);
        try {
            Connection connection = open();
            PreparedStatement preparedStatement = connection.prepareStatement(INSERT_DEFAULT_USERS_ROLES_QUERY);
            preparedStatement.setString(1, "admin");
            preparedStatement.setString(2, "insight-admin");
            preparedStatement.executeUpdate();
            preparedStatement.setString(1, "user");
            preparedStatement.setString(2, "insight-user");
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to insert default roles.");
        }
    }

    @Override
    protected Connection open() throws SQLException {
        if (!propertiesSet) {
            setProperties();
            propertiesSet = true;
        }

        return super.open();
    }

    private void ensureAuthenticationDatabaseExists() {
        if (databaseExists()) {
            return;
        }

        try {
            PreparedStatement preparedStatement = open().prepareStatement(DATABASE_CREATION_QUERY);
            preparedStatement.setString(1, properties.getDatabaseName());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to create authentication database.");
        }
    }

    private boolean databaseExists() {
        String configuredDatabaseName = properties.getDatabaseName();
        boolean databaseExists = false;

        try {
            ResultSet resultSet = open().getMetaData().getCatalogs();

            while (resultSet.next()) {
                if (properties.getDatabaseName().equals(resultSet.getString(1))) {
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

    private void ensureTableExists(String tableName, String tableCreationQuery) {
        if (tableExists(tableName)) {
            return;
        }

        try {
            Connection connection = open();
            connection.createStatement().executeUpdate(tableCreationQuery);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to create authentication table '" + tableName + "'.");
        }
    }

    private void setProperties() {
        setConnectionName(properties.getUsername());
        setConnectionPassword(properties.getPassword());
        setConnectionURL("jdbc:mysql://" + properties.getURL() + ":" + properties.getPort() + "/" + properties.getDatabaseName());
        setDriverName("org.mariadb.jdbc.Driver");

        System.out.println("[insight-jdbc] Properties set:");
        System.out.println("\t" + getConnectionName());
        System.out.println("\t" + getConnectionURL());
        System.out.println("\t" + getDriverName());
    }
}
