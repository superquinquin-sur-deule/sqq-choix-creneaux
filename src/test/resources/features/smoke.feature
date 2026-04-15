# language: fr
Fonctionnalité: Smoke test
  Vérification que l'application démarre correctement

  Scénario: Le health endpoint répond OK
    Quand j'appelle le endpoint de santé
    Alors je reçois le statut "ok"
