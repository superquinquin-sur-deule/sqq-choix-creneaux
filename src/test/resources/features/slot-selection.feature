# language: fr
Fonctionnalité: Choix de créneau

  Scénario: Un coopérateur peut voir les créneaux disponibles
    Quand j'appelle GET "/api/slots"
    Alors je reçois un code HTTP 200
    Et la réponse contient une liste de créneaux

  Scénario: Un coopérateur ne peut pas s'inscrire deux fois
    Étant donné que je suis déjà inscrit à un créneau
    Quand j'appelle POST "/api/slots/test-slot-id/register"
    Alors je reçois un code HTTP 409
