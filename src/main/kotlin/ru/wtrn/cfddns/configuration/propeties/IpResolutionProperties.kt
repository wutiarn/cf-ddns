package ru.wtrn.cfddns.configuration.propeties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("cf-ddns.ip-resolution")
data class IpResolutionProperties(
    val contactEmail: String,
    val timeout: Duration = Duration.ofSeconds(3),

    /**
     * ipv4/ipv6 fields used instead of Map<IpAddressType, VersionSpecificResolutionProperties>
     * to support Spring Boot Configuration Annotation Processor
     */
    val ipv4: VersionSpecificResolutionProperties = VersionSpecificResolutionProperties(),
    val ipv6: VersionSpecificResolutionProperties = VersionSpecificResolutionProperties()
) {
    data class VersionSpecificResolutionProperties(
        val active: Boolean = true,
        val warnOnFailure: Boolean = true
    )
}
