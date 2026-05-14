import { createApp } from 'vue'
import ArcoVue from '@arco-design/web-vue'
import '@arco-design/web-vue/dist/arco.css'
import App from './App.vue'
import router from './router'
import PageHeader from './components/PageHeader.vue'
import DesignSystem from './design-system'
import { initTheme } from './utils/theme'

initTheme()

const app = createApp(App)
app.use(ArcoVue)
app.use(DesignSystem)
app.use(router)
app.component('PageHeader', PageHeader)
app.mount('#app')
