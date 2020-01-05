package ru.wtrn.cfddns.model

enum class IpAddressType(val zoneType: String) {
    IPv4("A"),
    IPv6("AAAA");

    companion object {
        private val valuesByZoneType = values().associateBy { it.zoneType }
        fun getByZoneType(zoneType: String): IpAddressType? {
            return valuesByZoneType[zoneType]
        }
    }
}
