"""Comprueba cuentas, perfil y contraseña en una instancia local de prueba."""

import os
import secrets

from probar import AUTH, GATEWAY, USERS, llamar
from probar_operaciones import crear_usuario


def main():
    token = {"X-Service-Token": os.environ["INTERNAL_SERVICE_TOKEN"]}
    clave = secrets.token_urlsafe(20)
    correo = f"admin.{secrets.token_hex(4)}@example.cl"
    uid, admin = crear_usuario(token, correo, "Admin HTTP", "ADMINISTRADOR", clave)
    base = "/api/v1/auth"
    assert llamar(GATEWAY,base+"/perfil",200,cabeceras=admin)["id"] == uid
    perfil = {"nombre":"Nombre editado","apellido":"Apellido","correo":correo,"telefono":"+56912345678","foto":"/9j//9k="}
    assert llamar(GATEWAY,base+"/perfil",200,perfil,admin,"PUT")["foto"] == perfil["foto"]
    assert llamar(GATEWAY,base+"/admin/usuarios",200,cabeceras=admin)["totalUsuarios"] >= 1
    otro_correo = f"obs.{secrets.token_hex(4)}@example.cl"
    otro = llamar(GATEWAY,base+"/admin/usuarios",201,
        {"nombre":"Usuario editable","correo":otro_correo,"rol":"OBSERVADOR","contrasena":clave},admin)
    ruta = base+f"/admin/usuarios/{otro['id']}"
    assert llamar(GATEWAY,ruta,200,{"nombre":"Ascendido","correo":otro_correo,"rol":"SUPERVISOR"},admin,"PUT")["rol"] == "SUPERVISOR"
    llamar(GATEWAY,ruta+"/estado",200,{"activo":False},admin,"PUT")
    assert llamar(GATEWAY,base+"/admin/usuarios/auditoria",200,cabeceras=admin)
    llamar(GATEWAY,base+"/login",401,{"correo":otro_correo,"contrasena":clave})
    llamar(GATEWAY,base+"/recuperacion",202,{"correo":correo})
    llamar(GATEWAY,base+"/recuperacion",400,{"codigo":"0"*64,"nueva":clave},metodo="PUT")
    nueva = secrets.token_urlsafe(20)
    llamar(GATEWAY,base+"/contrasena",204,{"actual":clave,"nueva":nueva},admin,"PUT")
    llamar(GATEWAY,base+"/me",401,cabeceras=admin)
    llamar(GATEWAY,base+"/login",200,{"correo":correo,"contrasena":nueva})
    llamar(GATEWAY,"/internal/users",404)
    llamar(AUTH,"/internal/credentials",401,{"usuarioId":uid,"contrasena":clave})
    llamar(USERS,"/internal/users",401)
    print("Perfil, alta/edición/estado de cuentas, recuperación y renovación de sesión verificados.")


if __name__ == "__main__":
    main()
