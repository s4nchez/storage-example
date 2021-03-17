@file:Suppress("unused")

package org.http4k.example

import org.http4k.connect.storage.InMemory
import org.http4k.connect.storage.Storage
import org.http4k.connect.storage.asHttpHandler
import org.http4k.serverless.ApiGatewayV1LambdaFunction

class StorageFunction : ApiGatewayV1LambdaFunction(Storage.InMemory<String>().asHttpHandler())