#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

umask 077
mkdir -p .local
estado="$(cd .local && pwd -P)"
if [[ ! -s "$estado/jwt-private.pem" ]]; then
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$estado/jwt-private.pem"
fi
if [[ ! -s "$estado/jwt-public.pem" ]]; then
    openssl pkey -in "$estado/jwt-private.pem" -pubout -out "$estado/jwt-public.pem"
fi
if [[ ! -s "$estado/service-token" ]]; then
    openssl rand -hex 32 > "$estado/service-token"
fi
ruta_recursos="$estado"
case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) ruta_recursos="/$(cygpath -m "$estado")" ;;
esac
export JWT_PRIVATE_KEY="${JWT_PRIVATE_KEY:-file:$ruta_recursos/jwt-private.pem}"
export JWT_PUBLIC_KEY="${JWT_PUBLIC_KEY:-file:$ruta_recursos/jwt-public.pem}"
export INTERNAL_SERVICE_TOKEN="${INTERNAL_SERVICE_TOKEN:-$(cat "$estado/service-token")}"

servicios=(eureka-server user-service auth-service operations-service notification-service api-gateway)
for servicio in "${servicios[@]}"; do
    if [[ ! -f "$servicio/target/$servicio-0.0.1-SNAPSHOT.jar" ]]; then
        echo "Falta compilar $servicio. Ejecuta mvn clean verify desde backend." >&2
        exit 1
    fi
done

logs=$(mktemp -d "$estado/logs.XXXXXXXX")
procesos=()
detener() {
    local indice
    for indice in "${!procesos[@]}"; do
        kill "${procesos[$indice]}" 2>/dev/null || true
    done
    wait || true
}
trap detener EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

ejecutar() {
    local servicio=$1 direccion=${2:-127.0.0.1}
    set -- -jar "$servicio/target/$servicio-0.0.1-SNAPSHOT.jar"
    if [[ -n "${JACOCO_AGENT:-}" ]]; then
        set -- "-javaagent:$JACOCO_AGENT=destfile=$servicio/target/jacoco-integracion.exec,append=false,includes=cl.duocuc.rinconsur.*" "$@"
    fi
    SPRING_PROFILES_ACTIVE=local SERVER_ADDRESS="$direccion" \
        java "$@" > "$logs/$servicio.log" 2>&1 &
    procesos+=("$!")
}

ejecutar eureka-server
for ((intento=0; intento<60; intento++)); do
    if curl -fsS --max-time 2 http://127.0.0.1:8761/eureka/apps >/dev/null 2>&1; then break; fi
    if ! kill -0 "${procesos[0]}" 2>/dev/null; then
        echo "Eureka se detuvo. Revisa $logs/eureka-server.log." >&2
        exit 1
    fi
    sleep 1
done
curl -fsS --max-time 2 http://127.0.0.1:8761/eureka/apps >/dev/null
ejecutar user-service
ejecutar auth-service
ejecutar operations-service
ejecutar notification-service
ejecutar api-gateway "${GATEWAY_BIND:-127.0.0.1}"
echo "Iniciando servicios. Registros en $logs."

comprobar_procesos() {
    for indice in "${!procesos[@]}"; do
        if ! kill -0 "${procesos[$indice]}" 2>/dev/null; then
            echo "Se detuvo ${servicios[$indice]}. Revisa $logs." >&2
            exit 1
        fi
    done
}

listo() {
    local servicio respuesta
    for servicio in auth users operations notifications; do
        respuesta=$(curl -fsS --max-time 2 "http://127.0.0.1:8080/health/services/$servicio" 2>/dev/null) || return 1
        [[ "$respuesta" == *'"status":"UP"'* ]] || return 1
    done
}

limite=$((SECONDS + 150))
until listo; do
    comprobar_procesos
    if ((SECONDS >= limite)); then
        echo "El Gateway todavía no llega a todos los servicios. Revisa $logs." >&2
        exit 1
    fi
    sleep 2
done

echo "Rincón Sur está listo. Gateway en http://127.0.0.1:8080 y Eureka en http://127.0.0.1:8761."
if [[ "${GATEWAY_BIND:-127.0.0.1}" != 127.0.0.1 ]]; then
    echo "Desde otros PC, usa http://IP_DEL_SERVIDOR:8080/."
fi
echo "Pulsa Ctrl+C para detener el backend."
while true; do
    comprobar_procesos
    sleep 2
done
