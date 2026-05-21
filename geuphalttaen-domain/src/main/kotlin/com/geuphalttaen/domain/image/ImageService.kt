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

private val JPEG_MAGIC = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
private val PNG_MAGIC = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte())
private val RIFF_MAGIC = "RIFF".toByteArray(Charsets.US_ASCII)
private val WEBP_MAGIC = "WEBP".toByteArray(Charsets.US_ASCII)

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
        validateMagicBytes(data, contentType)

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

    private fun validateMagicBytes(data: ByteArray, contentType: String) {
        val valid = when (contentType) {
            "image/jpeg" -> data.size >= 3 && data.copyOfRange(0, 3).contentEquals(JPEG_MAGIC)
            "image/png" -> data.size >= 4 && data.copyOfRange(0, 4).contentEquals(PNG_MAGIC)
            "image/webp" -> data.size >= 12 &&
                data.copyOfRange(0, 4).contentEquals(RIFF_MAGIC) &&
                data.copyOfRange(8, 12).contentEquals(WEBP_MAGIC)
            else -> false
        }
        if (!valid) throw BusinessException(ErrorCode.IMAGE_INVALID_TYPE)
    }
}
