package com.example

import com.example.plugins.configureRouting
import com.example.plugins.configureTemplating
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.sql.DriverManager
import java.sql.ResultSet

fun main() {
    Class.forName("org.sqlite.JDBC")
    DriverManager.getConnection("jdbc:sqlite:./database.db").use { conn ->
        try {
            val stmt = conn.createStatement()

            val resultSet: ResultSet = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='users'")
            val tableExists = resultSet.next()

            if (!tableExists) {// safe check for loading table fail/ doesn't exist
                stmt.executeUpdate("CREATE TABLE users (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "name TEXT," +
                        "password TEXT)")

                stmt.executeUpdate("INSERT INTO users (name, password) VALUES ('Admin', 'admin')")
                //if the data table does not exist create one and fill it with admin user

            }

            val resultSetAll: ResultSet = stmt.executeQuery("SELECT * FROM users")
            println("Loading users from database...")// for bug testing with database
            while (resultSetAll.next()) {
                val name = resultSetAll.getString("name")
                val password = resultSetAll.getString("password")
                println("Username: ${name} Password: ${password}")
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureTemplating()
    configureRouting()
}

