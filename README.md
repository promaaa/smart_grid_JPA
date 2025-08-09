## Routes implémentées

### Routes d'ingress
- **POST `/ingress/windturbine`** : Reçoit les données des éoliennes (ID, timestamp, vitesse, puissance)
- **UDP port 12345** : Reçoit les données des panneaux solaires au format `id:temperature:power:timestamp`

### Routes API 

#### Gestion des utilisateurs
- **GET `/persons`** : Liste tous les IDs des utilisateurs
- **GET `/person/:id`** : Récupère les détails d'un utilisateur spécifique
- **POST `/person/:id`** : Met à jour un utilisateur existant
- **DELETE `/person/:id`** : Supprime un utilisateur
- **PUT `/person`** : Crée un nouvel utilisateur

#### Gestion des grilles
- **GET `/grids`** : Liste tous les IDs des grilles
- **GET `/grid/:id`** : Récupère les détails d'une grille spécifique
- **GET `/grid/:id/production`** : Calcule la production totale d'une grille
- **GET `/grid/:id/consumption`** : Calcule la consommation totale d'une grille

#### Gestion des capteurs
- **GET `/sensor/:id`** : Récupère les détails d'un capteur spécifique
- **GET `/sensors/:kind`** : Liste tous les capteurs d'un type spécifique (SolarPanel, WindTurbine, EVCharger)
- **GET `/consumers`** : Liste tous les capteurs consommateurs
- **GET `/producers`** : Liste tous les capteurs producteurs
- **POST `/sensor/:id`** : Met à jour un capteur existant

#### Gestion des mesures
- **GET `/measurement/:id`** : Récupère les détails d'une mesure spécifique
- **GET `/measurement/:id/values`** : Récupère les valeurs d'une mesure avec possibilité de filtrage par période

## Détails des tests effectués

### Tests de Connectivité et d'Initialisation (Backend)
- Vérification de l'injection et de l'utilisation correcte de l'`EntityManager` dans les handlers.
- Validation de la création du schéma de base de données et du chargement des données initiales via `init_database.sql`.

### Tests Manuels (API Backend avec Postman ou similaire)
- **Endpoints de Listing (GET)**: Test de `/grids`, `/persons`, `/sensors/:kind`, `/consumers`, `/producers` pour la correction et la complétude des données 
- **Endpoints de Détail (GET `/:id`)**: Test de `/person/:id`, `/grid/:id`, `/sensor/:id` (pour chaque type) avec des IDs valides, invalides, et non-existants. Vérification du chargement des données des tables jointes.
- **Endpoints de Mesures (GET)**: Test de `/measurement/:id` et `/measurement/:id/values` (avec et sans filtres temporels).
- **Endpoints de Calcul (GET)**: Test de `/grid/:id/production` et `/grid/:id/consumption`.
- **Endpoints d'Écriture (CRUD)**:
    - `PUT /person`: création (payloads valides/invalides).
    - `POST /person/:id` et `POST /sensor/:id`: modification (IDs existants/non-existants).
    - `DELETE /person/:id`: suppression.
- **Endpoints d'Ingress**:
    - `POST /ingress/windturbine`: persistance des `DataPoint`, calcul de `total_energy_produced` (attention au bogue potentiel).
    - Réception UDP (panneaux solaires): persistance des `DataPoint`, calcul d'énergie.
- **Robustesse et Gestion d'Erreurs**:
    - Tests des réponses pour les cas d'erreur (400, 404, 500).
    - Vérification de la validation des payloads (attention au bogue listé).
    - Comportement transactionnel (rollback en cas d'erreur).

### Tests avec le frontend
- Vérification de l'affichage et de la modification des données utilisateurs.
- Visualisation des données des capteurs et des grilles.
- Création et suppression d'utilisateurs via l'interface.
- Validation globale de l'interaction Frontend-Backend et de la cohérence des données persistées.

## Bogues résiduels identifiés
- Dans la route `/consumers` et `/producers`, certaines informations spécifiques des capteurs manquent (valeurs par défaut retournées)
- Problème potentiel dans le calcul de l'énergie totale produite pour les éoliennes
- Absence de validation complète des payloads JSON pour certaines routes
