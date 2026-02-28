# Todo API - Wirespec + Spring Boot Native

A contract-first REST API built with [Wirespec](https://github.com/flock-community/wirespec) and Spring Boot, compiled to a GraalVM native image.

## Overview

This project demonstrates how to use Wirespec for contract-first API development with Spring Boot and GraalVM Native Image support. The API contract is defined in a `.ws` file, and all Java types and handler interfaces are generated automatically during the build.

## API

Defined in [`src/main/wirespec/todo.ws`](src/main/wirespec/todo.ws):

| Method | Path              | Description       |
|--------|-------------------|-------------------|
| GET    | /api/todos        | List all todos    |
| GET    | /api/todos/{id}   | Get todo by ID    |
| POST   | /api/todos        | Create a todo     |
| PUT    | /api/todos/{id}   | Update a todo     |
| DELETE | /api/todos/{id}   | Delete a todo     |

## Prerequisites

- Java 21
- Docker (for building the native container image)

## Build & Run

### Native container image

```sh
./mvnw spring-boot:build-image -Pnative
docker run --rm -p 8080:8080 todo:0.0.1-SNAPSHOT
```

### Native executable (requires GraalVM)

```sh
./mvnw native:compile -Pnative
target/todo
```

### JVM mode

```sh
./mvnw spring-boot:run
```

## Tech Stack

- Spring Boot 3.5
- Wirespec (contract-first code generation)
- GraalVM Native Image
- Java 21
