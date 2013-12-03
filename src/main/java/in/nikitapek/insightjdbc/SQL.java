package in.nikitapek.insightjdbc;

import java.sql.*;

public class SQL {
    private static final String COMMIT_QUERY =
        "COMMIT";

    private SQL() {}

    static boolean databaseExists(Connection connection, String databaseName) {
        boolean databaseExists = false;

        try {
            ResultSet resultSet = connection.getMetaData().getCatalogs();

            while (resultSet.next()) {
                if (databaseName.equals(resultSet.getString(1))) {
                    databaseExists = true;
                }
            }

            resultSet.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to check if database '" + databaseName + "' exists.");
        }

        return databaseExists;
    }

    static boolean tableExists(Connection connection, String databaseName, String tableName) {
        boolean tableExists = false;

        if (!SQL.databaseExists(connection, databaseName)) {
            return false;
        }

        try {
            DatabaseMetaData meta = connection.getMetaData();
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

    static Statement createStatement(Connection connection) {
        Statement statement = null;
        try {
            statement = connection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to create statement.");
        }

        return statement;
    }

    static PreparedStatement prepareStatement(Connection connection, String query) {
        PreparedStatement preparedStatement = null;
        try {
            preparedStatement = connection.prepareStatement(query);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to prepare statement with query: " + query);
        }

        return preparedStatement;
    }

    static void closeStatement(Statement statement) {
        try {
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to close statement.");
        }
    }

    static boolean ensureTableExists(Connection connection, String databaseName, String tableName, String tableCreationQuery) {
        if (SQL.tableExists(connection, databaseName, tableName)) {
            return true;
        }

        Statement statement = SQL.createStatement(connection);
        try {
            statement.executeUpdate(tableCreationQuery);
            commit(connection);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to create authentication table '" + tableName + "'.");
        } finally {
            SQL.closeStatement(statement);
        }

        return false;
    }

    public static void commit(Connection connection) {
        Statement statement = SQL.createStatement(connection);
        try {
            statement.executeUpdate(COMMIT_QUERY);
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("[insight-jdbc] Failed to commit changes.");
        } finally {
            SQL.closeStatement(statement);
        }
    }
}
