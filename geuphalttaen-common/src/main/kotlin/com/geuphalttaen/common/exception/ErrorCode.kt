package com.geuphalttaen.common.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val code: String,
    val status: HttpStatus,
    val message: String,
) {
    UNAUTHORIZED("C401", HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    FORBIDDEN("C403", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
    NOT_FOUND("C404", HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR("C500", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // OAuth 에러
    OAUTH_INVALID_TOKEN("O001", HttpStatus.UNAUTHORIZED, "유효하지 않은 OAuth 토큰입니다."),
    UNSUPPORTED_PROVIDER("O002", HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth 제공자입니다."),

    // 인증 토큰 에러
    INVALID_TOKEN("A001", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_NOT_FOUND("A002", HttpStatus.UNAUTHORIZED, "토큰을 찾을 수 없습니다."),

    // 화장실 에러
    TOILET_NOT_FOUND("T001", HttpStatus.NOT_FOUND, "화장실 정보를 찾을 수 없습니다."),

    // 사용자 에러
    USER_NOT_FOUND("U001", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // 경로 에러
    ROUTE_NOT_FOUND("R001", HttpStatus.UNPROCESSABLE_ENTITY, "경로를 찾을 수 없습니다."),

    // 관리자 에러
    ADMIN_NOT_FOUND("AD001", HttpStatus.NOT_FOUND, "관리자 계정을 찾을 수 없습니다."),
    ADMIN_INVALID_CREDENTIALS("AD002", HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    TOILET_STATUS_INVALID("AD003", HttpStatus.BAD_REQUEST, "해당 상태로 변경할 수 없습니다."),
    ADMIN_ALREADY_EXISTS("AD004", HttpStatus.CONFLICT, "이미 관리자 계정이 존재합니다."),
    ADMIN_SEED_FORBIDDEN("AD005", HttpStatus.FORBIDDEN, "시드 시크릿이 올바르지 않습니다."),

    // 이미지 에러
    IMAGE_UPLOAD_FAILED("IMG001", HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다."),
    IMAGE_INVALID_TYPE("IMG002", HttpStatus.BAD_REQUEST, "허용되지 않는 이미지 형식입니다. (jpeg, png, webp만 허용)"),
    IMAGE_TOO_LARGE("IMG003", HttpStatus.BAD_REQUEST, "이미지 크기는 10MB를 초과할 수 없습니다."),
    IMAGE_INVALID_URL("IMG004", HttpStatus.BAD_REQUEST, "허용되지 않는 이미지 URL입니다."),

    // 리뷰 에러
    REVIEW_NOT_FOUND("RV001", HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    REVIEW_DUPLICATE("RV002", HttpStatus.BAD_REQUEST, "이미 리뷰를 작성하셨습니다."),
    REVIEW_RATING_INVALID("RV003", HttpStatus.BAD_REQUEST, "별점은 1~5 사이의 값이어야 합니다."),
}
