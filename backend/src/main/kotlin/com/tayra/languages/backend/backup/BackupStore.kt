package com.tayra.languages.backend.backup

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.NoSuchBucketException
import java.io.InputStream
import java.net.URI

/** Where backup files are kept: one S3-compatible bucket, a folder per account. */
@Component
class BackupStore(
    @Value("\${tayra.storage.endpoint}") endpoint: String,
    @Value("\${tayra.storage.region}") region: String,
    @Value("\${tayra.storage.bucket}") private val bucket: String,
    @Value("\${tayra.storage.access-key}") accessKey: String,
    @Value("\${tayra.storage.secret-key}") secretKey: String,
) {
    private val s3: S3Client = S3Client.builder()
        .endpointOverride(URI(endpoint))
        .region(Region.of(region))
        .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
        // MinIO and most other S3-compatible stores address buckets by path, not by host name.
        .forcePathStyle(true)
        .build()

    @Volatile
    private var bucketReady = false

    fun put(key: String, content: InputStream, size: Long) {
        ensureBucket()
        s3.putObject({ it.bucket(bucket).key(key).contentType("application/octet-stream") }, RequestBody.fromInputStream(content, size))
    }

    fun open(key: String): InputStream = s3.getObject { it.bucket(bucket).key(key) }

    fun delete(key: String) {
        s3.deleteObject { it.bucket(bucket).key(key) }
    }

    /** Made on first use where it is missing, as on a fresh development box; production buckets exist already. */
    private fun ensureBucket() {
        if (bucketReady) return
        try {
            s3.headBucket { it.bucket(bucket) }
        } catch (e: NoSuchBucketException) {
            s3.createBucket { it.bucket(bucket) }
        }
        bucketReady = true
    }
}
