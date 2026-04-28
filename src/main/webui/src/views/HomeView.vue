<template>
  <div class="flex min-h-[60vh] items-center justify-center">
    <div class="text-center max-w-md">
      <h1 class="text-4xl font-bold text-dark">
        Super<span class="text-primary">Quinquin</span>
      </h1>
      <p class="mt-2 text-lg text-brown">Choix de créneaux de bénévolat</p>

      <div v-if="isPending || isPendingReg" class="mt-8 text-brown/60">Chargement…</div>

      <div v-else-if="me" class="mt-8 space-y-4">
        <p class="text-dark">
          Bonjour, <strong>{{ me.firstName }} {{ me.lastName }}</strong> !
        </p>

        <div
          v-if="!me.cooperatorSynchronized"
          class="rounded-lg border border-amber-200 bg-amber-50 px-5 py-4 text-left text-sm text-amber-900"
        >
          <p class="font-semibold">Choix de créneau réservé au coopérateur principal</p>
          <p class="mt-2 opacity-90">
            Votre compte n'est pas enregistré comme coopérateur principal. Le choix
            du créneau doit être effectué par le coopérateur principal du binôme,
            qui choisit pour les deux.
          </p>
          <p class="mt-2 opacity-90">
            Si vous pensez qu'il s'agit d'une erreur, contactez le bureau des membres :
            <a class="underline" href="mailto:bureaudesmembres.deule@superquinquin.net"
              >bureaudesmembres.deule@superquinquin.net</a
            >.
          </p>
        </div>

        <template v-else>
          <div v-if="registration?.registeredSlotId">
            <p class="mb-3 text-sm text-brown">Vous avez déjà choisi un créneau.</p>
            <router-link
              to="/termine"
              class="inline-block rounded-lg bg-dark px-6 py-3 font-semibold text-white transition hover:bg-brown"
            >
              Voir mon créneau
            </router-link>
          </div>

          <div v-else>
            <p class="mb-3 text-sm text-brown">Vous n'avez pas encore choisi de créneau.</p>
            <router-link
              to="/choisir"
              class="inline-block rounded-lg bg-primary px-6 py-3 font-semibold text-dark transition hover:brightness-95"
            >
              Choisir mon créneau
            </router-link>
          </div>
        </template>
      </div>

      <div v-else-if="isError" class="mt-8 text-alert">
        Impossible de charger votre profil. Veuillez réessayer.
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useMe, useMyRegistration } from '@/composables/useMe'

const { data: me, isPending, isError } = useMe()
const { data: registration, isPending: isPendingReg } = useMyRegistration()
</script>
