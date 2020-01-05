package ru.wtrn.cfddns.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.withContext
import mu.KotlinLogging
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

    suspend fun patchRecordAddress(
        newAddresses: Map<IpAddressType, String>
    ): Map<IpAddressType, CloudFlareRecordUpdateResult> = withContext(Dispatchers.IO) {
        val currentRecords = getZoneRecords()

        val deferredResults = newAddresses.map { (addrType, newAddress) ->
            val record = currentRecords.recordsByType[addrType] ?: let {
                logger.warn { "Failed to find ${addrType.zoneType} record for configured subdomain. Skipping $addrType patch." }
                return@map addrType to CompletableDeferred(CloudFlareRecordUpdateResult.RECORD_NOT_FOUND)
            }

            if (record.content == newAddress) {
                return@map addrType to CompletableDeferred(CloudFlareRecordUpdateResult.UP_TO_DATE)
            }

            addrType to async {
                patchRecordContent(zoneId = currentRecords.zoneId, recordId = record.id, newContent = newAddress)
                CloudFlareRecordUpdateResult.UPDATED
            }
        }

        deferredResults.associate { (addrType, job) ->
            val result = try {
                job.await()
            } catch (e: Exception) {
                logger.warn(e) { "Failed to update ${addrType.zoneType} record" }
                CloudFlareRecordUpdateResult.FAILED
            }
            logger.debug { "${addrType.zoneType} record processed: $result" }
            addrType to result
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

    enum class CloudFlareRecordUpdateResult {
        UPDATED,
        UP_TO_DATE,
        RECORD_NOT_FOUND,
        FAILED
    }
}
