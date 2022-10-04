FROM node:latest AS npm
COPY package.json package.json
COPY package-lock.json package-lock.json
RUN npm install

FROM clojure:temurin-17-tools-deps-focal as java
RUN mkdir /app
WORKDIR /app
ADD deps.edn deps.edn
RUN clj -Sdeps '{:mvn/local-repo "./.m2/repository"}' -e "(prn \"Downloading deps\")"
COPY --from=npm node_modules ./
RUN clojure -P -A:build:shadow-cljs
COPY . .
RUN clojure -A:shadow-cljs release app
RUN clojure -T:build uberjar

FROM gcr.io/distroless/java17-debian11
COPY --from=java /app/target/toepen.jar /toepen.jar
COPY HealthCheck.java .
EXPOSE 8080
CMD ["toepen.jar"]
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=2 CMD ["java", "HealthCheck.java", "||", "exit", "1"]