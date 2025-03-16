package com.myorg;

public record Product(
        String id,
        Integer count,
        Integer price,
        String title,
        String description
) {
}
