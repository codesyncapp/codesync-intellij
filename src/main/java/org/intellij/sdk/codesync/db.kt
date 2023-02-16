package org.intellij.sdk.codesync

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

class db {
    private var connection: Connection? = null

    fun connect() {
        Class.forName("org.sqlite.JDBC")
        try{
            connection = DriverManager.getConnection("jdbc:sqlite:C:/Users/Gul Ahmed/.codesync/sample.db")
        }catch (exception: Exception) {
            // Code to handle the exception
        } finally {
            // Code that will be executed regardless of whether an exception was thrown or not
        }

    }

    fun disconnect() {
        connection?.close()
    }

    fun createTable(){
        try {
            val statement = connection?.createStatement()
            statement?.executeUpdate("drop table if exists myFiles")
            statement?.executeUpdate("create table myFiles (id integer, file string)")
        } catch (exception: Exception) {
            // Code to handle the exception
        } finally {
            // Code that will be executed regardless of whether an exception was thrown or not
        }
    }

    fun executeUpdate(text: String){
        try {
            val statement = connection?.createStatement()
            var stm: String = "insert into myFiles values(1, '"+text+"')"
            statement?.executeUpdate(stm)
        } catch (exception: Exception) {
            println(exception.message)
        } finally {
            // Code that will be executed regardless of whether an exception was thrown or not
        }
    }

    fun executeQuery(){
        try {
            val statement = connection?.createStatement()
            val rs: ResultSet? = statement?.executeQuery("select * from myFiles")
            while (rs?.next() == true) {
                println(rs?.getString("file"))
            }
        } catch (exception: Exception) {
            // Code to handle the exception
        } finally {
            // Code that will be executed regardless of whether an exception was thrown or not
        }
    }


}


//class db {
//
//    private var connection: Connection? = null
//
//    fun connect(){
//        connection = DriverManager.getConnection("jdbc:sqlite:sample.db")
//    }
//
//    fun disconnect(){
//        connection?.close()
//    }
//
//    fun executeQuery(){
//        val statement: Statement = connection?.createStatement()
//        statement.executeUpdate("drop table if exists myFiles")
//        statement.executeUpdate("create table myFiles (id integer, file string)")
//        statement.executeUpdate("insert into myFiles values(1, 'Event recorded in DB')")
//    }
//
//    fun connectDB(){
//
//        Class.forName("org.sqlite.JDBC")
//
//        try {
//            // create a database connection
//            connection = DriverManager.getConnection("jdbc:sqlite:sample.db")
//            val statement: Statement = connection?.createStatement()
//            statement.setQueryTimeout(30) // set timeout to 30 sec.
//            statement.executeUpdate("drop table if exists myFiles")
//            statement.executeUpdate("create table myFiles (id integer, file string)")
//            statement.executeUpdate("insert into myFiles values(1, 'Event recorded in DB')")
//            val rs: ResultSet = statement.executeQuery("select * from myFiles")
//            while (rs.next()) {
//                // read the result set
//                println(rs.getString("file"))
//            }
//        } catch (e: SQLException) {
//            // if the error message is "out of memory",
//            // it probably means no database file is found
//            System.err.println(e.message)
//        } finally {
//            try {
//                if (connection != null) connection.close()
//            } catch (e: SQLException) {
//                // connection close failed.
//                System.err.println(e.message)
//            }
//        }
//    }
//
//}