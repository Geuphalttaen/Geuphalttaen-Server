package com.geuphalttaen.domain.image

interface ImageStoragePort {
    /**
     * 바이트 배열을 R2에 업로드한다. 반환값 없음 — URL은 toPublicUrl(key)로 조합한다.
     */
    fun upload(objectKey: String, contentType: String, data: ByteArray)

    /**
     * 오브젝트 키 또는 구 형식 전체 URL을 공개 URL로 변환한다.
     * DB에 구 도메인 포함 URL이 저장된 경우도 경로를 추출해 현재 publicUrl로 재조합한다.
     */
    fun toPublicUrl(keyOrUrl: String): String

    /**
     * 오브젝트 키 또는 전체 URL이 자체 버킷에 속하는지 검증한다.
     */
    fun isOwnUrl(keyOrUrl: String): Boolean

    /**
     * 업로드 기본 폴더 prefix. 형식: {profile}/{folder}
     */
    fun baseFolder(): String
}
