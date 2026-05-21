package com.geuphalttaen.infra.r2

import com.geuphalttaen.domain.image.ImageStoragePort
import com.geuphalttaen.domain.image.PresignedUploadResult
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URI
import java.time.Duration

@Component
@EnableConfigurationProperties(CloudflareR2Properties::class)
class CloudflareR2Client(
    private val properties: CloudflareR2Properties,
) : ImageStoragePort {

    private val presigner: S3Presigner by lazy {
        S3Presigner.builder()
            .endpointOverride(URI.create("https://${properties.accountId}.r2.cloudflarestorage.com"))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.accessKeyId, properties.secretAccessKey),
                ),
            )
            .region(Region.of("auto"))
            .build()
    }

    override fun generatePresignedPutUrl(
        objectKey: String,
        contentType: String,
        expiresInSeconds: Long,
    ): PresignedUploadResult {
        val putRequest = PutObjectRequest.builder()
            .bucket(properties.bucketName)
            .key(objectKey)
            .contentType(contentType)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(expiresInSeconds))
            .putObjectRequest(putRequest)
            .build()

        val presigned: PresignedPutObjectRequest = presigner.presignPutObject(presignRequest)

        return PresignedUploadResult(
            presignedUrl = presigned.url().toString(),
            objectKey = objectKey,
            publicUrl = "${properties.publicUrl.trimEnd('/')}/$objectKey",
        )
    }
}
