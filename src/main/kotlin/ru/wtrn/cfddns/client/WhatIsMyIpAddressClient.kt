package ru.wtrn.cfddns.client

import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import ru.wtrn.cfddns.configuration.propeties.IpResolutionProperties
import ru.wtrn.cfddns.model.IpAddressType
import java.util.UUID

@Component
class WhatIsMyIpAddressClient(
    private val ipResolutionProperties: IpResolutionProperties
) {
    private val logger = KotlinLogging.logger { }

    private val webClient = WebClient.builder()
        .defaultHeader(HttpHeaders.USER_AGENT, "WTRN ClodFlare DDNS Agent (+${ipResolutionProperties.contactEmail})")
        .build()

    suspend fun getCurrentIpv4Address(): String? {
        return callEndpoint("https://ipv4bot.whatismyipaddress.com", IpAddressType.IPv4, ipResolutionProperties.ipv4)
    }

    suspend fun getCurrentIpv6Address(): String? {
        return callEndpoint("https://ipv6bot.whatismyipaddress.com", IpAddressType.IPv6, ipResolutionProperties.ipv6)
    }

    private suspend fun callEndpoint(
        url: String,
        addressType: IpAddressType,
        properties: IpResolutionProperties.VersionSpecificResolutionProperties
    ): String? {
        if (!properties.active) {
            logger.debug { "Skipping request to $url: $addressType resolution disabled" }
            return null
        }
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
                    .also {
                        logger.debug { "GET $url returned $it" }
                    }
            } catch (e: Exception) {
                val msg = { "Failed to get $url. Returning null instead" }
                when(properties.warnOnFailure) {
                    true -> logger.warn(e, msg)
                    false -> logger.debug(e, msg)
                }
                null
            }
            return@withContext result
        }
    }
}
