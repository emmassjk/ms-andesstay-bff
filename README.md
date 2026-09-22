# ms-andesstay-bff

BFF (Backend For Frontend) del caso **AndesStay** (EP1 - DSY1107, Desarrollo Cloud Native I).

Se ubica entre el frontend Angular (login con **AWS Cognito**) y los microservicios de
dominio (`ms-andesstay-catalog`, `ms-andesstay-audit`). Su responsabilidad, segun el
encargo, es:

1. **Validar el JWT** emitido por el IDaaS (Cognito): firma, emisor (`iss`),
   App Client (`client_id`) y vigencia (`exp`/`nbf`).
2. **Autorizar por rol** (claim `cognito:groups` del token) antes de dejar pasar la
   peticion, con las mismas reglas que aplican los microservicios.
3. **Reenviar (proxy)** la peticion ya validada, junto con el mismo access token,
   hacia el microservicio de dominio correspondiente y devolver su respuesta al frontend.

```
Angular (Cognito) --Bearer access_token--> [AWS API Gateway] --> ms-andesstay-bff --Bearer access_token--> ms-andesstay-catalog
                                                                  (valida JWT,                         \-> ms-andesstay-audit
                                                                   aplica roles)                          \-> ms-andesstay-reservations
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
│   └── WebClientConfig.java    # WebClient hacia catalog, audit y reservations
├── security/
│   ├── CognitoAudienceValidator.java           # Valida el claim "client_id"
│   ├── CognitoJwtAuthenticationConverter.java  # Mapea "cognito:groups" -> ROLE_*
│   ├── BearerTokenPropagationFilter.java       # Reenvia el access token al microservicio
│   ├── RestAuthenticationEntryPoint.java       # 401 homogeneo (token invalido)
│   └── RestAccessDeniedHandler.java            # 403 homogeneo (rol insuficiente)
├── client/                     # CatalogClient, AuditClient, ReservationsClient (llamadas HTTP)
├── controller/                 # CatalogProxyController, AuditProxyController, ReservationsProxyController
├── dto/                        # UnitDto, UnitRequestDto, AuditEventDto, ReservationDto, PageResponse, ErrorResponse
└── exception/GlobalExceptionHandler.java   # Traduce errores del downstream
```

## Configuracion (AWS Cognito)

El BFF usa el **mismo User Pool y App Client** que `ms-andesstay-catalog` y
`ms-andesstay-audit`. Datos necesarios:

| Dato                          | Donde se usa                                                    |
|-------------------------------|-----------------------------------------------------------------|
| Region del User Pool          | `issuer-uri` (`https://cognito-idp.{region}.amazonaws.com/...`) |
| User Pool ID                  | `issuer-uri` (`.../{userPoolId}`)                               |
| App Client ID                 | `cognito.app-client-id` (debe calzar con `client_id` del token) |
| Grupos del User Pool (`ADMIN`, `AUDITOR`) | claim `cognito:groups` del token                    |

Variables de entorno (ver `application.yml` y `.env.example`):

```bash
export COGNITO_REGION=us-east-1
export COGNITO_USER_POOL_ID=us-east-1_XXXXXXXXX
export COGNITO_APP_CLIENT_ID=xxxxxxxxxxxxxxxxxxxxxxxxxx
export CATALOG_SERVICE_URL=http://localhost:8081
export AUDIT_SERVICE_URL=http://localhost:8083
export RESERVATIONS_SERVICE_URL=http://localhost:8084
export FRONTEND_URL=http://localhost:4200
```

> **El frontend debe mandar el ACCESS token, no el ID token.** Los access tokens de
> Cognito no traen `aud`, traen `client_id`; los ID tokens al reves. Por eso se valida
> `client_id`, y un ID token enviado como Bearer se rechaza con `401`.

Si `COGNITO_USER_POOL_ID` queda en `CHANGE_ME`, el BFF igual arranca (para desarrollo
local), pero rechaza todos los tokens con `401` hasta que se configure el User Pool.

## Como correrlo local

1. Levantar `ms-andesstay-catalog` (puerto 8081) y `ms-andesstay-audit` (puerto 8083)
   con su base de datos, configurados con el mismo User Pool.
2. Exportar las variables de entorno de arriba (o copiar `.env.example` a `.env`).
3. Ejecutar:
   ```bash
   mvn spring-boot:run
   ```
4. El BFF queda arriba en `http://localhost:8080`.

## Endpoints expuestos al frontend

| Metodo       | Ruta                            | Requiere                          | Se reenvia a                              |
|--------------|---------------------------------|-----------------------------------|-------------------------------------------|
| GET          | `/api/catalog/units`            | Usuario autenticado               | catalog `GET /api/catalog/units`          |
| GET          | `/api/catalog/units/{id}`       | Usuario autenticado               | catalog                                   |
| POST         | `/api/catalog/units`            | Grupo `ADMIN`                     | catalog                                   |
| PUT          | `/api/catalog/units/{id}`       | Grupo `ADMIN`                     | catalog                                   |
| DELETE       | `/api/catalog/units/{id}`       | Grupo `ADMIN`                     | catalog                                   |
| GET          | `/api/audit`                    | Grupo `ADMIN` o `AUDITOR`         | audit `GET /api/audit`                    |
| GET          | `/api/reservations/**`          | Usuario autenticado               | reservations                              |
| POST         | `/api/reservations/**`          | Grupo `HUESPED`, `RECEPCIONISTA` o `ADMIN` | reservations                   |
| PUT / PATCH  | `/api/reservations/**`          | Grupo `RECEPCIONISTA` o `ADMIN`   | reservations                              |
| DELETE       | `/api/reservations/**`          | Grupo `ADMIN`                     | reservations                              |

`/api/audit` acepta los query params opcionales `usuario`, `tipoEvento`, `desde`, `hasta`
(ISO-8601), `page` (default 0) y `size` (default 20, maximo 100), y responde una pagina
`{ content, number, size, totalElements, totalPages, first, last }`.

Todas requieren `Authorization: Bearer <access_token>` obtenido por el frontend al hacer
login con Cognito. Sin token o con token invalido -> `401`. Token valido pero sin el
grupo requerido -> `403`.

```bash
curl -H "Authorization: Bearer $ACCESS_TOKEN" \
     "http://localhost:8080/api/audit?tipoEvento=RESERVA_CONFIRMADA&desde=2026-09-01T00:00:00Z"

curl -H "Authorization: Bearer $ACCESS_TOKEN" \
     "http://localhost:8080/api/reservations/my"
```

## Como se cubre cada criterio de la pauta (Indicador 2, BFF/API Manager)

- **Valida issuer y audience correctamente**: `JwtConfig` combina
  `JwtValidators.createDefaultWithIssuer(...)` (valida `iss`) con
  `CognitoAudienceValidator` (valida `client_id`, el equivalente a `aud` en los access
  tokens de Cognito) en un `DelegatingOAuth2TokenValidator`.
- **Verifica la firma del token y su vigencia**: `NimbusJwtDecoder`, creado a
  partir del documento de descubrimiento OIDC del User Pool, valida la firma
  contra las llaves publicas (JWKS) y las claims `exp`/`nbf` de forma
  automatica.
- **Aplica autorizacion por rol cuando corresponde**: reglas
  `authenticated()` / `hasRole(...)` / `hasAnyRole(...)` en `SecurityConfig`, alimentadas por
  `CognitoJwtAuthenticationConverter` (claim `cognito:groups` -> `ROLE_*`). El resource
  server esta conectado al `SecurityFilterChain` (`.oauth2ResourceServer(...)`). Cada
  dominio tiene su bloque explícito de `requestMatchers` (catalogo, auditoria y reservas)
  en lugar de depender únicamente del genérico `anyRequest().authenticated()`.
- **Responde con codigos de error adecuados**: `RestAuthenticationEntryPoint`
  (401 - sin token / token invalido), `RestAccessDeniedHandler` (403 - rol
  insuficiente) y `GlobalExceptionHandler` (400 en validaciones, 4xx
  propagados desde el microservicio, 502 si no responde).

## Pruebas

```bash
mvn test
```

- `CatalogProxySecurityTest` / `AuditProxySecurityTest` / `ReservationsProxySecurityTest`
  (`@WebMvcTest` + `spring-security-test`, importando `SecurityConfig` real): sin token ->
  `401`; token sin el grupo requerido -> `403`; con el grupo correcto -> pasa la
  autorizacion; el tamano de pagina de `/api/audit` se acota.
- `CognitoJwtAuthenticationConverterTest`: `cognito:groups` -> `ROLE_*`.
- `CognitoAudienceValidatorTest`: acepta el `client_id` esperado; rechaza otro App Client y
  rechaza un ID token (solo trae `aud`).
- `BearerTokenPropagationFilterTest`: el access token del usuario se reenvia al microservicio.

## Pendiente / siguiente fase

- Implementar `ReservationsClient`, `ReservationsProxyController` y los DTOs
  correspondientes cuando `ms-andesstay-reservations` esté disponible (la regla
  de seguridad en `SecurityConfig` ya está en su lugar).
- Agregar `ReservationsProxySecurityTest` con los escenarios de cada rol.
- Desplegar en EC2 detras de AWS API Gateway, segun lo definido en el
  encargo para el resto de los componentes.

## Equipo

Jean Flores, Christian Fuentes, Emmanuel Valenzuela.
