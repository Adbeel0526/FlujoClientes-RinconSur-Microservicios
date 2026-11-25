#!/usr/bin/env bash
set -eu

# Conserva la entrada interactiva mientras Python lee el programa del heredoc.
exec python3 - 3<&0 <<'PY'
import getpass
import json
import os
import sys
import warnings
from urllib.error import HTTPError, URLError
from urllib.request import HTTPRedirectHandler, ProxyHandler, Request, build_opener
from uuid import UUID

GATEWAY = "http://127.0.0.1:8080"
LOGIN = "/api/v1/auth/login"
ACCESOS = "/api/v1/operations/accesos"
NOMBRES = ("Acceso 1", "Acceso 2", "Acceso 3", "Acceso 4")


class ErrorInicializacion(Exception):
    pass


class SinRedirecciones(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


http = build_opener(ProxyHandler({}), SinRedirecciones())


def llamar(ruta, esperado, *, datos=None, token=None, aceptar_conflicto=False):
    cabeceras = {"Accept": "application/json"}
    cuerpo = None
    if datos is not None:
        cabeceras["Content-Type"] = "application/json"
        cuerpo = json.dumps(datos).encode("utf-8")
    if token is not None:
        cabeceras["Authorization"] = f"Bearer {token}"

    solicitud = Request(GATEWAY + ruta, data=cuerpo, headers=cabeceras)
    operacion = f"{solicitud.get_method()} {ruta}"
    try:
        with http.open(solicitud, timeout=10) as respuesta:
            if respuesta.status != esperado:
                raise ErrorInicializacion(f"{operacion} devolvió HTTP {respuesta.status}.")
            contenido = respuesta.read()
    except HTTPError as error:
        error.close()
        if aceptar_conflicto and error.code == 409:
            return None
        raise ErrorInicializacion(f"{operacion} devolvió HTTP {error.code}.") from None
    except (URLError, OSError):
        raise ErrorInicializacion(f"{operacion} no obtuvo una respuesta completa.") from None

    try:
        resultado = json.loads(contenido)
    except ValueError:
        raise ErrorInicializacion(f"{operacion} devolvió JSON inválido.") from None
    if resultado is None:
        raise ErrorInicializacion(f"{operacion} devolvió una respuesta vacía.")
    return resultado


def iniciar_sesion():
    with os.fdopen(3, encoding="utf-8") as entrada:
        if not entrada.isatty():
            raise ErrorInicializacion("Ejecuta el script desde una terminal interactiva.")
        print("Correo del administrador: ", end="", file=sys.stderr, flush=True)
        correo = entrada.readline().strip()
        if not correo:
            raise ErrorInicializacion("Debes ingresar el correo del administrador.")
        with warnings.catch_warnings():
            warnings.simplefilter("error", getpass.GetPassWarning)
            try:
                contrasena = getpass.getpass("Contraseña del administrador: ")
            except getpass.GetPassWarning:
                raise ErrorInicializacion("La terminal no permite ocultar la contraseña.") from None

    sesion = llamar(LOGIN, 200, datos={"correo": correo, "contrasena": contrasena})
    if not isinstance(sesion, dict) or not isinstance(sesion.get("usuario"), dict):
        raise ErrorInicializacion("El login devolvió una respuesta inesperada.")
    if sesion["usuario"].get("rol") != "ADMINISTRADOR":
        raise ErrorInicializacion("La cuenta autenticada debe tener el rol ADMINISTRADOR.")
    token = sesion.get("accessToken")
    if not isinstance(token, str) or not token or not token.isascii() or any(c.isspace() for c in token):
        raise ErrorInicializacion("El login no devolvió un token de acceso válido.")
    return token


def listar(token):
    accesos = llamar(ACCESOS, 200, token=token)
    if not isinstance(accesos, list):
        raise ErrorInicializacion("El listado de accesos devolvió una respuesta inesperada.")

    por_nombre = {}
    for acceso in accesos:
        if (
            not isinstance(acceso, dict)
            or not isinstance(acceso.get("nombre"), str)
            or not isinstance(acceso.get("activo"), bool)
        ):
            raise ErrorInicializacion("El listado devolvió un acceso con datos inválidos.")
        por_nombre[acceso["nombre"].strip().lower()] = acceso
    return por_nombre


def crear_faltantes(token, creados, existentes, inactivos):
    por_nombre = listar(token)
    for nombre in NOMBRES:
        clave = nombre.lower()
        try:
            acceso = por_nombre.get(clave)
            if acceso is None:
                creado = llamar(
                    ACCESOS, 201, token=token,
                    datos={"nombre": nombre, "activo": True}, aceptar_conflicto=True,
                )
                if creado is not None:
                    if (
                        not isinstance(creado, dict)
                        or creado.get("nombre") != nombre
                        or creado.get("activo") is not True
                        or not isinstance(creado.get("id"), str)
                    ):
                        raise ErrorInicializacion("El alta devolvió datos inesperados.")
                    try:
                        UUID(creado["id"])
                    except ValueError:
                        raise ErrorInicializacion("El alta devolvió un UUID inválido.") from None
                    creados.append(nombre)
                    continue

                por_nombre = listar(token)
                acceso = por_nombre.get(clave)
                if acceso is None:
                    raise ErrorInicializacion("HTTP 409 y el acceso sigue ausente en el listado.")

            existentes.append(nombre)
            if not acceso["activo"]:
                inactivos.append(nombre)
        except ErrorInicializacion as error:
            raise ErrorInicializacion(f"Falló {nombre}. {error}") from None


def main():
    creados, existentes, inactivos = [], [], []
    salida = 0
    try:
        crear_faltantes(iniciar_sesion(), creados, existentes, inactivos)
    except ErrorInicializacion as error:
        print(f"Error: {error}", file=sys.stderr)
        salida = 1
    except (EOFError, KeyboardInterrupt):
        print("\nInicialización interrumpida.", file=sys.stderr)
        salida = 1

    for etiqueta, nombres in (
        ("Accesos creados", creados),
        ("Accesos existentes", existentes),
        ("Accesos existentes inactivos", inactivos),
    ):
        print(f"{etiqueta} ({len(nombres)}): {', '.join(nombres) or 'ninguno'}")
    return salida


if __name__ == "__main__":
    sys.exit(main())
PY
