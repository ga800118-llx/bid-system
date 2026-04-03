import { createApp } from '"'"'vue'"'"'
import ArcoVue from '"'"'@arco-design/web-vue'"'"'
import '"'"'@arco-design/web-vue/dist/arco.css'"'"'
import ElementPlus from '"'"'element-plus'"'"'
import '"'"'element-plus/dist/index.css'"'"'
import * as ElementPlusIconsVue from '"'"'@element-plus/icons-vue'"'"'
import App from '"'"'./App.vue'"'"'
import router from '"'"'./router'"'"'

const app = createApp(App)

// Element Plus icons (still needed for Login/Register until migrated)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(ArcoVue)
app.use(ElementPlus)
app.use(router)
app.mount('"'"'#app'"'"')
