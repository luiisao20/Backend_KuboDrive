FROM maven as builder
WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTest

FROM openjdk:27-ea-20-jdk-oraclelinux10 as runtime
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
CMD [ "java", "jar", "-DskipTest", "app.jar" ]