package com.myorg;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.IResource;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.MethodOptions;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.AssetCode;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.IBucket;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class ImportProductsStack extends Stack {

    private static final AssetCode IMPORT_SERVICE_JAR = Code.fromAsset("import-service/target/import-service-0.1.jar");


    public ImportProductsStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Reference existing S3 bucket by name
        IBucket bucket = Bucket.fromBucketName(this, "ExistingBucket", "rsschool-task-5");

        Function importProductsFileFunction = Function.Builder.create(this, "ImportProductsFile")
                .functionName("ImportProductsFile")
                .runtime(Runtime.JAVA_21)
                .code(IMPORT_SERVICE_JAR)
                .timeout(Duration.seconds(46))
                .handler("com.myorg.ImportProductsFileHandler")
                .role(createLambdaExecutionRole())
                .build();

        // Grant Lambda permissions to interact with the bucket
        bucket.grantReadWrite(importProductsFileFunction);

        RestApi api = createRestApi();
        IResource root = api.getRoot();

        IResource importBucket = root.addResource("import");
        importBucket.addMethod("GET",
                LambdaIntegration.Builder.create(importProductsFileFunction).build(),
                MethodOptions.builder()
                        .requestParameters(Map.of(
                                "method.request.querystring.name", true // name is required
                        ))
                        .build()
        );
    }

    private RestApi createRestApi() {
        return RestApi.Builder.create(this, "ImportProductsServiceApi")
                .description("This service serves to import products.")
                .restApiName("Import products service")
                .build();
    }

    private Role createLambdaExecutionRole() {
        return Role.Builder.create(this, "LambdaExecutionRole")
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(List.of(
                        ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole")
                ))
                .build();
    }
}