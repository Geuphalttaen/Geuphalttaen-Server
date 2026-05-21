package com.geuphalttaen.infra.r2

import com.geuphalttaen.domain.image.ImageStoragePort
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.net.URI

@Component
@EnableConfigurationProperties(CloudflareR2Properties::class)
class CloudflareR2Client(
    private val properties: CloudflareR2Properties,
) : ImageStoragePort {

    @PostConstruct
    fun validate() {
        require(properties.accountId.isNotBlank()) { "cloudflare.r2.account-id must not be blank" }
        require(properties.accessKeyId.isNotBlank()) { "cloudflare.r2.access-key-id must not be blank" }
        require(properties.secretAccessKey.isNotBlank()) { "cloudflare.r2.secret-access-key must not be blank" }
        require(properties.bucketName.isNotBlank()) { "cloudflare.r2.bucket-name must not be blank" }
        require(properties.publicUrl.isNotBlank()) { "cloudflare.r2.public-url must not be blank" }
    }

    private val s3ClientLazy = lazy {
        S3Client.builder()
            .endpointOverride(URI.create("https://${properties.accountId}.r2.cloudflarestorage.com"))
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.accessKeyId, properties.secretAccessKey),
                ),
            )
            .region(Region.of("auto"))
            .build()
    }
    private val s3Client: S3Client by s3ClientLazy

    override fun upload(objectKey: String, contentType: String, data: ByteArray): String {
        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(properties.bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build(),
            RequestBody.fromBytes(data),
        )
        return "${properties.publicUrl.trimEnd('/')}/$objectKey"
    }

    override fun isOwnUrl(url: String): Boolean {
        val base = properties.publicUrl.trimEnd('/') + "/"
        return url.startsWith(base)
    }

    @PreDestroy
    fun close() {
        if (s3ClientLazy.isInitialized()) s3Client.close()
    }
}
