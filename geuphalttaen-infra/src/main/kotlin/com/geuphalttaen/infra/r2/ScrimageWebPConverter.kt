package com.geuphalttaen.infra.r2

import com.geuphalttaen.domain.image.ImageConversionPort
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.webp.WebpWriter
import org.springframework.stereotype.Component

@Component
class ScrimageWebPConverter : ImageConversionPort {

    override fun toWebP(data: ByteArray, maxDimension: Int, quality: Int): ByteArray {
        val image = ImmutableImage.loader().fromBytes(data)
        val longerSide = maxOf(image.width, image.height)
        val resized = if (longerSide > maxDimension) {
            val scale = maxDimension.toDouble() / longerSide
            image.scale(scale)
        } else {
            image
        }
        return resized.bytes(WebpWriter.DEFAULT.withQ(quality))
    }
}
