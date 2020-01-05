package ru.wtrn.cfddns.configuration.propeties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("cf-ddns")
data class CloudflareProperties(
    val email: String,
    val apiToken: String,
    val zoneName: String,
    val subdomain: String
)
