package ru.wtrn.cfddns.dto.api

import ru.wtrn.cfddns.model.IpAddressType
import ru.wtrn.cfddns.service.CloudflareService
import ru.wtrn.cfddns.service.CurrentIpAddressesResolutionService

data class StatusResponse(
    val currentIpAddresses: Map<IpAddressType, String>,
    val cloudflareZoneRecords: CloudflareService.ZoneRecords
)
