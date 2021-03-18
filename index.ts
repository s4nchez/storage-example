import * as pulumi from "@pulumi/pulumi";
import * as aws from "@pulumi/aws";
import {RolePolicyAttachment} from "@pulumi/aws/iam";

const bucket = new aws.s3.Bucket("http4k-storage-example");

const defaultRole = new aws.iam.Role("http4k-storage-example-lambda-default-role", {
    assumeRolePolicy: `{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": "sts:AssumeRole",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Effect": "Allow",
      "Sid": ""
    }
  ]
}
`
});

new RolePolicyAttachment("http4k-storage-example-lambda-default-role-policy",
    {
        role: defaultRole,
        policyArn: aws.iam.ManagedPolicies.AWSLambdaBasicExecutionRole
    });

const storageFunction = new aws.lambda.Function("http4k-storage-example-lambda", {
    code: new pulumi.asset.FileArchive("build/distributions/storage-examples-1.0-SNAPSHOT.zip"),
    handler: "org.http4k.example.StorageFunction",
    role: defaultRole.arn,
    runtime: "java11",
    timeout: 15,
    memorySize: 512
});

const logGroupApi = new aws.cloudwatch.LogGroup("http4k-storage-example-api-route", {
    name: "http4k-storage-example",
    retentionInDays: 1
});

const apiGatewayPermission = new aws.lambda.Permission("http4k-storage-example-lambda-gateway-permission", {
    action: "lambda:InvokeFunction",
    "function": storageFunction.name,
    principal: "apigateway.amazonaws.com"
});

const storageApi = new aws.apigatewayv2.Api("http4k-storage-example-api", {
    protocolType: "HTTP"
});

const storageApiDefaultStage = new aws.apigatewayv2.Stage("default", {
    apiId: storageApi.id,
    autoDeploy: true,
    name: "$default",
    accessLogSettings: {
        destinationArn: logGroupApi.arn,
        format: `{"requestId": "$context.requestId", "requestTime": "$context.requestTime", "httpMethod": "$context.httpMethod", "httpPath": "$context.path", "status": "$context.status", "integrationError": "$context.integrationErrorMessage"}`
    }
})

const storageApiLambdaIntegration = new aws.apigatewayv2.Integration("http4k-storage-example-api-lambda-integration", {
    apiId: storageApi.id,
    integrationType: "AWS_PROXY",
    integrationUri: storageFunction.arn,
    payloadFormatVersion: "1.0"
});

const storageApiDefaultRoute = new aws.apigatewayv2.Route("http4k-storage-example-api-route", {
    apiId: storageApi.id,
    routeKey: `$default`,
    target: pulumi.interpolate`integrations/${storageApiLambdaIntegration.id}`
});

export const bucketName = bucket.id;
export const stageUri = storageApiDefaultStage.invokeUrl;