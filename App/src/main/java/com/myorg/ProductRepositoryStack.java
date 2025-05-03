package com.myorg;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.IResource;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.dynamodb.ITable;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.dynamodb.TableAttributes;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.AssetCode;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.eventsources.SqsEventSource;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.EmailSubscription;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class ProductRepositoryStack extends Stack {
    private static final AssetCode LAMBDA_JAR = Code.fromAsset("lambda/target/lambda-0.1.jar");
    private final Queue catalogItemsQueue;

    public ProductRepositoryStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        ITable productsTable = importTable("ProductsTable", "products");
        ITable stocksTable = importTable("StocksTable", "stocks");
        catalogItemsQueue = Queue.Builder.create(this, "CatalogItemsQueue")
                .queueName("catalogItemsQueue")
                .visibilityTimeout(Duration.seconds(60))
                .build();

        // Create the SNS topic
        Topic createProductTopic = Topic.Builder.create(this, "CreateProductTopic")
                .topicName("createProductTopic")
                .build();

        // Add an email subscription (replace with your own email)
        createProductTopic.addSubscription(
                EmailSubscription.Builder.create("victorkozlov120695@gmail.com").build()
        );

        Role lambdaRole = createLambdaExecutionRole();

        Function getProductsListFunction =
                createLambdaFunction("GetProductList", "GetProductList",
                        "com.myorg.GetProductsListHandler", lambdaRole).build();
        Function getProductByIdFunction =
                createLambdaFunction("GetProductListById", "GetProductById",
                        "com.myorg.GetProductsById", lambdaRole).build();
        Function createProductFunction =
                createLambdaFunction("CreateProduct", "CreateProduct",
                        "com.myorg.CreateProduct", lambdaRole).build();
        Function catalogBatchProcess =
                createLambdaFunction("CatalogBatchProcess", "CatalogBatchProcess",
                        "com.myorg.CatalogBatchProcessHandler", lambdaRole)
                        .environment(Map.of("PRODUCT_TOPIC_ARN", createProductTopic.getTopicArn()))
                        .build();

        productsTable.grantFullAccess(getProductsListFunction);
        stocksTable.grantFullAccess(getProductsListFunction);
        catalogItemsQueue.grantConsumeMessages(catalogBatchProcess);
        // Grant the Lambda permission to publish to the SNS topic
        createProductTopic.grantPublish(catalogBatchProcess);

        catalogBatchProcess.addEventSource(SqsEventSource.Builder.create(catalogItemsQueue)
                .batchSize(5)
                .build());

        RestApi api = createRestApi();
        setupApiResources(api, getProductsListFunction, createProductFunction, getProductByIdFunction);

        CfnOutput.Builder.create(this, "URL").value(api.getRoot().getPath() + "products").build();
    }

    private ITable importTable(String id, String tableName) {
        return Table.fromTableAttributes(this, id, TableAttributes.builder()
                .tableName(tableName)
                .build());
    }

    private Role createLambdaExecutionRole() {
        return Role.Builder.create(this, "LambdaExecutionRole")
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(List.of(
                        ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole"),
                        ManagedPolicy.fromAwsManagedPolicyName("AmazonDynamoDBFullAccess")
                ))
                .build();
    }

    private Function.Builder createLambdaFunction(String constructId, String functionName, String handler, Role role) {
        return Function.Builder.create(this, constructId)
                .functionName(functionName)
                .runtime(Runtime.JAVA_21)
                .code(LAMBDA_JAR)
                .handler(handler)
                .role(role)
                .timeout(Duration.seconds(46));
    }

    private RestApi createRestApi() {
        return RestApi.Builder.create(this, "ProductServiceApi")
                .description("This service serves products.")
                .restApiName("Product Service")
                .build();
    }

    private void setupApiResources(RestApi api, Function getListFn, Function createFn, Function getByIdFn) {
        IResource root = api.getRoot();
        IResource products = root.addResource("products");

        products.addMethod("GET", LambdaIntegration.Builder.create(getListFn).build());
        products.addMethod("POST", LambdaIntegration.Builder.create(createFn).build());

        products.addResource("{productId}")
                .addMethod("GET", LambdaIntegration.Builder.create(getByIdFn).build());
    }

    public Queue getCatalogItemsQueue() {
        return catalogItemsQueue;
    }
}
