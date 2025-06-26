package com.example

import com.example.db.JsonToPostgresImporter
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {

    configureSerialization()
    configureMonitoring()
    configureTemplating()
    configureRouting()

    JsonToPostgresImporter.importData()

}
