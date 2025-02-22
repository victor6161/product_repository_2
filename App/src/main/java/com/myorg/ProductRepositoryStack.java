package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.apigateway.IResource;
import software.amazon.awscdk.services.events.targets.ApiGateway;
import software.amazon.awscdk.services.lambda.AssetCode;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Code;
import software.constructs.Construct;

public class ProductRepositoryStack extends Stack {
    private static final AssetCode LAMBDA_JAR = Code.fromAsset("lambda/target/lambda-0.1.jar");

    public ProductRepositoryStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public ProductRepositoryStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Function getProductsListFunction = Function.Builder.create(this, "GetProductList")
                .functionName("GetProductList")
                .runtime(Runtime.JAVA_21)
                .code(LAMBDA_JAR)
                .handler("com.myorg.GetProductsListHandler")
                .build();

        // Define the GetProductDetailsHandler Lambda function
        Function getProductByIdFunction = Function.Builder.create(this, "GetProductListById")
                .functionName("GetProductById")
                .runtime(Runtime.JAVA_21)
                .code(LAMBDA_JAR)
                .handler("com.myorg.GetProductsById")
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
