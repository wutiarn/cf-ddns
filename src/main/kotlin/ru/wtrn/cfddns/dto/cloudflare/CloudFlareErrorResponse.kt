package ru.wtrn.cfddns.dto.cloudflare

data class CloudFlareErrorResponse(
    val errors: List<Error>
) {
    data class Error(
        val code: Int,
        val message: String
    )
}
