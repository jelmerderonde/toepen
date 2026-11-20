FROM node:latest AS npm
COPY package.json package.json
COPY package-lock.json package-lock.json
RUN npm install

FROM clojure:temurin-22-tools-deps-jammy as java
RUN mkdir /app
WORKDIR /app
ADD deps.edn deps.edn
RUN clj -Sdeps '{:mvn/local-repo "./.m2/repository"}' -e "(prn \"Downloading deps\")"
RUN clojure -P -A:build:shadow-cljs
COPY . .
COPY --from=npm node_modules node_modules
RUN clojure -A:shadow-cljs release app
RUN clojure -T:build uberjar
RUN javac --release 21 HealthCheck.java

FROM gcr.io/distroless/java21-debian12:nonroot
COPY --from=java /app/target/toepen.jar /toepen.jar
COPY --from=java /app/HealthCheck.class /HealthCheck.class
EXPOSE 8080
CMD ["-Xmx180m", "-jar", "/toepen.jar"]
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=2 CMD ["java", "-cp", "/", "HealthCheck"]