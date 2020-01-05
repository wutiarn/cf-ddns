package ru.wtrn.cfddns.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import ru.wtrn.cfddns.dto.api.StatusResponse
import ru.wtrn.cfddns.service.CloudflareService
import ru.wtrn.cfddns.service.CurrentIpAddressesResolutionService
import ru.wtrn.cfddns.watcher.AddressChangesWatcher

@RestController
class StatusController(
    private val currentIpAddressesResolutionService: CurrentIpAddressesResolutionService,
    private val cloudflareService: CloudflareService,
    private val addressChangesWatcher: AddressChangesWatcher
) {
    @GetMapping("status")
    suspend fun status(): StatusResponse {
        return StatusResponse(
            currentIpAddresses = currentIpAddressesResolutionService.getCurrentIpAddresses(),
            cloudflareZoneRecords = cloudflareService.getZoneRecords(),
            reportedIpAddresses = addressChangesWatcher.reportedAddresses
        )
    }
}
