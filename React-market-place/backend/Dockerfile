FROM eclipse-temurin:21-jdk-jammy

WORKDIR /app

COPY src/main/java ./src/main/java

RUN mkdir -p classes \
    && find src/main/java -name "*.java" -print0 | xargs -0 javac -encoding UTF-8 -d classes

ENV PORT=10000
EXPOSE 10000

CMD ["sh", "-c", "java -cp /app/classes com.example.ecommerce.EcommerceBackendApplication"]

