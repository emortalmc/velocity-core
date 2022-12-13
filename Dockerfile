FROM eclipse-temurin:17-jre-alpine

# Download packages
RUN apk add --no-cache \
    wget

# We manually set the Velocity version to avoid bugs
ENV VELOCITY_JAR_URL "https://api.papermc.io/v2/projects/velocity/versions/3.1.2-SNAPSHOT/builds/200/downloads/velocity-3.1.2-SNAPSHOT-200.jar"

RUN mkdir /app
WORKDIR /app

# Download the Velocity jar
RUN wget -O velocity.jar $VELOCITY_JAR_URL
COPY run/velocity.toml .
COPY run/server-icon.png .

# Move Core Plugin
RUN mkdir /app/plugins
WORKDIR /app/plugins

COPY build/libs/*-all.jar velocity-core.jar
# Go back to the base directory for our server
WORKDIR /app
CMD ["java", "-jar", "-Xmx4G", "-Xms4G", "/app/velocity.jar"]