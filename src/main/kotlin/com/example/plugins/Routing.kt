package com.example.plugins

import kotlinx.html.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import io.ktor.server.routing.*



object Users : IntIdTable() {
    val name = varchar("name", 25)
    val password = varchar("password", 30)
}

fun getUserById(id: Int): User? {
    return transaction {//got some tutors and classmate help to understand transaction required changing dependency's
        Users.select { Users.id eq id }
            .singleOrNull()?.let {
                User(it[Users.id].value, it[Users.name], it[Users.password])
            }
    }
}

data class User(val id: Int, val name: String, val password: String)


fun getUserfromDB(): List<User> {
    val users = mutableListOf<User>()
    transaction {
        Users.selectAll().forEach {
            val id = it[Users.id].value
            val name = it[Users.name]
            val password = it[Users.password]
            users.add(User(id, name, password))
        }
    }
    return users
}

fun Application.configureRouting() {
    Database.connect("jdbc:sqlite:./database.db", driver = "org.sqlite.JDBC")
    routing {
        get("/") {
            call.respondHtml {
                head {
                    title("Home")
                }
                body {
                    h1 { +"Welcome to REEL 2" }
                    h3 { +"The only real social media platform makes a surprising comeback" }
                    p {
                        a(href = "/create") { +"Create Account" }
                        h2 { +"  " }//acts as a new line character
                        a(href = "/view") { +"Active Users" }
                    }
                }
            }
        }

        post("/create") {
            try {
                val parameters = call.receiveParameters()
                val name = parameters["name"]
                val password = parameters["password"]

                if (name.isNullOrBlank() || password.isNullOrBlank()) {
                    println("Bad input, redirecting to /error")
                    call.respondRedirect("/error")
                    return@post
                }

                transaction {
                    Users.insertAndGetId {
                        it[Users.name] = name
                        it[Users.password] = password
                    }
                }

                //println("User created successfully")
            } catch (e: Exception) {
                call.respondRedirect("/error")
            }
            call.respondRedirect("/view")
        }

        get("/create") {
            call.respondHtml {
                head {
                    title("Create Get")
                }
                body {
                    h1 { +"Create Account" }
                    form("/create", method = FormMethod.post) {
                        p {
                            +"Username"
                            textInput(name = "name") {}
                            h2 { +"  " }// another dumb new line replacement
                            +"Password"
                            textInput(name = "password") {}
                        }
                        p { submitInput { value = "Create Account" } }
                    }
                }
            }
        }
        get("/error") {
            call.respondHtml {
                head { title("Error page") }
                body {
                    h2 { +"Error Bad input" }
                    a(href = "/") { +"Home" }
                }
            }
        }
        get("/view") {
            try {
                val users = getUserfromDB()

                call.respondHtml {
                    head {
                        title("View Users")
                    }
                    body {
                        h3 { +"Active Users" }
                        ul {
                            for (user in users) {
                                +"Username: ${user.name} Password: ${user.password}"

                                p {
                                    a(href = "/change?id=${user.id}") { +"Reset Password" }
                                    // weird method to avoid problems that come from form for each user
                                }

                                form("/delete", method = FormMethod.post) {
                                    hiddenInput(name = "id") {
                                        value = user.id.toString()
                                    }
                                    submitInput { value = "Delete Account" }
                                }
                            }

                        }
                        p { a(href = "/") { +"Home" } }
                    }

                }
            } catch (e: Exception) {
                call.respondRedirect("/")
            }
        }

        post("/delete") {
            try {
                val userId = call.receiveParameters()["id"]?.toIntOrNull()

                if (userId != null) {
                    transaction {
                        Users.deleteWhere { Users.id eq userId }
                    }
                    call.respondRedirect("/view")
                } else {
                    call.respondRedirect("/view")
                }
            } catch (e: Exception) {
                call.respondRedirect("/view")
            }
        }
        get("/change") {
            val userId = call.parameters["id"]?.toIntOrNull()

            if (userId != null) {
                try {


                val user = getUserById(userId)


                    call.respondHtml {
                        head {
                            title("Reset Password")
                        }
                        body {
                            h2 { +"Reset Password for ${user?.name}" }


                            form(action = "/update-password", method = FormMethod.post) {
                                hiddenInput(name = "id") { value = userId.toString() }
                                p {
                                    label { +"New Password:" }
                                    textInput(name = "newPassword") {}
                                }
                                p { submitInput { value = "Update Password" } }
                            }
                        }
                    }
                } catch (e: Exception){
                    call.respondRedirect("/view")
                }
            } else {
                call.respondRedirect("/view")
            }
        }
        post("/update-password") {
            val parameters = call.receiveParameters()
            val userId = parameters["id"]?.toIntOrNull()
            val newPassword = parameters["newPassword"]

            if (userId != null && !newPassword.isNullOrBlank()) {
                transaction {
                    Users.update({ Users.id eq userId }) {
                        it[password] = newPassword
                    }
                }
                call.respondRedirect("/view")
            } else {
                call.respondHtml {
                    head { title("Error") }
                    body {
                        p { +"Bad Input for New Password" }
                        p { +"Password was not changed!" }
                        a(href = "/"){+"Home"}
                    }
                }
            }
        }
        route("/{...}") {
            handle { call.respondRedirect("/") }
        }

    }
}


