# ==== Build stage ====
FROM maven:3.9.8-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# ==== Runtime stage ====
FROM eclipse-temurin:21-jre
WORKDIR /app
# (opsional) user non-root
RUN useradd -ms /bin/bash appuser
USER appuser

# copy jar
COPY --from=build /app/target/*SNAPSHOT*.jar /app/app.jar

EXPOSE 8088
ENV JAVA_OPTS=""
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]