@file:Suppress("unused")

package org.http4k.example

import dev.forkhandles.result4k.onFailure
import org.http4k.aws.AwsCredentials
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.Environment.Companion.ENV
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.connect.amazon.containercredentials.ContainerCredentials
import org.http4k.connect.amazon.containercredentials.Http
import org.http4k.connect.amazon.containercredentials.action.getCredentials
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.s3.Http
import org.http4k.connect.amazon.s3.S3Bucket
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.connect.storage.S3
import org.http4k.connect.storage.Storage
import org.http4k.connect.storage.asHttpHandler
import org.http4k.contract.security.BasicAuthSecurity
import org.http4k.core.Credentials
import org.http4k.lens.string
import org.http4k.lens.uri
import org.http4k.serverless.ApiGatewayV1LambdaFunction

data class Entry(val value: String)

private fun s3Storage(): Storage<Entry> {
    val environment = ENV
    val bucketName = EnvironmentKey.string().map(BucketName.Companion::of).required("BUCKET")(environment)
    val region = EnvironmentKey.string().map(Region.Companion::of).required("AWS_REGION")(environment)
    val credentialsUri = EnvironmentKey.uri().required("AWS_CONTAINER_CREDENTIALS_FULL_URI")(environment)
    return Storage.S3(
        S3Bucket.Http(bucketName, region,
            {
                ContainerCredentials.Http().getCredentials(credentialsUri)
                    .onFailure { error("Could not get credentials ${it.reason}") }.asHttp4k()
            })
    )
}

private fun security() = BasicAuthSecurity(
    "http4k-storage-example",
    Credentials("http4k", EnvironmentKey.string().required("API_TOKEN")(ENV))
)

class StorageFunction : ApiGatewayV1LambdaFunction(s3Storage().asHttpHandler(storageSecurity = security()))