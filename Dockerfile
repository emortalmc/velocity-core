FROM eclipse-temurin:21-jre-alpin

# Download packages
RUN apk add --no-cache wget

# We manually set the Velocity version to avoid bugs
ENV VELOCITY_JAR_URL "https://fill-data.papermc.io/v1/objects/61249fa5b9b33bc7e3223581eab6aedad790a295caf0e39da2ff3c8ec9d9117f/velocity-3.4.0-SNAPSHOT-523.jar"

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
