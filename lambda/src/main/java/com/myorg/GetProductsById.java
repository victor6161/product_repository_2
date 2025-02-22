package com.myorg;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GetProductsById implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    public static final List<Product> PRODUCTS = Arrays.asList(
            new Product(1, "Product 1", 100.0),
            new Product(2, "Product 2", 150.0),
            new Product(3, "Product 3", 200.0)
    );

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.setStatusCode(200);
        response.setHeaders(Map.of("Content-Type", "application/json"));
        int productId = Integer.parseInt(event.getPathParameters().get("productId"));
        var productResult = PRODUCTS
                .stream()
                .filter(product ->
                        product.id() == productId).findFirst();
        try {
            String responseBody = OBJECT_MAPPER.writeValueAsString(productResult.get());
            response.setBody(responseBody);
        } catch (Exception e) {
            response.setStatusCode(500);
            response.setBody("{\"message\":\"Internal server error\"}1111" + e.getMessage());
        }

        return response;
    }
}
