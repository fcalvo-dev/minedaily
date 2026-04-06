# MineDaily — Plan de Entrega (Documento 4)

Basado en:

- `docs/01-product-rules.md`
- `docs/02-domain-model.md`
- `docs/03-api-contract.md`

## 1. Propósito

Este documento define un **plan de entrega liviano** para el MVP de MineDaily.

No busca reemplazar un backlog detallado ni una herramienta de gestión. Su objetivo es ordenar el trabajo en una secuencia razonable para que el equipo pueda avanzar con foco, evitando abrir demasiados frentes a la vez y reduciendo retrabajo entre backend y frontend.

El plan prioriza:

- slices verticales funcionales
- cierres de arquitectura antes del detalle de implementación
- validación temprana de las reglas de producto más sensibles
- un orden de entrega que permita probar el flujo real del juego cuanto antes

---

## 2. Principios de ejecución

## 2.1 Construir de adentro hacia afuera

El orden recomendado es:

1. reglas de dominio y persistencia
2. casos de uso y API
3. integración frontend
4. endurecimiento técnico

Esto evita que el frontend quede apoyado sobre contratos inestables o comportamientos todavía no cerrados.

---

## 2.2 Priorizar slices verticales por valor

No conviene implementar "toda la base de datos", luego "todo el backend" y recién al final "algo jugable".

Conviene entregar por slices funcionales:

- challenge vigente
- sesión activa
- jugada real
- cierre de partida
- leaderboard

Cada slice debe dejar al sistema en un estado más usable y testeable que el anterior.

---

## 2.3 El backend sigue siendo la fuente de verdad

Todas las etapas del plan deben preservar esta decisión ya cerrada:

- el backend resuelve el challenge vigente
- el backend conoce el tablero autoritativo
- el backend procesa acciones del jugador
- el frontend consume estado visible y contratos estables

---

## 2.4 Reducir decisiones técnicas irreversibles al principio

En las primeras etapas conviene evitar optimizaciones prematuras como:

- websocket o tiempo real
- deltas de tablero en vez de snapshot completo
- replay completo de movimientos
- anti-cheat avanzado
- rankings históricos complejos

El objetivo del MVP es que el daily challenge funcione bien, no maximizar sofisticación técnica desde el día uno.

---

## 2.5 Cada etapa debe tener salida verificable

Cada etapa del plan debe terminar con algo que pueda comprobarse.

Ejemplos:

- endpoint funcional con contrato estable
- flujo jugable extremo a extremo
- test de integración relevante
- pantalla del frontend usable
- generación reproducible verificada

---

## 3. Alcance del MVP que este plan asume

Este plan asume como alcance del MVP:

- challenge diario vigente
- generación determinística e idempotente
- backend como fuente de verdad
- primera sesión elegible con 3 vidas
- sesiones posteriores recreativas
- tablero visible por snapshot
- acciones `reveal` y `toggle-flag`
- leaderboard público del challenge vigente
- estado del usuario respecto del challenge actual

Quedan fuera de este plan base:

- ranking histórico complejo
- replay de movimientos
- challenges pasados navegables
- múltiples modos de juego
- amigos, ligas o features sociales
- tiempo real multiusuario

---

## 4. Estrategia general de entrega

La estrategia recomendada es dividir el trabajo en **6 etapas**.

1. Fundaciones del challenge diario
2. Ciclo de vida de sesión
3. Gameplay autoritativo
4. Leaderboard
5. Integración frontend MVP
6. Hardening técnico

La lógica de esta secuencia es simple:

- primero hay que saber **qué challenge existe**
- luego hay que poder **abrir y recuperar una sesión**
- después hay que poder **jugar de verdad**
- recién entonces tiene sentido exponer **ranking**
- una vez estable el flujo principal, se pule frontend y robustez

---

## 5. Etapa 0 — Base ya existente

A efectos del plan, se parte del siguiente baseline ya disponible:

- backend levantando
- frontend levantando
- conexión básica frontend-backend
- `/api/health`
- Swagger/OpenAPI funcionando
- PostgreSQL por Docker
- repo en GitHub y workflow local operativo

Esto permite empezar directamente en la capa de producto/dominio sin tener que rehacer bootstrap del proyecto.

---

## 6. Etapa 1 — Fundaciones del challenge diario

## Objetivo

Tener resuelto el challenge vigente de forma determinística, reproducible e idempotente.

## Qué debería quedar listo

### Backend / dominio

- resolver `challengeDate` según `America/Argentina/Cordoba`
- resolver ventana oficial del challenge
- modelar `DailyChallenge`
- modelar la `ChallengeBoardDefinition` interna
- implementar `BoardGenerator`
- implementar provisión idempotente por `challengeDate`
- soportar generación automática y fallback lazy generation

### Persistencia

Definir y crear la persistencia mínima para challenges.

Como mínimo debe poder guardarse:

- `challengeId`
- `challengeDate`
- ventana temporal
- timezone
- rows
- cols
- mineCount
- `internalSeed`
- `generatorVersion`
- timestamps de creación

### API

Dejar funcionales:

- `GET /api/challenges/current`
- `POST /internal/dev/challenges/provision`

### Frontend

Pantalla mínima capaz de:

- consultar challenge vigente
- mostrar fecha lógica, rollover y configuración pública del board
- validar que el flujo básico de lectura funciona

## Criterios de salida

La etapa se considera cerrada cuando:

- dos requests para el mismo `challengeDate` no generan dos challenges distintos
- el challenge vigente cambia correctamente según la regla de las 21:00 hs de Córdoba
- el endpoint público devuelve el challenge correcto y consistente
- el endpoint interno/dev permite forzar provisión para desarrollo

## Riesgo principal

Cerrar mal esta etapa genera inconsistencias en todas las siguientes. Por eso debe resolverse antes de tocar sesiones de juego.

---

## 7. Etapa 2 — Ciclo de vida de sesión

## Objetivo

Poder iniciar, recuperar y consultar la sesión del usuario para el challenge vigente.

## Qué debería quedar listo

### Backend / dominio

- modelar `GameSession`
- soportar primera sesión elegible y sesiones posteriores recreativas
- soportar una sola sesión `IN_PROGRESS` por usuario y challenge
- resolver si la sesión es primera, elegible y/o activa
- calcular el `CurrentUserChallengeStatus`

### Persistencia

Definir y crear la persistencia mínima para sesiones.

Como mínimo debe poder guardarse:

- `sessionId`
- `userId`
- `challengeId`
- `attemptNumber`
- `status`
- `isFirstAttempt`
- `isLeaderboardEligible`
- `startedAt`
- `endedAt`
- `maxLives`
- `remainingLives`
- `errorCount`
- `clickCount`

Además, debe quedar resuelta la estrategia para persistir el estado visible de la sesión.

Esa estrategia puede implementarse de distintas formas, pero el documento recomienda elegir una sola para el MVP y no dejarla ambigua.

Opciones razonables:

- snapshot serializado completo
- modelo persistido por celda visible
- estructura interna equivalente mientras respete el contrato de API

No hace falta optimizar todavía. Hace falta que sea correcta y simple.

### API

Dejar funcionales:

- `GET /api/challenges/current/status`
- `GET /api/challenges/current/sessions/active`
- `POST /api/challenges/current/sessions`
- `GET /api/sessions/{sessionId}`

### Frontend

Pantalla/flujo mínimo capaz de:

- detectar si el usuario ya tiene una sesión activa
- crear o recuperar la sesión correcta
- rehidratar el board visible
- mostrar vidas, errores y clicks

## Criterios de salida

La etapa se considera cerrada cuando:

- crear sesión devuelve la activa si ya existe una `IN_PROGRESS`
- el usuario puede refrescar el navegador y recuperar exactamente su sesión
- el estado competitivo del usuario es consistente con su primera sesión
- no se pueden abrir dos sesiones activas paralelas para el mismo challenge

## Riesgo principal

Definir tarde la persistencia del estado visible del board complica mucho gameplay y frontend. Conviene decidirlo en esta etapa.

---

## 8. Etapa 3 — Gameplay autoritativo

## Objetivo

Hacer jugable MineDaily extremo a extremo con la mecánica de 3 vidas.

## Qué debería quedar listo

### Backend / dominio

- procesar `reveal`
- procesar `toggle-flag`
- aplicar reveal en cascada
- descontar vida al pisar mina
- registrar `errorCount`
- registrar `clickCount`
- detectar `WON`
- detectar `LOST`
- cerrar sesión cuando corresponda
- devolver snapshot visible actualizado después de cada acción

### Reglas que deben validarse en esta etapa

- una mina pisada consume 1 vida y suma 1 error
- la sesión sigue mientras queden vidas y existan safe cells sin revelar
- se gana al revelar todas las safe cells
- se pierde al llegar a 0 vidas
- al perder se revelan todas las minas y no las safe cells ocultas
- el reveal en cascada cuenta como 1 click
- acciones inválidas no cambian el estado visible ni el conteo de clicks

### API

Dejar funcionales:

- `POST /api/sessions/{sessionId}/actions/reveal`
- `POST /api/sessions/{sessionId}/actions/toggle-flag`

### Frontend

Pantalla jugable del tablero con:

- render de grid
- reveal de celdas
- toggle de flags
- actualización de snapshot completo
- indicador de vidas restantes
- indicador de errores y clicks
- estados finales de victoria/derrota

## Criterios de salida

La etapa se considera cerrada cuando:

- un usuario puede jugar una sesión completa desde inicio hasta cierre
- los resultados observables coinciden con las reglas del Documento 1 y el contrato del Documento 3
- refresh o reconexión no rompe el progreso de una sesión en curso
- una sesión finalizada ya no admite nuevas acciones

## Riesgo principal

Esta es la etapa con más lógica de negocio. Conviene cubrirla con tests de integración antes de avanzar al leaderboard.

---

## 9. Etapa 4 — Leaderboard

## Objetivo

Publicar el ranking diario del challenge vigente en base a la primera sesión ganada y elegible de cada usuario.

## Qué debería quedar listo

### Backend / dominio

- derivar elegibilidad competitiva desde `GameSession`
- incluir solo sesiones `WON` y elegibles
- ordenar por:
  1. menor `durationMs`
  2. menor `errorCount`
  3. menor `clickCount`
  4. desempate técnico determinístico
- exponer `remainingLives` como dato derivado útil para UX

### API

Dejar funcional:

- `GET /api/challenges/current/leaderboard`

Con decisión ya cerrada para MVP:

- público
- top 50
- sin paginación

### Frontend

Pantalla/listado del ranking diario con:

- posición
- display name
- tiempo
- errores
- clicks
- vidas restantes

## Criterios de salida

La etapa se considera cerrada cuando:

- una victoria elegible aparece correctamente en ranking
- una derrota o sesión no elegible no aparece
- el orden del ranking respeta exactamente los criterios del Documento 3
- el frontend muestra el top 50 correctamente

## Riesgo principal

Si la elegibilidad o el orden se calculan fuera del backend, el ranking pierde integridad. Todo cálculo oficial debe quedar del lado servidor.

---

## 10. Etapa 5 — Integración frontend MVP

## Objetivo

Pulir la experiencia mínima usable del producto sobre contratos ya estables.

## Qué debería quedar listo

### Flujos de usuario

- ver el challenge vigente
- iniciar o retomar una sesión
- jugar hasta victoria o derrota
- ver su estado competitivo del día
- consultar el leaderboard diario

### UX mínima recomendada

- pantalla home/simple entrypoint
- módulo del challenge actual
- CTA clara para jugar o retomar
- timer derivado del servidor mostrado de manera consistente
- tablero visible claro
- feedback visual de pérdida de vida
- feedback final de victoria/derrota
- acceso simple al leaderboard

### Manejo de estados

- loading
- empty state cuando no hay sesión activa
- error state razonable
- recuperación de refresh
- sesión finalizada correctamente reflejada

## Criterios de salida

La etapa se considera cerrada cuando:

- un usuario puede completar todo el journey del MVP sin herramientas manuales
- frontend y backend operan sobre los contratos cerrados sin lógica duplicada innecesaria
- la UX ya permite demo funcional del producto

---

## 11. Etapa 6 — Hardening técnico

## Objetivo

Dar robustez suficiente al MVP para que no sea solo una demo frágil.

## Qué debería quedar listo

### Backend

- tests unitarios en reglas puras relevantes
- tests de integración para casos críticos
- manejo consistente de errores HTTP
- validaciones de conflictos de estado
- logging útil en generación, creación de sesión y acciones
- protección razonable contra doble submit o retries simples

### Base de datos

- constraints alineados al dominio
- índices mínimos razonables
- unicidad por challenge lógico
- protección para evitar más de una sesión activa por usuario/challenge si la estrategia elegida lo soporta

### Frontend

- manejo consistente de errores de red
- deshabilitar acciones mientras hay request en vuelo cuando aplique
- recovery simple ante recarga o reconexión

### Operativo

- documentación básica de levantado local
- seeds/dev tools claros
- datos de prueba repetibles

## Criterios de salida

La etapa se considera cerrada cuando:

- los escenarios críticos del MVP tienen cobertura razonable
- el sistema soporta uso manual normal sin inconsistencias obvias
- el equipo puede levantar, probar y demostrar el producto con baja fricción

---

## 12. Orden recomendado dentro del backend

Si hubiera que bajar esto a un orden más concreto de implementación backend, la secuencia sugerida sería:

1. resolver challenge vigente y persistencia de `DailyChallenge`
2. provisión idempotente + endpoint interno/dev
3. contrato público `current challenge`
4. `GameSession` + persistencia de sesión
5. `status` + `active session`
6. creación/retoma de sesión
7. modelo interno del board visible de sesión
8. acciones `reveal` y `toggle-flag`
9. cierre de sesión por victoria/derrota
10. leaderboard
11. endurecimiento y tests

Esto permite validar primero la parte menos interactiva y luego montar la lógica de juego encima de una base estable.

---

## 13. Orden recomendado dentro del frontend

La secuencia sugerida para frontend sería:

1. pantalla simple de challenge vigente
2. estado del usuario actual
3. creación o recuperación de sesión
4. render del board snapshot
5. reveal y flags
6. feedback de vidas/errores/clicks
7. estados finales
8. leaderboard
9. pulido de UX y errores

Esto evita invertir tiempo de UI compleja antes de tener contratos jugables reales.

---

## 14. Paralelización posible

Aunque el plan es secuencial en lógica, hay trabajo que puede paralelizarse.

## Backend puede avanzar en paralelo con frontend cuando ya estén estables:

- `CurrentChallengeView`
- `CurrentUserChallengeStatusView`
- `GameSessionView`
- `SessionActionResponse`
- `LeaderboardEntryView`

## Frontend puede mockear temporalmente:

- challenge actual
- sesión activa
- snapshot visible del board
- leaderboard

Pero la recomendación es no sostener mocks demasiado tiempo una vez que los contratos reales existan.

---

## 15. Decisiones que deberían cerrarse antes de codificar demasiado

Aunque los documentos anteriores ya dejaron mucho bastante bien definido, conviene no avanzar demasiado sin cerrar estas decisiones técnicas concretas:

- estrategia exacta de persistencia del estado visible del board de una sesión
- mecanismo exacto de autenticación del MVP
- formato final de IDs
- shape final de errores compartido por backend
- estrategia de constraints para impedir sesiones activas duplicadas

Estas decisiones no requieren un Documento 5, pero sí una definición breve antes de profundizar implementación.

---

## 16. Criterio de “MVP terminado”

A efectos de este plan, el MVP puede considerarse terminado cuando un usuario puede:

1. abrir la app
2. ver el challenge vigente
3. iniciar o retomar su sesión del día
4. jugar en el tablero compartido con 3 vidas
5. ganar o perder con reglas consistentes
6. ver su estado del día
7. consultar el leaderboard público del challenge vigente

Y todo eso ocurre con:

- backend autoritativo
- challenge reproducible
- contratos de API consistentes
- persistencia suficiente para recuperar progreso

---

## 17. Resumen ejecutivo

El plan recomendado para MineDaily es construir el MVP en esta secuencia:

- challenge vigente
- sesión del usuario
- gameplay real
- leaderboard
- integración frontend
- hardening

La clave del orden no es técnica solamente; es de producto.

MineDaily recién empieza a existir como producto real cuando se puede:

- resolver el challenge oficial del día
- jugarlo de forma autoritativa
- cerrar una sesión válida
- comparar resultados en ranking

Todo lo demás puede evolucionar después.
