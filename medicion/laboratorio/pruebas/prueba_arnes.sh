#!/bin/bash
set -u
AQUI="$(cd "$(dirname "$0")" && pwd)"
ARNES="$AQUI/../../escritura/carga_escritura.sh"
FIX="$AQUI/fixture"
TMP="$(mktemp -d)"
FALLOS=0
extraer() { sed -n "/^$1() {/,/^}/p" "$ARNES"; }
ok() { echo "ok   - $1"; }
mal() { echo "FALLA - $1"; FALLOS=$((FALLOS + 1)); }
igual() { if [ "$2" = "$3" ]; then ok "$1"; else mal "$1 (esperado '$3', obtenido '$2')"; fi; }

log() { echo "[log] $*" >> "$TMP/log.txt"; }
fallo() { echo "FALLO: $*" >> "$TMP/log.txt"; exit 1; }
for f in flags_de a_mb opciones_jvm aplicar_perfil verificar_k6 fijar_dockerfile; do eval "$(extraer $f)"; done
. "$AQUI/../imagenes.sh"
BASE_JVM="-XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=128m"
OBSERVACION_JVM="-Xlog:gc:file=/tmp/jvm_gc.log"
XMX=1g
DESPUES_DIR="$FIX"
SAL="$TMP"

igual "flags_de lee ENV JAVA_OPTS del Dockerfile de producción" "$(flags_de "$FIX")" "-Xmx192m -XX:+UseSerialGC -Xss512k -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -XX:TieredStopAtLevel=1 -XX:CICompilerCount=1 -XX:MaxDirectMemorySize=32m"
flags_de "$TMP/no_existe" > /dev/null 2>&1 && mal "flags_de debe fallar sin Dockerfile" || ok "flags_de falla sin Dockerfile"
igual "a_mb 2g" "$(a_mb 2g)" "2048"
igual "a_mb 512m" "$(a_mb 512m)" "512"

opciones_jvm P
case "$JAVA_OPTS_LAB" in "-Xmx192m -XX:+UseSerialGC "*"-XX:MaxDirectMemorySize=32m -Xlog:gc:file=/tmp/jvm_gc.log") ok "configuración P = flags de producción + observación" ;; *) mal "configuración P: $JAVA_OPTS_LAB" ;; esac
opciones_jvm D
case "$JAVA_OPTS_LAB" in "-Xmx1g "*) ok "configuración D conserva -Xmx\$XMX" ;; *) mal "configuración D: $JAVA_OPTS_LAB" ;; esac

PERFIL_LAB=CAPACIDAD-2G; PERFIL_EFECTIVO=CAPACIDAD-2G; MEMORIA=2g; CPUS=1; CFG_NIVEL=D; CFG_CONTROL=E; CPUS_PREP=1; MEM_PREP=2g; UMBRAL_GENERADOR_CPU_PCT=80; HEAP_EN_VIVO=1
aplicar_perfil
igual "CAPACIDAD-2G no cambia memoria" "$MEMORIA/$XMX/$CFG_NIVEL/$CFG_CONTROL/$PERFIL_EFECTIVO" "2g/1g/D/E/CAPACIDAD-2G"

PERFIL_LAB=CAPACIDAD-2G; PERFIL_EFECTIVO=CAPACIDAD-2G; MEMORIA=2g; XMX=1g; CPUS=2
aplicar_perfil
case "$PERFIL_EFECTIVO" in "CAPACIDAD-2G con parámetros distintos"*"CPU 2, memoria 2g, heap 1g)") ok "CAPACIDAD-2G con 2 CPU se rotula como variante" ;; *) mal "rótulo de variante: $PERFIL_EFECTIVO" ;; esac
CPUS=1

PERFIL_LAB=PRODUCCION-512M; PERFIL_EFECTIVO=PRODUCCION-512M; MEMORIA=2g; XMX=1g
aplicar_perfil
igual "PRODUCCION-512M fija memoria, heap y configuración" "$MEMORIA/$XMX/$CFG_NIVEL/$CFG_CONTROL/$CPUS_PREP/$MEM_PREP" "512m/192m/P/P/1/2g"

PERFIL_LAB=PRODUCCION-512M; MEMORIA=2g; XMX=1g; DESPUES_DIR="$FIX"
( VARIANTES="D:a F:b" aplicar_perfil ) > /dev/null 2>&1 && mal "PRODUCCION-512M con VARIANTES debe fallar" || ok "PRODUCCION-512M con VARIANTES falla"
( MODO_CORTE=1 aplicar_perfil ) > /dev/null 2>&1 && mal "PRODUCCION-512M con corte debe fallar" || ok "PRODUCCION-512M con corte falla"
( ESCENARIO_VISUAL=caida aplicar_perfil ) > /dev/null 2>&1 && mal "PRODUCCION-512M con visual debe fallar" || ok "PRODUCCION-512M con visual falla"
( DESPUES_DIR="$TMP" aplicar_perfil ) > /dev/null 2>&1 && mal "PRODUCCION-512M sin Dockerfile debe fallar" || ok "PRODUCCION-512M sin Dockerfile falla"
PERFIL_LAB=OTRO
( aplicar_perfil ) 2> /dev/null && mal "perfil desconocido debe fallar" || ok "perfil desconocido falla"
PERFIL_LAB=CAPACIDAD-2G; UMBRAL_GENERADOR_CPU_PCT=abc
( aplicar_perfil ) 2> /dev/null && mal "umbral no numérico debe fallar" || ok "umbral no numérico falla"
UMBRAL_GENERADOR_CPU_PCT=65.5; HEAP_EN_VIVO=1; MEMORIA=2g
aplicar_perfil && ok "umbral decimal aceptado" || mal "umbral decimal"

IMG_MAVEN="maven:3.9.6-eclipse-temurin-21-alpine@sha256:aaaa"; IMG_JRE="eclipse-temurin:21-jre-alpine@sha256:bbbb"
fijar_dockerfile
igual "Dockerfile fijado reemplaza las dos bases" "$(grep '^FROM' "$TMP/Dockerfile.fijado" | tr '\n' '|')" "FROM maven:3.9.6-eclipse-temurin-21-alpine@sha256:aaaa AS build|FROM eclipse-temurin:21-jre-alpine@sha256:bbbb|"
igual "Dockerfile fijado conserva el resto del archivo" "$(diff <(grep -v '^FROM' "$FIX/Dockerfile.measurement") <(grep -v '^FROM' "$TMP/Dockerfile.fijado") | wc -l | tr -d ' ')" "0"

mkdir -p "$TMP/otro"; printf 'FROM alpine:3\nRUN true\n' > "$TMP/otro/Dockerfile.measurement"
DESPUES_DIR="$TMP/otro"; SAL="$TMP"; BASES_FIJADAS=sí
fijar_dockerfile
igual "base desconocida: aviso y sin fijar" "$BASES_FIJADAS" "no"
( BASES_ESTRICTAS=1 fijar_dockerfile ) > /dev/null 2>&1 && mal "modo estricto debe fallar" || ok "modo estricto falla con base desconocida"
unset BASES_ESTRICTAS

K6_VERSION=2.4.0
k6() { echo "k6 v2.4.0 (commit/dd9395906d, go1.26.11, linux/amd64)"; }
verificar_k6 && ok "k6 en la versión fijada" || mal "k6 en la versión fijada"
k6() { echo "k6 v2.3.0 (commit/e088784614, go1.26.8, linux/amd64)"; }
( verificar_k6 ) > /dev/null 2>&1 && mal "k6 de otra versión debe fallar" || ok "k6 de otra versión falla"
K6_VERSION_LIBRE=1 verificar_k6 && ok "K6_VERSION_LIBRE permite otra versión" || mal "K6_VERSION_LIBRE"

rm -rf "$TMP"
[ "$FALLOS" = 0 ] && echo "todas las comprobaciones pasaron" || { echo "$FALLOS comprobaciones fallaron"; exit 1; }
