package com.myorg;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetProductsById implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private static final DynamoDB dynamoDB = new DynamoDB(client);
    private static final Table productsTable = dynamoDB.getTable("products");
    private static final Table stocksTable = dynamoDB.getTable("stocks");
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        context.getLogger().log("CUSTOM MESSAGE Lambda execution started get Product by id...\n");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setHeaders(getCorsHeaders());
        String productId = event.getPathParameters().get("productId");

        Item result = productsTable.getItem("id", productId);
        Product product = null;
        if (result != null) {
            int price = result.getInt("price");
            String title = result.getString("title");
            String description = result.getString("description");
            var stockItem = stocksTable.getItem("product_id", productId);
            Integer count = null;
            if (stockItem != null) {
                count = stockItem.getInt("count");
            }
            product = new Product(productId, count, price, title, description);
        } else {
            response.setStatusCode(404);
            response.setBody("{\"message\":\"Product Not Found\"}");
        }
        try {
            String responseBody = objectMapper.writeValueAsString(product);
            response.setBody(responseBody);
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody("{\"message\":\"Internal server error\"}1111" + e.getMessage());
        }

        return response;
    }

    private Map<String, String> getCorsHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Access-Control-Allow-Origin", "*"); // Разрешает все домены
        headers.put("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS"); // Разрешенные методы
        headers.put("Access-Control-Allow-Headers", "Content-Type, Authorization");
        return headers;
    }
}
