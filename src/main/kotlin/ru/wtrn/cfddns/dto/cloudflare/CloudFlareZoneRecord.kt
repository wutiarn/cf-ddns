package ru.wtrn.cfddns.dto.cloudflare

data class CloudFlareZoneRecord(
    val id: String,
    val type: String,
    val content: String
)
