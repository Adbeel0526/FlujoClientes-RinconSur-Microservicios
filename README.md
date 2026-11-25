# Backend Rincón Sur

Abre esta carpeta en **Visual Studio Code** con JDK 21, Maven y las extensiones de Java y Spring Boot. El `pom.xml` de la raíz permite compilar todo el backend. Cada módulo tiene su propio `pom.xml`, aplicación Spring Boot y configuración. La [aplicación Android](https://github.com/Adbeel0526/FlujoClientes-RinconSur) se abre por separado en Android Studio.

| Módulo | Puerto | Responsabilidad y datos |
|---|---:|---|
| `eureka-server` | 8761 | Descubrimiento de servicios |
| `api-gateway` | 8080 | Entrada de Retrofit y rutas `lb://` |
| `auth-service` | 8081 | Inicio de sesión, JWT y contraseñas. Esquema `rinconsur_auth` |
| `user-service` | 8082 | Cuentas, perfiles y su auditoría. Esquema `rinconsur_usuarios` |
| `operations-service` | 8083 | Accesos, jornada, sesiones, conteos e indicadores. Esquema `rinconsur_operaciones` |
| `notification-service` | 8084 | Entrega de correo de recuperación por SMTP |

Auth consulta usuarios y solicita correo con WebClient. Operations usa WebClient para verificar la sesión y consultar observadores. Los clientes resuelven los servicios por nombre con Eureka. Los servicios conservan las rutas existentes de Retrofit y la autenticación por portador.

## Arranque con XAMPP

1. Inicia MySQL en XAMPP. Importa [local/xampp/crear-bases.sql](local/xampp/crear-bases.sql) desde phpMyAdmin o el cliente `mysql`. Flyway crea las tablas al arrancar. Los valores locales predeterminados son `127.0.0.1:3306`, usuario `root` y contraseña vacía. Si XAMPP usa otros valores, exporta `MYSQL_PORT`, `MYSQL_USER` y `MYSQL_PASSWORD` antes del arranque.
2. Ejecuta `mvn clean verify` en esta carpeta.
3. Ejecuta `bash local/start.sh` en macOS o en Git Bash en Windows. Windows debe tener Java, Maven, OpenSSL y curl disponibles en la terminal. Las claves JWT y el token interno se crean en `.local/`. Los registros de ejecución quedan en esa misma carpeta.
4. Abre `http://127.0.0.1:8761/` y espera que se registren los servicios. Desde el PC servidor comprueba `http://127.0.0.1:8080/health/services/auth` y las rutas equivalentes para `users`, `operations` y `notifications`.
5. Para conectar clientes en otros PC de la red, inicia con `GATEWAY_BIND=0.0.0.0 bash local/start.sh`. La URL de Retrofit es `http://IP_DEL_SERVIDOR:8080/`.

Puedes asignar una conexión y credenciales distintas a cada servicio con los prefijos `AUTH_DB_`, `USUARIOS_DB_` y `OPERACIONES_DB_` y los sufijos `URL`, `USERNAME` y `PASSWORD`. Para configurar la recuperación por correo usa `SMTP_HOST`, `SMTP_PORT`, `SMTP_FROM`, `SMTP_USER` y `SMTP_PASSWORD`. El correo de recuperación requiere un servidor SMTP configurado.

## OpenAPI y contratos

En el PC servidor, abre `http://127.0.0.1:8081/swagger-ui/index.html` para Auth y `http://127.0.0.1:8083/swagger-ui/index.html` para Operations. Cada servicio funcional también publica `/v3/api-docs` en su propio puerto. Los endpoints internos de usuarios, credenciales y correo se usan entre servicios y están protegidos por el token interno.

| Ruta en el Gateway | Uso |
|---|---|
| `POST /api/v1/auth/login`, `GET /api/v1/auth/me` | Sesión y rol |
| `/api/v1/auth/perfil`, `/api/v1/auth/contrasena`, `/api/v1/auth/recuperacion` | Perfil y contraseña |
| `/api/v1/auth/admin/usuarios/**` | Listado, alta, edición, estado y auditoría de cuentas |
| `/api/v1/operations/accesos`, `/api/v1/operations/observadores`, `/api/v1/operations/configuracion` | Catálogo y jornada |
| `/api/v1/operations/sesiones/**` | Programación, estados y registros |
| `GET /api/v1/operations/dashboard`, `GET /api/v1/operations/auditoria` | Indicadores e historial administrativo |

## Cuenta inicial y pruebas

Para crear las cuentas de demostración por rol, ejecuta *bash local/crear-usuarios-iniciales.sh* con el backend iniciado. El script pedirá la contraseña de cada cuenta nueva sin mostrarla en pantalla.

Con los servicios arriba, desde `backend` en otra terminal:

```sh
export INTERNAL_SERVICE_TOKEN="$(cat .local/service-token)"
python3 local/probar.py admin@example.cl --crear --rol ADMINISTRADOR
```

La herramienta solicita la contraseña. Crea los demás roles desde Administración. Para probar con cuentas descartables y una instancia dedicada de XAMPP, ejecuta `python3 local/probar_operaciones.py`. Ejecuta `python3 local/probar_cuentas.py` también para comprobar perfil, edición de cuentas y vigencia del token tras cambiar contraseña. La prueba de operaciones modifica la jornada de la instancia y crea registros. Comprueba que el conteo del Observador aparece en la consulta del Supervisor, que los reintentos conservan un solo conteo y que las sesiones concurrentes respetan la disponibilidad.

`mvn clean verify` ejecuta las pruebas JUnit y produce los informes JaCoCo en los módulos con pruebas. Los informes completos muestran la cobertura alcanzada por módulo y permiten revisar las líneas faltantes.

Para medir la prueba HTTP aparte, inicia con `JACOCO_AGENT=/ruta/org.jacoco.agent-0.8.14-runtime.jar bash local/start.sh`. Detén el backend después de la prueba y genera el informe del módulo con `mvn -f operations-service/pom.xml jacoco:report -Djacoco.dataFile=target/jacoco-integracion.exec`.
