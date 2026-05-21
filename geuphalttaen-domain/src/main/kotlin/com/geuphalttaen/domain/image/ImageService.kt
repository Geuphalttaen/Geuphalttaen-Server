package com.geuphalttaen.domain.image

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import org.springframework.stereotype.Service
import java.util.UUID

data class ImageUploadResult(
    val url: String,
    val originalUrl: String,
)

private val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/png", "image/webp")
private const val MAX_IMAGE_BYTES = 10 * 1024 * 1024L // 10MB

@Service
class ImageService(
    private val imageStoragePort: ImageStoragePort,
    private val imageConversionPort: ImageConversionPort,
) {
    fun upload(data: ByteArray, contentType: String): ImageUploadResult {
        if (contentType !in ALLOWED_CONTENT_TYPES) {
            throw BusinessException(ErrorCode.IMAGE_INVALID_TYPE)
        }
        if (data.size > MAX_IMAGE_BYTES) {
            throw BusinessException(ErrorCode.IMAGE_TOO_LARGE)
        }

        val uuid = UUID.randomUUID()
        val ext = contentType.substringAfter("/").let { if (it == "jpeg") "jpg" else it }

        val originalKey = "toilet-images/original/$uuid.$ext"
        val originalUrl = imageStoragePort.upload(originalKey, contentType, data)

        val webpBytes = imageConversionPort.toWebP(data)
        val webpKey = "toilet-images/webp/$uuid.webp"
        val webpUrl = imageStoragePort.upload(webpKey, "image/webp", webpBytes)

        return ImageUploadResult(url = webpUrl, originalUrl = originalUrl)
    }

    fun validateUrls(urls: List<String>) {
        val invalid = urls.filter { !imageStoragePort.isOwnUrl(it) }
        if (invalid.isNotEmpty()) {
            throw BusinessException(ErrorCode.IMAGE_INVALID_URL)
        }
    }
}
