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

        String databaseName = properties.getDatabaseName();

        ensureAuthenticationDatabaseExists(open(), databaseName);
        ensureUsersTableExists(open(), databaseName);
        ensureRolesTableExists(open(), databaseName);
        ensureUsersRolesTableExists(open(), databaseName);
    }

    @Override
    protected Connection open() {
        if (!propertiesSet) {
            setProperties();
            propertiesSet = true;
        }

        Connection connection;

        try {
            connection = super.open();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to create connection to database.");
            return null;
        }

        return connection;
    }

    private static void ensureAuthenticationDatabaseExists(Connection connection, String databaseName) {
        if (SQL.databaseExists(connection, databaseName)) {
            return;
        }

        PreparedStatement preparedStatement = SQL.prepareStatement(connection, DATABASE_CREATION_QUERY);
        try {
            preparedStatement.setString(1, databaseName);
            preparedStatement.executeUpdate();
            SQL.commit(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to create authentication database.");
        } finally {
            SQL.closeStatement(preparedStatement);
        }
    }

    private static void ensureUsersTableExists(Connection connection, String databaseName) {
        if (SQL.ensureTableExists(connection, databaseName, "tomcat_users", USERS_TABLE_CREATION_QUERY)) {
            return;
        }

        PreparedStatement preparedStatement = SQL.prepareStatement(connection, INSERT_DEFAULT_USERS_QUERY);
        try {
            preparedStatement.setString(1, "admin");
            preparedStatement.setString(2, "21232f297a57a5a743894a0e4a801fc3");
            preparedStatement.executeUpdate();
            preparedStatement.setString(1, "user");
            preparedStatement.executeUpdate();
            SQL.commit(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to insert default users.");
        } finally {
            SQL.closeStatement(preparedStatement);
        }
    }

    private static void ensureRolesTableExists(Connection connection, String databaseName) {
        if (SQL.ensureTableExists(connection, databaseName, "tomcat_roles", ROLES_TABLE_CREATION_QUERY)) {
            return;
        }

        PreparedStatement preparedStatement = SQL.prepareStatement(connection, INSERT_DEFAULT_ROLES_QUERY);
        try {
            preparedStatement.setString(1, "insight-admin");
            preparedStatement.executeUpdate();
            preparedStatement.setString(1, "insight-user");
            preparedStatement.executeUpdate();
            SQL.commit(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to insert default roles.");
        } finally {
            SQL.closeStatement(preparedStatement);
        }
    }

    private static void ensureUsersRolesTableExists(Connection connection, String databaseName) {
        if (SQL.ensureTableExists(connection, databaseName, "tomcat_users_roles", USERS_ROLES_TABLE_CREATION_QUERY)) {
            return;
        }

        PreparedStatement preparedStatement = SQL.prepareStatement(connection, INSERT_DEFAULT_USERS_ROLES_QUERY);
        try {
            preparedStatement.setString(1, "admin");
            preparedStatement.setString(2, "insight-admin");
            preparedStatement.executeUpdate();
            preparedStatement.setString(1, "user");
            preparedStatement.setString(2, "insight-user");
            preparedStatement.executeUpdate();
            SQL.commit(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to insert default users roles.");
        } finally {
            SQL.closeStatement(preparedStatement);
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
