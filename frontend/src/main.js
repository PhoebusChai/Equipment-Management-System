import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import "element-plus/dist/index.css";
import "./styles/tailwind.css";
import "./styles/theme.css";
import { getDb } from "./mock/db";

// 初始化本地 Mock 数据库（若本地不存在则写入种子数据）
getDb();

createApp(App).use(router).mount("#app");
