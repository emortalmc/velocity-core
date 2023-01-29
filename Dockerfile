FROM eclipse-temurin:17-jre-alpine

# Download packages
RUN apk add --no-cache \
    wget

# We manually set the Velocity version to avoid bugs
ENV VELOCITY_JAR_URL "https://api.papermc.io/v2/projects/velocity/versions/3.2.0-SNAPSHOT/builds/216/downloads/velocity-3.2.0-SNAPSHOT-216.jar"
ENV VIA_VERSION_JAR_URL "https://github.com/ViaVersion/ViaVersion/releases/download/4.5.1/ViaVersion-4.5.1.jar"

RUN mkdir /app
WORKDIR /app

# Download the Velocity jar
RUN wget -O velocity.jar $VELOCITY_JAR_URL
COPY run/velocity.toml .
COPY run/server-icon.png .

# Move Core Plugin
RUN mkdir /app/plugins
WORKDIR /app/plugins

RUN wget -O viaversion.jar $VIA_VERSION_JAR_URL
COPY run/plugins/viaversion/config.yml viaversion/config.yml

#RUN wget -O viabackwards.jar https://github.com/ViaVersion/ViaBackwards/releases/download/4.5.1/ViaBackwards-4.5.1.jar
COPY build/libs/*-all.jar velocity-core.jar
# Go back to the base directory for our server
WORKDIR /app
CMD ["java", "-jar", "/app/velocity.jar"]