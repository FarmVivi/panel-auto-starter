# PanelAutoStarter

Plugin pour BungeeCord et Velocity permettant de démarrer et arrêter automatiquement vos serveurs Pterodactyl en fonction du nombre de joueurs.

## Fonctionnalités

- Démarrage automatique des serveurs quand un joueur se connecte
- Arrêt automatique des serveurs inactifs
- Système de file d'attente pour gérer les connexions pendant le démarrage
- Compatible BungeeCord et Velocity

## Installation

1. Téléchargez le plugin correspondant à votre proxy (BungeeCord ou Velocity)
2. Placez le fichier `.jar` dans le dossier `plugins`
3. Configurez le fichier `config.yml` avec vos informations Pterodactyl
4. Redémarrez votre proxy

## Configuration

Éditez le fichier `config.yml` :

```yaml
pterodactyl:
  url: https://panel.exemple.com
  token: votre_token_api

servers:
  lobby:
    id: id_du_serveur_pterodactyl
```

**Important :** Le nom du serveur (ex: `lobby`) doit correspondre exactement au nom du serveur configuré dans le fichier de configuration de votre proxy (BungeeCord ou Velocity). C'est ce qui permet au plugin de faire la liaison entre la configuration du proxy et les serveurs Pterodactyl.

## Licence

Projet maintenu par FarmVivi
