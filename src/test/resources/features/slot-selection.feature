# language: fr
Fonctionnalité: Choix d'un créneau par un coopérateur

  Scénario: Happy path — un coopérateur choisit et confirme un créneau
    Étant donné que des créneaux sont disponibles
    Quand je me rends sur la page de choix de créneau
    Et je sélectionne le créneau du mardi 10h00
    Et je continue vers la confirmation
    Et je confirme mon choix
    Alors je vois la page "Créneau confirmé !"
