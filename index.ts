import * as pulumi from "@pulumi/pulumi";
import * as aws from "@pulumi/aws";

// Create an AWS resource (S3 Bucket)
const bucket = new aws.s3.Bucket("http4k-storage-example");

// Export the name of the bucket
export const bucketName = bucket.id;

const functionPackage = new aws.s3.BucketObject("http4k-storage-lambda-package", {
    key: "deployment/storage-examples-1.0-SNAPSHOT.zip",
    bucket: bucket.id,
    source: new pulumi.asset.FileAsset("build/distributions/storage-examples-1.0-SNAPSHOT.zip")
});