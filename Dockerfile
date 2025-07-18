FROM --platform=$TARGETPLATFORM azul/zulu-openjdk:21-jre

# Download packages
RUN apt-get update && apt-get install -y wget

# We manually set the Velocity version to avoid bugs
ENV VELOCITY_JAR_URL "https://fill-data.papermc.io/v1/objects/f82780ce33035ebe3d6ea7981f0e6e8a3e41a64f2080ef5c0f1266fada03cbee/velocity-3.4.0-SNAPSHOT-522.jar"

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
