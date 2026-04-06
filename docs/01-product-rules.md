# MineDaily — Reglas de Producto

## 1. Propósito

MineDaily es una aplicación web de **Buscaminas diario** donde todos los usuarios juegan el mismo challenge vigente.

El objetivo del producto es combinar:

- la lógica clásica de Buscaminas
- un formato de desafío diario compartido
- competencia mediante leaderboard
- progresión personal mediante estadísticas

---

## 2. Idea principal del producto

Cada día existe un **challenge oficial**.

Ese challenge:

- es igual para todos los usuarios
- tiene un único tablero asociado
- se genera de manera determinística
- puede reconstruirse internamente a partir de su configuración y seed

MineDaily no busca ser un Buscaminas infinito, sino un producto centrado en:

- el challenge vigente
- la comparación entre jugadores
- la repetición diaria

---

## 3. Challenge vigente

El sistema siempre tiene un **challenge vigente**.

### Reglas del challenge vigente

- existe un solo challenge activo a la vez
- todos los usuarios juegan ese mismo challenge
- el backend es la fuente de verdad del challenge
- el challenge se identifica por una fecha lógica de negocio, no solo por fecha calendario

### Fecha lógica de negocio

La `challengeDate` representa la fecha lógica del challenge según la ventana de negocio oficial de MineDaily en timezone `America/Argentina/Cordoba`.

Para este documento, se deja abierta la semántica exacta de naming visible de esa fecha, pero debe cumplirse que:

- la `challengeDate` identifica de manera unívoca al challenge oficial de una ventana diaria de negocio
- esa identificación debe ser consistente en backend, base de datos, API y leaderboard
- la regla exacta de asignación de `challengeDate` a la ventana horaria se cerrará definitivamente en los documentos de dominio y API antes de implementación final

Esta decisión se deja abierta a propósito para no forzar una convención demasiado temprano, pero sin perder consistencia conceptual.

---

## 4. Regla horaria del producto

### Timezone oficial

La timezone oficial del producto es:

`America/Argentina/Cordoba`

### Hora de cambio del challenge

El challenge cambia todos los días a las:

`21:00 hs`

### Comportamiento esperado

- hasta las 20:59:59, sigue vigente el challenge actual
- a partir de las 21:00:00, pasa a estar vigente el siguiente challenge

### Implicancia

El producto no se rige por “cambio a medianoche”, sino por una ventana diaria con rollover a las 21:00 hs de Córdoba.

Toda resolución del challenge vigente debe basarse en esta regla horaria y no en la fecha calendario del cliente.

---

## 5. Generación del challenge

Cada challenge debe poder ser generado de manera determinística a partir de:

- configuración del tablero
- versión del generador
- seed interna

### Reglas de generación

- un challenge debe poder reconstruirse internamente en cualquier momento
- la generación debe ser reproducible
- la seed es un dato interno del backend
- el frontend no debe recibir la seed real ni conocer la posición de las minas
- la generación del challenge oficial debe ser idempotente para una misma `challengeDate`
- si múltiples procesos intentan generar el mismo challenge lógico, el sistema debe terminar con una única definición oficial válida

### Objetivos de esta decisión

- reproducibilidad
- auditoría
- debugging
- historial de challenges
- posibilidad de evolucionar el algoritmo sin romper challenges anteriores

---

## 6. Información pública vs privada

### Información pública del challenge

El frontend puede recibir:

- challengeId
- challengeDate
- rows
- cols
- mineCount
- rolloverAt
- estado del usuario respecto al challenge actual

### Información privada del challenge

El backend no debe exponer:

- internalSeed
- posiciones de minas
- detalles internos del algoritmo de generación

---

## 7. Reglas del juego

### Tablero

En la versión inicial del producto, cada challenge tiene:

- tamaño fijo
- cantidad fija de minas

Estos valores podrán cambiar en el futuro, pero inicialmente deben mantenerse estables para simplificar el desarrollo y el ranking.

### Interacciones básicas del jugador

El jugador puede realizar las siguientes acciones:

- revelar una celda
- marcar o desmarcar una bandera

### Sistema de vidas

En la versión inicial del producto, cada partida elegible del usuario comienza con:

- **3 vidas**

### Pérdida de vida

Cuando el jugador revela una mina:

- pierde 1 vida
- la mina revelada debe mostrarse visualmente
- la partida continúa en el mismo tablero si todavía le quedan vidas disponibles
- el tablero no debe regenerarse ni alterarse

### Regla de tablero único

El challenge oficial debe mantenerse completamente estático para todos los usuarios.

Esto implica que:

- no existe una regla de cortesía que mueva minas para un usuario particular
- no existe protección especial del primer click en el challenge diario oficial
- si el primer click del usuario cae sobre una mina, pierde una vida inmediatamente
- todos los competidores enfrentan exactamente la misma configuración del tablero y los mismos riesgos

### Condiciones de derrota

El jugador pierde su partida cuando agota sus 3 vidas.

### Condiciones de victoria

El jugador gana cuando revela todas las celdas que no contienen minas, aunque durante la partida haya pisado una o más minas y haya perdido vidas.

---

## 8. Intentos por día

Un usuario puede jugar múltiples veces el challenge vigente.

### Regla principal

**Solo la primera partida del usuario para el challenge vigente puede contar para el leaderboard.**

### Nueva interpretación de “primer intento”

En MineDaily, el **primer intento** no equivale a “una sola vida”, sino a una **única partida o sesión elegible** con hasta 3 vidas disponibles sobre el tablero oficial del día.

Esa primera partida termina únicamente cuando ocurre una de estas dos condiciones:

- el usuario completa el tablero y gana
- el usuario pierde sus 3 vidas y pierde

### Aclaración de alcance

El sistema puede registrar múltiples partidas de un mismo usuario para el mismo challenge, pero la elegibilidad para leaderboard queda determinada únicamente por la primera partida completa de ese usuario para ese challenge.

### Casos posibles

#### Caso 1: gana en la primera partida
- esa partida cuenta para el leaderboard
- puede haber ganado con 3, 2 o 1 vida restante
- las siguientes partidas del mismo challenge no puntúan

#### Caso 2: pierde la primera partida
- esto ocurre cuando agota sus 3 vidas antes de completar el tablero
- puede volver a jugar
- las siguientes partidas no puntúan
- solo son recreativas o de práctica

### Objetivo de esta regla

- reducir frustración sin romper el desafío diario compartido
- mantener la tensión del challenge diario
- evitar grinding para mejorar tiempo
- hacer el leaderboard más justo

---

## 9. Leaderboard

Cada challenge tiene su propio leaderboard.

### Qué partidas participan

Solo participan partidas que cumplan ambas condiciones:

- sean la primera partida del usuario para ese challenge
- hayan terminado en victoria

### Definición de error

Para el MVP, **1 error = 1 mina pisada = 1 vida perdida**.

Esto convierte a la precisión del jugador en una métrica observable y comparable sin alterar el tablero oficial.

### Orden del ranking

El ranking diario se ordena por:

1. menor tiempo
2. menor cantidad de errores
3. menor cantidad de clicks

### Interpretación del ranking

A igualdad de tiempo, rankea más alto quien haya terminado con menos errores, es decir, quien haya conservado más vidas.

Si también empatan en errores, rankea más alto quien haya necesitado menos clicks.

### Desempate técnico final

Si dos entradas empatan en todos los criterios de ranking definidos por producto, el sistema deberá aplicar un desempate técnico determinístico definido a nivel de implementación.

El objetivo es que el orden final del leaderboard siempre sea estable y reproducible.

### Qué no entra al leaderboard

No deben entrar:

- partidas perdidas
- partidas que no sean primera partida del usuario para ese challenge
- partidas recreativas posteriores

---

## 10. Estado del usuario respecto al challenge

Para el challenge actual, el sistema debe poder determinar si el usuario:

- no jugó todavía
- tiene una primera partida en curso
- ya jugó y ganó
- ya jugó y perdió
- ya tiene una partida que cuenta para leaderboard
- ya no puede volver a puntuar ese día

Además, para una primera partida ganada, el sistema debe poder conocer al menos:

- cantidad de vidas restantes al finalizar
- cantidad de errores cometidos
- métricas de ranking asociadas a esa victoria

Este estado es importante para que el frontend pueda mostrar correctamente la situación del usuario frente al challenge vigente.

### Regla de derivación

El estado del usuario respecto del challenge actual debe poder derivarse de sus partidas registradas para ese challenge.

---

## 11. Backend como fuente de verdad

El backend debe ser el dueño de la verdad de negocio.

### Responsabilidades del backend

- decidir cuál es el challenge vigente
- generar el challenge
- guardar la seed interna
- reconstruir el tablero oficial
- validar vidas, errores y resultado de la partida
- validar si una partida puntúa o no
- calcular leaderboard
- exponer información pública al frontend

### Responsabilidades del frontend

- renderizar la interfaz
- permitir interacción del usuario
- mostrar estado del challenge
- consumir la API

### Regla importante

El frontend no debe ser dueño del tablero real completo ni de la lógica autoritativa del challenge diario.

---

## 12. Generación automática y generación manual

### Generación automática

El sistema debe soportar generación automática del challenge vigente al momento correspondiente.

### Fallback

Si por algún motivo el challenge vigente no existe al momento de consultarlo, el backend puede generarlo de forma lazy al recibir la primera petición.

### Generación manual para desarrollo

Debe existir un mecanismo manual para generar challenges en desarrollo.

Ese mecanismo:

- no debe considerarse parte de la API pública principal
- debe estar limitado a desarrollo o a uso interno
- sirve para pruebas, debugging y aceleración del desarrollo

---

## 13. API pública conceptual inicial

A nivel de producto, el frontend necesita consultar al menos:

- el challenge vigente
- el leaderboard del challenge vigente
- el estado del usuario frente al challenge actual

Los endpoints concretos se definirán en el documento de contrato de API.

---

## 14. Objetivos del MVP

El MVP del producto debe permitir:

- obtener el challenge vigente
- jugar el tablero del día con sistema de 3 vidas
- registrar partidas
- aplicar la regla de primera partida elegible
- calcular errores como minas pisadas
- ver leaderboard diario
- ver estado básico del usuario

---

## 15. Fuera de alcance inicial

Las siguientes cosas no forman parte del MVP inicial:

- amigos
- chat
- ligas semanales
- replay completo de jugadas
- múltiples modos de juego
- ranking global histórico complejo
- anti-cheat avanzado
- multiplayer en tiempo real

---

## 16. Principios del producto

MineDaily debe priorizar:

- simplicidad de reglas
- reproducibilidad del challenge
- fairness del leaderboard
- claridad de estado para el usuario
- reducción razonable de frustración sin alterar el tablero compartido
- capacidad de evolucionar sin reescribir el núcleo

---

## 17. Invariantes de producto

Las siguientes reglas deben mantenerse siempre verdaderas en el sistema:

- debe existir como máximo un challenge oficial por `challengeDate`
- la generación del challenge oficial debe ser idempotente para una misma `challengeDate`
- todos los usuarios juegan el mismo challenge vigente
- el challenge diario no debe modificar minas ni reconfigurarse según el usuario
- el frontend nunca recibe la seed real ni la ubicación completa de minas
- cada primera partida elegible empieza con exactamente 3 vidas
- cada mina revelada durante una partida descuenta exactamente 1 vida y suma exactamente 1 error
- una partida termina solo por victoria o por agotamiento de las 3 vidas
- solo la primera partida del usuario para ese challenge puede ser elegible para leaderboard
- solo partidas ganadas y elegibles pueden aparecer en el leaderboard
- el orden final del leaderboard debe ser determinístico

---

## 18. Preguntas abiertas

Estas decisiones quedan abiertas para documentos siguientes:

- tamaño exacto inicial del tablero
- cantidad exacta de minas
- si el backend validará jugadas una por una o solo el resultado final
- si se persistirá el detalle de cada movimiento
- cómo se representará el estado visible del tablero en el frontend
- cómo se manejará autenticación en el MVP
- definición exacta de “click” para métricas de ranking
- qué tratamiento visual y funcional tendrán las minas ya pisadas dentro de una partida en curso
- si una primera partida puede quedar persistida como “en curso” entre refresh, reconexión o cambio de dispositivo
- convención final de asignación visible de `challengeDate` a la ventana diaria de negocio

---

## 19. Resumen ejecutivo

MineDaily es un Buscaminas diario competitivo.

Sus reglas centrales son:

- existe un challenge vigente único
- cambia todos los días a las 21:00 hs de Córdoba
- el backend mantiene una seed interna y es la fuente de verdad
- el frontend no conoce la seed real ni el tablero autoritativo
- todos los usuarios juegan exactamente el mismo tablero oficial
- cada primera partida elegible se juega con 3 vidas
- pisar una mina no reinicia el tablero: descuenta una vida y la partida continúa
- si el primer click cae en una mina, el usuario pierde una vida inmediatamente
- un usuario puede jugar varias veces, pero solo su primera partida puede puntuar
- el leaderboard incluye únicamente primeras partidas ganadas
- el ranking se ordena por tiempo, errores y clicks
- la generación del challenge debe ser reproducible e idempotente por challenge lógico

Estas reglas definen el comportamiento base del producto y guían el diseño del dominio, la API y la implementación.
