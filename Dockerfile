# syntax=docker/dockerfile:1.7

FROM node:latest AS npm
COPY package.json package.json
COPY package-lock.json package-lock.json
RUN --mount=type=cache,target=/root/.npm \
    npm ci

FROM clojure:temurin-17-tools-deps-focal as java
RUN mkdir /app
WORKDIR /app
COPY deps.edn deps.edn
RUN --mount=type=cache,target=/root/.m2 \
    clojure -P -A:build:shadow-cljs
COPY --from=npm /node_modules ./node_modules/
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    --mount=type=cache,target=/app/.shadow-cljs \
    clojure -A:shadow-cljs release app
RUN --mount=type=cache,target=/root/.m2 \
    clojure -T:build uberjar

FROM gcr.io/distroless/java17-debian11
COPY --from=java /app/target/toepen.jar /toepen.jar
COPY HealthCheck.java .
EXPOSE 8080
CMD ["-XX:MaxRAMPercentage=75.0", \
      "-XX:+HeapDumpOnOutOfMemoryError", \
      "-XX:HeapDumpPath=/tmp/heapdump.hprof", \
      "-XX:+ExitOnOutOfMemoryError", \
      "-Xlog:gc*:file=/tmp/gc.log:time,level,tags:filecount=5,filesize=10M", \
      "toepen.jar"]
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=2 CMD ["java", "HealthCheck.java", "||", "exit", "1"]
