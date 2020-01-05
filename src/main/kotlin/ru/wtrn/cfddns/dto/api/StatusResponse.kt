package ru.wtrn.cfddns.dto.api

import ru.wtrn.cfddns.model.IpAddressType
import ru.wtrn.cfddns.service.CloudflareService

data class StatusResponse(
    val currentIpAddresses: Map<IpAddressType, String>,
    val reportedIpAddresses: Map<IpAddressType, String>?,
    val cloudflareZoneRecords: CloudflareService.ZoneRecords
)
