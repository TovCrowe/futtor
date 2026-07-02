# Torneo Tracker

Organizador de torneos de fútbol formato mexicano: tabla general (todos contra
todos) seguida de liguilla (eliminación directa entre los mejores clasificados).
Pensado para organizadores de ligas amateur/cascaritas que hoy llevan esto en
Excel o a mano.

## Stack

- Java 21, Spring Boot, Maven
- Spring Web, Spring Data JPA, Validation, Lombok
- H2 en memoria (desarrollo) — Postgres se agrega hasta que se necesite
  persistencia real
- **NO agregar Spring Security todavía** — se incorpora solo si se necesita
  login de organizadores en una fase futura. No agregarla antes de tiempo.

## Filosofía del proyecto

Este es el segundo intento después de dos proyectos que no llegaron a nada
usable: `pc` (esqueleto vacío, dependencias sin funcionalidad) y un
"trámites guide" abandonado por exceso de alcance antes de empezar.
Regla general: **cada fase debe terminar con algo end-to-end funcional**
antes de tocar la siguiente. No adelantar entidades ni endpoints de fases
futuras. Si el alcance de una fase se siente pesado, es señal de recortarla,
no de aguantarse.

## Roadmap por fases

- **Fase 0 (actual):** tabla general, sin liguilla todavía.
  - Entidades: `Tournament`, `Team`, `Match`
  - Endpoints:
    - `POST /api/torneos/{id}/partidos` — capturar resultado
    - `GET /api/torneos/{id}/tabla` — tabla calculada al vuelo desde los
      partidos jugados (PJ, PG, PE, PP, GF, GC, DG, Pts)
  - **La tabla nunca se persiste como entidad propia** — siempre se calcula
    desde `Match`, que es la única fuente de verdad. Evita bugs de
    sincronización.
  - Puntos: victoria = 3, empate = 1, derrota = 0 (formato mexicano estándar,
    confirmar si se quiere cambiar)
  - Sin autenticación, sin frontend todavía
- **Fase 1:** liguilla
  - Top N de la tabla (configurable), cruces cabeza-cola (1º vs último
    clasificado, 2º vs penúltimo, etc.)
  - Partidos ida/vuelta o partido único (configurable)
  - Desempate en marcador global: mejor posición en tabla general → penales
    (NO usar regla de gol de visitante, ya no se usa en Liga MX)
- **Fase 2 (stretch):** `Player`, goleo individual, tarjetas
- **Fase 3 (stretch):** multi-torneo por organizador, login

## Estructura de paquetes

```
com.tov.futtor
├── torneo/
│   ├── Tournament.java, Team.java, Match.java
│   ├── TournamentRepository.java, TeamRepository.java, MatchRepository.java
│   ├── TournamentService.java    (incluye lógica de cálculo de tabla)
│   ├── TournamentController.java
│   └── dto/          (nunca exponer entidades JPA directamente en la API)
├── liguilla/           (Fase 1+)
└── config/
```

## Convenciones de código

- DTOs de respuesta separados de las entidades JPA
- Capas: Controller → Service → Repository (sin lógica de negocio en el controller)
- Nombres de clases, entidades y campos en inglés (Tournament, Team, Match,
  goalsHome...). Los nombres de paquetes se mantienen en español por dominio
  (`torneo/`, `liguilla/`)
- Lógica de cálculo de tabla vive en el Service, cubierta con tests unitarios
  desde el principio (es la parte más propensa a bugs sutiles: empates,
  criterios de desempate)
