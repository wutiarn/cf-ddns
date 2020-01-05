package ru.wtrn.cfddns.dto.cloudflare

data class CloudFlareResponse <T : Any>(
    val result: T
)
