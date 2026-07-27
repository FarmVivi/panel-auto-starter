# PanelAutoStarter

Plugin BungeeCord et Velocity qui démarre et arrête automatiquement vos serveurs Minecraft en fonction du nombre de joueurs, via l'API de votre panel.

Compatible **Pterodactyl** et **Pelican**.

> Vous venez de PterodactylAutoStarter 2.x ? La procédure de migration est détaillée dans les [notes de la release 3.0.0](https://github.com/FarmVivi/panel-auto-starter/releases) — attention, le dossier de données change de nom.

## Fonctionnalités

- Démarrage automatique des serveurs quand un joueur se connecte
- Arrêt automatique des serveurs inactifs
- Système de file d'attente pour gérer les connexions pendant le démarrage
- Ping personnalisé (favicon et MOTD) reflétant l'état du serveur : hors-ligne, en démarrage
- Compatible BungeeCord et Velocity

## Compatibilité

| | Versions supportées |
|---|---|
| Panel | Pterodactyl, Pelican |
| BungeeCord | 1.21+, Java 21 ou supérieur |
| Velocity | 4.0+, Java 25 ou supérieur (exigé par Velocity 4 lui-même) |

## Installation

1. Téléchargez le plugin correspondant à votre proxy :
   - `panel-auto-starter-bungee-<version>.jar` pour BungeeCord
   - `panel-auto-starter-velocity-<version>.jar` pour Velocity
2. Placez le fichier `.jar` dans le dossier `plugins`
3. Démarrez le proxy une première fois pour générer la configuration
4. Éditez le `config.yml` généré, puis redémarrez le proxy

## Configuration

```yaml
panel:
  # pterodactyl ou pelican
  type: pterodactyl
  url: https://panel.exemple.com
  token: ptlc_xxxxxxxx

queue:
  # Serveur d'attente où les joueurs patientent pendant le démarrage
  server: lobby

servers:
  lobby:
    id: id_du_serveur_dans_le_panel
```

Le `token` doit être un token d'API **client**, créé depuis votre compte utilisateur sur le panel — pas un token d'API application.

**Important :** le nom du serveur (ex. `lobby`) doit correspondre exactement au nom déclaré dans la configuration de votre proxy (BungeeCord ou Velocity). C'est ce qui permet au plugin de faire la liaison entre la configuration du proxy et les serveurs du panel.

### Configuration du proxy Velocity

Réglez `ping-passthrough` sur `disabled` dans `velocity.toml` :

```toml
ping-passthrough = "disabled"
```

C'est le plugin qui se charge de faire remonter le MOTD des serveurs backend, hôte par hôte. Velocity, lui, ne sait pas distinguer son adresse principale d'un forced host : réglé sur `"all"`, il affiche le MOTD du premier serveur de la liste `try` — souvent le limbo — y compris quand un joueur ping l'adresse du proxy.

Avec `disabled`, chacun retrouve son MOTD : le proxy sur son adresse, chaque backend sur son forced host, et les états « hors-ligne » / « démarrage » du plugin quand le serveur n'est pas disponible.

Bénéfice annexe : Velocity n'interroge plus un backend à chaque ping client, le plugin servant depuis son cache.

### Réglages du démarrage

```yaml
server-start:
  # Fréquence de vérification de l'état du serveur, en secondes
  check-interval-normal: 15
  # Même chose, mais pendant un démarrage (vérification plus rapprochée)
  check-interval-startup: 3
  # Délai avant de commencer à téléporter les joueurs une fois le serveur en ligne
  wait-before-teleport: 5
  # Délai entre deux téléportations, pour éviter de surcharger le serveur
  teleport-delay: 1
```

## Architecture

Projet Maven multi-module :

| Module | Rôle |
|---|---|
| `panel-auto-starter-common` | Logique métier, abstraction du panel, chargement de la configuration |
| `panel-auto-starter-bungee` | Implémentation BungeeCord |
| `panel-auto-starter-velocity` | Implémentation Velocity |

### Support des deux panels

L'accès au panel passe par les interfaces `PanelClient` et `PanelServer` (package `fr.farmvivi.panelautostarter.panel`), qui n'exposent **aucun type** de la bibliothèque sous-jacente.

L'API client de Pelican étant volontairement maintenue compatible avec celle de Pterodactyl, une seule implémentation — adossée à [Pterodactyl4J](https://github.com/mattmalec/Pterodactyl4J) — sert les deux panels aujourd'hui. `PelicanPanelClient` étend l'implémentation Pterodactyl sans rien surcharger ; il existe comme point d'extension pour le jour où Pelican fera diverger un endpoint.

## Build

```bash
mvn clean package
```

**Le JDK 25 est requis pour compiler.** `velocity-api` 4.0.0 est distribué en bytecode Java 25 (class file 69) : un JDK antérieur ne peut pas lire ses classes. Ce n'est pas un choix du projet, c'est une contrainte de Velocity 4.

Le bytecode produit cible Java 21, ce qui garde le jar BungeeCord chargeable sur un serveur en Java 21. Les serveurs Velocity 4 tournent de toute façon en Java 25.

Les jars sont produits dans `panel-auto-starter-bungee/target/` et `panel-auto-starter-velocity/target/`.

## Licence

Projet maintenu par FarmVivi
