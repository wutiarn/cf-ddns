package ru.wtrn.cfddns.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import ru.wtrn.cfddns.configuration.propeties.CloudflareProperties
import ru.wtrn.cfddns.dto.cloudflare.CloudFlareResponse
import ru.wtrn.cfddns.dto.cloudflare.CloudFlareZone
import ru.wtrn.cfddns.dto.cloudflare.CloudFlareZoneRecord
import ru.wtrn.cfddns.model.IpAddressType

@Service
class CloudflareService(
    private val properties: CloudflareProperties
) {
    private val logger = KotlinLogging.logger { }

    private val webClient = WebClient.builder()
        .baseUrl("https://api.cloudflare.com/client/v4/")
        .defaultHeader("X-Auth-Email", properties.email)
        .defaultHeader("X-Auth-Key", properties.authKey)
        .build()

    suspend fun patchRecordAddress(newAddresses: Map<IpAddressType, String>) = withContext(Dispatchers.IO) {
        val currentRecords = getZoneRecords()

        newAddresses.mapNotNull { (addrType, newAddress) ->
            val record = currentRecords.recordsByType[addrType] ?: let {
                logger.warn { "Failed to find ${addrType.zoneType} record for configured subdomain. Skipping $addrType patch." }
                return@mapNotNull null
            }

            if (record.content == newAddress) {
                logger.debug {
                    "${addrType.zoneType} already up to date"
                }
                return@mapNotNull null
            }

            addrType to async {
                patchRecordContent(zoneId = currentRecords.zoneId, recordId = record.id, newContent = newAddress)
            }
        }.forEach { (addrType, job) ->
            try {
                job.await()
                logger.debug { "${addrType.zoneType} record successfully updated" }
            } catch (e: Exception) {
                logger.warn(e) { "Failed to update ${addrType.zoneType} record" }
            }
        }
    }

    suspend fun getZoneRecords(): ZoneRecords {
        val zoneId = webClient.get()
            .uri {
                it.path("zones")
                    .queryParam("name", properties.zoneName)
                    .build()
            }
            .attribute("name", properties.zoneName)
            .retrieve()
            .awaitBody<CloudFlareResponse<List<CloudFlareZone>>>()
            .result
            .firstOrNull()
            ?.id ?: throw IllegalStateException("Cannot find requested zone id")

        val recordsByType = webClient.get()
            .uri {
                it.path("zones/{zoneId}/dns_records")
                    .queryParam("name", "${properties.subdomain}.${properties.zoneName}")
                    .build(zoneId)
            }
            .attribute("name", "${properties.subdomain}.${properties.zoneName}")
            .retrieve()
            .awaitBody<CloudFlareResponse<List<CloudFlareZoneRecord>>>()
            .result
            /**
             * Match A/AAAA zoneRecord type with IpAddressType, skipping unsupported types (like TXT, CNAME and so on)
             */
            .mapNotNull { zoneRecord ->
                IpAddressType.getByZoneType(zoneRecord.type)?.let { zoneType ->
                    zoneType to zoneRecord
                }
            }
            .toMap()

        return ZoneRecords(
            zoneId = zoneId,
            recordsByType = recordsByType
        )
    }

    private suspend fun patchRecordContent(zoneId: String, recordId: String, newContent: String) {
        webClient.patch()
            .uri {
                it.path("zones/{zoneId}/dns_records/{recordId}")
                    .build(zoneId, recordId)
            }
            .bodyValue(
                mapOf(
                    "content" to newContent
                )
            )
            .exchange()
            .awaitFirst()
    }

    data class ZoneRecords(
        val zoneId: String,
        val recordsByType: Map<IpAddressType, CloudFlareZoneRecord>
    )
}
