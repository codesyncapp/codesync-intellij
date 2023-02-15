package org.intellij.sdk.codesync


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;


class db {

    fun connectDB(){

        Class.forName("org.sqlite.JDBC")

        var connection: Connection? = null
        try {
            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:sample.db")
            val statement: Statement = connection.createStatement()
            statement.setQueryTimeout(30) // set timeout to 30 sec.
            statement.executeUpdate("drop table if exists myFiles")
            statement.executeUpdate("create table myFiles (id integer, file string)")
            statement.executeUpdate("insert into myFiles values(1, 'Event recorded in DB')")
            val rs: ResultSet = statement.executeQuery("select * from myFiles")
            while (rs.next()) {
                // read the result set
                println(rs.getString("file"))
            }
        } catch (e: SQLException) {
            // if the error message is "out of memory",
            // it probably means no database file is found
            System.err.println(e.message)
        } finally {
            try {
                if (connection != null) connection.close()
            } catch (e: SQLException) {
                // connection close failed.
                System.err.println(e.message)
            }
        }
    }

}