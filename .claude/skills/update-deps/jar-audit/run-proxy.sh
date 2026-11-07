#!/bin/sh
# run-proxy.sh <velocity|bungee> <dossier-des-jars-proxy> <jar-du-plugin> <port-hote> <dossier-de-travail>
# Démarre le proxy avec le plugin dans Docker (JRE 25). La console lit /tmp/in :
#   docker exec pas-<velocity|bungee>-<port> sh -c 'echo "pas status" > /tmp/in'
# Arrêt propre : echo "end" (Velocity) ou "end"/"shutdown" (BungeeCord) dans /tmp/in.
# Sous Git Bash, exporter MSYS_NO_PATHCONV=1, sinon /srv devient un chemin Windows.
set -eu
P=$1; PROXIES=$2; PLUGIN=$3; PORT=$4; W=$5
if [ "$P" = velocity ]; then JAR=velocity.jar; CPORT=25565; else JAR=BungeeCord.jar; CPORT=25577; fi
rm -rf "$W"; mkdir -p "$W/plugins"
cp "$PROXIES/$JAR" "$W/proxy.jar"; cp "$PLUGIN" "$W/plugins/"
N=pas-$P-$PORT; docker rm -f "$N" >/dev/null 2>&1 || true
# Chemin absolu obligatoire : un chemin relatif fait créer un volume nommé, vide.
WIN=$(cd "$W" && (pwd -W 2>/dev/null || pwd))
docker run -d --name "$N" -p "$PORT:$CPORT" -v "$WIN:/srv" -w /srv eclipse-temurin:25-jre \
  sh -c 'mkfifo /tmp/in; (tail -f /tmp/in) | java -Xmx512m -jar proxy.jar' >/dev/null
echo "$N"
