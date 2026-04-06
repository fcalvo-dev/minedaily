# MineDaily — Contrato de API (Documento 3)

Basado en:

- `docs/01-product-rules-v3-3-lives.md`
- `docs/02-domain-model-v2-3-lives.md`

## 1. Propósito

Este documento define un **contrato de API conceptual para el MVP** de MineDaily.

Su objetivo es cerrar las decisiones necesarias para que backend y frontend trabajen sobre una misma interfaz, sin entrar todavía en detalles de implementación fina como controladores concretos, entidades JPA o estructura final de paquetes.

Este documento sí toma decisiones que en los documentos anteriores quedaron abiertas, especialmente sobre:

- estilo general de la API
- representación del challenge vigente
- representación de sesiones y tablero visible
- manejo de acciones del jugador
- contratos públicos vs contratos internos/dev

---

## 2. Decisiones de API que este documento deja cerradas

## 2.1 La API del juego será server-authoritative por acción

Para el MVP, MineDaily adopta un enfoque **server-authoritative**.

Eso significa que:

- el backend conoce el tablero oficial real
- el frontend no conoce la seed ni la ubicación completa de minas
- el frontend no resuelve el resultado real de una jugada por sí solo
- cada acción de juego relevante se envía al backend
- el backend responde con el nuevo estado visible de la sesión

Esta decisión está alineada con las reglas de producto y simplifica:

- autoridad de negocio
- integridad del challenge compartido
- validación de vidas y errores
- consistencia del leaderboard
- anti-cheat básico del MVP

---

## 2.2 `challengeDate` queda cerrada en la API

En la API pública, `challengeDate` se define como:

> la fecha local de Córdoba correspondiente al instante de inicio de la ventana oficial del challenge.

Formato:

- `YYYY-MM-DD`

Ejemplo:

- si un challenge empieza el `2026-04-06T21:00:00-03:00`
- y termina el `2026-04-07T20:59:59.999-03:00`
- entonces su `challengeDate` es `2026-04-06`

Esto implica que el challenge vigente durante gran parte del día calendario siguiente puede seguir teniendo la `challengeDate` del día anterior.

Por eso el frontend **no debe inferir la vigencia por la fecha calendario local del cliente**, sino por los campos temporales devueltos por el backend.

---

## 2.3 La sesión puede persistir en backend y rehidratarse

Para el MVP, una sesión `IN_PROGRESS` puede persistirse y ser rehidratada.

Esto permite:

- refresh del navegador
- reconexión
- continuidad de una primera sesión en curso

A nivel de contrato, el frontend podrá consultar si existe una sesión activa del usuario para el challenge vigente.

---

## 2.4 Existe una sola sesión activa por usuario y challenge

Para un mismo usuario y challenge vigente:

- puede existir **como máximo una** sesión `IN_PROGRESS`
- el backend nunca debe crear una segunda sesión activa en paralelo
- si el usuario intenta iniciar sesión y ya existe una activa, la API debe devolver esa misma sesión

Esta regla simplifica el contrato, evita duplicados por refresh o doble submit y reduce ambigüedad para frontend.

---

## 2.5 `clickCount` queda definido

Para el MVP, `clickCount` se define como la cantidad de acciones que **cambian válidamente el estado visible** de la sesión.

Cuenta como click:

- un `reveal` sobre una celda oculta y operable
- un `toggle-flag` sobre una celda oculta y operable

No cuenta como click:

- requests inválidos
- acciones rechazadas por conflicto de estado
- clicks sobre una sesión ya finalizada
- acciones sobre celdas que no cambian el estado visible

Ejemplos concretos:

- `reveal` sobre celda ya revelada: no suma click
- `reveal` sobre celda flagged: operación inválida, no suma click
- `toggle-flag` sobre celda revelada: operación inválida, no suma click

No existe `chord` en el MVP.

---

## 2.6 Hay reveal en cascada y cuenta como un solo click

Si el usuario revela una celda segura con `adjacentMineCount = 0`, el backend debe aplicar la apertura automática de la zona conectada según la lógica clásica de Buscaminas.

Para el contrato del MVP:

- toda la cascada se procesa como resultado de una sola acción `reveal`
- toda la cascada cuenta como **un único click**
- el backend responde con el snapshot visible final después de aplicar toda la expansión

---

## 2.7 La API solo expone estado visible del tablero

La API nunca debe exponer:

- `internalSeed`
- layout completo autoritativo de minas
- detalles internos del generador

La API solo puede exponer:

- configuración pública del board
- estado visible de celdas según lo que el usuario ya reveló o marcó
- minas que hayan sido reveladas legítimamente por el propio curso de la sesión
- minas expuestas por la regla de cierre visual al perder

---

## 2.8 La condición de victoria queda cerrada

Una sesión se gana cuando **todas las celdas que no contienen minas** quedaron reveladas.

Aclaración importante para el sistema de 3 vidas:

- haber pisado una o más minas no impide ganar
- mientras el usuario conserve al menos 1 vida y revele todas las safe cells, la sesión termina en `WON`

---

## 2.9 Al perder, se revelan todas las minas y no las safe cells ocultas

Cuando la sesión termina en `LOST`:

- el backend debe revelar visualmente todas las minas del tablero en el snapshot final
- las safe cells que el usuario nunca reveló deben permanecer ocultas
- la sesión queda cerrada y no admite nuevas acciones

Esta decisión simplifica la UX del MVP y evita exponer más información de la necesaria.

---

## 2.10 Las sesiones posteriores existen, pero no puntúan

Después de que la primera sesión del usuario para un challenge termina:

- el usuario puede iniciar nuevas sesiones sobre el mismo challenge
- esas sesiones son recreativas / de práctica
- no son elegibles para leaderboard

Para el MVP, esas sesiones reutilizan las mismas reglas básicas de juego, incluyendo las 3 vidas.

---

## 2.11 El leaderboard vigente será público y limitado

Para el MVP:

- `GET /api/challenges/current/leaderboard` será público
- devolverá el **top 50** del challenge vigente
- no tendrá paginación en esta primera versión

Esto mantiene el producto simple y suficiente para el challenge diario inicial.

---

## 3. Convenciones generales

## 3.1 Base path

API pública:

- `/api/**`

API interna/dev:

- `/internal/dev/**`

---

## 3.2 Formato

- JSON para request y response
- UTF-8
- timestamps en ISO-8601 con offset
- timezone oficial del negocio: `America/Argentina/Cordoba`

---

## 3.3 Identificadores

Los IDs públicos (`challengeId`, `sessionId`) deben tratarse como **identificadores opacos**.

El contrato no obliga a UUID ni a un formato particular, aunque puede usarse UUID en implementación.

---

## 3.4 Autenticación

Para el MVP:

- `/api/health` es público
- endpoints de challenge público y leaderboard son públicos
- endpoints de estado del usuario y sesiones requieren usuario autenticado

La mecánica exacta de autenticación puede cerrarse más adelante, pero el contrato asume que:

- existe un usuario autenticado
- el backend obtiene ese usuario del contexto de seguridad
- la API no recibe `userId` en body ni query para operaciones normales de juego

---

## 3.5 Errores HTTP

Convención sugerida:

- `200 OK` para lecturas y acciones exitosas
- `201 Created` para creación real de una nueva sesión
- `204 No Content` cuando no existe sesión activa
- `400 Bad Request` para request mal formado
- `401 Unauthorized` cuando falta autenticación
- `403 Forbidden` cuando el usuario no puede acceder a un recurso
- `404 Not Found` cuando el recurso no existe
- `409 Conflict` para conflictos de negocio o estado
- `422 Unprocessable Entity` para coordenadas válidas sintácticamente pero inválidas para la operación

---

## 3.6 Error payload sugerido

```json
{
  "timestamp": "2026-04-06T10:30:12-03:00",
  "status": 409,
  "code": "SESSION_ALREADY_FINISHED",
  "message": "La sesión ya terminó y no admite nuevas acciones.",
  "path": "/api/sessions/ses_123/actions/reveal"
}
```

El shape exacto puede ajustarse luego, pero conviene mantener:

- código técnico estable
- mensaje entendible
- path
- status HTTP alineado

---

## 4. Read models / DTOs públicos

## 4.1 `CurrentChallengeView`

Representa el challenge vigente desde el punto de vista público.

```json
{
  "challengeId": "ch_2026-04-06",
  "challengeDate": "2026-04-06",
  "timezone": "America/Argentina/Cordoba",
  "windowStartAt": "2026-04-06T21:00:00-03:00",
  "windowEndAt": "2026-04-07T20:59:59.999-03:00",
  "rolloverAt": "2026-04-07T21:00:00-03:00",
  "board": {
    "rows": 10,
    "cols": 10,
    "mineCount": 18
  }
}
```

### Notas

- `rolloverAt` indica el próximo cambio de challenge
- `windowStartAt` y `windowEndAt` permiten al frontend evitar asumir lógica de medianoche
- `board` solo contiene configuración pública

---

## 4.2 `CurrentUserChallengeStatusView`

Representa el estado derivado del usuario respecto del challenge vigente.

```json
{
  "challengeId": "ch_2026-04-06",
  "challengeDate": "2026-04-06",
  "state": "IN_PROGRESS",
  "canStillScoreToday": true,
  "hasActiveSession": true,
  "activeSessionId": "ses_123",
  "firstSession": {
    "exists": true,
    "status": "IN_PROGRESS",
    "isLeaderboardEligible": true,
    "remainingLives": 2,
    "errorCount": 1,
    "clickCount": 17,
    "durationMs": 48231
  }
}
```

### `state` sugeridos

- `NOT_PLAYED`
- `IN_PROGRESS`
- `FIRST_SESSION_WON`
- `FIRST_SESSION_LOST`

### Notas

- el estado está enfocado en la **primera sesión** del usuario para el challenge vigente
- `canStillScoreToday` solo será `true` mientras no exista una primera sesión finalizada
- si luego el usuario juega sesiones recreativas, este view sigue representando la situación competitiva del día
- este endpoint existe para resolver CTA y estado general, no para rehidratar el tablero

---

## 4.3 `CellView`

Representa una celda desde la perspectiva visible del usuario.

```json
{
  "row": 3,
  "col": 5,
  "state": "REVEALED_SAFE",
  "adjacentMineCount": 2
}
```

### `state` del MVP

- `HIDDEN`
- `FLAGGED`
- `REVEALED_SAFE`
- `REVEALED_MINE`

### Notas

- `adjacentMineCount` solo tiene valor en `REVEALED_SAFE`
- `REVEALED_MINE` representa una mina ya pisada y visualmente visible en esa sesión o una mina revelada por cierre visual al perder
- la API no necesita exponer safe cells ocultas ni datos internos del board real

---

## 4.4 `BoardSnapshotView`

Representa el tablero visible actual de una sesión.

```json
{
  "rows": 10,
  "cols": 10,
  "cells": [
    [{ "row": 0, "col": 0, "state": "HIDDEN", "adjacentMineCount": null }],
    [{ "row": 1, "col": 0, "state": "FLAGGED", "adjacentMineCount": null }]
  ]
}
```

### Decisión para el MVP

Para simplificar frontend y contrato, el backend devuelve un **snapshot completo visible** del tablero de la sesión.

Más adelante, si hace falta optimizar, puede evolucionarse a un modelo de deltas.

---

## 4.5 `GameSessionView`

Representa una sesión completa del usuario.

```json
{
  "sessionId": "ses_123",
  "challengeId": "ch_2026-04-06",
  "challengeDate": "2026-04-06",
  "status": "IN_PROGRESS",
  "isFirstAttempt": true,
  "isLeaderboardEligible": true,
  "startedAt": "2026-04-07T11:12:43-03:00",
  "endedAt": null,
  "durationMs": 48231,
  "lives": {
    "maxLives": 3,
    "remainingLives": 2
  },
  "performance": {
    "errorCount": 1,
    "clickCount": 17
  },
  "board": {
    "rows": 10,
    "cols": 10,
    "cells": []
  }
}
```

### Reglas contractuales

- `status` puede ser `IN_PROGRESS`, `WON`, `LOST`
- `isLeaderboardEligible` depende solo del backend
- `durationMs` en sesión en curso se calcula con reloj del servidor
- `endedAt` solo se informa al finalizar
- este view es el contrato de rehidratación completa de una sesión

---

## 4.6 `SessionActionResponse`

Representa la respuesta a una acción del jugador.

```json
{
  "action": {
    "type": "REVEAL",
    "row": 3,
    "col": 5,
    "result": "MINE_HIT"
  },
  "session": {
    "sessionId": "ses_123",
    "challengeId": "ch_2026-04-06",
    "challengeDate": "2026-04-06",
    "status": "IN_PROGRESS",
    "isFirstAttempt": true,
    "isLeaderboardEligible": true,
    "startedAt": "2026-04-07T11:12:43-03:00",
    "endedAt": null,
    "durationMs": 48231,
    "lives": {
      "maxLives": 3,
      "remainingLives": 2
    },
    "performance": {
      "errorCount": 1,
      "clickCount": 17
    },
    "board": {
      "rows": 10,
      "cols": 10,
      "cells": []
    }
  }
}
```

### `action.result` sugeridos

Para `REVEAL`:

- `SAFE_REVEAL`
- `SAFE_REVEAL_CASCADE`
- `MINE_HIT`
- `SESSION_WON`
- `SESSION_LOST`

Para `TOGGLE_FLAG`:

- `FLAG_ADDED`
- `FLAG_REMOVED`

---

## 4.7 `LeaderboardEntryView`

```json
{
  "position": 1,
  "user": {
    "displayName": "Fran"
  },
  "durationMs": 58342,
  "errorCount": 0,
  "clickCount": 41,
  "remainingLives": 3
}
```

### Notas

- el leaderboard solo incluye sesiones `WON` y elegibles
- `remainingLives` es un dato derivado útil para UX, aunque el ranking usa `errorCount`
- `displayName` es el identificador público visible del usuario en ranking y debe ser estable para ese usuario en la experiencia pública del MVP
- el orden devuelto por la API es la fuente de verdad del ranking visible

---

## 4.8 `DailyLeaderboardView`

```json
{
  "challengeId": "ch_2026-04-06",
  "challengeDate": "2026-04-06",
  "entries": []
}
```

### Notas

- para el MVP devuelve como máximo 50 entradas
- no incluye paginación en esta primera versión

---

## 5. Endpoints públicos del MVP

## 5.1 Health

### `GET /api/health`

Estado técnico básico del backend.

### Response

```json
{
  "status": "UP"
}
```

---

## 5.2 Obtener challenge vigente

### `GET /api/challenges/current`

Devuelve la definición pública del challenge vigente.

### Response `200 OK`

Body: `CurrentChallengeView`

### Reglas

- si el challenge vigente aún no existe persistido, el backend puede provisionarlo en forma lazy antes de responder
- esa provisión es interna; no cambia el contrato público

---

## 5.3 Obtener leaderboard del challenge vigente

### `GET /api/challenges/current/leaderboard`

Devuelve el leaderboard diario del challenge vigente.

### Response `200 OK`

Body: `DailyLeaderboardView`

### Reglas de orden

La API devuelve las entradas ya ordenadas por:

1. menor `durationMs`
2. menor `errorCount`
3. menor `clickCount`
4. `endedAt` más temprano
5. `sessionId` ascendente como desempate técnico final

El orden devuelto por la API es autoritativo.

### Reglas de exposición

- el endpoint es público
- devuelve como máximo 50 entradas
- no tiene paginación en el MVP

---

## 5.4 Obtener estado del usuario para el challenge vigente

### `GET /api/challenges/current/status`

Requiere autenticación.

Devuelve el estado competitivo del usuario frente al challenge vigente.

### Response `200 OK`

Body: `CurrentUserChallengeStatusView`

### Regla de uso

Este endpoint existe para que el frontend decida el estado general de la pantalla y los CTAs disponibles.

No reemplaza a la consulta de sesión activa cuando hace falta rehidratar el tablero.

---

## 5.5 Obtener sesión activa del usuario para el challenge vigente

### `GET /api/challenges/current/sessions/active`

Requiere autenticación.

Devuelve la sesión `IN_PROGRESS` del usuario para el challenge vigente, si existe.

### Response `200 OK`

Body: `GameSessionView`

### Response `204 No Content`

Cuando no existe sesión activa para el challenge vigente.

### Regla

Este endpoint existe para rehidratar la partida completa, incluyendo snapshot visible del board, vidas y performance.

---

## 5.6 Iniciar una nueva sesión para el challenge vigente

### `POST /api/challenges/current/sessions`

Requiere autenticación.

Inicia una nueva sesión para el challenge vigente.

### Response

- `201 Created` si se creó una nueva sesión
- `200 OK` si ya existía una sesión activa y el backend devuelve esa misma sesión

Body: `GameSessionView`

### Reglas de negocio

- si existe una sesión `IN_PROGRESS` del usuario para el challenge vigente, el backend debe devolver **esa misma sesión** y no crear otra
- si no existe ninguna sesión previa del usuario para ese challenge, la nueva sesión será:
  - `isFirstAttempt = true`
  - `isLeaderboardEligible = true`
  - `remainingLives = 3`
- si ya existe una primera sesión finalizada, la nueva sesión será recreativa:
  - `isFirstAttempt = false`
  - `isLeaderboardEligible = false`
  - `remainingLives = 3`

### Motivo de esta decisión

Esto evita duplicar sesiones activas por refresh, retry o doble submit del frontend.

---

## 5.7 Obtener una sesión por id

### `GET /api/sessions/{sessionId}`

Requiere autenticación.

Devuelve una sesión del usuario autenticado.

### Response `200 OK`

Body: `GameSessionView`

### Errores típicos

- `404` si no existe
- `403` si la sesión no pertenece al usuario autenticado

---

## 5.8 Revelar celda

### `POST /api/sessions/{sessionId}/actions/reveal`

Requiere autenticación.

### Request

```json
{
  "row": 3,
  "col": 5
}
```

### Response `200 OK`

Body: `SessionActionResponse`

### Reglas de negocio

- la sesión debe existir y pertenecer al usuario autenticado
- la sesión debe estar en `IN_PROGRESS`
- la coordenada debe estar dentro del board
- solo puede revelarse una celda oculta y no flagged
- si la celda ya estaba revelada, la operación es inválida
- si la celda estaba flagged, la operación es inválida
- si la celda contiene mina:
  - se revela visualmente
  - `remainingLives` decrementa en 1
  - `errorCount` incrementa en 1
  - la sesión continúa si quedan vidas
  - la sesión termina en `LOST` si las vidas llegan a 0
  - si la sesión termina en `LOST`, el snapshot final debe revelar todas las minas
- si la celda es segura y tiene `adjacentMineCount = 0`, el backend aplica reveal en cascada
- toda acción `reveal` aplicada válidamente incrementa `clickCount` una sola vez, incluso si hubo cascada
- si después del reveal todas las safe cells quedaron reveladas, la sesión termina en `WON`

### Regla de retries / idempotencia del MVP

Si una request duplicada llega cuando la celda ya no es operable porque la primera ya fue aplicada, la segunda debe fallar como operación inválida y no producir cambios adicionales.

### Errores típicos

- `404` sesión inexistente
- `403` sesión ajena
- `409` sesión finalizada
- `422` coordenada fuera de rango o celda no operable para `reveal`

---

## 5.9 Marcar o desmarcar bandera

### `POST /api/sessions/{sessionId}/actions/toggle-flag`

Requiere autenticación.

### Request

```json
{
  "row": 3,
  "col": 5
}
```

### Response `200 OK`

Body: `SessionActionResponse`

### Reglas de negocio

- la sesión debe existir y pertenecer al usuario autenticado
- la sesión debe estar en `IN_PROGRESS`
- la coordenada debe estar dentro del board
- solo puede togglearse una celda oculta
- no puede togglearse una celda ya revelada
- si la acción cambia efectivamente el estado de la celda, incrementa `clickCount`
- la bandera no modifica `errorCount`
- la bandera no modifica `remainingLives`

### Regla de retries / idempotencia del MVP

Si una request duplicada llega cuando la celda ya no es operable bajo la misma precondición esperada, la segunda no debe producir efectos adicionales.

### Errores típicos

- `404` sesión inexistente
- `403` sesión ajena
- `409` sesión finalizada
- `422` celda fuera de rango o no operable para flag

---

## 6. Contratos internos / dev

Estos endpoints no forman parte de la API pública normal del producto.

Deben estar protegidos por perfil, rol o configuración explícita de entorno.

---

## 6.1 Provisionar challenge manualmente

### `POST /internal/dev/challenges/provision`

Permite generar o asegurar la existencia de un challenge en forma manual para desarrollo o debugging.

### Request sugerido

```json
{
  "challengeDate": "2026-04-06"
}
```

`challengeDate` puede ser opcional. Si no se envía, el backend usa la fecha lógica vigente según el reloj de negocio.

### Response `200 OK`

```json
{
  "challengeId": "ch_2026-04-06",
  "challengeDate": "2026-04-06",
  "created": true
}
```

### Reglas

- debe ser idempotente para la misma `challengeDate`
- si el challenge ya existe, puede devolverse con `created = false`
- no debe exponer `internalSeed`

---

## 6.2 Observación sobre generación automática

Aunque exista este endpoint interno/dev, la provisión normal del challenge oficial sigue pudiendo ocurrir por:

- job programado
- fallback lazy desde API pública

El endpoint manual es solo una herramienta operativa.

---

## 7. Reglas de negocio reflejadas en el contrato

## 7.1 Challenge vigente

- el challenge vigente se resuelve con reloj del backend
- la timezone oficial es `America/Argentina/Cordoba`
- el rollover ocurre a las `21:00:00`
- el frontend no debe resolver vigencia por su cuenta

---

## 7.2 Leaderboard

- solo entran sesiones `WON`
- solo entra la primera sesión elegible del usuario para ese challenge
- las sesiones recreativas nunca aparecen
- el orden visible es autoritativo según la API
- el criterio de orden es `durationMs`, luego `errorCount`, luego `clickCount`
- si persiste empate, se usa `endedAt` y luego `sessionId` como desempate técnico determinístico

---

## 7.3 Vidas y errores

- toda sesión empieza con `maxLives = 3`
- una mina pisada revela esa mina, descuenta una vida y suma un error
- `errorCount = 1 mina pisada = 1 vida perdida`
- la sesión pierde solo cuando `remainingLives = 0`
- una sesión puede ganarse habiendo cometido errores, siempre que todas las safe cells hayan sido reveladas antes de quedarse sin vidas

---

## 7.4 Exposición del tablero

- el frontend nunca recibe el tablero completo autoritativo
- el backend solo devuelve el estado visible de la sesión
- una mina puede aparecer en la respuesta si fue pisada legítimamente o si la sesión cerró en derrota y se aplica la revelación final de minas
- al perder no se revelan automáticamente safe cells no descubiertas

---

## 7.5 Operabilidad de celdas

- `reveal` solo opera sobre celdas ocultas no flagged
- `toggle-flag` solo opera sobre celdas ocultas no reveladas
- una acción inválida no debe cambiar el board ni los contadores de performance
- una request duplicada que llegue tarde no debe producir efectos adicionales

---

## 8. Flujo recomendado de frontend para el MVP

## 8.1 Carga inicial

1. `GET /api/challenges/current`
2. `GET /api/challenges/current/status`
3. si `hasActiveSession = true`, entonces `GET /api/challenges/current/sessions/active`
4. si no hay sesión activa, el usuario puede iniciar una nueva con `POST /api/challenges/current/sessions`

### Notas

- `status` existe para decidir estado competitivo y CTAs
- `sessions/active` existe para rehidratar la partida completa
- el frontend no necesita pedir ambos en loop; el uso recomendado es secuencial según necesidad

---

## 8.2 Jugar una partida

1. frontend muestra el snapshot actual del board
2. usuario hace `reveal` o `toggle-flag`
3. frontend envía la acción al backend
4. backend devuelve `SessionActionResponse`
5. frontend re-renderiza con el snapshot devuelto
6. si `status` cambia a `WON` o `LOST`, la sesión queda cerrada

---

## 8.3 Volver a jugar tras terminar la primera sesión

1. el estado del usuario ya no permite seguir puntuando
2. el frontend aún puede ofrecer “jugar otra vez”
3. eso llama nuevamente a `POST /api/challenges/current/sessions`
4. el backend crea una sesión no elegible para leaderboard

---

## 9. Decisiones explícitamente fuera de alcance del contrato actual

Este documento no fija todavía:

- shape exacto OpenAPI final
- nombres finales de packages o controllers
- estrategia exacta de autenticación
- optimización por deltas de board en vez de snapshot completo
- replay de moves
- websockets o actualizaciones en tiempo real
- histórico de leaderboards por challenge pasado

---

## 10. Recomendaciones de implementación derivadas del contrato

## Recomendación 1

Modelar los endpoints de juego sobre `GameSession`, no sobre “board suelto”.

## Recomendación 2

Mantener el frontend lo más tonto posible respecto a verdad de negocio:

- no calcula vidas
- no decide victoria o derrota
- no decide elegibilidad
- no conoce minas ocultas

## Recomendación 3

Encapsular la lógica de jugadas en una capa clara de aplicación/dominio para que el contrato HTTP no contamine las reglas.

## Recomendación 4

Mantener el endpoint interno de provisionamiento fuera del circuito público y fuera de Swagger principal si eso ayuda a no mezclar API del producto con herramientas de desarrollo.

---

## 11. Resumen ejecutivo

El contrato de API del MVP de MineDaily queda definido sobre un modelo **server-authoritative**.

La API pública permite:

- consultar el challenge vigente
- consultar el leaderboard vigente
- consultar el estado competitivo del usuario
- iniciar una sesión
- recuperar una sesión activa
- ejecutar acciones de juego sobre una sesión

El backend sigue siendo la fuente de verdad para:

- challenge vigente
- tablero real
- vidas
- errores
- resultado de sesión
- elegibilidad para leaderboard

El frontend recibe únicamente:

- información pública del challenge
- estado derivado del usuario
- snapshots visibles del tablero de una sesión

Esta decisión mantiene coherencia con el producto: un único challenge diario compartido, tablero real privado, primera sesión elegible, sistema de 3 vidas y ranking justo sobre el mismo escenario para todos.
