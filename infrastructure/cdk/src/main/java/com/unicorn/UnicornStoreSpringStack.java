package com.unicorn;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.apigateway.LambdaRestApi;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.events.EventBus;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.*;
import software.constructs.Construct;

import java.util.List;
import java.util.Map;

public class UnicornStoreSpringStack extends Stack {

    public UnicornStoreSpringStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        var eventBridge = createEventBus();
        var unicornStoreSpringLambda = createUnicornLambdaFunction();

        //Permission for Spring Boot Lambda Function
        eventBridge.grantPutEventsTo(unicornStoreSpringLambda);

        //Setup a Proxy-Rest API to access the Spring Lambda function
        var restApi = setupRestApi(unicornStoreSpringLambda);

        //Create output values for later reference
        new CfnOutput(this, "unicorn-store-spring-function-arn", CfnOutputProps.builder()
                .value(unicornStoreSpringLambda.getFunctionArn())
                .build());

        new CfnOutput(this, "ApiEndpointSpring", CfnOutputProps.builder()
                .value(restApi.getUrl())
                .build());
    }

    private RestApi setupRestApi(Function unicornStoreSpringLambda) {
        return LambdaRestApi.Builder.create(this, "UnicornStoreSpringApi")
                .restApiName("UnicornStoreSpringApi")
                .handler(unicornStoreSpringLambda)
                .build();
    }

    private ILayerVersion getWebAdapterLayer(){
        return LayerVersion.fromLayerVersionArn(this, "WebAdapterLayer", String.format("arn:aws:lambda:%s:753240598075:layer:LambdaAdapterLayerX86:7", Stack.of(this).getRegion()));
    }

    private Function createUnicornLambdaFunction() {
        return Function.Builder.create(this, "UnicornStoreSpringFunction")
                .runtime(Runtime.PROVIDED_AL2)
                .functionName("unicorn-store-spring-boot-3")
                .memorySize(2048)
                .handler("lambda-web-adapter-runner.sh")
                .layers(List.of(getWebAdapterLayer()))
                .timeout(Duration.seconds(29))
                .code(Code.fromAsset("../../spring-custom-runtime.zip"))
                .environment(Map.of(
                        "RUST_LOG", "info",
                        "READINESS_CHECK_PATH", "/actuator/health",
                        "REMOVE_BASE_PATH", "/v1",
                        "MANAGEMENT_HEALTH_DISKSPACE_PATH", "/tmp"

                ))
                .build();
    }

    private EventBus createEventBus() {
        return EventBus.Builder.create(this, "UnicornEventBus")
                .eventBusName("unicorns-spring")
                .build();
    }

}
