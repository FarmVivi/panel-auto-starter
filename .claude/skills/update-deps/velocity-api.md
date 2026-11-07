# velocity-api

Annexe du skill `update-deps`. Tout ce qui suit a été vérifié lors du passage 4.2.0 → 4.3.1
(2026-11-07).

## Trouver ce qui a changé

PaperMC ne publie presque pas de release GitHub (4.3.0 n'en a pas) et ne tague pas toutes les
versions (pas de `v4.2.0`). La source fiable est l'historique de `gradle.properties` : chaque
version y a son commit « Release X.Y.Z ».

```bash
gh api "repos/PaperMC/Velocity/commits?path=gradle.properties&sha=<tag-ou-branche>&per_page=15" \
  --jq '.[] | .sha[0:10] + " " + .commit.committer.date[0:10] + " " + (.commit.message|split("\n")[0])'
gh api "repos/PaperMC/Velocity/compare/<sha-release-actuelle>...<sha-release-cible>" > compare.json
```

Dans `compare.json`, ne lire en détail que les fichiers sous `api/` (le contrat qu'on compile) et
`gradle/libs.versions.toml` (les versions d'Adventure, Guava, Gson, slf4j que l'API tire). Les
commits sous `proxy/` se jugent par leur message, puis par leur patch s'ils touchent une phase où
le plugin agit : ping (status), connexion au serveur initial, arrêt.

Les versions publiées, y compris celles sans tag :
`curl -s https://repo.papermc.io/repository/maven-public/com/velocitypowered/velocity-api/maven-metadata.xml`

## Ce qu'elle fournit à l'exécution

Velocity embarque Adventure, slf4j, Guava et Gson ; le shade Velocity les exclut (voir
[jar-audit.md](jar-audit.md)). Un bump qui change la version d'Adventure dans
`libs.versions.toml` change donc celle que le plugin utilise sur Velocity, quelle que soit
`adventure.version` : la comparer à la nôtre.

## Compatibilité avec les proxys plus anciens

Compiler contre une version plus récente ne casse un proxy plus ancien que si le code appelle une
API ajoutée entre-temps. Chercher les `@since` des méthodes ajoutées dans le diff de `api/`, puis
les chercher dans `panel-auto-starter-velocity`. Le réseau de production tourne en Velocity 4.1.
