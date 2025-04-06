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
import software.amazon.awscdk.services.lambda.eventsources.S3EventSource;
import software.amazon.awscdk.services.lambda.eventsources.S3EventSourceProps;
import software.amazon.awscdk.services.s3.*;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.Source;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class ImportProductsStack extends Stack {

    private static final AssetCode IMPORT_SERVICE_JAR = Code.fromAsset("import-service/target/import-service-0.1.jar");
    public static final String BUCKET_NAME_CDK = "rsschool-task-5-cdk";

public ImportProductsStack(final Construct scope, final String id, final StackProps props, final Queue queue) {
        super(scope, id, props);

        // 1. Create the S3 bucket
        Bucket bucket = Bucket.Builder.create(this, BUCKET_NAME_CDK)
                .bucketName(BUCKET_NAME_CDK)
                .versioned(false)
                .cors(List.of(CorsRule.builder()
                        .allowedOrigins(List.of("*"))
                        .allowedMethods(List.of(
                                HttpMethods.GET,
                                HttpMethods.PUT,
                                HttpMethods.POST
                        ))
                        .allowedHeaders(List.of("*"))
                        .build()))
                .build();

        Role role = createLambdaExecutionRole();

        Function importProductsFileFunction = Function.Builder.create(this, "ImportProductsFile")
                .functionName("ImportProductsFile")
                .runtime(Runtime.JAVA_21)
                .code(IMPORT_SERVICE_JAR)
                .timeout(Duration.seconds(46))
                .handler("com.myorg.ImportProductsFileHandler")
                .role(role)
                .build();

        // Grant Lambda permissions to interact with the bucket
        bucket.grantReadWrite(importProductsFileFunction);

        // 2. Deploy 'uploaded/.keep' into the S3 bucket
        BucketDeployment.Builder.create(this, "DeployUploadedFolder")
                .sources(List.of(Source.asset("./App/uploaded")))
                .destinationBucket(bucket)
                .destinationKeyPrefix("uploaded/")
                .build();

        Function importFileParserFunction = Function.Builder.create(this, "ImportFileParser")
                .functionName("ImportFileParser")
                .runtime(Runtime.JAVA_21)
                .code(IMPORT_SERVICE_JAR)
                .timeout(Duration.seconds(46))
                .handler("com.myorg.ImportFileParserHandler")
                .role(role)
                .build();

        bucket.grantRead(importFileParserFunction);
        queue.grantSendMessages(importFileParserFunction);

        S3EventSource s3EventSource = new S3EventSource(
                bucket,
                S3EventSourceProps.builder()
                        .events(List.of(EventType.OBJECT_CREATED))
                        .filters(List.of(NotificationKeyFilter.builder()
                                .prefix("uploaded/")
                                .build()))
                        .build()
        );
        importFileParserFunction.addEventSource(s3EventSource);

        RestApi api = createRestApi();
        IResource root = api.getRoot();

        IResource importBucket = root.addResource("import");
        importBucket.addMethod("GET",
                LambdaIntegration.Builder.create(importProductsFileFunction).build(),
                MethodOptions.builder()
                        .requestParameters(Map.of(
                                "method.request.querystring.name", true
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