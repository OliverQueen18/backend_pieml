# PIE ML - Backend

Plateforme d'Immatriculation des Engins du Mali — API Spring Boot.

## Prérequis

- Java 21
- Maven 3.9+
- Docker (PostgreSQL)

## Démarrage

```bash
# Base de données
docker compose up -d

# Backend
mvn spring-boot:run
```

API disponible sur `http://localhost:7000`

## Compte démo (citoyen)

- Email: `abdoulaye.traore@example.ml`
- Mot de passe: `password123`
- OTP (inscription): `123456`

## Compte administrateur

- Email: `admin@pie.ml`
- Mot de passe: `password123`
- Rôle: `SUPER_ADMIN`

## Endpoints principaux

| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/auth/register` | Inscription |
| POST | `/api/auth/verify-otp` | Validation OTP |
| POST | `/api/auth/login` | Connexion |
| GET | `/api/public/stats` | Statistiques publiques |
| GET | `/api/citizen/dashboard` | Tableau de bord |
| POST | `/api/citizen/dossiers` | Nouvelle demande |
| POST | `/api/citizen/dossiers/{id}/documents` | Upload document |
| POST | `/api/citizen/dossiers/{id}/payment` | Paiement TrésorPay |
| POST | `/api/citizen/dossiers/{id}/appointment` | Rendez-vous |
