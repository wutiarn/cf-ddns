package ru.wtrn.cfddns.watcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import org.springframework.stereotype.Component
import ru.wtrn.cfddns.configuration.propeties.WatcherProperties
import ru.wtrn.cfddns.model.IpAddressType
import ru.wtrn.cfddns.service.CloudflareService
import ru.wtrn.cfddns.service.CurrentIpAddressesResolutionService
import java.time.Duration
import java.time.Instant
import javax.annotation.PostConstruct

@Component
class AddressChangesWatcher(
    private val watcherProperties: WatcherProperties,
    private val currentIpAddressesResolutionService: CurrentIpAddressesResolutionService,
    private val cloudflareService: CloudflareService
) {
    private val logger = KotlinLogging.logger { }

    private var reportedAddresses: Map<IpAddressType, String>? = null

    @PostConstruct
    fun setup() {
        GlobalScope.async(Dispatchers.IO) {
            while (true) {
                val mdcContextMap = mapOf("startTime" to Instant.now().toString())
                withContext(MDCContext(mdcContextMap)) {
                    try {
                        withTimeout(watcherProperties.timeout.toMillis()) {
                            checkForAddressChanges()
                        }
                    } catch (e: Exception) {
                        logger.warn { "Exception occurred in ip address changes watcher" }
                    }
                }
                delay(Duration.ofMinutes(1).toMillis())
            }
        }
    }

    suspend fun checkForAddressChanges() {
        val currentAddresses = currentIpAddressesResolutionService.getCurrentIpAddresses()
        if (currentAddresses != reportedAddresses) {
            logger.info {
                val types = currentAddresses.keys + (reportedAddresses?.keys ?: emptySet())
                val details = types.map { "$it: ${reportedAddresses?.get(it)} -> ${currentAddresses[it]}" }.joinToString()
                "Address change detected. $details."
            }
        }
        cloudflareService.patchRecordAddress(currentAddresses)
        reportedAddresses = currentAddresses
        logger.info { "Successfully reported current addresses" }
    }
}
