package com.geuphalttaen.domain.image

interface ImageStoragePort {
    /**
     * 바이트 배열을 R2에 직접 업로드하고 public URL을 반환한다.
     */
    fun upload(objectKey: String, contentType: String, data: ByteArray): String

    /**
     * 주어진 URL이 자체 R2 버킷에 속하는지 검증한다.
     */
    fun isOwnUrl(url: String): Boolean
}
