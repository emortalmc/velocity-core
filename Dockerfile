FROM eclipse-temurin:25-jre-alpine

# Download packages
RUN apk add --no-cache wget

# We manually set the Velocity version to avoid bugs
ENV VELOCITY_JAR_URL "https://fill-data.papermc.io/v1/objects/ef1a852bfae7397e84907837925e7ad21c6312066290edaae401b77f6f423ac3/velocity-3.4.0-SNAPSHOT-558.jar"

RUN mkdir /app
WORKDIR /app

# Download the Velocity jar
RUN wget -O velocity.jar $VELOCITY_JAR_URL
#COPY run/velocity*.jar velocity.jar
COPY run/velocity.toml .
COPY run/server-icon.png .

# Move Core Plugin, install third-party plugins
RUN mkdir /app/plugins
WORKDIR /app/plugins
COPY build/libs/*-all.jar velocity-core.jar

# Go back to the base directory for our server
WORKDIR /app

ENTRYPOINT ["java"]
CMD ["-jar", "/app/velocity.jar"]
