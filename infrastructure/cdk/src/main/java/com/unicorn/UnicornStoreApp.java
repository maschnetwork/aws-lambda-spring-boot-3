package com.unicorn;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class UnicornStoreApp {

    public static void main(final String[] args) {
        App app = new App();

        new UnicornStoreSpringStack(app, "UnicornStoreSpringStack", StackProps.builder()
                .build());

        app.synth();
    }
}
