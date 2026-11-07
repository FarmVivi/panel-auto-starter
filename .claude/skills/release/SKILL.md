---
name: release
description: Préparer et publier une release GitHub de PanelAutoStarter (vX.Y.Z) — choix du numéro, inventaire des changements depuis le dernier tag, vérifications du jar livré, rédaction des notes pour les propriétaires de serveurs dans la forme maison, publication avec gh, contrôle des jars attachés. À utiliser pour « sors une version », « fais la release », « prépare les notes de version ». S'améliore et s'élague à chaque passage.
---

# Release

## Objectif

Publier une version que les propriétaires de serveurs peuvent installer **sans surprise** : des
notes qui disent ce qui change pour eux, ce qui ne change pas, et toute action manuelle ; des
jars vérifiés là où les tests ne regardent pas.

Deux règles :

- **Rien ne sort sans accord explicite.** Un tag et une release sont publics et notifient les
  abonnés. Montrer le numéro, le titre et les notes complètes, puis attendre le « go ».
- **Les notes sont écrites pour le propriétaire de serveur, pas pour le changelog.** Il veut
  savoir : que dois-je faire, qu'est-ce qui change pour mes joueurs, qu'est-ce qui reste pareil.

Lire d'abord **Pièges** et **Journal des passages** en bas de ce fichier.

## Déroulé

### 1. État et numéro

```bash
git fetch --tags -q && git status -sb | head -1     # master à jour, arbre propre
git describe --tags --abbrev=0                       # dernier tag publié
gh release list --limit 3
mvn -q help:evaluate -Dexpression=project.version -DforceStdout
git log --oneline <dernier-tag>..HEAD
```

- **Patch** (Z) : maintenance, dépendances, correctifs sans réglage nouveau.
- **Mineure** (Y) : fonctionnalité, réglage ajouté, comportement visible qui change.
- **Majeure** (X) : migration manuelle obligatoire ou plateforme abandonnée.

La version de `master` est un `-SNAPSHOT` que le workflow `Release Build` dépouille. Elle n'est
**pas** remontée après une release : si elle vaut déjà la version publiée (`3.4.2-SNAPSHOT`
après la sortie de `v3.4.2`), la monter vers la cible avant de taguer, sinon le jar sortirait
sous l'ancien numéro.

```bash
mvn versions:set -DnewVersion=X.Y.Z-SNAPSHOT -DgenerateBackupPoms=false -DprocessAllModules
```

### 2. Inventaire des changements

Partir des commits, pas de la mémoire : `git log <tag>..HEAD --format='%h %s%n%b'`. Les corps de
commit contiennent le *pourquoi* et ce qui a été mesuré : c'est la matière des notes. Classer
chaque commit :

| Classe | Va dans les notes ? |
|---|---|
| Visible des joueurs ou des admins | oui, section dédiée |
| Réglage ajouté / renommé / supprimé | oui, avec le bloc YAML et le défaut |
| Dépendance livrée ou fournie par le proxy | oui, avec ce que ça change (souvent : rien) |
| Dépendance de test ou plugin de build | une ligne dans « Everything else », portée précisée |
| Outillage du dépôt (CI, Renovate, skills, AGENTS.md) | non, sauf effet sur l'utilisateur |

Vérifier le diff de `config.yml` et `lang/` depuis le tag : c'est lui qui dit si « No setting is
added, removed or renamed » est vrai.

```bash
git diff <tag>..HEAD --stat -- '*config.yml' '*lang/*'
```

### 3. Vérifier ce qui sera livré

```bash
mvn clean verify -P release        # JDK 25, lire mvn -v
```

Puis les contrôles d'AGENTS.md, *Releasing*, sur **le jar nommé** (jamais un glob : quatre jars
dans `target/`) :

```bash
for m in velocity bungee; do
  J=panel-auto-starter-$m/target/panel-auto-starter-$m-X.Y.Z-SNAPSHOT.jar
  echo "$m $(stat -c %s $J) octets"
  unzip -l $J | grep -c retrooper/packetevents   # 0
  unzip -l $J | grep -c lang/messages            # 2
  unzip -l $J | grep -c com/google/common        # 0
done
unzip -l panel-auto-starter-velocity/target/panel-auto-starter-velocity-X.Y.Z-SNAPSHOT.jar | grep -c net/kyori   # 0
javap -verbose -cp panel-auto-starter-velocity/target/classes \
  fr.farmvivi.panelautostarter.velocity.VelocityPlugin | grep major     # 65
grep -rn mattmalec panel-auto-starter-common/src/main/java | grep -v /panel/pterodactyl/   # rien
```

Une taille de jar qui bouge beaucoup depuis la release précédente se comprend avant de publier
(`gh release view <tag> --json assets -q '.assets[] | .name + " " + (.size|tostring)'`).

### 4. Rédiger les notes

En **anglais**, dans un fichier du scratchpad. La forme est stable depuis v3.0.0 ; la suivre :

```markdown
<Une ou deux phrases : quel genre de release, pour qui. Si c'est vrai, le dire tôt :
"Nothing changes in what the plugin does, and nothing changes in your configuration.">

## <Un sujet par section, titré par ce que le lecteur y gagne>

<Le pourquoi, avec les faits mesurés. Les blocs YAML des réglages nouveaux, avec leur défaut.
Ce que le lecteur doit faire, en **gras**.>

## Everything else, current

- <dépendance> <version> (<test only | build only | optional at runtime | provided by the proxy>)

## Upgrading from X.Y.(Z-1)

Drop in the new jar. <Réglages ajoutés / retirés / renommés, ou "No setting is added, removed or
renamed, `config.yml` and `lang/` are untouched">, and **nothing changes for your players** <sauf …>.

Velocity users: `ping-passthrough` should still be `disabled`, as in 3.1.0.

## Requirements

<"Unchanged since 3.1.0." ou ce qui change, en gras>

| Platform | Minimum version |
|---|---|
| BungeeCord | 1.21+, Java 21 |
| Velocity | **4.0+, Java 25** (required by Velocity 4 itself) |
| Panel | Pterodactyl or Pelican |
| PacketEvents | optional, for inventory menus |

**Full Changelog**: https://github.com/FarmVivi/panel-auto-starter/compare/<tag-précédent>...vX.Y.Z
```

Titre : `vX.Y.Z - <ce que le propriétaire de serveur y gagne>`, court, sans verbe à
l'impératif (« Minecraft 26.3 clients, dependencies refreshed »).

Ton : phrases complètes, pas de liste de commits, pas de jargon interne (« invariant 6 »). Dire
explicitement ce qui **ne** change **pas** : un comportement nouveau désactivé par défaut ne
rassure que s'il est écrit. Toute affirmation technique doit venir d'un corps de commit ou d'une
vérification faite pendant ce passage.

Relire les notes de la release précédente (`gh release view <tag>`) pour garder le ton et ne pas
contredire ce qu'elle annonçait.

### 5. Faire valider

Montrer à l'utilisateur : numéro, titre, notes complètes, et la liste des commits couverts.
Attendre son accord explicite. Corriger et remontrer si besoin.

### 6. Publier

Seulement après accord :

```bash
git add pom.xml */pom.xml && git -c commit.gpgsign=false commit -m "chore: release X.Y.Z"  # si la version a bougé
git push origin master
gh release create vX.Y.Z --target master --title "vX.Y.Z - …" --notes-file <notes.md>
```

### 7. Contrôler après publication

Le workflow `Release Build` construit et attache les deux jars.

```bash
gh run list --workflow "Release Build" --limit 1
gh run watch <id> --exit-status
gh release view vX.Y.Z --json assets -q '.assets[] | .name + " " + (.size|tostring)'
```

Attendu : exactement `panel-auto-starter-bungee-X.Y.Z.jar` et
`panel-auto-starter-velocity-X.Y.Z.jar`, sans `-SNAPSHOT`, de tailles proches de celles
mesurées au §3. Télécharger le jar Velocity publié et relancer sur lui les deux comptages
`net/kyori` et `lang/messages` : c'est ce jar-là que les gens installent.

### 8. Entretenir ce skill (dernière étape, systématique)

- **Ajouter** ce que le passage a appris et qui a été vérifié : piège, commande qui a marché,
  correction d'une instruction fausse.
- **Élaguer** : une instruction qui n'a servi à rien dans les **trois derniers passages** du
  journal, et qui ne garde pas contre un piège connu, est supprimée ou condensée. Une
  vérification devenue impossible à rater (garantie par un test, un workflow) sort aussi.
- **Mettre à jour** le gabarit des notes si la forme a évolué pendant ce passage.
- **Rester court** : moins de ~300 lignes. Au-delà, déplacer dans une annexe de ce dossier.
- **Consigner** le passage dans le journal (une ligne), en gardant les cinq derniers seulement.

Signaler dans le rapport final ce qui a été ajouté, élagué ou corrigé dans le skill.

## Pièges

- **Version déjà publiée sur `master`.** `3.4.2-SNAPSHOT` alors que `v3.4.2` existe : ne pas
  taguer sans monter la version (§1).
- **Glob sur `target/*-SNAPSHOT.jar`** : quatre jars, `unzip -l` refuse et les comptages
  affichent 0. Toujours nommer le jar.
- **Annonces devenues fausses.** Les notes jusqu'à v3.4.2 disent que Pterodactyl4J est vérifié à
  la main parce que Dependabot ne l'indexe pas ; Renovate le suit désormais. Ne pas recopier
  une phrase d'une release précédente sans la revérifier.

## Journal des passages

| Date | Version | Ce que le passage a changé dans le skill |
|---|---|---|
| — | — | Skill créé le 2026-11-07, pas encore exécuté. |
