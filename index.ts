import * as pulumi from "@pulumi/pulumi";
import * as aws from "@pulumi/aws";

// Create an AWS resource (S3 Bucket)
const bucket = new aws.s3.Bucket("http4k-storage-example");

// Export the name of the bucket
export const bucketName = bucket.id;

const defaultRole = new aws.iam.Role("http4k-storage-example-lambda-default-role", {assumeRolePolicy: `{
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
`});

const storageFunction = new aws.lambda.Function("http4k-storage-example-lambda", {
    code: new pulumi.asset.FileArchive("build/distributions/storage-examples-1.0-SNAPSHOT.zip"),
    handler: "org.http4k.example.StorageFunction",
    role: defaultRole.arn,
    runtime: "java11",
});

const apiGatewayPermission = new aws.lambda.Permission("http4k-storage-example-lambda-gateway-permission", {
    action: "lambda:InvokeFunction",
    "function": storageFunction.name,
    principal: "apigateway.amazonaws.com"
});