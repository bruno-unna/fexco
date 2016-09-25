# Extending from a ready-to-use vert.x image
FROM vertx/vertx3
MAINTAINER Bruno Unna <bruno.unna@gmail.com>

# Information about the verticle
ENV VERTICLE_NAME com.fexco.test.mockService.MockService
ENV VERTICLE_FILE target/mock-service-0.1.0.jar
ENV VERTICLE_HOME /usr/verticles

# Preferred exposed port
EXPOSE 8080

# Copy the actual verticle to the container
COPY $VERTICLE_FILE $VERTICLE_HOME/

# And launch the verticle
WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["vertx run $VERTICLE_NAME -cp $VERTICLE_HOME/*"]