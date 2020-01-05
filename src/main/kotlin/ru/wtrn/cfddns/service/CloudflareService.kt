package ru.wtrn.cfddns.service

import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import ru.wtrn.cfddns.configuration.propeties.CloudflareProperties
import ru.wtrn.cfddns.dto.cloudflare.CloudFlareResponse
import ru.wtrn.cfddns.dto.cloudflare.CloudFlareZone
import ru.wtrn.cfddns.dto.cloudflare.CloudFlareZoneRecord

@Service
class CloudflareService(
    private val properties: CloudflareProperties
) {
    private val webClient = WebClient.builder()
        .baseUrl("https://api.cloudflare.com/client/v4/")
        .defaultHeader("X-Auth-Email", properties.email)
        .defaultHeader("X-Auth-Key", properties.authKey)
        .build()

    private suspend fun getZoneRecords(): ZoneRecords {
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
            .associateBy { it.type }

        return ZoneRecords(
            zoneId = zoneId,
            recordsByType = recordsByType
        )
    }

    data class ZoneRecords(
        val zoneId: String,
        val recordsByType: Map<String, CloudFlareZoneRecord>
    )
}
