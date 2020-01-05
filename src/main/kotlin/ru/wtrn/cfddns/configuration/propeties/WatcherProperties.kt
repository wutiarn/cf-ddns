package ru.wtrn.cfddns.configuration.propeties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.time.Duration

@ConstructorBinding
@ConfigurationProperties("cf-ddns.watcher")
data class WatcherProperties(
    val interval: Duration = Duration.ofMinutes(5),
    val timeout: Duration = Duration.ofSeconds(30),
    val enabled: Boolean = true
)
