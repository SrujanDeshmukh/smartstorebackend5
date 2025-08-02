# Use Eclipse Temurin JDK 17 (compatible with Spring Boot)
FROM eclipse-temurin:17-jdk

# Set working directory
WORKDIR /app

# Copy Maven wrapper & pom.xml first to leverage caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (helps with build caching)
RUN ./mvnw dependency:go-offline

# Copy all source code
COPY src ./src

# Build the Spring Boot application
RUN ./mvnw clean package -DskipTests

# Run the application
CMD ["java", "-jar", "target/*.jar"]
