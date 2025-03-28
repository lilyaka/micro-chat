# syntax=docker/dockerfile:1
FROM gradle:7.6.1-jdk17-alpine AS builder
WORKDIR /app
COPY build.gradle.kts .
COPY gradle.properties .
COPY ./library ./library
COPY settings.gradle.kts.build settings.gradle.kts
RUN echo 'include("apps:chat-server")' >> settings.gradle.kts
COPY ./apps/chat-server ./apps/chat-server
RUN --mount=type=cache,id=gradle,target=/root/.gradle \
    --mount=type=cache,id=gradle,target=/home/gradle/.gradle \
    gradle :apps:chat-server:bootJar --no-daemon

FROM openjdk:17-alpine
EXPOSE 8600
WORKDIR /app
COPY --from=builder /app/apps/chat-server/build/libs/*.jar chat-server.jar
ENTRYPOINT ["java", "-jar" ,"chat-server.jar"]
