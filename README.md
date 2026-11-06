# ms-andesstay-bff

BFF (Backend For Frontend) del caso **AndesStay** (EP1 - DSY1107, Desarrollo Cloud Native I).

Se ubica entre el frontend Angular (login con **AWS Cognito**) y los microservicios de
dominio. Su responsabilidad, segun el encargo, es:

1. **Validar el JWT** emitido por el IDaaS (Cognito): firma, emisor (`iss`),
   App Client (`client_id`) y vigencia (`exp`/`nbf`).
2. **Autorizar por rol** (claim `cognito:groups` del token) antes de dejar pasar la
   peticion, con las mismas reglas que aplican los microservicios.
3. **Reenviar (proxy)** la peticion ya validada, junto con el mismo access token,
   hacia el microservicio de dominio correspondiente y devolver su respuesta al frontend.

```
Angular (Cognito) --Bearer access_token--> [AWS API Gateway] --> ms-andesstay-bff --Bearer access_token--> ms-andesstay-catalog
                                                                  (valida JWT,                          \-> ms-andesstay-audit
                                                                   aplica roles)                         \-> ms-andesstay-reservations
                                                                                                          \-> ms-andesstay-users
                                                                                                            (validan el JWT otra vez)
```

## Stack

- Java 17
- Spring Boot 3.3.4 (Web, Security, OAuth2 Resource Server, Validation)
- `spring-webflux` (solo el modulo, no el starter completo) para usar
  `WebClient` como cliente HTTP hacia los microservicios, manteniendo el BFF como
  una app servlet clasica (`spring.main.web-application-type: servlet`).

## Estructura

```
src/main/java/cl/duoc/andesstay/bff/
├── BffApplication.java
├── config/
│   ├── JwtConfig.java          # JwtDecoder: valida firma + issuer + client_id
│   ├── SecurityConfig.java     # Reglas de autorizacion por rol y CORS
│   └── WebClientConfig.java    # WebClient hacia catalog, audit, reservations y users
├── security/
│   ├── CognitoAudienceValidator.java           # Valida el claim "client_id"
│   ├── CognitoJwtAuthenticationConverter.java  # Mapea "cognito:groups" -> ROLE_*
│   ├── BearerTokenPropagationFilter.java       # Reenvia el access token al microservicio
│   ├── RestAuthenticationEntryPoint.java       # 401 homogeneo (token invalido)
│   └── RestAccessDeniedHandler.java            # 403 homogeneo (rol insuficiente)
├── client/
│   ├── CatalogClient.java         # Llamadas HTTP hacia ms-andesstay-catalog
│   ├── AuditClient.java           # Llamadas HTTP hacia ms-andesstay-audit
│   ├── ReservationClient.java     # Llamadas HTTP hacia ms-andesstay-reservations
│   └── UsersClient.java           # Llamadas HTTP hacia ms-andesstay-users (Map passthrough)
├── controller/
│   ├── CatalogProxyController.java         # /api/catalog/units/**
│   ├── AuditProxyController.java           # /api/audit/**
│   ├── ReservationsProxyController.java    # /api/reservations/**
│   └── UsersProxyController.java           # /api/users/**
├── dto/
│   ├── UnitDto.java, UnitRequestDto.java           # Catalogo
│   ├── ReservationRequestDto.java, ReservationResponseDto.java  # Reservas
│   ├── AuditEventDto.java, PageResponse.java       # Auditoria
│   └── ErrorResponse.java                          # Cuerpo de error homogeneo
└── exception/GlobalExceptionHandler.java   # Traduce errores del downstream
```

## Configuracion (AWS Cognito)

El BFF usa el **mismo User Pool y App Client** que los microservicios de dominio.
Datos necesarios:

| Dato                          | Donde se usa                                                    |
|-------------------------------|-----------------------------------------------------------------|
| Region del User Pool          | `issuer-uri` (`https://cognito-idp.{region}.amazonaws.com/...`) |
| User Pool ID                  | `issuer-uri` (`.../{userPoolId}`)                               |
| App Client ID                 | `cognito.app-client-id` (debe calzar con `client_id` del token) |
| Grupos del User Pool          | claim `cognito:groups` del token                                |

Variables de entorno (ver `application.yml` y `.env.example`):

```bash
export COGNITO_REGION=us-east-1
export COGNITO_USER_POOL_ID=us-east-1_XXXXXXXXX
export COGNITO_APP_CLIENT_ID=xxxxxxxxxxxxxxxxxxxxxxxxxx
export CATALOG_SERVICE_URL=http://localhost:8081
export RESERVATIONS_SERVICE_URL=http://localhost:8082
export AUDIT_SERVICE_URL=http://localhost:8083
export USERS_SERVICE_URL=http://localhost:8085
export FRONTEND_URL=http://localhost:4200
```

> **El frontend debe mandar el ACCESS token, no el ID token.** Los access tokens de
> Cognito no traen `aud`, traen `client_id`; los ID tokens al reves. Por eso se valida
> `client_id`, y un ID token enviado como Bearer se rechaza con `401`.

Si `COGNITO_USER_POOL_ID` queda en `CHANGE_ME`, el BFF igual arranca (para desarrollo
local), pero rechaza todos los tokens con `401` hasta que se configure el User Pool.

## Puertos de los microservicios

| Servicio                  | Puerto por defecto |
|---------------------------|--------------------|
| ms-andesstay-bff          | 8080               |
| ms-andesstay-catalog      | 8081               |
| ms-andesstay-reservations | 8082               |
| ms-andesstay-audit        | 8083               |
| ms-andesstay-users        | 8085               |

## Como correrlo local

1. Levantar los microservicios de dominio en sus puertos respectivos,
   configurados con el mismo User Pool de Cognito.
2. Exportar las variables de entorno de arriba (o copiar `.env.example` a `.env`).
3. Ejecutar:
   ```bash
   mvn spring-boot:run
   ```
4. El BFF queda arriba en `http://localhost:8080`.

## Endpoints expuestos al frontend

### Usuarios (`/api/users` → ms-andesstay-users)

| Metodo | Ruta                | Requiere            | Descripcion                     |
|--------|---------------------|---------------------|---------------------------------|
| GET    | `/api/users/me`     | Usuario autenticado | Perfil del usuario autenticado  |
| GET    | `/api/users`        | Grupo `ADMIN`       | Listar todos los usuarios       |
| GET    | `/api/users/{id}`   | Grupo `ADMIN`       | Detalle de un usuario           |
| POST   | `/api/users`        | Usuario autenticado | Registrar nuevo usuario (201)   |
| PUT    | `/api/users/{id}`   | Grupo `ADMIN`       | Actualizar usuario              |
| DELETE | `/api/users/{id}`   | Grupo `ADMIN`       | Eliminar usuario (204)          |

### Catalogo (`/api/catalog/units` → ms-andesstay-catalog)

| Metodo | Ruta                      | Requiere            |
|--------|---------------------------|---------------------|
| GET    | `/api/catalog/units`      | Usuario autenticado |
| GET    | `/api/catalog/units/{id}` | Usuario autenticado |
| POST   | `/api/catalog/units`      | Grupo `ADMIN`       |
| PUT    | `/api/catalog/units/{id}` | Grupo `ADMIN`       |
| DELETE | `/api/catalog/units/{id}` | Grupo `ADMIN`       |

### Auditoria (`/api/audit` → ms-andesstay-audit)

| Metodo | Ruta         | Requiere                  |
|--------|--------------|---------------------------|
| GET    | `/api/audit` | Grupo `ADMIN` o `AUDITOR` |

`/api/audit` acepta los query params opcionales `usuario`, `tipoEvento`, `desde`, `hasta`
(ISO-8601), `page` (default 0) y `size` (default 20, maximo 100), y responde una pagina
`{ content, number, size, totalElements, totalPages, first, last }`.

### Reservas (`/api/reservations` → ms-andesstay-reservations)

| Metodo       | Ruta                                  | Requiere                                    |
|--------------|---------------------------------------|---------------------------------------------|
| GET          | `/api/reservations`                   | Usuario autenticado                         |
| GET          | `/api/reservations/mias`              | Usuario autenticado                         |
| GET          | `/api/reservations/{id}`              | Usuario autenticado                         |
| POST         | `/api/reservations`                   | Grupo `HUESPED`, `RECEPCIONISTA` o `ADMIN`  |
| PUT          | `/api/reservations/{id}/confirmar`    | Grupo `RECEPCIONISTA` o `ADMIN`             |
| PUT          | `/api/reservations/{id}/checkin`      | Grupo `RECEPCIONISTA` o `ADMIN`             |
| PUT          | `/api/reservations/{id}/checkout`     | Grupo `RECEPCIONISTA` o `ADMIN`             |
| PUT          | `/api/reservations/{id}/cancelar`     | Grupo `RECEPCIONISTA` o `ADMIN`             |
| DELETE       | `/api/reservations/{id}`              | Grupo `ADMIN`                               |

Todas requieren `Authorization: Bearer <access_token>` obtenido por el frontend al hacer
login con Cognito. Sin token o con token invalido -> `401`. Token valido pero sin el
grupo requerido -> `403`.

```bash
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
     "http://localhost:8080/api/audit?tipoEvento=RESERVA_CONFIRMADA&desde=2026-09-01T00:00:00Z"

curl -H "Authorization: Bearer $ACCESS_TOKEN" \
     "http://localhost:8080/api/reservations/mias"
```

## Como se cubre cada criterio de la pauta (Indicador 2, BFF/API Manager)

- **Valida issuer y audience correctamente**: `JwtConfig` combina
  `JwtValidators.createDefaultWithIssuer(...)` (valida `iss`) con
  `CognitoAudienceValidator` (valida `client_id`, el equivalente a `aud` en los access
  tokens de Cognito) en un `DelegatingOAuth2TokenValidator`.
- **Verifica la firma del token y su vigencia**: `NimbusJwtDecoder`, creado a
  partir del documento de descubrimiento OIDC del User Pool, valida la firma
  contra las llaves publicas (JWKS) y las claims `exp`/`nbf` de forma
  automatica. `CognitoAudienceValidator` refuerza la verificacion de `exp`
  explicitamente.
- **Aplica autorizacion por rol cuando corresponde**: reglas
  `authenticated()` / `hasRole(...)` / `hasAnyRole(...)` en `SecurityConfig`, alimentadas por
  `CognitoJwtAuthenticationConverter` (claim `cognito:groups` -> `ROLE_*`). El resource
  server esta conectado al `SecurityFilterChain` (`.oauth2ResourceServer(...)`). Cada
  dominio tiene su bloque explicito de `requestMatchers` (catalogo, auditoria, reservas
  y usuarios) en lugar de depender unicamente del generico `anyRequest().authenticated()`.
- **Responde con codigos de error adecuados**: `RestAuthenticationEntryPoint`
  (401 - sin token / token invalido), `RestAccessDeniedHandler` (403 - rol
  insuficiente) y `GlobalExceptionHandler` (400 en validaciones, 4xx
  propagados desde el microservicio, 502 si no responde).

## Pruebas

```bash
mvn test
```

- `CatalogProxySecurityTest`: sin token -> 401; token sin grupo ADMIN -> no puede escribir
  (403); con ADMIN -> puede crear (201) y eliminar (204).
- `AuditProxySecurityTest`: sin token -> 401; sin grupo ADMIN ni AUDITOR -> 403; con
  AUDITOR o ADMIN -> 200; tamano de pagina se acota al maximo permitido.
- `ReservationsProxySecurityTest`: sin token -> 401; GET con token sin grupos -> 200;
  POST con HUESPED -> 201; POST con AUDITOR -> 403; PUT confirmar con HUESPED -> 403;
  PUT confirmar con RECEPCIONISTA -> 200; DELETE con RECEPCIONISTA -> 403; DELETE con
  ADMIN -> 204.
- `UsersProxySecurityTest`: sin token -> 401; GET /me con token sin grupos -> 200;
  GET /api/users con AUDITOR -> 403; con ADMIN -> 200; POST con token sin grupos -> 201;
  DELETE con ADMIN -> 204; DELETE sin ADMIN -> 403.
- `CognitoJwtAuthenticationConverterTest`: `cognito:groups` -> `ROLE_*`; scopes ->
  `SCOPE_*`.
- `CognitoAudienceValidatorTest`: acepta el `client_id` esperado; rechaza otro App Client;
  rechaza un ID token (solo trae `aud`); rechaza token expirado.
- `BearerTokenPropagationFilterTest`: el access token del usuario se reenvia al
  microservicio; sin usuario autenticado no agrega Authorization.

## Equipo

Jean Flores, Christian Fuentes, Emmanuel Valenzuela.
