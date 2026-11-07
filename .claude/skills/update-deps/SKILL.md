---
name: update-deps
description: Monter une ou plusieurs dépendances du projet (librairie Maven, plugin de build, JDK, action GitHub) à partir d'une version annoncée, d'une PR Renovate collée ou citée par son numéro, du Dependency Dashboard, ou d'une demande « fais le bump ». Lit les release notes, croise les breaking changes avec l'usage RÉEL du code, bumpe, adopte les nouvelles API, compile, teste, et rend un rapport. S'améliore lui-même à chaque exécution.
---

# Montée de version de dépendance

## Objectif

Monter une dépendance **sans régression silencieuse**, et en profiter pour **aligner le code sur la
nouvelle version**, pas seulement le faire compiler.

Trois règles centrales :

- **Un breaking change ne compte que s'il touche ce que le projet utilise vraiment.** La plupart des
  changements annoncés sont hors sujet ; le travail consiste à le prouver, pas à le supposer.
- **Un changement de valeur par défaut est plus dangereux qu'une API supprimée.** Une API supprimée
  casse à la compilation ; un défaut qui change compile, passe souvent les tests, et casse en prod.
- **Ici, compiler contre une version ne dit pas laquelle tourne.** Une bonne part des dépendances
  est `provided` ou déjà présente dans le proxy (voir « Où tourne quoi »). Le build vert ne prouve
  rien sur la version que le serveur charge réellement.

Travailler une dépendance (ou un groupe couplé) à la fois, de bout en bout. Commiter localement,
un commit par dépendance, mais ne **jamais pousser** sans accord : le projet livre par étapes,
l'utilisateur teste sur son réseau entre deux.

Lire d'abord la section **MAJ en attente** en bas de ce fichier.

## Où tourne quoi

Avant tout bump, situer la dépendance dans ce tableau. C'est lui qui dit où chercher la casse.

| Dépendance | Portée | Qui la fournit à l'exécution |
|---|---|---|
| `velocity-api`, `bungeecord-api` | `provided` | le proxy |
| `packetevents-api` | `provided`, optionnelle | le plugin PacketEvents s'il est installé (jamais shadé) |
| `gson` | `provided` | le proxy |
| `adventure-*`, `slf4j-api` | `compile`, exclus du shade Velocity | Velocity : le proxy. BungeeCord : notre jar (il n'a pas Adventure) |
| `bungeecord-config` | `compile`, exclu du shade BungeeCord | BungeeCord : le proxy. Velocity : notre jar |
| `Pterodactyl4J`, okhttp, okio, `org.json` | `compile` / `runtime`, shadés | notre jar, partout |
| Guava | exclu à la source | personne ne l'appelle (vérifié au bytecode) |

Les deux proxys chargent en **parent-first** (code de leur `PluginClassLoader` lu) : une classe
que le proxy embarque masque toujours la nôtre. Une nouvelle dépendance d'exécution se compare
donc aux jars des proxys (BungeeCord : jars imbriqués dans `META-INF/libraries/`), et la
vérification passe par un vrai proxy : aucun test ne voit une classe manquante. Méthode et
commandes : [jar-audit.md](jar-audit.md).
| `junit-jupiter`, `mockito-core`, `snakeyaml` | `test` | personne en prod |
| plugins Maven, actions, JDK | build | la CI / le poste |

Conséquence : pour une dépendance `provided`, la question n'est pas « la nouvelle version
compile-t-elle ? » mais « les proxys que les utilisateurs font tourner fournissent-ils cette
version ? ». Compiler contre une API plus récente que celle du proxy donne un `NoSuchMethodError`
en prod, jamais en test.

Inversement, **`provided` ne veut pas dire « absent du jar » pour ses transitives** : quand une
autre dépendance tire la même librairie en `compile`, Maven garde la version la plus proche et
la portée la plus large. C'est ainsi que `velocity-api` choisit la version de Guava et de slf4j
shadés dans le jar Velocity (détail dans [velocity-api.md](velocity-api.md)).

## Déroulé

### 1. Cadrer l'usage réel (toujours en premier)

- Les versions sont des propriétés du `pom.xml` racine (`<properties>`), gérées dans
  `<dependencyManagement>` et `<pluginManagement>`. Les modules n'en déclarent aucune.
- Chercher **toutes les autres références** à la version ou à ses contraintes :
  `grep -rn "<artefact>\|<version>" --include=*.md --include=*.yml --include=*.json5 .`
  (README, AGENTS.md, workflows, `renovate.json5`).
- Lister ce que le code utilise réellement :
  `grep -rhn "^import <package>" --include=*.java . | sort | uniq -c | sort -rn`.
  Distinguer direct et transitif : `mvn -q dependency:tree -pl panel-auto-starter-common`.
- Repérer les **couplages** (section « À ne pas bumper sans réfléchir »).

### 2. Lire les release notes — OBLIGATOIRE avant de toucher un fichier

- Pour **chaque version intermédiaire** entre l'actuelle et la cible : release notes, changelog,
  section breaking changes / migration. `gh release list -R <owner>/<repo>` puis
  `gh release view <tag> -R <owner>/<repo>` les donne en entier, sans troncature.
- Si une PR Renovate est fournie, vérifier qu'elle propose bien la **dernière** version
  (`gh pr view <n>` ; le Dependency Dashboard liste aussi les MAJ en attente d'approbation).
- Pas de notes publiées : lire le diff entre les deux tags
  (`gh api repos/<o>/<r>/compare/<a>...<b>`) et le dire dans le rapport.
- Changelog long : le récupérer en entier dans le scratchpad et y chercher localement
  (`breaking|removed|deprecat|default|migrat|java|jdk|protocol`) plutôt que le survoler.
- La branche principale amont est parfois en retard sur les releases : chercher aussi dans les
  branches de maintenance.

### 3. Évaluer l'impact (le cœur du travail)

Croiser chaque point des notes avec l'usage relevé au §1 :

1. **API supprimées, renommées, dépréciées** : sommes-nous concernés ?
2. **Exigences d'environnement** : JDK minimal, et **version du bytecode livré**
   (`javap -verbose -cp <jar> <classe> | grep major` : 65 = Java 21, 69 = Java 25). Une
   dépendance `compile` en bytecode > 65 casse le jar BungeeCord sur Java 21.
3. **Changements de défaut** : les chercher activement. Pour chacun, le code fixe-t-il la valeur
   (sans impact) ou s'appuie-t-il sur le défaut (suivre en connaissance de cause, ou épingler
   l'ancienne valeur avec un commentaire qui dit pourquoi) ?
4. **Comportement à l'exécution** : sérialisation (MOTD persistés en JSON Adventure), threads
   (invariants 3 à 5 d'AGENTS.md : rien ne bloque le thread réseau), rendu des traductions
   (invariant 9), paquets construits à la main (`menu/chest`, **non couvert par les tests**).
5. **Version fournie par le proxy** pour tout ce qui n'est pas shadé (tableau ci-dessus).
6. **Conflits transitifs** : `mvn dependency:tree -Dverbose` sur les modules concernés.

Le changelog n'est pas la vérité : pour un point subtil, lire le **code source** de la version
cible. Rendre un verdict explicite par point : « concerné / pas concerné, parce que… ».

### 4. Bumper

Une seule propriété dans le `pom.xml` racine, plus les références jumelles du §1. Monter dans le
même lot ce que l'exigence d'environnement impose. Les fichiers suivis sont en CRLF : préférer
l'outil Edit, ou des motifs `\r?\n`.

Si le bump vient d'une PR Renovate, travailler sur sa branche (`gh pr checkout <n>`) pour que
la PR reflète le travail, ou en local puis fermer la PR : le demander à l'utilisateur.

### 5. Adopter les nouveautés de la version

- Remplacer les API dépréciées par leurs successeurs.
- Adopter les nouvelles API ou options utiles au projet quand les notes les recommandent.
- **Vérifier la couverture avant de généraliser** : un nouveau pattern ne couvre pas forcément
  tous les cas.
- Au-delà d'une quinzaine de sites, scripter le changement, puis faire une passe de cohérence.
- Commentaires et Javadoc en **français**, qui expliquent pourquoi (AGENTS.md, *Conventions*).

### 6. Vérifier

- `mvn clean verify -P release` avec **JDK 25**, à lire dans `mvn -v` : le `java` du shell
  peut être un autre JDK (1.8 sur ce poste) sans que Maven l'utilise.
- **Zéro nouveau warning.** Faire le build de référence sur `master` **avant** de bumper, puis
  comparer en neutralisant les numéros de ligne :

  ```bash
  mvn -B clean verify -P release > build-before.log 2>&1   # avant le bump
  mvn -B clean verify -P release > build-after.log 2>&1    # après
  norm(){ grep WARN "$1" | sed -E 's/:[0-9]+:/:N:/; s/[0-9]+ warnings/N warnings/' | sort; }
  diff <(norm build-before.log) <(norm build-after.log)
  ```

  Le diff montre aussi les jars embarqués par le shade (warnings « overlapping ») : un
  changement de version là, c'est le contenu du jar livré qui change.
- Si `common` ne trouve plus **ses propres classes** à la compilation des tests alors que le
  bump ne le touche pas : relancer avant de chercher. Vu une fois, disparu au second build,
  probablement l'IDE qui réécrivait `target/` en parallèle.
- Un test vert ne prouve rien sur un changement de défaut : écrire un test ciblé sur chaque
  comportement identifié au §3, et **le vérifier en cassant le code** (AGENTS.md, *Working
  method* : copie de côté, mutation qui échoue si le motif est absent, une seule classe de test,
  restauration, `diff`).
- Les quatre contrôles d'AGENTS.md, *Releasing* (bytecode 65, `mattmalec` confiné,
  PacketEvents non shadé, deux bundles `lang/messages`), sur le jar nommé, pas sur un glob.
- Ce qui ne se teste pas ici (menus coffre, comportement sur un vrai proxy) : le dire, et
  proposer à l'utilisateur ce qu'il doit essayer sur son réseau.

### 7. Rapporter

Pour chaque dépendance : version avant/après, ce qui a été **lu** (liens vers les notes ou le
compare), les points d'impact et leur verdict, ce qui a été modifié, ce qui a été vérifié et
comment (« retirer X tue N tests »), et ce qui reste à prouver sur le réseau de l'utilisateur.

Proposer un message de commit Conventional Commits (`chore(deps): …`, `chore(deps-dev): …`
pour les tests et plugins), avec un corps qui dit ce qui a été lu et vérifié. Commit non signé :
`git -c commit.gpgsign=false commit`.

Une montée impossible pour l'instant (bloquant mesuré) ne se masque pas en la désactivant dans
`renovate.json5` : la consigner dans **MAJ en attente** ci-dessous (quoi, pourquoi, comment
vérifier que c'est levé), à relire à chaque exécution.

### 8. Améliorer ce skill (dernière étape, systématique)

Ce skill est un **document vivant**. Avant de rendre le rapport :

- **Ajouter** les pièges rencontrés pendant ce run, surtout ceux qui ont fait perdre du temps ou
  cassé en silence, et les commandes qui ont **réellement** marché.
- **Élaguer** autant qu'ajouter : supprimer ce qui s'est révélé faux, obsolète ou trop spécifique ;
  fusionner les pièges proches en une règle générale.
- **Corriger** toute instruction que le run a prouvée inexacte.
- Tenir à jour « À ne pas bumper sans réfléchir ».
- **Garder le skill court** (< ~400 lignes). Au-delà, un fichier annexe par dépendance piégeuse
  dans ce dossier, et un pointeur ici.

Ne documenter que du **vérifié pendant le run**, jamais du supposé. Signaler la mise à jour (ou
l'absence de mise à jour) du skill dans le rapport final.

## À ne pas bumper sans réfléchir

- **`velocity-api`** : son bytecode impose le JDK de build (4.0.0 = Java 25). Il suit les
  versions du jeu, comme **PacketEvents** : les deux se montent ensemble quand une version de
  Minecraft sort, sinon un client trop récent reçoit des paquets dans l'ancien format sans
  qu'aucune erreur ne le signale (AGENTS.md, *Gotchas*). Release notes quasi absentes :
  méthode de lecture et pièges dans [velocity-api.md](velocity-api.md).
- **`maven.compiler.release` (21)** : ce n'est pas une dépendance, ne pas le monter pour suivre
  un JDK. Il garde le jar BungeeCord chargeable sur Java 21.
- **`adventure.version`** : une propriété pour six artefacts. Une majeure touche le rendu des
  traductions (invariant 9) et le format JSON des MOTD persistés.
- **`bungeecord.version`** : partagée par `bungeecord-api` et `bungeecord-config`.
- **`mockito.version`** : attaché en `-javaagent` via `maven-dependency-plugin` (argLine de
  surefire). Un changement de packaging de l'agent casse tous les tests d'un coup.
- **`Pterodactyl4J`** : dépôt hors Central (`repo.mattmalec.com`), numérotation `2.BETA_<n>`.
  Confiné à `panel/pterodactyl/` (invariant 1). Il sert aussi pour Pelican, qu'il ne vise pas :
  relire les appels HTTP réellement faits, pas seulement l'API Java.
- **JDK de la CI** : LTS seulement (règle dans `renovate.json5`).

## Renovate

Configuration : `renovate.json5` à la racine. MAJ hebdomadaires (lundi matin, Europe/Paris),
actions GitHub non majeures groupées, `velocity-api` et PacketEvents prioritaires et annotés.
Les dépendances qui partagent une propriété Maven sont regroupées par Renovate lui-même.

Valider une modification de la config sans pousser (Node requis) :

```bash
npm i --prefix <scratchpad>/renov renovate
<scratchpad>/renov/node_modules/.bin/renovate-config-validator --strict renovate.json5
# Dry-run sur le dépôt local ; le fichier de config doit être suivi par git (git add -N).
LOG_LEVEL=debug LOG_FORMAT=json RENOVATE_BASE_DIR=<scratchpad>/renov-cache \
  <scratchpad>/renov/node_modules/.bin/renovate --platform=local --dry-run=lookup > dry.jsonl
```

Le message `packageFiles with updates` du log liste chaque dépendance et les MAJ proposées. Les
nombreux `error fetching versions … not-found` sont normaux : Renovate interroge chaque dépôt du
pom et seul celui qui héberge l'artefact répond. Sans `GITHUB_COM_TOKEN`, les actions GitHub
sortent en `github-token-required` : seules les MAJ Maven sont vérifiables ainsi.

## MAJ en attente

_Aucune pour l'instant._

<!-- Format : ### <dépendance> <actuelle> → <cible>
     Bloquant : … (mesuré comment)
     Levé quand : … (commande ou signal à vérifier) -->
