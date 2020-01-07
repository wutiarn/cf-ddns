package ru.wtrn.cfddns.client

import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import ru.wtrn.cfddns.configuration.propeties.CloudflareProperties
import ru.wtrn.cfddns.dto.cloudflare.CloudFlareErrorResponse
import ru.wtrn.cfddns.dto.cloudflare.CloudFlareResponse
import ru.wtrn.cfddns.dto.cloudflare.CloudFlareZone
import ru.wtrn.cfddns.dto.cloudflare.CloudFlareZoneRecord
import ru.wtrn.cfddns.model.IpAddressType

@Component
class CloudFlareClient(
    private val properties: CloudflareProperties
) {
    private val webClient = WebClient.builder()
        .baseUrl("https://api.cloudflare.com/client/v4/")
        .defaultHeader("X-Auth-Email", properties.email)
        .defaultHeader("X-Auth-Key", properties.authKey)
        .filter(responseErrorFilter)
        .build()

    suspend fun findZoneIdByZoneName(zoneName: String): String {
        return webClient.get()
            .uri {
                it.path("zones")
                    .queryParam("name", zoneName)
                    .build()
            }
            .retrieve()
            .awaitBody<CloudFlareResponse<List<CloudFlareZone>>>()
            .result
            .firstOrNull()
            ?.id ?: throw IllegalStateException("Cannot find requested zone id")
    }

    suspend fun findZoneRecord(zoneId: String, recordName: String): List<CloudFlareZoneRecord> {
        return webClient.get()
            .uri {
                it.path("zones/{zoneId}/dns_records")
                    .queryParam("name", recordName)
                    .build(zoneId)
            }
            .retrieve()
            .awaitBody<CloudFlareResponse<List<CloudFlareZoneRecord>>>()
            .result
    }

    suspend fun patchRecordContent(zoneId: String, recordId: String, newContent: String) {
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

    companion object {
        private val responseErrorFilter = ExchangeFilterFunction.ofResponseProcessor { response ->
            when (response.statusCode().isError) {
                false -> Mono.just(response)
                true -> {
                    response.bodyToMono<CloudFlareErrorResponse>()
                        .flatMap { errorResponse ->
                            Mono.error<ClientResponse>(
                                IllegalStateException(
                                    "Received error response from CloudFlare. Errors: ${errorResponse.errors}"
                                )
                            )
                        }
                }
            }
        }
    }
}
