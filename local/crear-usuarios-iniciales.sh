#!/usr/bin/env bash
set -eu

directorio_local="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
exec python3 - "$directorio_local/../.local/service-token" <<'PY'
import getpass
import json
import os
import sys
import warnings
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import HTTPRedirectHandler, ProxyHandler, Request, build_opener

GATEWAY = "http://127.0.0.1:8080"
AUTH = "http://127.0.0.1:8081"
USUARIOS = "http://127.0.0.1:8082"
RUTA_USUARIOS = "/internal/users"

CUENTAS = (
    ("Administrador", "admin@rincon.cl", "ADMINISTRADOR"),
    ("Supervisor 1", "supervisor1@rincon.cl", "SUPERVISOR"),
    ("Supervisor 2", "supervisor2@rincon.cl", "SUPERVISOR"),
    ("Supervisor 3", "supervisor3@rincon.cl", "SUPERVISOR"),
    ("Observador 1", "observador1@rincon.cl", "OBSERVADOR"),
    ("Observador 2", "observador2@rincon.cl", "OBSERVADOR"),
    ("Observador 3", "observador3@rincon.cl", "OBSERVADOR"),
)


class ErrorInicializacion(Exception):
    pass


class SinRedirecciones(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


http = build_opener(ProxyHandler({}), SinRedirecciones())


def solicitar(base, ruta, esperado, *, token=None, datos=None, aceptar_404=False):
    metodo = "POST" if datos is not None else "GET"
    cabeceras = {"Accept": "application/json"}
    cuerpo = None
    if token is not None:
        cabeceras["X-Service-Token"] = token
    if datos is not None:
        cabeceras["Content-Type"] = "application/json"
        cuerpo = json.dumps(datos).encode("utf-8")

    solicitud = Request(base + ruta, data=cuerpo, headers=cabeceras, method=metodo)
    operacion = f"{metodo} {ruta}"
    try:
        with http.open(solicitud, timeout=10) as respuesta:
            if respuesta.status != esperado:
                raise ErrorInicializacion(f"{operacion} devolvió HTTP {respuesta.status}.")
            contenido = respuesta.read()
    except HTTPError as error:
        error.close()
        if aceptar_404 and error.code == 404:
            return None
        raise ErrorInicializacion(f"{operacion} devolvió HTTP {error.code}.") from None
    except (URLError, OSError):
        raise ErrorInicializacion(f"{operacion} no obtuvo respuesta del servicio.") from None

    try:
        datos_respuesta = json.loads(contenido)
    except ValueError:
        raise ErrorInicializacion(f"{operacion} devolvió JSON inválido.") from None
    if not isinstance(datos_respuesta, dict):
        raise ErrorInicializacion(f"{operacion} devolvió una respuesta inesperada.")
    return datos_respuesta


def obtener_token():
    token = os.environ.get("INTERNAL_SERVICE_TOKEN", "").strip()
    if not token:
        try:
            token = Path(sys.argv[1]).read_text(encoding="utf-8").strip()
        except (OSError, UnicodeError):
            raise ErrorInicializacion("No se pudo leer .local/service-token.") from None
    if not token or not token.isascii() or any(caracter.isspace() for caracter in token):
        raise ErrorInicializacion("El token interno está vacío o tiene un formato inválido.")
    return token


def inicializar(creadas, existentes):
    token = obtener_token()
    for servicio in ("auth", "users", "operations", "notifications"):
        salud = solicitar(GATEWAY, f"/health/services/{servicio}", 200)
        if salud.get("status") != "UP":
            raise ErrorInicializacion(f"El servicio {servicio} no está disponible.")

    pendientes = []
    diferencias = []
    for cuenta in CUENTAS:
        _, correo, rol = cuenta
        consulta = urlencode({"correo": correo})
        usuario = solicitar(
            USUARIOS, f"{RUTA_USUARIOS}/por-correo?{consulta}", 200,
            token=token, aceptar_404=True,
        )
        if usuario is None:
            pendientes.append(cuenta)
            continue
        correo_actual = usuario.get("correo")
        if not isinstance(correo_actual, str) or correo_actual.lower() != correo:
            raise ErrorInicializacion(f"User Service devolvió un correo distinto para {correo}.")
        rol_actual = usuario.get("rol")
        if rol_actual != rol:
            diferencias.append(f"{correo} tiene el rol {rol_actual}; se esperaba {rol}.")
            existentes.append(f"{correo} (rol distinto)")
        else:
            existentes.append(correo)

    for diferencia in diferencias:
        print(f"Aviso: {diferencia}", file=sys.stderr)

    contrasenas = {}
    with warnings.catch_warnings():
        warnings.simplefilter("error", getpass.GetPassWarning)
        for _, correo, _ in pendientes:
            try:
                contrasena = getpass.getpass(f"Contraseña para {correo}: ")
            except (EOFError, getpass.GetPassWarning):
                raise ErrorInicializacion("Abre una terminal interactiva para ingresar las contraseñas.") from None
            if not contrasena.strip() or not 8 <= len(contrasena.encode("utf-8")) <= 72:
                raise ErrorInicializacion(f"La contraseña para {correo} debe tener entre 8 y 72 bytes.")
            contrasenas[correo] = contrasena

    for nombre, correo, rol in pendientes:
        try:
            usuario = solicitar(
                USUARIOS, RUTA_USUARIOS, 201, token=token,
                datos={"nombre": nombre, "correo": correo, "rol": rol, "activo": True},
            )
        except ErrorInicializacion as error:
            raise ErrorInicializacion(f"No se pudo confirmar la creación de {correo}. {error}") from None

        usuario_id = usuario.get("id")
        if (
            type(usuario_id) is not int or usuario_id <= 0
            or usuario.get("correo") != correo or usuario.get("rol") != rol
            or usuario.get("activo") is not True
        ):
            raise ErrorInicializacion(
                f"Cuenta incompleta: {correo}. User Service devolvió datos inesperados tras el alta."
            )

        try:
            credencial = solicitar(
                AUTH, "/internal/credentials", 201, token=token,
                datos={"usuarioId": usuario_id, "contrasena": contrasenas[correo]},
            )
            if credencial.get("usuarioId") != usuario_id:
                raise ErrorInicializacion("Auth Service devolvió un identificador inesperado.")
        except ErrorInicializacion as error:
            raise ErrorInicializacion(
                f"Cuenta incompleta: {correo} (ID {usuario_id}). "
                f"No se pudo confirmar el registro de su credencial. {error}"
            ) from None

        creadas.append(correo)

    return 1 if diferencias else 0


creadas = []
existentes = []
try:
    salida = inicializar(creadas, existentes)
except ErrorInicializacion as error:
    print(f"Error: {error}", file=sys.stderr)
    salida = 1

print(f"Cuentas creadas ({len(creadas)}): {', '.join(creadas) or 'ninguna'}")
print(f"Cuentas que ya existían ({len(existentes)}): {', '.join(existentes) or 'ninguna'}")
sys.exit(salida)
PY
