@file:Suppress("unused")

package org.http4k.example

import dev.forkhandles.result4k.onFailure
import org.http4k.cloudnative.env.Environment.Companion.ENV
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.connect.amazon.containercredentials.*
import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.s3.Http
import org.http4k.connect.amazon.s3.S3Bucket
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.connect.storage.S3
import org.http4k.connect.storage.Storage
import org.http4k.connect.storage.asHttpHandler
import org.http4k.contract.security.BasicAuthSecurity
import org.http4k.core.*
import org.http4k.lens.string
import org.http4k.lens.uri
import org.http4k.serverless.ApiGatewayV1LambdaFunction

data class Entry(val value: String)

class StorageFunction : ApiGatewayV1LambdaFunction(s3Storage().asHttpHandler(storageSecurity = security()))

private fun s3Storage(): Storage<Entry> {
    val environment = ENV
    val bucketName = EnvironmentKey.string().map(BucketName.Companion::of).required("BUCKET")(environment)
    val region = EnvironmentKey.string().map(Region.Companion::of).required("AWS_REGION")(environment)
    val credentialsUri = EnvironmentKey.uri().required("AWS_CONTAINER_CREDENTIALS_FULL_URI")(environment)
    val token = EnvironmentKey.required("AWS_CONTAINER_AUTHORIZATION_TOKEN")(environment)

    val containerCredentials = ContainerCredentials.Http(token = ContainerCredentialsAuthToken(token))
    val credentials = containerCredentials.getCredentials(credentialsUri)
        .onFailure { error("Could not get credentials ${it.reason}, full URI = $credentialsUri") }
        .asHttp4k()

    return Storage.S3(S3Bucket.Http(bucketName, region, { credentials }))
}

private fun security() = BasicAuthSecurity(
    "http4k-storage-example",
    Credentials("http4k", EnvironmentKey.string().required("API_TOKEN")(ENV))
)
