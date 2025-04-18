package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class ProductRepositoryApp {
    public static void main(final String[] args) {
        App app = new App();

        new ProductRepositoryStack(app, "ProductRepositoryStack", StackProps.builder().build());
        new ImportProductsStack(app, "ImportProductsStack", StackProps.builder().build());

        app.synth();
    }
}

