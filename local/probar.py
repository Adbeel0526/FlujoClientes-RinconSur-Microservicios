import argparse
import getpass
import json
import os
from urllib.error import HTTPError
from urllib.request import Request, urlopen


GATEWAY = "http://127.0.0.1:8080"
AUTH = "http://127.0.0.1:8081"
USERS = "http://127.0.0.1:8082"


def llamar(base, ruta, esperado, datos=None, cabeceras=None, metodo=None):
    cuerpo = json.dumps(datos).encode() if datos is not None else None
    solicitud = Request(
        base + ruta,
        data=cuerpo,
        method=metodo,
        headers={"Content-Type": "application/json", **(cabeceras or {})},
    )
    try:
        with urlopen(solicitud, timeout=10) as respuesta:
            estado = respuesta.status
            contenido = respuesta.read()
    except HTTPError as error:
        estado = error.code
        contenido = b""
    if estado != esperado:
        raise SystemExit(f"{solicitud.get_method()} {ruta}: HTTP {estado}, se esperaba {esperado}")
    return json.loads(contenido) if contenido else None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("correo")
    parser.add_argument("--crear", action="store_true")
    parser.add_argument("--rol", choices=("ADMINISTRADOR", "SUPERVISOR", "OBSERVADOR"), default="OBSERVADOR")
    args = parser.parse_args()
    contrasena = getpass.getpass("Contraseña de la cuenta de prueba: ")
    if not 8 <= len(contrasena.encode()) <= 72:
        raise SystemExit("Usa entre 8 y 72 bytes UTF-8 para la contraseña de prueba.")

    for servicio in ("auth", "users", "operations", "notifications"):
        salud = llamar(GATEWAY, f"/health/services/{servicio}", 200)
        if salud["status"] != "UP":
            raise SystemExit(f"{servicio} todavía no está listo")

    if args.crear:
        interno = {"X-Service-Token": os.environ["INTERNAL_SERVICE_TOKEN"]}
        usuario = llamar(USERS, "/internal/users", 201, {
            "nombre": f"{args.rol.capitalize()} de prueba", "correo": args.correo, "rol": args.rol,
        }, interno)
        llamar(AUTH, "/internal/credentials", 201, {
            "usuarioId": usuario["id"], "contrasena": contrasena,
        }, interno)

    login = "/api/v1/auth/login"
    perfil = "/api/v1/auth/me"
    llamar(GATEWAY, "/internal/users", 404)
    llamar(GATEWAY, "/internal/credentials", 404, {})
    llamar(GATEWAY, perfil, 401)
    llamar(GATEWAY, perfil, 401, cabeceras={"Authorization": "Bearer invalido"})
    llamar(GATEWAY, login, 401, {"correo": args.correo, "contrasena": contrasena + "x" if len(contrasena.encode()) < 72 else "Otra-contrasena-valida"})
    acceso = llamar(GATEWAY, login, 200, {"correo": args.correo, "contrasena": contrasena})
    usuario = llamar(GATEWAY, perfil, 200, cabeceras={"Authorization": f"Bearer {acceso['accessToken']}"})
    if usuario["correo"].lower() != args.correo.lower() or not usuario["activo"]:
        raise SystemExit("El perfil devuelto no coincide con la cuenta activa de prueba.")
    print(f"Verificación correcta: cuatro servicios disponibles, login y perfil vía Gateway ({usuario['rol']}).")


if __name__ == "__main__":
    main()
