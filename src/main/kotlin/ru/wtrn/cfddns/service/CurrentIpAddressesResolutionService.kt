package ru.wtrn.cfddns.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Service
class CurrentIpAddressesResolutionService {
    private val logger = KotlinLogging.logger { }
    private val webClient = WebClient.builder()
        .build()

    suspend fun getCurrentIpAddresses(): CurrentIpAddresses = withContext(Dispatchers.IO) {
        val ipv4Address = async { getCurrentIpv4Address() }
        val ipv6Address = async { getCurrentIpv6Address() }
        return@withContext CurrentIpAddresses(
            ipv4 = ipv4Address.await(),
            ipv6 = ipv6Address.await()
        )
    }

    private suspend fun getCurrentIpv4Address(): String? {
        return callIpfy("https://api.ipify.org")
    }

    private suspend fun getCurrentIpv6Address(): String? {
        return callIpfy("https://api6.ipify.org")
    }

    private suspend fun callIpfy(url: String): String? {
        return webClient.get()
            .uri(url)
            .retrieve()
            .bodyToMono(String::class.java)
            .onErrorResume { e ->
                logger.debug(e) { "Failed to get $url. Returning null instead" }
                Mono.empty<String>()
            }
            .doOnNext {
                logger.info { "GET $url returned $it" }
            }
            .awaitFirstOrNull()
    }

    data class CurrentIpAddresses(
        val ipv4: String?,
        val ipv6: String?
    )
}
