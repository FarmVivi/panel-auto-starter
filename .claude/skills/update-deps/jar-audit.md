# Audit du contenu des jars

Annexe du skill `update-deps`. Méthode vérifiée le 2026-11-07 (Velocity 4.3.1 build 45,
BungeeCord build 2111 en 26.1-R0.1-SNAPSHOT), quand les jars sont passés de 5,2 Mo à 1,2 Mo
(Velocity) et 2,1 Mo (BungeeCord). À refaire quand une dépendance d'exécution entre ou change de
portée, ou quand un bump fait bouger la taille d'un jar.

## Pourquoi ça compte

Les deux proxys chargent les classes d'un plugin en **parent-first** : `PluginClassLoader`
(Velocity) et `PluginClassloader` (BungeeCord) appellent `super.loadClass`, qui délègue au
chargeur du proxy avant de lire le jar du plugin. Une classe que le proxy embarque masque donc
toujours la nôtre. La shader ne sert à rien. La retirer ne change rien à l'exécution, **sauf**
si le proxy ne la fournit pas : là, `NoClassDefFoundError`, et seulement sur un vrai proxy.

## 1. Ce que nos jars embarquent

```bash
mvn -B -DskipTests compile dependency:list -DincludeScope=runtime   # dans le réacteur entier
```

Lancé avec `-pl <module>` seul, il échoue : `common` n'est pas résolu hors réacteur. Le shade ne
journalise pas ses inclusions, d'où cette commande.

## 2. Ce que les proxys fournissent

```bash
curl -s https://fill.papermc.io/v3/projects/velocity/versions/<version>/builds/latest  # -> downloads["server:default"].url
curl -sL -o BungeeCord.jar https://ci.md-5.net/job/BungeeCord/lastSuccessfulBuild/artifact/bootstrap/target/BungeeCord.jar
```

BungeeCord est un **bootstrap** : ses classes sont dans des jars imbriqués
(`unzip -q -j BungeeCord.jar 'META-INF/libraries/*.jar' -d bc-libs`). Lister le jar de tête ne
montre que trois classes, ce qui fait croire qu'il ne fournit rien.

Comparer classe par classe, groupé par paquet :

```bash
cls(){ unzip -Z1 "$1" | grep '\.class$' | grep -v '^META-INF/versions' | sort -u; }
cls notre.jar > ours.txt; cls velocity.jar > proxy.txt          # BungeeCord : boucler sur bc-libs/*.jar
comm -12 ours.txt proxy.txt > common.txt
# puis compter par paquet : classes chez nous / déjà dans le proxy
```

Un paquet à 100 % fourni s'exclut du shade du module concerné (`artifactSet/excludes`). Un paquet
partiellement fourni se garde.

## 3. Qui appelle vraiment une librairie

Une dépendance transitive peut n'être appelée par personne (Guava, tiré par le POM de
`bungeecord-config`). Le vérifier au bytecode plutôt qu'au POM :

```bash
javap -v -cp <dossier-du-jar-décompressé> <classes…> | grep -oE "com/google/common/[A-Za-z/]+" | sort | uniq -c
```

## 4. Prouver sur un vrai proxy

Scripts dans [jar-audit/](jar-audit/) : `run-proxy.sh` démarre un proxy dans Docker avec le
plugin, `ping.cjs` envoie un ping de statut. Lancer **l'ancien jar et le nouveau côte à côte**
et comparer les logs normalisés : la différence attendue est nulle.

La config par défaut suffit à exercer presque tout :

- le démarrage charge la config (`bungeecord-config` + snakeyaml), les traductions, et
  Pterodactyl4J fait une vraie requête vers `panel.exemple.com` (okhttp, `org.json`) ;
- le ping via un **forced host** qui pointe vers `lobby` passe par le MOTD du plugin
  (Velocity : `lobby.example.com` par défaut ; BungeeCord : remplacer `pvp.md-5.net: pvp` par
  `pvp.md-5.net: lobby` dans `config.yml` puis redémarrer). Un ping sur l'adresse nue renvoie le
  MOTD du proxy et n'exerce pas le plugin ;
- `pas status` et `pas start lobby` dans `/tmp/in` exercent les commandes et le panel ;
- `end` puis `shutdown` arrêtent proprement.

Les jars de référence se construisent dans un worktree, sans toucher l'arbre de travail :
`git worktree add <scratchpad>/wt HEAD`, `mvn -q -DskipTests package` dedans, puis
`git worktree remove --force`.

Bruit connu, présent avec les deux jars : BungeeCord logue un `SEVERE … ErrorManager` après la
désactivation des plugins, et signale un premier ping à plus de 50 ms.
