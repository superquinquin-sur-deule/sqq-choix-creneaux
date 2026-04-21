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
