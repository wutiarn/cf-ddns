package ru.wtrn.cfddns.client

import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import ru.wtrn.cfddns.configuration.propeties.CloudflareProperties
import ru.wtrn.cfddns.configuration.propeties.IpResolutionProperties
import ru.wtrn.cfddns.dto.cloudflare.CloudFlareErrorResponse
import ru.wtrn.cfddns.dto.cloudflare.CloudFlareResponse
import ru.wtrn.cfddns.dto.cloudflare.CloudFlareZone
import ru.wtrn.cfddns.dto.cloudflare.CloudFlareZoneRecord
import ru.wtrn.cfddns.model.IpAddressType
import java.util.UUID

@Component
class WhatIsMyIpAddressClient(
    private val ipResolutionProperties: IpResolutionProperties
) {
    private val logger = KotlinLogging.logger { }

    private val webClient = WebClient.builder()
        .build()

    suspend fun getCurrentIpv4Address(): String? {
        return callEndpoint("https://ipv4bot.whatismyipaddress.com")
    }

    suspend fun getCurrentIpv6Address(): String? {
        return callEndpoint("https://ipv6bot.whatismyipaddress.com")
    }

    private suspend fun callEndpoint(url: String): String? {
        MDC.put("requestId", UUID.randomUUID().toString())
        return withContext(MDCContext()) {
            logger.debug { "Starting request to $url" }
            val result = try {
                webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .timeout(ipResolutionProperties.timeout)
                    .awaitFirstOrNull()
            } catch (e: Exception) {
                logger.debug(e) { "Failed to get $url. Returning null instead" }
                null
            }
            logger.debug { "GET $url returned $result" }
            return@withContext result
        }
    }
}
