FROM openjdk:13-alpine
WORKDIR /app
COPY . .
RUN chmod +x gradlew
RUN ./gradlew build --console=plain --info

FROM openjdk:13-alpine
WORKDIR /app
ARG ARTEFACT_NAME=cf-ddns
COPY --from=0 /app/build/libs/${ARTEFACT_NAME}.jar .
CMD java -Xms64m -Xmx128m -jar ${ARTEFACT_NAME}.jar
