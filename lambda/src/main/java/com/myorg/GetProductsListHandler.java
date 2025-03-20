package com.myorg;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GetProductsListHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private static final DynamoDB dynamoDB = new DynamoDB(client);
    private static final Table productsTable = dynamoDB.getTable("products");
    private static final Table stocksTable = dynamoDB.getTable("stocks");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        context.getLogger().log("CUSTOM MESSAGE Lambda execution started...\n");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setHeaders(Map.of("Content-Type", "application/json"));
        try {
            // get all products
            ItemCollection<ScanOutcome> productsScan = productsTable.scan();
            List<Product> productList = new ArrayList<>();

            for (Item productItem : productsScan) {
                String productId = productItem.getString("id");
                var stockItem = stocksTable.getItem("product_id", productId);
                Integer count = null;
                if (stockItem != null) {
                    count = stockItem.getInt("count");
                }
                int price = productItem.getInt("price");
                String title = productItem.getString("title");
                String description = productItem.getString("description");
                productList.add(new Product(productId, count, price, title, description));
            }
            String responseBody = objectMapper.writeValueAsString(productList);

            response.setBody(responseBody);
        } catch (Exception e) {
            context.getLogger().log("!!!!!!!EXCEPTION MESSAGE\n" + e.getMessage());
            response.setStatusCode(500);
            response.setBody("{\"message\":\"Internal server error\"}1111" + e.getMessage());
        }

        return response;
    }
}
