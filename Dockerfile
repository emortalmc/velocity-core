FROM --platform=$TARGETPLATFORM azul/zulu-openjdk:21-jre

# Download packages
RUN apt-get update && apt-get install -y wget

# We manually set the Velocity version to avoid bugs
ENV VELOCITY_JAR_URL "https://api.papermc.io/v2/projects/velocity/versions/3.3.0-SNAPSHOT/builds/398/downloads/velocity-3.3.0-SNAPSHOT-398.jar"
#ENV VIA_VERSION_JAR_URL "https://github.com/ViaVersion/ViaVersion/releases/download/4.9.2/ViaVersion-4.9.2.jar"

RUN mkdir /app
WORKDIR /app

# Download the Velocity jar
RUN wget -O velocity.jar $VELOCITY_JAR_URL
COPY run/velocity.toml .
COPY run/server-icon.png .

# Move Core Plugin, install third-party plugins
RUN mkdir /app/plugins
WORKDIR /app/plugins
COPY build/libs/*-all.jar velocity-core.jar

RUN #wget -O viaversion.jar $VIA_VERSION_JAR_URL
#COPY run/plugins/viaversion/config.yml viaversion/config.yml

RUN #wget -O viabackwards.jar https://github.com/ViaVersion/ViaBackwards/releases/download/4.9.1/ViaBackwards-4.9.1.jar

# Go back to the base directory for our server
WORKDIR /app

ENTRYPOINT ["java"]
CMD ["-Dlog4j2.debug", "-jar", "/app/velocity.jar"]
