@file:Suppress("unused")

package org.http4k.example

import org.http4k.aws.AwsCredentials
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.connect.amazon.model.BucketName
import org.http4k.connect.amazon.model.Region
import org.http4k.connect.amazon.s3.Http
import org.http4k.connect.amazon.s3.S3Bucket
import org.http4k.connect.storage.S3
import org.http4k.connect.storage.Storage
import org.http4k.connect.storage.asHttpHandler
import org.http4k.lens.string
import org.http4k.serverless.ApiGatewayV1LambdaFunction

data class Entry(val value: String)

private fun s3Storage(): Storage<Entry> {
    val environment = Environment.ENV
    val bucketName = EnvironmentKey.string().map(BucketName.Companion::of).required("BUCKET")(environment)
    val region = EnvironmentKey.string().map(Region.Companion::of).required("AWS_REGION")(environment)
    val credentials = AwsCredentials(
        accessKey = EnvironmentKey.string().required("AWS_ACCESS_KEY_ID")(environment),
        secretKey = EnvironmentKey.string().required("AWS_SECRET_ACCESS_KEY")(environment),
        sessionToken = EnvironmentKey.string().optional("AWS_SESSION_TOKEN")(environment)
    )
    return Storage.S3(S3Bucket.Http(bucketName, region, { credentials }))
}

class StorageFunction : ApiGatewayV1LambdaFunction(s3Storage().asHttpHandler())