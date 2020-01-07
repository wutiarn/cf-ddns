package ru.wtrn.cfddns.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import ru.wtrn.cfddns.client.WhatIsMyIpAddressClient
import ru.wtrn.cfddns.model.IpAddressType
import java.util.UUID

@Service
class CurrentIpAddressesResolutionService(
    private val whatIsMyIpAddressClient: WhatIsMyIpAddressClient
) {
    suspend fun getCurrentIpAddresses(): Map<IpAddressType, String> = withContext(Dispatchers.IO) {
        listOf(
            IpAddressType.IPv4 to async { whatIsMyIpAddressClient.getCurrentIpv4Address() },
            IpAddressType.IPv6 to async { whatIsMyIpAddressClient.getCurrentIpv6Address() }
        ).mapNotNull { (addrType, deferred) ->
            deferred.await()?.let {
                addrType to it
            }
        }.toMap()
    }
}
