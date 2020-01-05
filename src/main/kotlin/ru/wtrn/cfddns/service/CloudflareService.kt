package ru.wtrn.cfddns.service

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import ru.wtrn.cfddns.configuration.propeties.CloudflareProperties

@Service
class CloudflareService(
    private val properties: CloudflareProperties
) {
    private val webClient = WebClient.builder()
        .baseUrl("https://api.cloudflare.com/client/v4/")
        .defaultHeader("X-Auth-Email", properties.email)
        .defaultHeader("X-Auth-Key", properties.authKey)
        .build()

    private var cachedZoneRecordSpec: ZoneRecordSpec? = null

    suspend fun getZoneRecordSpec(): ZoneRecordSpec {
        return cachedZoneRecordSpec ?: queryZoneRecordSpec().also {
            cachedZoneRecordSpec = it
        }
    }

    private suspend fun queryZoneRecordSpec(): ZoneRecordSpec {
        val zoneListResponse = webClient.get()
                .uri("zones")
                .attribute("name", properties.zoneName)
                .retrieve()
                .awaitBody<JsonNode>()
        val zoneId = zoneListResponse.path("result")
            .path(0)
            .path("id")
            .textValue() ?: throw IllegalStateException("Failed to retrieve zoneId from response $zoneListResponse")

        val recordsResponse = webClient.get()
            .uri("zones/$zoneId/dns_records")
            .attribute("name", properties.zoneName)
            .retrieve()
            .awaitBody<JsonNode>()
    }

    data class ZoneRecordSpec(
        val zoneId: String,
        val recordId: String
    )
}
