package de.hallo5000;

import de.hallo5000.datatypes.Transaction;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class PostgresManager {

    private Connection connection;
    private Statement statement;

    private final String DB_NAME = "SaWPaymentS_DB";

    public void initDB(){
        String jdbcURL = "jdbc:postgresql://localhost:5432/"+DB_NAME;
        String username = "postgres";
        String password = "password";

        try {
            // Establish the connection
            connection = DriverManager.getConnection(jdbcURL, username, password);
            System.out.println("Connected to PostgreSQL database!");

            // Create a statement
            statement = connection.createStatement();

            statement.execute("CREATE DATABASE "+DB_NAME);
            statement.execute("CREATE TABLE IF NOT EXISTS transactions(customer_id INT PRIMARY KEY, service VARCHAR(100), checked BOOLEAN)");
        }
        catch (Exception e) {
            System.err.println("database initialization failed!");
            e.printStackTrace();
        }
    }

    public void closeDB() throws SQLException {
        // Close the connection
        connection.close();
        System.out.println("Connection closed.");
    }

    public void addTransaction(Transaction transaction){
        try{
            statement.execute("INSERT INTO transactions (customer_id, service, checked) VALUES ('"+transaction.getTransaction().customerref+"', '"+transaction.getService()+"', '"+transaction.isChecked()+"')");
        }catch(SQLException ex){
            System.err.println("tried to add a transaction to the db and failed!");
            ex.printStackTrace();
        }
    }

}
