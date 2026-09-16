# ms-andesstay-bff

BFF (Backend For Frontend) del caso **AndesStay** (EP1 - DSY1107, Desarrollo Cloud Native I).

Se ubica entre el frontend Angular (con MSAL / Azure AD) y los microservicios de
dominio (por ejemplo `ms-andesstay-catalog`). Su responsabilidad, segun el
encargo, es:

1. **Validar el JWT** emitido por el IDaaS (Azure AD): firma, emisor (`iss`),
   audiencia (`aud`) y vigencia (`exp`/`nbf`).
2. **Autorizar por rol** (claim `roles` del token) antes de dejar pasar la
   peticion.
3. **Reenviar (proxy)** la peticion ya validada hacia el microservicio de
   dominio correspondiente y devolver su respuesta al frontend.

```
Angular (MSAL) --Bearer JWT--> [AWS API Gateway] --> ms-andesstay-bff --> ms-andesstay-catalog (EC2)
                                                         (valida JWT,          (por ahora no valida
                                                          aplica roles)         JWT, ver su README)
```

## Stack

- Java 17
- Spring Boot 3.3.4 (Web, Security, OAuth2 Resource Server, Validation)
- `spring-webflux` (solo el modulo, no el starter completo) para usar
  `WebClient` como cliente HTTP hacia el catalogo, manteniendo el BFF como
  una app servlet clasica (`spring.main.web-application-type: servlet`).

## Estructura

```
src/main/java/cl/duoc/andesstay/bff/
├── BffApplication.java
├── config/
│   ├── JwtConfig.java          # JwtDecoder: valida firma + issuer + audience
│   ├── SecurityConfig.java     # Reglas de autorizacion por rol y CORS
│   └── WebClientConfig.java    # WebClient hacia ms-andesstay-catalog
├── security/
│   ├── AudienceValidator.java              # Valida el claim "aud"
│   ├── AzureAdJwtAuthenticationConverter.java # Mapea "roles" -> ROLE_*
│   ├── RestAuthenticationEntryPoint.java   # 401 homogeneo (token invalido)
│   └── RestAccessDeniedHandler.java        # 403 homogeneo (rol insuficiente)
├── client/CatalogClient.java   # Llamadas HTTP al microservicio de catalogo
├── controller/CatalogProxyController.java  # Endpoints expuestos al frontend
├── dto/                        # UnitDto, UnitRequestDto, ErrorResponse
└── exception/GlobalExceptionHandler.java   # Traduce errores del downstream
```

## Configuracion (Azure AD / IDaaS)

En Azure AD (Entra ID) se necesita registrar la API que expone este BFF y
tomar tres datos:

| Dato                         | Donde se usa                                   |
|-------------------------------|-------------------------------------------------|
| Directory (tenant) ID          | `issuer-uri` (`.../{tenant-id}/v2.0`)          |
| Application (client) ID de la API | `azure.ad.audience` (debe calzar con el `aud` del token) |
| App roles definidos en el manifiesto (`ADMIN`, `CLIENTE`) | claim `roles` del token |

Variables de entorno esperadas (ver `application.yml`):

```bash
export AZURE_TENANT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
export AZURE_API_CLIENT_ID=yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy
export CATALOG_SERVICE_URL=http://localhost:8081
export FRONTEND_URL=http://localhost:4200
```

## Como correrlo local

1. Levantar primero `ms-andesstay-catalog` (puerto 8081) y su base de datos.
2. Exportar las variables de entorno de arriba.
3. Ejecutar:
   ```bash
   mvn spring-boot:run
   ```
4. El BFF queda arriba en `http://localhost:8080`.

## Endpoints expuestos al frontend

| Metodo | Ruta                        | Rol requerido     |
|--------|-----------------------------|--------------------|
| GET    | `/api/catalog/units`        | ADMIN o CLIENTE    |
| GET    | `/api/catalog/units/{id}`   | ADMIN o CLIENTE    |
| POST   | `/api/catalog/units`        | ADMIN              |
| PUT    | `/api/catalog/units/{id}`   | ADMIN              |
| DELETE | `/api/catalog/units/{id}`   | ADMIN              |

Todas requieren header `Authorization: Bearer <JWT>` obtenido por el
frontend via MSAL. Sin token o con token invalido -> `401`. Token valido
pero rol insuficiente -> `403`.

## Como se cubre cada criterio de la pauta (Indicador 2, BFF/API Manager)

- **Valida issuer y audience correctamente**: `JwtConfig` combina
  `JwtValidators.createDefaultWithIssuer(...)` (valida `iss`) con
  `AudienceValidator` (valida `aud`) en un `DelegatingOAuth2TokenValidator`.
- **Verifica la firma del token y su vigencia**: `NimbusJwtDecoder`, creado a
  partir del documento de descubrimiento OIDC de Azure AD, valida la firma
  contra las llaves publicas (JWKS) y las claims `exp`/`nbf` de forma
  automatica.
- **Aplica autorizacion por rol cuando corresponde**: reglas
  `hasRole(...)` / `hasAnyRole(...)` en `SecurityConfig`, alimentadas por
  `AzureAdJwtAuthenticationConverter` (claim `roles` -> `ROLE_*`).
- **Responde con codigos de error adecuados**: `RestAuthenticationEntryPoint`
  (401 - sin token / token invalido), `RestAccessDeniedHandler` (403 - rol
  insuficiente) y `GlobalExceptionHandler` (400 en validaciones, 404/4xx
  propagados desde el catalogo, 502 si el catalogo no responde).

## Pruebas

`CatalogProxySecurityTest` cubre, con `@WebMvcTest` + `spring-security-test`:

- request sin token -> `401`
- token valido sin el rol requerido -> `403`
- token valido con rol `ADMIN` -> pasa la autorizacion (`200`)

```bash
mvn test
```

## Pendiente / siguiente fase

- Reenviar el JWT (o un token de servicio) hacia `ms-andesstay-catalog`
  cuando ese microservicio implemente su propio filtro de validacion JWT.
- Desplegar en EC2 detras de AWS API Gateway, segun lo definido en el
  encargo para el resto de los componentes.

## Equipo

Jean Flores, Christian Fuentes, Emmanuel Valenzuela.
