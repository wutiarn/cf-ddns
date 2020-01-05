package ru.wtrn.cfddns.dto.api

import ru.wtrn.cfddns.service.CloudflareService
import ru.wtrn.cfddns.service.CurrentIpAddressesResolutionService

data class StatusResponse(
    val currentIpAddresses: CurrentIpAddressesResolutionService.CurrentIpAddresses,
    val cloudflareZoneRecords: CloudflareService.ZoneRecords
)
