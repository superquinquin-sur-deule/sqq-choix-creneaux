import { createApp } from 'vue'
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import { VueQueryPlugin, type VueQueryPluginOptions } from '@tanstack/vue-query'

import App from './App.vue'
import router from './router'

import './assets/style.css'

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

const vueQueryOptions: VueQueryPluginOptions = {
  queryClientConfig: {
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        retry: 1,
        refetchOnWindowFocus: false,
      },
    },
  },
}

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(VueQueryPlugin, vueQueryOptions)

window.addEventListener('vite:preloadError', () => {
  window.location.reload()
})

app.mount('#app')
