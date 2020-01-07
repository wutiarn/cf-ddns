package ru.wtrn.cfddns.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.springframework.stereotype.Service
import ru.wtrn.cfddns.client.CloudFlareClient
import ru.wtrn.cfddns.configuration.propeties.CloudflareProperties
import ru.wtrn.cfddns.dto.cloudflare.CloudFlareZoneRecord
import ru.wtrn.cfddns.model.IpAddressType

@Service
class CloudflareService(
    private val properties: CloudflareProperties,
    private val cloudFlareClient: CloudFlareClient
) {
    private val logger = KotlinLogging.logger { }

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
                cloudFlareClient.patchRecordContent(zoneId = currentRecords.zoneId, recordId = record.id, newContent = newAddress)
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
        val zoneId = cloudFlareClient.findZoneIdByZoneName(properties.zoneName)
        val records = cloudFlareClient.findZoneRecord(
            zoneId = zoneId,
            recordName = "${properties.subdomain}.${properties.zoneName}"
        )

        /**
         * Match A/AAAA zoneRecord type with IpAddressType, skipping unsupported types (like TXT, CNAME and so on)
         */
        val recordsByType = records.mapNotNull { zoneRecord ->
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
