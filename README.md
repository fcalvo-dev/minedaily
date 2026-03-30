# MineDaily

MineDaily es una aplicación web de **Buscaminas diario** donde todos los usuarios juegan el mismo tablero del día.

La idea combina la lógica clásica de Buscaminas con el formato de desafío diario:

- un tablero único por día
- mismo tablero para todos
- leaderboard diario
- estadísticas personales
- enfoque web full-stack

## Objetivo del proyecto

Este proyecto nace con dos objetivos principales:

1. construir un producto web real, prolijo y escalable
2. usarlo como proyecto de aprendizaje y portfolio

También busca servir como práctica de tecnologías modernas de backend y frontend.

## Estado actual

Actualmente el proyecto ya cuenta con:

- backend creado con Spring Boot
- conexión a PostgreSQL
- migraciones con Flyway
- configuración inicial de Spring Security
- endpoint de health check
- base del proyecto versionada con Git

## Endpoint disponible

GET /api/health

Respuesta esperada:

{
  "status": "ok"
}

## Idea del producto

MineDaily es un juego web donde cada día se publica un tablero de Buscaminas compartido por todos los jugadores.

### Reglas base planteadas

- cada día hay un tablero nuevo
- todos los jugadores juegan el mismo tablero
- solo la **primera partida del día** cuenta para el leaderboard
- si el usuario vuelve a jugar ese día, puede hacerlo, pero ya no puntúa
- el ranking diario se ordena por:
  1. menor tiempo
  2. menos clics
  3. menos errores

## Stack tecnológico

### Backend
- Java 21
- Spring Boot
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- Maven

### Frontend
- React
- TypeScript
- Vite

### Infraestructura
- Docker
- Docker Compose

## Estructura del proyecto

minedaily/
├─ backend/
├─ frontend/
├─ docs/
├─ docker-compose.yml
└─ README.md

## Cómo levantar el proyecto localmente

### 1. Levantar PostgreSQL

docker compose up -d

### 2. Ejecutar el backend

En Windows PowerShell:

cd backend
.\mvnw spring-boot:run

### 3. Probar el endpoint de health

http://localhost:8080/api/health

## Configuración actual de base de datos

La aplicación usa PostgreSQL local con Docker.

Valores actuales de desarrollo:

- database: minedaily
- user: minedaily
- password: minedaily

> Esta configuración es solo para desarrollo local.

## Roadmap inicial

### Fase 1
- [x] bootstrap del backend
- [x] conexión a PostgreSQL
- [x] migración inicial con Flyway
- [x] endpoint /api/health

### Fase 2
- [ ] bootstrap del frontend con React
- [ ] layout base de la aplicación
- [ ] integración frontend-backend

### Fase 3
- [ ] modelo de usuario
- [ ] autenticación
- [ ] tablero diario
- [ ] persistencia de partidas

### Fase 4
- [ ] leaderboard diario
- [ ] estadísticas personales
- [ ] reglas de puntaje del juego

## Convenciones de trabajo

- main se usa como rama estable
- las nuevas features se desarrollan en ramas separadas
- los commits intentan ser pequeños, claros y descriptivos

Ejemplos:

- chore: bootstrap backend with Spring Boot and PostgreSQL
- feat: add health check endpoint
- feat: bootstrap React frontend

## Próximos pasos

Los próximos pasos del proyecto son:

1. crear el frontend con React + Vite
2. definir el modelo inicial del juego
3. construir la lógica del tablero diario
4. empezar a conectar backend y frontend

## Autor

Francisco Calvo
GitHub: fcalvo-dev