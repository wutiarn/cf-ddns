package ru.wtrn.cfddns.configuration.propeties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("cf-ddns.ip-resolution")
data class IpResolutionProperties(
    val timeout: Duration = Duration.ofSeconds(3)
)
