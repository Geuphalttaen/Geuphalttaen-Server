package com.geuphalttaen.domain.image

data class PresignedUploadResult(
    val presignedUrl: String,
    val objectKey: String,
    val publicUrl: String,
)

interface ImageStoragePort {
    /**
     * R2에 직접 업로드할 수 있는 presigned PUT URL을 발급한다.
     * @param objectKey 저장될 오브젝트 키 (예: "toilet-images/uuid.jpg")
     * @param contentType MIME 타입 (예: "image/jpeg")
     * @param expiresInSeconds URL 유효 시간 (초), 기본 300
     */
    fun generatePresignedPutUrl(
        objectKey: String,
        contentType: String,
        expiresInSeconds: Long = 300,
    ): PresignedUploadResult
}
