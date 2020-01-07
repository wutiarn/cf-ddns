package ru.wtrn.cfddns.configuration.propeties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("cf-ddns.cloudflare")
data class CloudflareProperties(
    val email: String,
    val authKey: String,
    val zoneName: String,
    val subdomain: String
)
