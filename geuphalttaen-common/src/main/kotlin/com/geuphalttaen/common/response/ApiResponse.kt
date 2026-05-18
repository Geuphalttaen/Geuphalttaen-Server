package com.geuphalttaen.common.response

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: ErrorBody? = null,
) {
    data class ErrorBody(
        val code: String,
        val message: String,
    )

    companion object {
        fun <T> ok(data: T): ApiResponse<T> = ApiResponse(success = true, data = data)

        fun <T> ok(): ApiResponse<T> = ApiResponse(success = true)

        fun <T> error(code: String, message: String): ApiResponse<T> =
            ApiResponse(success = false, error = ErrorBody(code = code, message = message))
    }
}
