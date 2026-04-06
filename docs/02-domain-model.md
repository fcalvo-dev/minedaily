# MineDaily — Modelo de Dominio (Documento 2)

Basado en `docs/01-product-rules.md`.

## 1. Propósito

Este documento define un **modelo de dominio conceptual** para MineDaily alineado con la mecánica actual de producto:

- challenge diario oficial
- tablero único compartido
- backend como fuente de verdad
- primera partida elegible con **3 vidas**
- leaderboard diario basado en sesiones ganadas

No busca cerrar todavía clases, DTOs ni tablas definitivas. Su objetivo es establecer:

- cuáles son los conceptos principales del negocio
- qué responsabilidades tiene cada concepto
- qué invariantes deben preservarse
- qué piezas pertenecen al núcleo del dominio
- qué piezas conviene tratar como proyecciones o modelos derivados

La idea es llegar al Documento 3 de API con un lenguaje compartido y un modelo consistente.

---

## 2. Principios de modelado

El dominio de MineDaily debe respetar estas ideas:

- existe un único challenge oficial vigente según una fecha lógica de negocio
- el backend es autoritativo respecto del challenge, las vidas, el resultado y la elegibilidad
- la definición real del tablero es privada
- la generación del challenge debe ser reproducible e idempotente
- el challenge diario no cambia según el usuario
- la primera partida elegible del usuario es una **sesión completa** con hasta 3 vidas
- leaderboard y estado del usuario son, idealmente, **proyecciones derivadas** del dominio base

---

## 3. Lenguaje ubicuo inicial

### Challenge
Desafío oficial del día lógico.

### Challenge vigente
Challenge que corresponde al momento actual según la timezone oficial y la regla de rollover.

### Challenge date
Fecha lógica de negocio que identifica el challenge.

### Challenge window
Ventana temporal oficial del challenge, determinada por la timezone del producto y el rollover de las 21:00 hs de Córdoba.

### Board definition
Definición interna suficiente para reconstruir el tablero oficial.

### Internal seed
Seed privada utilizada para generación determinística.

### Game session
Partida concreta de un usuario sobre un challenge.

### First eligible session
Primera sesión registrada de un usuario para un challenge. Es la única que puede puntuar.

### Life loss
Evento de juego en el que el usuario revela una mina, pierde 1 vida y suma 1 error.

### Error
Para el MVP, 1 error equivale a 1 mina pisada y a 1 vida perdida.

### Leaderboard eligibility
Propiedad derivada de una sesión que indica si puede competir en ranking.

### Leaderboard entry
Representación visible de una sesión ganadora y elegible dentro del ranking diario.

### User challenge status
Estado derivado del usuario respecto del challenge vigente.

---

## 4. Núcleo del dominio

Para el MVP, el núcleo del dominio puede pensarse alrededor de estas piezas:

1. `DailyChallenge`
2. `ChallengeBoardDefinition`
3. `GameSession`
4. servicios de dominio que resuelven challenge vigente, generación, progreso de sesión y elegibilidad

`LeaderboardEntry` y `UserStats` existen conceptualmente, pero conviene tratarlos inicialmente como **modelos derivados / proyecciones**.

La decisión de modelado más importante es esta:

> el dominio no gira solo alrededor del tablero, sino alrededor de la relación entre un `DailyChallenge` oficial y una `GameSession` competitiva del usuario sobre ese challenge.

---

## 5. DailyChallenge

## Rol
Representa el challenge oficial de una fecha lógica determinada.

## Responsabilidades

- identificar el challenge del día lógico
- delimitar su ventana de vigencia
- referenciar su definición interna de tablero
- exponer su configuración pública permitida
- preservar unicidad por `challengeDate`
- actuar como punto de referencia oficial para sesiones y leaderboard

## Atributos conceptuales

- `challengeId`
- `challengeDate`
- `windowStartAt`
- `windowEndAt`
- `rolloverAt`
- `timezone`
- `boardPublicConfig`
- referencia o composición de `ChallengeBoardDefinition`
- metadatos de generación

## Observaciones

`DailyChallenge` no es solo “un tablero”; es el **objeto oficial de negocio** que identifica qué se juega en una ventana diaria de negocio.

La regla fuerte del dominio no es “hay muchos boards”, sino:

> hay como máximo un challenge oficial por `challengeDate`.

## Sobre `challengeDate`

Como en el Documento 1 la convención visible se dejó abierta, en dominio conviene desacoplar:

- la **identidad lógica** del challenge
- la **ventana horaria oficial** en la que ese challenge está vigente

Eso permite cerrar la convención final más adelante sin romper el corazón del modelo.

---

## 6. ChallengeBoardDefinition

## Rol
Representa la definición interna necesaria para reconstruir el tablero oficial.

## Responsabilidades

- encapsular configuración del tablero
- encapsular la información interna necesaria para reconstruir la ubicación real de minas
- asociar la generación a una versión de algoritmo
- permitir reproducibilidad, auditoría y debugging

## Contenido conceptual

- `rows`
- `cols`
- `mineCount`
- `internalSeed`
- `generatorVersion`
- parámetros internos adicionales si el algoritmo evoluciona
- opcionalmente un `boardFingerprint` técnico para verificación interna futura

## Naturaleza en el modelo

Para el MVP, `ChallengeBoardDefinition` conviene modelarla como parte interna de `DailyChallenge`, no como agregado autónomo.

Puede verse como:

- una entidad interna asociada, o
- un value object persistido

La decisión técnica puede cerrarse más adelante, pero conceptualmente sigue siendo una pieza **privada del backend**.

## Regla clave

La board definition existe para el backend. No es un contrato público hacia el frontend.

---

## 7. GameSession

## Rol
Representa una partida concreta jugada por un usuario sobre un challenge.

## Responsabilidades

- registrar que el usuario inició una partida para un challenge
- capturar inicio, progreso y cierre de la sesión
- registrar vidas, errores y métricas de performance
- almacenar el resultado final de la partida
- fijar si esa sesión es la primera partida del usuario para ese challenge
- fijar si esa sesión es elegible para leaderboard

## Atributos conceptuales

- `sessionId`
- `userId`
- `challengeId`
- `attemptNumber`
- `startedAt`
- `endedAt`
- `status` (`IN_PROGRESS`, `WON`, `LOST` para el MVP)
- `maxLives`
- `remainingLives`
- `errorCount`
- `clickCount`
- `durationMs`
- `isFirstAttempt`
- `isLeaderboardEligible`

## Regla central

Una `GameSession` es el lugar natural para capturar la regla:

> solo la primera partida del usuario para un challenge puede ser elegible para leaderboard.

## Nueva interpretación de sesión

Con la mecánica actual, una `GameSession` ya no representa una oportunidad de muerte súbita, sino una **sesión competitiva completa** con hasta 3 vidas sobre el mismo tablero oficial.

La sesión termina únicamente cuando:

- el usuario gana revelando todas las celdas seguras, o
- el usuario pierde sus 3 vidas

## Estado mínimo sugerido

Para el MVP, el estado mínimo de la sesión puede pensarse así:

- `IN_PROGRESS`: la sesión ya existe y todavía no terminó
- `WON`: el usuario completó el tablero antes de agotar sus vidas
- `LOST`: el usuario agotó sus 3 vidas

No hace falta introducir más estados mientras no se cierre una política explícita de abandono, timeout o recuperación compleja.

## Observación importante

`isLeaderboardEligible` no debería depender de lo que mande el frontend, sino de reglas calculadas por backend al crear y cerrar la sesión.

---

## 8. SessionOutcome y SessionPerformance

Aunque podrían modelarse solo como campos sueltos dentro de `GameSession`, conceptualmente conviene separar dos ideas.

## 8.1 SessionOutcome

Representa el resultado de negocio de la sesión.

### Componentes conceptuales

- `status`
- `remainingLives`
- `won` / `lost` como derivación semántica
- `finishedAt`

### Regla principal

- una sesión `WON` termina con `remainingLives` entre 1 y 3
- una sesión `LOST` termina con `remainingLives = 0`

## 8.2 SessionPerformance

Representa las métricas que se usan para ranking y análisis.

### Componentes conceptuales

- `durationMs`
- `errorCount`
- `clickCount`

### Regla principal

Para el MVP:

- `errorCount` mide minas pisadas
- `clickCount` queda pendiente de definición exacta de producto/API
- `durationMs` se usa como criterio principal de ranking

Esta separación no es obligatoria en implementación, pero ayuda a distinguir:

- resultado de negocio
- performance competitiva

---

## 9. Life model dentro de la sesión

La mecánica de 3 vidas merece una explicitación propia en el dominio.

## Regla base

Toda primera partida elegible comienza con:

- `maxLives = 3`
- `remainingLives = 3`
- `errorCount = 0`

## Al revelar una mina

Cuando el usuario revela una mina durante una sesión:

- pierde exactamente 1 vida
- suma exactamente 1 error
- la mina queda visualmente revelada
- el tablero oficial no se regenera ni se altera
- la sesión continúa si `remainingLives > 0`
- la sesión termina en `LOST` si `remainingLives = 0`

## Consecuencia de modelado

La lógica de vida/error no pertenece al frontend como autoridad. Debe estar protegida por el backend o por el modelo de dominio.

---

## 10. LeaderboardEntry

## Rol
Representa una fila visible del ranking para un challenge.

## Recomendación de modelado

Para el MVP, `LeaderboardEntry` conviene modelarla como una **proyección derivada** de `GameSession`, no como una entidad maestra independiente.

## Fuente de verdad sugerida

La verdad base sería:

- challenge oficial
- sesiones registradas
- regla de elegibilidad
- resultado final de sesión

A partir de eso, el leaderboard puede calcularse o materializarse.

## Orden conceptual

El ranking diario se construye con sesiones que cumplan:

- `isFirstAttempt = true`
- `isLeaderboardEligible = true`
- `status = WON`

Y se ordena por:

1. menor `durationMs`
2. menor `errorCount`
3. menor `clickCount`
4. desempate técnico determinístico

## Observación importante

En el modelo actual, el leaderboard no premia solo velocidad. También refleja precisión dentro del mismo tablero compartido.

---

## 11. UserStats

## Rol
Representa estadísticas agregadas de un usuario.

## Recomendación de modelado

Para el MVP, conviene verla como una **proyección futura**, no como parte del núcleo transaccional.

## Posibles métricas futuras

- challenges jugados
- challenges ganados
- win rate
- promedio de tiempo
- promedio de errores
- racha diaria
- mejor posición histórica

## Conclusión

No hace falta poner `UserStats` en el centro del modelo inicial.

---

## 12. Move

## Rol futuro
Representaría una acción puntual dentro de una partida:

- reveal cell
- toggle flag
- chord u otras acciones futuras

## Relación con el MVP

Dado que todavía no está cerrado si el backend validará jugadas una por una o solo el resultado final, `Move` debe quedar como **concepto futuro**.

## Cuándo tendría sentido modelarlo fuerte

- replay
- auditoría detallada
- validación server-authoritative por jugada
- anti-cheat más estricto
- recuperación exacta de partidas en curso

## Recomendación MVP

No incluir `Move` como entidad principal todavía.

---

## 13. Servicios de dominio principales

## 13.1 BusinessClock / CurrentChallengeResolver

### Responsabilidad
Resolver cuál es el challenge vigente según:

- hora actual
- timezone oficial
- regla de rollover a las 21:00 hs de Córdoba

### Observación
Esta lógica no debería quedar dispersa en controladores ni servicios de aplicación usando `now()` directo.

Conviene encapsularla detrás de una abstracción explícita y testeable.

---

## 13.2 ChallengeProvisioningService

### Responsabilidad
Garantizar que el challenge vigente exista.

### Debe soportar

- generación automática programada
- fallback lazy generation
- idempotencia para una misma `challengeDate`
- consistencia si múltiples procesos intentan generar el mismo challenge lógico

### Regla clave

Si dos procesos intentan generar el mismo challenge, el resultado final debe ser una única definición oficial válida.

---

## 13.3 BoardGenerator

### Responsabilidad
Construir la definición interna del tablero a partir de:

- configuración de tablero
- versionado del generador
- seed interna

### Rol en el dominio
Es un servicio puro del núcleo, útil para:

- reproducibilidad
- testing
- debugging
- evolución del algoritmo

---

## 13.4 SessionLifecycleService

### Responsabilidad
Encapsular las reglas de evolución de una `GameSession`.

### Debe poder resolver

- creación de la sesión con 3 vidas iniciales
- pérdida de vida al pisar mina
- incremento de error asociado a esa pérdida de vida
- continuidad o cierre de la sesión según vidas restantes
- determinación de victoria al completar el tablero
- cálculo final de métricas de cierre

### Motivo
La mecánica de 3 vidas es una regla central del producto y conviene que no quede repartida entre controlador, frontend y lógica ad hoc.

---

## 13.5 SessionEligibilityPolicy

### Responsabilidad
Determinar si una sesión:

- es la primera partida del usuario para un challenge
- puede puntuar
- debe quedar fuera del leaderboard

### Motivo
Encapsula una de las reglas más importantes del producto y evita que quede dispersa entre controlador, servicio y query.

---

## 13.6 LeaderboardService / LeaderboardAssembler

### Responsabilidad
Obtener y ordenar el leaderboard de un challenge.

### Observación
Puede ser un servicio de lectura o aplicación más que un servicio de dominio puro, según cómo termine implementándose.

---

## 14. Value Objects útiles

Estos objetos pueden ayudar a mantener el modelo claro.

### 14.1 BoardSize
- `rows`
- `cols`

### 14.2 MineCount
Cantidad de minas del tablero.

### 14.3 GenerationMetadata
- `internalSeed`
- `generatorVersion`
- quizá `boardFingerprint` futuro

### 14.4 ChallengeWindow
- `challengeDate`
- `startsAt`
- `endsAt`
- `timezone`

### 14.5 SessionPerformance
- `durationMs`
- `errorCount`
- `clickCount`

### 14.6 LifeState
- `maxLives`
- `remainingLives`

No es obligatorio implementarlos todos así, pero ayudan a separar conceptos y evitar primitivas sueltas por todos lados.

---

## 15. Invariantes del dominio

Estas reglas deben sostenerse siempre.

## 15.1 Challenge

- existe como máximo un `DailyChallenge` oficial por `challengeDate`
- el challenge oficial debe poder reconstruirse de forma determinística
- la generación del challenge debe ser idempotente por `challengeDate`
- todos los usuarios juegan el mismo challenge vigente

## 15.2 Privacidad y exposición

- el frontend no recibe `internalSeed`
- el frontend no recibe la disposición autoritativa completa de minas
- el frontend no decide la elegibilidad de leaderboard

## 15.3 Sesiones

- una `GameSession` pertenece a un único usuario y a un único challenge
- para un par `userId + challengeId` existe exactamente una primera sesión
- solo la primera sesión puede ser elegible para leaderboard
- una sesión perdida nunca entra al leaderboard
- una sesión no elegible nunca entra al leaderboard

## 15.4 Vidas y errores

- toda primera sesión elegible comienza con exactamente 3 vidas
- en el MVP, `0 <= remainingLives <= 3`
- cada mina pisada descuenta exactamente 1 vida
- cada mina pisada suma exactamente 1 error
- mientras la sesión esté en progreso, debe cumplirse que `errorCount = maxLives - remainingLives`
- una sesión `LOST` termina con `remainingLives = 0`
- una sesión `WON` termina con `remainingLives >= 1`

## 15.5 Leaderboard

- solo sesiones `WON` y elegibles pueden aparecer en el leaderboard
- el orden final del leaderboard debe ser determinístico
- el orden funcional del ranking es: tiempo, errores, clicks

---

## 16. Relaciones entre conceptos

## Relación principal

- un `DailyChallenge` tiene una definición interna de tablero
- un `DailyChallenge` puede tener muchas `GameSession`
- un usuario puede tener muchas `GameSession`
- el leaderboard de un challenge se deriva de sus sesiones elegibles y ganadas
- el estado del usuario frente al challenge actual se deriva de sus sesiones para ese challenge

## Relación de negocio más importante

El valor del producto nace de cruzar:

- un tablero oficial único
- una sesión competitiva del usuario con 3 vidas
- una regla fuerte de primera partida elegible

---

## 17. Qué parece agregado raíz y qué no

## Agregados candidatos

### `DailyChallenge`
Buen candidato a agregado raíz porque:

- concentra identidad oficial del challenge diario
- protege unicidad por fecha lógica
- encapsula definición interna del tablero

### `GameSession`
También parece agregado raíz porque:

- tiene ciclo de vida propio
- registra estado, vidas, errores y métricas
- puede crearse, progresar y cerrarse independientemente
- concentra la regla de elegibilidad individual del usuario

## No agregar como raíz en MVP

### `ChallengeBoardDefinition`
Mejor como parte interna del challenge.

### `LeaderboardEntry`
Mejor como proyección o vista derivada.

### `UserStats`
Mejor como agregado analítico o read model futuro.

### `Move`
Demasiado fino para el MVP si aún no se cerró validación por jugada.

---

## 18. Modelo de lectura sugerido

Aunque el dominio transaccional se apoye en challenge y sesiones, el frontend necesita vistas más cómodas.

## Read models conceptuales

### `CurrentChallengeView`
Expone solo información pública del challenge vigente.

### `CurrentUserChallengeStatusView`
Podría exponer, al menos:

- no jugó todavía
- tiene primera partida en curso
- vidas restantes de esa primera partida si está en curso
- ya ganó
- ya perdió
- ya tiene una partida que cuenta para leaderboard
- ya no puede volver a puntuar ese día

### `DailyLeaderboardView`
Lista ordenada de entradas visibles del challenge vigente.

### `CurrentSessionView` (opcional según API)
Podría servir si el MVP decide soportar continuidad de sesión en curso o rehidratación después de refresh.

Esto permite mantener una buena separación entre:

- modelo de escritura / verdad
- modelo de lectura / UX

---

## 19. Decisiones de modelado recomendadas para el MVP

## Recomendación 1
Mantener `DailyChallenge` y `GameSession` como centro del modelo.

## Recomendación 2
Tratar `ChallengeBoardDefinition` como parte interna del challenge, no como agregado separado.

## Recomendación 3
Tratar `LeaderboardEntry` y `UserStats` como derivados.

## Recomendación 4
Modelar explícitamente las vidas dentro de `GameSession`, no solo como una métrica cosmética.

## Recomendación 5
Encapsular la regla horaria en un servicio explícito y testeable.

## Recomendación 6
Encapsular la lógica de progreso de sesión en un servicio o agregado que proteja reglas de vidas, errores y cierre.

## Recomendación 7
No modelar `Move` todavía salvo que el Documento 3 defina una API server-authoritative por movimiento.

---

## 20. Preguntas abiertas que pasan al Documento 3

Estas decisiones todavía deben cerrarse antes de implementar API de manera estable:

- si el backend crea sesión al empezar o solo al finalizar partida
- si la API será server-authoritative por movimiento o submit de resultado final
- cómo se representa el tablero visible del usuario
- si la sesión guarda snapshot parcial del tablero o solo métricas finales
- definición exacta de `clickCount`
- qué tratamiento visual y funcional tienen las minas ya pisadas en una partida en curso
- si una primera partida puede quedar persistida como `IN_PROGRESS` entre refresh, reconexión o cambio de dispositivo
- si el leaderboard se calcula on demand o se materializa
- cómo se autentica al usuario en el MVP
- convención final visible de `challengeDate` para la ventana diaria de negocio

---

## 21. Resumen ejecutivo

El modelo de dominio inicial de MineDaily puede organizarse alrededor de dos raíces fuertes:

- `DailyChallenge`
- `GameSession`

`DailyChallenge` representa el desafío oficial del día lógico y encapsula su definición interna reproducible.

`GameSession` representa la partida concreta del usuario sobre ese challenge, incluyendo su ciclo de vida, sus 3 vidas, sus errores y su resultado competitivo.

`LeaderboardEntry` y `UserStats` conviene tratarlos inicialmente como derivados, no como verdad primaria.

La consecuencia más importante del modelo actual es esta:

> MineDaily no es solo un tablero diario compartido; es un tablero diario compartido sobre el cual cada usuario disputa una única sesión competitiva elegible con hasta 3 vidas.

Ese concepto debe guiar el diseño de API y la implementación del MVP.
