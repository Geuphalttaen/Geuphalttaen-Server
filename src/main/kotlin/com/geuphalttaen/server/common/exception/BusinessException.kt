package com.geuphalttaen.server.common.exception

import org.springframework.http.HttpStatus

open class BusinessException(
    val status: HttpStatus,
    override val message: String,
) : RuntimeException(message)

class NotFoundException(message: String = "리소스를 찾을 수 없습니다.") :
    BusinessException(HttpStatus.NOT_FOUND, message)
