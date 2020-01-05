package ru.wtrn.cfddns.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import ru.wtrn.cfddns.dto.StatusResponse
import ru.wtrn.cfddns.service.CurrentIpAddressesResolutionService

@RestController
class StatusController(
    private val currentIpAddressesResolutionService: CurrentIpAddressesResolutionService
) {
    @GetMapping("status")
    suspend fun status(): StatusResponse {
        val currentIpAddresses = currentIpAddressesResolutionService.getCurrentIpAddresses()
        return StatusResponse(
            currentIpAddresses = currentIpAddresses
        )
    }
}
