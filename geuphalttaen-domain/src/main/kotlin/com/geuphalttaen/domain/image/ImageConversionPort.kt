package com.geuphalttaen.domain.image

interface ImageConversionPort {
    /**
     * 이미지 바이트 배열을 WebP로 변환한다.
     * @param data 원본 이미지 바이트 배열
     * @param maxDimension 긴 쪽 최대 픽셀 수 (초과 시 비율 유지 축소)
     * @param quality WebP 품질 (0–100)
     */
    fun toWebP(data: ByteArray, maxDimension: Int = 1920, quality: Int = 80): ByteArray
}
