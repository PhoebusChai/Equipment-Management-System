import { createApp } from "vue";
import ElementPlus from "element-plus";
import App from "./App.vue";
import router from "./router";
import "element-plus/dist/index.css";
import "./styles/tailwind.css";
import "./styles/theme.css";

createApp(App).use(router).use(ElementPlus).mount("#app");
