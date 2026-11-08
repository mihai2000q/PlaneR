package app.planer.server.common.plugins

import app.planer.server.common.transactions.ExposedTransactionRunner
import app.planer.server.common.transactions.TransactionRunner
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.plugins.di.dependencies
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.configureDatabase() {
    val config = environment.config
    val setting = { key: String -> config.property("database.$key").getString() }

    val dataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:postgresql://${setting("host")}:${setting("port")}/${setting("name")}"
            username = setting("user")
            password = setting("password")
            maximumPoolSize = 10
        },
    )
    Database.connect(dataSource)
    dependencies { provide<TransactionRunner> { ExposedTransactionRunner() } }

    monitor.subscribe(ApplicationStopped) { dataSource.close() }
}
