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
import software.amazon.awscdk.services.events.targets.ApiGateway;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.AssetCode;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.constructs.Construct;

import java.util.List;

public class ProductRepositoryStack extends Stack {
    private static final AssetCode LAMBDA_JAR = Code.fromAsset("lambda/target/lambda-0.1.jar");

    public ProductRepositoryStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public ProductRepositoryStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        ITable productsTable = Table.fromTableAttributes(this, "ProductsTable", TableAttributes.builder()
                .tableName("products")
                .build());

        ITable stocksTable = Table.fromTableAttributes(this, "StocksTable", TableAttributes.builder()
                .tableName("stocks")
                .build());

        Role lambdaRole = Role.Builder.create(this, "LambdaExecutionRole")
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(List.of(
                        ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole"),
                        ManagedPolicy.fromAwsManagedPolicyName("AmazonDynamoDBFullAccess")
                        ))
                .build();

        Function getProductsListFunction = Function.Builder.create(this, "GetProductList")
                .functionName("GetProductList")
                .runtime(Runtime.JAVA_21)
                .code(LAMBDA_JAR)
                .handler("com.myorg.GetProductsListHandler")
                .role(lambdaRole)
                .timeout(Duration.seconds(46))
                .build();

        // ✅ grant read db access to lambda
        productsTable.grantFullAccess(getProductsListFunction);
        stocksTable.grantFullAccess(getProductsListFunction);

        // Define the GetProductDetailsHandler Lambda function
        Function getProductByIdFunction = Function.Builder.create(this, "GetProductListById")
                .functionName("GetProductById")
                .runtime(Runtime.JAVA_21)
                .code(LAMBDA_JAR)
                .handler("com.myorg.GetProductsById")
                .role(lambdaRole)
                .timeout(Duration.seconds(46))
                .build();

        Function createProductFunction = Function.Builder.create(this, "CreateProduct")
                .functionName("CreateProduct")
                .runtime(Runtime.JAVA_21)
                .code(LAMBDA_JAR)
                .handler("com.myorg.CreateProduct")
                .role(lambdaRole)
                .timeout(Duration.seconds(46))
                .build();

        ApiGateway api = ApiGateway.Builder.create(
                        RestApi.Builder
                                .create(this, "ProductServiceApi")
                                .description("This service serves products.")
                                .restApiName("Product Service")
                                .build())
                .build();

        IResource root = api.getIRestApi().getRoot();

        IResource products = root.addResource("products");
        // get all products
        products.addMethod("GET",
                LambdaIntegration.Builder
                        .create(getProductsListFunction)
                        .build()
        );

        products.addMethod("POST",
                LambdaIntegration.Builder
                        .create(createProductFunction)
                        .build()
        );

        // get product by id
        products.addResource("{productId}")
                .addMethod("GET",
                        LambdaIntegration.Builder
                                .create(getProductByIdFunction)
                                .build()
                );

        CfnOutput.Builder.create(this, "URL").value(root.getPath() + "products").build();
    }
}
