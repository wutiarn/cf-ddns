package ru.wtrn.cfddns.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import ru.wtrn.cfddns.model.IpAddressType
import java.lang.Exception
import java.util.UUID

@Service
class CurrentIpAddressesResolutionService {
    private val logger = KotlinLogging.logger { }
    private val webClient = WebClient.builder()
        .build()

    suspend fun getCurrentIpAddresses(): Map<IpAddressType, String> = withContext(Dispatchers.IO) {
        listOf(
            IpAddressType.IPv4 to async { getCurrentIpv4Address() },
            IpAddressType.IPv6 to async { getCurrentIpv6Address() }
        ).mapNotNull { (addrType, deferred) ->
            deferred.await()?.let {
                addrType to it
            }
        }.toMap()
    }

    private suspend fun getCurrentIpv4Address(): String? {
        return callEndpoint("http://ipv4bot.whatismyipaddress.com")
    }

    private suspend fun getCurrentIpv6Address(): String? {
        return callEndpoint("http://ipv6bot.whatismyipaddress.com")
    }

    private suspend fun callEndpoint(url: String): String? {
        val mdcContext = MDCContext(
            mapOf(
                "requestId" to UUID.randomUUID().toString()
            )
        )
        return withContext(mdcContext) {
            logger.info { "Starting request to $url" }
            val result = try {
                webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(String::class.java)
                    .awaitFirstOrNull()
            } catch (e: Exception) {
                logger.debug(e) { "Failed to get $url. Returning null instead" }
                null
            }
            logger.info { "GET $url returned $result" }
            return@withContext result
        }
    }
}
