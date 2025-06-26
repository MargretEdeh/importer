package com.example.db

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SchemaUtils.create
import java.io.File
import java.math.BigDecimal

@Serializable
data class RateEntry(val prefix: String, val rate: Double)

object CountryCarriers : Table("country_carriers") {
    val id = integer("id").autoIncrement()
    val countryCode = varchar("country_code", length = 50)
    val carrierName = varchar("carrier_name", length = 100)
    val prefixes = varchar("prefixes", 50)
    override val primaryKey = PrimaryKey(id)
}

object ProviderRates : Table("provider_rates") {
    val id = integer("id").autoIncrement()
    val providerName = varchar("provider_name", 100)
    val prefix = varchar("prefix", 20)
    val rate = decimal("rate", 10, 5)
    override val primaryKey = PrimaryKey(id)
}

object JsonToPostgresImporter {
    fun importData() {
        // Establish DB connection before doing anything
        Database.connect(
            url = System.getenv("DB_URL") ?: "jdbc:postgresql://db:5432/esdiacrates",
            driver = System.getenv("DB_DRIVER") ?: "org.postgresql.Driver",
            user = System.getenv("DB_USER") ?: "postgres",
            password = System.getenv("DB_PASSWORD") ?: "saga@2500"
        )


        transaction {
            // Automatically create tables if they don't exist
            create(CountryCarriers, ProviderRates)
        }

        val dataDir = File("src/main/resources/data")
        val ratesDir = File("src/main/resources/rates")

        // 1. Import country_carriers
        println("📥 Importing country_carriers...")
        dataDir.listFiles { file: File -> file.extension == "json" }?.forEach { file ->
            val countryCode = file.nameWithoutExtension
            val json = Json.parseToJsonElement(file.readText()).jsonObject

            json.forEach { (carrier, prefixes) ->
                prefixes.jsonArray.forEach { prefixElement ->
                    val prefix = prefixElement.jsonPrimitive.content
                    transaction {
                        CountryCarriers.insert {
                            it[CountryCarriers.countryCode] = countryCode
                            it[CountryCarriers.carrierName] = carrier
                            it[CountryCarriers.prefixes] = prefix
                        }
                    }
                }
            }
        }

        // 2. Import provider_rates
        println("📥 Importing provider_rates...")
        ratesDir.listFiles { file: File -> file.extension == "json" }?.forEach { file ->
            val providerName = file.nameWithoutExtension
            val json = Json.parseToJsonElement(file.readText()).jsonObject

            json.forEach { (_, entries) ->
                entries.jsonArray.forEach { entry ->
                    val prefix = entry.jsonObject["prefix"]?.jsonPrimitive?.content ?: return@forEach
                    val rate = entry.jsonObject["rate"]?.jsonPrimitive?.doubleOrNull ?: return@forEach
                    transaction {
                        ProviderRates.insert {
                            it[ProviderRates.providerName] = providerName
                            it[ProviderRates.prefix] = prefix
                            it[ProviderRates.rate] = BigDecimal.valueOf(rate)
                        }
                    }
                }
            }
        }

        println("✅ Data import completed!")
    }
}
