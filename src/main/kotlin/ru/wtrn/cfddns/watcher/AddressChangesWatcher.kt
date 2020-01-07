package ru.wtrn.cfddns.watcher

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import ru.wtrn.cfddns.configuration.propeties.WatcherProperties
import ru.wtrn.cfddns.model.IpAddressType
import ru.wtrn.cfddns.service.CloudflareService
import ru.wtrn.cfddns.service.CurrentIpAddressesResolutionService
import java.time.Instant
import java.util.UUID
import javax.annotation.PostConstruct

@Component
class AddressChangesWatcher(
    private val watcherProperties: WatcherProperties,
    private val currentIpAddressesResolutionService: CurrentIpAddressesResolutionService,
    private val cloudflareService: CloudflareService,
    private val configurableApplicationContext: ConfigurableApplicationContext
) {
    private val logger = KotlinLogging.logger { }

    var reportedAddresses: Map<IpAddressType, String>? = null

    @PostConstruct
    fun setup() {
        if (!watcherProperties.enabled) {
            logger.warn { "AddressChangesWatcher disabled in properties" }
            return
        }
        @Suppress("DeferredResultUnused")
        GlobalScope.async(Dispatchers.IO) {
            logger.info { "AddressChangesWatcher started with interval ${watcherProperties.interval}" }
            while (true) {
                try {
                    checkForAddressChanges()
                } catch (e: Exception) {
                    logger.warn(e) { "Potentially non-recoverable exception occurred in ip address changes watcher. Terminating." }
                    configurableApplicationContext.close()
                }
                delay(watcherProperties.interval.toMillis())
            }
        }
    }

    private suspend fun checkForAddressChanges() {
        MDC.put("watcherCycleId", UUID.randomUUID().toString())
        withContext(MDCContext()) {
            withTimeout(watcherProperties.timeout.toMillis()) {
                doCheckForAddressChanges()
            }
        }
    }

    private suspend fun doCheckForAddressChanges() {
        logger.debug { "Checking for address changes" }
        val currentAddresses = currentIpAddressesResolutionService.getCurrentIpAddresses()
        if (currentAddresses != reportedAddresses) {
            logger.info {
                val types = currentAddresses.keys + (reportedAddresses?.keys ?: emptySet())
                val details = types.joinToString { "$it: ${reportedAddresses?.get(it)} -> ${currentAddresses[it]}" }
                "Address change detected. $details."
            }
            val result = cloudflareService.patchRecordAddress(currentAddresses)
            reportedAddresses = currentAddresses
            logger.info {
                val details = result.entries.joinToString { "${it.key} ${it.value}" }
                "Cloudflare response: $details"
            }
        }
    }
}
