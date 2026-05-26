package com.geuphalttaen.domain.image

import com.geuphalttaen.common.exception.BusinessException
import com.geuphalttaen.common.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any

@ExtendWith(MockitoExtension::class)
class ImageServiceTest {

    @Mock
    private lateinit var imageStoragePort: ImageStoragePort

    @Mock
    private lateinit var imageConversionPort: ImageConversionPort

    private lateinit var imageService: ImageService

    @BeforeEach
    fun setUp() {
        imageService = ImageService(imageStoragePort, imageConversionPort)
    }

    // ──────────────────────────────────────────
    // upload
    // ──────────────────────────────────────────

    @Test
    fun `upload - JPEG 매직 바이트가 올바르면 성공한다`() {
        val jpegBytes = jpegMagic() + ByteArray(100)
        `when`(imageStoragePort.baseFolder()).thenReturn("toilet-images")
        `when`(imageStoragePort.upload(any(), any(), any())).thenReturn("https://cdn.example.com/img.webp")
        `when`(imageConversionPort.toWebP(any(), any(), any())).thenReturn(ByteArray(50))

        val result = imageService.upload(jpegBytes, "image/jpeg")

        assertThat(result.url).isEqualTo("https://cdn.example.com/img.webp")
        assertThat(result.originalUrl).isEqualTo("https://cdn.example.com/img.webp")
    }

    @Test
    fun `upload - PNG 매직 바이트가 올바르면 성공한다`() {
        val pngBytes = pngMagic() + ByteArray(100)
        `when`(imageStoragePort.baseFolder()).thenReturn("toilet-images")
        `when`(imageStoragePort.upload(any(), any(), any())).thenReturn("https://cdn.example.com/img.webp")
        `when`(imageConversionPort.toWebP(any(), any(), any())).thenReturn(ByteArray(50))

        val result = imageService.upload(pngBytes, "image/png")

        assertThat(result.url).isNotBlank()
    }

    @Test
    fun `upload - 허용되지 않은 contentType은 IMAGE_INVALID_TYPE 예외를 던진다`() {
        assertThatThrownBy { imageService.upload(ByteArray(10), "image/gif") }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.IMAGE_INVALID_TYPE)
    }

    @Test
    fun `upload - 10MB 초과 파일은 IMAGE_TOO_LARGE 예외를 던진다`() {
        val oversized = ByteArray(10 * 1024 * 1024 + 1)
        assertThatThrownBy { imageService.upload(oversized, "image/jpeg") }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.IMAGE_TOO_LARGE)
    }

    @Test
    fun `upload - JPEG contentType이지만 PNG 매직 바이트면 IMAGE_INVALID_TYPE 예외를 던진다`() {
        val pngBytes = pngMagic() + ByteArray(100)
        assertThatThrownBy { imageService.upload(pngBytes, "image/jpeg") }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.IMAGE_INVALID_TYPE)
    }

    // ──────────────────────────────────────────
    // validateUrls
    // ──────────────────────────────────────────

    @Test
    fun `validateUrls - 모든 URL이 자체 도메인이면 통과한다`() {
        `when`(imageStoragePort.isOwnUrl(any())).thenReturn(true)

        imageService.validateUrls(listOf("https://cdn.example.com/img1.webp", "https://cdn.example.com/img2.webp"))
    }

    @Test
    fun `validateUrls - 외부 URL이 포함되면 IMAGE_INVALID_URL 예외를 던진다`() {
        `when`(imageStoragePort.isOwnUrl("https://cdn.example.com/img.webp")).thenReturn(true)
        `when`(imageStoragePort.isOwnUrl("https://evil.com/malware.exe")).thenReturn(false)

        assertThatThrownBy {
            imageService.validateUrls(listOf("https://cdn.example.com/img.webp", "https://evil.com/malware.exe"))
        }
            .isInstanceOf(BusinessException::class.java)
            .extracting { (it as BusinessException).errorCode }
            .isEqualTo(ErrorCode.IMAGE_INVALID_URL)
    }

    // ──────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────

    private fun jpegMagic() = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())
    private fun pngMagic() = byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte())
}
