package com.myorg;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.ItemCollection;
import com.amazonaws.services.dynamodbv2.document.ScanOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class GetProductsListHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String PRODUCTS_TABLE = System.getenv("PRODUCTS_TABLE");
    private static final String STOCKS_TABLE = System.getenv("STOCKS_TABLE");

    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private static final DynamoDB dynamoDB = new DynamoDB(client);
    private static final Table productsTable = dynamoDB.getTable("products");
    private static final Table stocksTable = dynamoDB.getTable("stocks");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        context.getLogger().log("CUSTOM MESSAGE Lambda execution started...\n");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setHeaders(Map.of("Content-Type", "application/json"));
        try {
            // get all products
            ItemCollection<ScanOutcome> productsScan = productsTable.scan();
            context.getLogger().log("!!!!!!!productsTable.scan...\n");
            context.getLogger().log("!!!!!!!productsTable.scan...\n" + productsScan);
            context.getLogger().log("!!!!!!!get total count .scan...\n" + productsScan.getAccumulatedItemCount());


            List<Map<String, Object>> productList = new ArrayList<>();
            stocksTable.scan();

            ItemCollection<ScanOutcome> stocksScan = stocksTable.scan();
            context.getLogger().log("!!!!!!!stocksTable.scan...\n");
            context.getLogger().log("!!!!!!!stocksTable.scan...\n" + stocksScan);
            context.getLogger().log("!!!!!!!get total count .scan...\n" + stocksScan.getAccumulatedItemCount());

            Map<String, Integer> stockMap = new HashMap<>();

            for (Item stockItem : stocksScan) {
                String productId = stockItem.getString("product_id");
                context.getLogger().log("!!!!!!!productId...\n" + productId);
                Integer count = stockItem.getInt("count");
                context.getLogger().log("!!!!!!!count...\n" + count);

                if (productId != null) {
                    stockMap.put(productId, count);
                }
            }
            context.getLogger().log("!!!!!!!stockMap" + stockMap);

            for (Item productItem : productsScan) {
                String productId = productItem.getString("id");
                context.getLogger().log("!!!!!!!productItem" + productItem);

                // Получаем количество товара (если есть)
                int count = stockMap.getOrDefault(productId, 0);

                // Собираем продукт
                Map<String, Object> product = Map.of(
                        "id", productId,
                        "title", productItem.getString("title"),
                        "description", productItem.getString("description"),
                        "price", productItem.getInt("price"),
                        "count", count
                );
                context.getLogger().log("!!!!!!!productItem" + product);

                productList.add(product);
            }
            context.getLogger().log("!!!!!!!OBJECT_MAPPER reached...\n");
            String responseBody = OBJECT_MAPPER.writeValueAsString(productList);
            context.getLogger().log("!!!!!!!OBJECT_MAPPER finished...\n");

            response.setBody(responseBody);
        } catch (Throwable e) {
            response.setStatusCode(500);
            response.setBody("{\"message\":\"Internal server error\"}1111" + e.getMessage());
        }

        return response;
    }
}
