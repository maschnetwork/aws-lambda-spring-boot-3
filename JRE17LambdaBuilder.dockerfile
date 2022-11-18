FROM --platform=linux/amd64 amazonlinux:2

# Update the packages and install Amazon Corretto 17 and Zip
RUN yum -y update
RUN yum install -y java-17-amazon-corretto-jmods zip tar

# Copy the pre-built jar to the image
WORKDIR /software/
COPY unicorn-store-spring/target/store-spring-1.0.0.jar store-spring-1.0.0.jar

# Find JDK module dependencies dynamically from the uber jar
RUN jdeps -q \
    --ignore-missing-deps \
    --multi-release 17 \
    --print-module-deps \
    store-spring-1.0.0.jar > jre-deps.info

# Create a slim Java 17 JRE which only contains the required modules to run the function
RUN jlink --verbose \
    --compress 2 \
    --strip-java-debug-attributes \
    --no-header-files \
    --no-man-pages \
    --output /jre17-slim \
    # Need to add additional modules as per: https://github.com/aws-samples/kubernetes-for-java-developers/issues/19
    --add-modules $(cat jre-deps.info),java.xml,jdk.unsupported,java.sql,java.naming,java.desktop,java.management,java.security.jgss,java.instrument

# Use Javas Application Class Data Sharing feature
# It creates the file /jre17-slim/lib/server/classes.jsa
RUN /jre17-slim/bin/java -Xshare:dump

# Package everything together into a custom runtime archive
WORKDIR /
COPY lambda-web-adapter-runner.sh lambda-web-adapter-runner.sh
RUN chmod 755 lambda-web-adapter-runner.sh
RUN cp /software/store-spring-1.0.0.jar spring-uber.jar
RUN zip -r spring-custom-runtime.zip \
    lambda-web-adapter-runner.sh  \
    spring-uber.jar \
    /jre17-slim
