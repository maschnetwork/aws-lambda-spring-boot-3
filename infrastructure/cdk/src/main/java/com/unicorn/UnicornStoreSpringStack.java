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
        // Create Version and Alias
        var liveAlias = unicornStoreSpringLambda.addAlias("live");
        // Enable SnapStart
        CfnFunction cfnFunction = (CfnFunction) unicornStoreSpringLambda.getNode().getDefaultChild();
        cfnFunction.addPropertyOverride("SnapStart", Map.of("ApplyOn", "PublishedVersions"));

        // Permission for Spring Boot Lambda Function
        eventBridge.grantPutEventsTo(unicornStoreSpringLambda);

        // Setup a Proxy-Rest API to access the Spring Lambda function
        var restApi = setupRestApi(liveAlias);

        // Create output values for later reference
        new CfnOutput(this, "unicorn-store-spring-function-arn", CfnOutputProps.builder()
                .value(unicornStoreSpringLambda.getFunctionArn())
                .build());

        new CfnOutput(this, "ApiEndpointSpring", CfnOutputProps.builder()
                .value(restApi.getUrl())
                .build());
    }

    private RestApi setupRestApi(IFunction handler) {
        return LambdaRestApi.Builder.create(this, "UnicornStoreSpringApi")
                .restApiName("UnicornStoreSpringApi")
                .handler(handler)
                .build();
    }

    private ILayerVersion getWebAdapterLayer() {
        return LayerVersion.fromLayerVersionArn(this, "WebAdapterLayer", String
                .format("arn:aws:lambda:%s:753240598075:layer:LambdaAdapterLayerX86:10", Stack.of(this).getRegion()));
    }

    private Function createUnicornLambdaFunction() {
        return Function.Builder.create(this, "UnicornStoreSpringFunction")
                .runtime(Runtime.JAVA_11)
                .functionName("unicorn-store-spring-boot-3")
                .memorySize(2048)
                .handler("lambda-web-adapter-runner.sh")
                .layers(List.of(getWebAdapterLayer()))
                .timeout(Duration.seconds(29))
                .code(Code.fromAsset("../../spring-custom-runtime.zip"))
                .environment(Map.of(
                        "AWS_LAMBDA_EXEC_WRAPPER", "/opt/bootstrap",
                        "READINESS_CHECK_PATH", "/actuator/health",
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
