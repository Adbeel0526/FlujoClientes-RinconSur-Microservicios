from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, time, timedelta
import getpass
import os
from uuid import uuid4
from zoneinfo import ZoneInfo

from probar import AUTH, GATEWAY, USERS, llamar


def crear_usuario(interno, correo, nombre, rol, contrasena):
    usuario = llamar(USERS, "/internal/users", 201, {
        "nombre": nombre, "correo": correo, "rol": rol
    }, interno)
    llamar(AUTH, "/internal/credentials", 201, {
        "usuarioId": usuario["id"], "contrasena": contrasena
    }, interno)
    respuesta = llamar(GATEWAY, "/api/v1/auth/login", 200, {"correo": correo, "contrasena": contrasena})
    return usuario["id"], {"Authorization": f"Bearer {respuesta['accessToken']}"}


def operacion(ruta, esperado=200, token=None, datos=None, metodo=None):
    return llamar(GATEWAY, f"/api/v1/operations/{ruta}", esperado, datos, token, metodo)


def main():
    interno = {"X-Service-Token": os.environ["INTERNAL_SERVICE_TOKEN"]}
    contrasena = getpass.getpass("Contraseña de las cuentas desechables: ")
    if not 8 <= len(contrasena.encode()) <= 72:
        raise SystemExit("Usa entre 8 y 72 bytes UTF-8.")

    sufijo = uuid4().hex[:10]
    personas = {}
    for clave, rol in (
        ("admin", "ADMINISTRADOR"), ("supervisor", "SUPERVISOR"),
        ("observador", "OBSERVADOR"), ("otro", "OBSERVADOR"), ("inactivo", "OBSERVADOR"),
    ):
        personas[clave] = crear_usuario(interno, f"{clave}.{sufijo}@example.cl", clave, rol, contrasena)
    admin = personas["admin"][1]
    supervisor_id, supervisor = personas["supervisor"]
    observador_id, observador = personas["observador"]
    otro_id, otro = personas["otro"]
    inactivo_id, inactivo = personas["inactivo"]

    llamar(USERS, f"/internal/users/{inactivo_id}/estado", 200, {"activo": False}, interno, "PATCH")
    operacion("sesiones", 401, inactivo)
    operacion("sesiones", 401)
    operacion("dashboard", 401, {"Authorization": "Bearer invalido"})
    operacion("accesos", 403, supervisor, {"nombre": "Prohibido"})
    operacion("observadores", 403, observador)
    configuracion = operacion("configuracion", 200, admin)
    horario = {"apertura": "08:00", "cierre": "22:00", "minimo": 30, "maximo": 60}
    operacion("configuracion", 403, supervisor, horario, "PUT")
    operacion("configuracion", 400, admin, {**horario, "maximo": 20}, "PUT")
    operacion("configuracion", 400, admin, {**horario, "cierre": "08:40"}, "PUT")
    operacion("configuracion", 400, admin, {**horario, "apertura": "22:00"}, "PUT")
    operacion("configuracion", 200, admin, horario, "PUT")
    acceso = operacion("accesos", 201, admin, {"nombre": f"Acceso {sufijo}"})
    otro_acceso = operacion("accesos", 201, admin, {"nombre": f"Otro acceso {sufijo}"})
    operacion("accesos", 409, admin, {"nombre": f" acceso {sufijo.upper()} "})
    acceso_inactivo = operacion("accesos", 201, admin, {"nombre": f"Inactivo {sufijo}"})
    operacion(f"accesos/{acceso_inactivo['id']}", 200, admin, {
        "nombre": acceso_inactivo["nombre"], "activo": False
    }, "PUT")
    assert acceso in operacion("accesos", 200, supervisor)
    assert all(a["activo"] for a in operacion("accesos", 200, supervisor))
    nuevo = llamar(GATEWAY, "/api/v1/auth/admin/usuarios", 201, {
        "nombre": "Nuevo observador", "correo": f"nuevo.{sufijo}@example.cl",
        "rol": "OBSERVADOR", "contrasena": contrasena,
    }, admin)
    assert nuevo["id"] in {u["id"] for u in operacion("observadores", 200, admin)}
    vistos = {u["id"] for u in operacion("observadores", 200, supervisor)}
    assert {observador_id, otro_id, nuevo["id"]} <= vistos
    assert not {inactivo_id, supervisor_id} & vistos

    ahora = datetime.now(ZoneInfo(configuracion["zona"]))
    fecha = ahora.date() + timedelta(days=1)
    def fecha_hora(hora, minuto=0, dia=fecha):
        return datetime.combine(dia, time(hora, minuto)).isoformat(timespec="minutes")

    datos = {
        "accesoId": acceso["id"], "observadorId": observador_id,
        "inicio": fecha_hora(10), "fin": fecha_hora(11),
    }
    operacion("sesiones", 403, observador, datos)
    operacion("sesiones", 403, admin, datos)
    for invalido in (
        {"inicio": fecha_hora(10, dia=fecha - timedelta(days=2))},
        {"inicio": (ahora - timedelta(minutes=10)).replace(tzinfo=None).isoformat(timespec="minutes")},
        {"inicio": fecha_hora(7, 45), "fin": fecha_hora(8, 30)},
        {"inicio": fecha_hora(21, 45), "fin": fecha_hora(22, 15)},
        {"fin": fecha_hora(10)}, {"fin": fecha_hora(10, 29)}, {"fin": fecha_hora(11, 1)},
        {"accesoId": acceso_inactivo["id"]}, {"observadorId": inactivo_id},
        {"observadorId": supervisor_id},
    ):
        operacion("sesiones", 400, supervisor, {**datos, **invalido})

    sesion = operacion("sesiones", 201, supervisor, datos)
    mensaje_pausa = ("El observador ya tiene una sesión en ese horario o a menos de 10 minutos. "
                     "Deja al menos 10 minutos entre sesiones.")
    mensaje_ocupado = "Tienes otra sesión en curso o debes esperar 10 minutos desde que terminó."
    assert codigo_operacion(supervisor, {
        **datos, "inicio": fecha_hora(10, 15), "fin": fecha_hora(11, 15)
    }, mensaje_pausa) == 409
    assert codigo_operacion(supervisor, datos, mensaje_pausa) == 409
    assert codigo_operacion(
        supervisor, {**datos, "accesoId": otro_acceso["id"]}, mensaje_pausa
    ) == 409
    sid = sesion["id"]
    detalle = f"sesiones/{sid}"
    assert [s["id"] for s in operacion("sesiones", 200, observador)] == [sid]
    assert operacion("sesiones", 200, otro) == []
    operacion(f"{detalle}/estado/EN_CURSO", 404, otro, metodo="PUT")
    operacion(f"{detalle}/estado/EN_CURSO", 403, supervisor, metodo="PUT")
    operacion(f"{detalle}/estado/FINALIZADA", 409, observador, metodo="PUT")
    assert operacion(f"{detalle}/estado/EN_CURSO", 200, observador, metodo="PUT")["estado"] == "EN_CURSO"
    for categoria in ("TRANSITO", "VISITA", "NO_DETERMINADO"):
        registro = {"id": str(uuid4()), "categoria": categoria}
        operacion(f"{detalle}/registros", 200, observador, registro)
        operacion(f"{detalle}/registros", 200, observador, registro)
    final = operacion(f"{detalle}/estado/FINALIZADA", 200, observador, metodo="PUT")
    assert final["conteo"] == {"transito": 1, "visita": 1, "noDeterminado": 1, "total": 3}
    operacion(f"{detalle}/estado/FINALIZADA", 409, observador, metodo="PUT")
    operacion(f"{detalle}/estado/EN_CURSO", 409, observador, metodo="PUT")
    assert any(s["id"] == sid and s["estado"] == "FINALIZADA" for s in operacion("sesiones", 200, supervisor))
    assert operacion("sesiones?historial=true", 200, observador) == [final]
    assert operacion("dashboard", 200, observador)["conteo"]["total"] == 3
    global_supervisor = operacion("dashboard", 200, supervisor)
    assert any(d["id"] == acceso["id"] and d["conteo"]["total"] == 3 for d in global_supervisor["porAcceso"])
    assert any(d["id"] == str(observador_id) and d["conteo"]["total"] == 3 for d in global_supervisor["porObservador"])
    assert operacion("dashboard", 200, admin)["usuariosActivos"] >= 4

    pausa_antes = {**datos, "inicio": fecha_hora(9, 20), "fin": fecha_hora(9, 50)}
    pausa_despues = {
        **datos, "accesoId": otro_acceso["id"],
        "inicio": fecha_hora(11, 10), "fin": fecha_hora(11, 40),
    }
    assert codigo_operacion(
        supervisor, {**pausa_antes, "fin": fecha_hora(9, 51)}, mensaje_pausa
    ) == 409
    demasiado_pronto = {**pausa_despues, "inicio": fecha_hora(11, 9)}
    assert codigo_operacion(supervisor, demasiado_pronto, mensaje_pausa) == 409
    operacion("sesiones", 201, supervisor, pausa_antes)
    contigua = operacion("sesiones", 201, supervisor, pausa_despues)
    ruta_contigua = f"sesiones/{contigua['id']}"
    assert codigo_operacion(
        admin, demasiado_pronto, mensaje_pausa, ruta_contigua, "PUT"
    ) == 409
    assert codigo_operacion(observador, mensaje=mensaje_ocupado,
                            ruta=f"{ruta_contigua}/estado/EN_CURSO", metodo="PUT") == 409
    assert contigua in operacion("sesiones", 200, observador)

    competidora = {**datos, "observadorId": otro_id}
    segunda_competidora = {**demasiado_pronto, "observadorId": otro_id}
    with ThreadPoolExecutor(max_workers=2) as piscina:
        lotes = list(piscina.map(
            lambda candidata: codigo_operacion(supervisor, candidata),
            (competidora, segunda_competidora),
        ))
    assert sorted(lotes) == [201, 409]
    otra = operacion("sesiones", 200, otro)[0]
    ruta = f"sesiones/{otra['id']}"
    nueva = {**competidora, "inicio": fecha_hora(11), "fin": fecha_hora(12)}
    operacion(ruta, 200, supervisor, nueva, "PUT")
    corregida = {**nueva, "observadorId": nuevo["id"], "inicio": fecha_hora(21, 30), "fin": fecha_hora(22)}
    operacion(ruta, 403, observador, corregida, "PUT")
    assert operacion(ruta, 200, admin, corregida, "PUT")["observadorId"] == nuevo["id"]
    operacion(f"{ruta}/estado/CANCELADA", 403, observador, metodo="PUT")
    operacion(f"{ruta}/estado/CANCELADA", 200, admin, metodo="PUT")
    operacion(ruta, 409, admin, corregida, "PUT")
    operacion(f"{ruta}/estado/EN_CURSO", 404, otro, metodo="PUT")
    assert operacion("sesiones", 201, supervisor, corregida)["id"] != otra["id"]
    jornada_anterior = operacion("configuracion", 200, supervisor)
    jornada_nueva = {"apertura": "12:00", "cierre": "22:00", "minimo": 10, "maximo": 40}
    operacion("configuracion", 200, admin, jornada_nueva, "PUT")
    guardada = operacion("configuracion", 200, supervisor)
    assert time.fromisoformat(guardada["apertura"]) == time(12)
    assert time.fromisoformat(guardada["cierre"]) == time(22)
    assert (guardada["minimo"], guardada["maximo"]) == (10, 40)
    assert jornada_anterior != guardada
    assert codigo_operacion(
        supervisor, datos, "La sesión debe quedar dentro del horario laboral de 12:00 a 22:00"
    ) == 400
    borde = {**datos, "inicio": fecha_hora(12)}
    assert codigo_operacion(
        supervisor, {**borde, "fin": fecha_hora(12, 9)}, "La sesión no puede durar menos de 10 minutos"
    ) == 400
    assert codigo_operacion(
        supervisor, {**borde, "fin": fecha_hora(12, 41)}, "La sesión no puede durar más de 40 minutos"
    ) == 400
    minima = operacion("sesiones", 201, supervisor, {**borde, "fin": fecha_hora(12, 10)})
    maxima = operacion("sesiones", 201, supervisor, {
        **borde, "inicio": fecha_hora(21, 20), "fin": fecha_hora(22),
    })
    assert minima["estado"] == maxima["estado"] == "PROGRAMADA"
    assert final in operacion("sesiones?historial=true", 200, observador)
    operacion("configuracion", 200, admin, horario, "PUT")

    reservas = []
    for entrada, inicio, minuto, fin, minuto_fin in (
        (acceso, 16, 0, 16, 40), (otro_acceso, 16, 50, 17, 30),
    ):
        reservas.append(operacion("sesiones", 201, supervisor, {
            "accesoId": entrada["id"], "observadorId": otro_id,
            "inicio": fecha_hora(inicio, minuto), "fin": fecha_hora(fin, minuto_fin),
        })["id"])
    with ThreadPoolExecutor(max_workers=2) as piscina:
        inicios = list(piscina.map(
            lambda id_sesion: codigo_operacion(otro, mensaje=mensaje_ocupado,
                                                ruta=f"sesiones/{id_sesion}/estado/EN_CURSO", metodo="PUT"),
            reservas,
        ))
    assert sorted(inicios) == [200, 409]
    abierta = reservas[inicios.index(200)]
    pendiente = reservas[inicios.index(409)]
    operacion(f"sesiones/{abierta}/estado/FINALIZADA", 200, otro, metodo="PUT")
    assert codigo_operacion(otro, mensaje=mensaje_ocupado,
                            ruta=f"sesiones/{pendiente}/estado/EN_CURSO", metodo="PUT") == 409
    assert any(s["id"] == pendiente and s["estado"] == "PROGRAMADA"
               for s in operacion("sesiones", 200, otro))

    operacion("auditoria", 403, supervisor)
    auditoria = operacion("auditoria", 200, admin)
    acciones = {evento["accion"] for evento in auditoria if evento["referencia"] == sid}
    assert acciones == {"SESION_PROGRAMADA", "SESION_INICIADA", "SESION_FINALIZADA"}
    assert any(evento["accion"] == "CONFIGURACION_ACTUALIZADA" for evento in auditoria)
    print(f"Operations verificado. Cuenta Android: supervisor.{sufijo}@example.cl")
    print(f"Cuenta observadora: observador.{sufijo}@example.cl. Acceso: {acceso['nombre']}.")


def codigo_operacion(token, datos=None, mensaje=None, ruta="sesiones", metodo="POST"):
    from urllib.error import HTTPError
    from urllib.request import Request, urlopen
    import json

    solicitud = Request(
        f"{GATEWAY}/api/v1/operations/{ruta}",
        json.dumps(datos).encode() if datos is not None else None,
        {"Content-Type": "application/json", **token},
        method=metodo,
    )
    try:
        with urlopen(solicitud, timeout=15) as respuesta:
            return respuesta.status
    except HTTPError as error:
        if mensaje is not None:
            assert json.load(error)["message"] == mensaje
        return error.code


if __name__ == "__main__":
    main()
