FROM openjdk:13-alpine
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew build --console=plain --info

FROM openjdk:13-alpine
WORKDIR /app
COPY --from=0 /app/build/libs/cf-ddns.jar .
CMD java -Xms64m -Xmx128m -jar cf-ddns.jar
