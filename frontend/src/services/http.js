import axios from "axios";
import { ElMessage } from "element-plus";

const http = axios.create({
  baseURL: "/api",
  timeout: 10000
});

function closeLoadingMessage(config) {
  if (config && config.__loadingMessageHandler) {
    config.__loadingMessageHandler.close();
    config.__loadingMessageHandler = null;
  }
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) config.headers.Authorization = `Bearer ${token}`;

  if (config.showLoadingMessage) {
    config.__loadingMessageHandler = ElMessage({
      type: "info",
      message: config.loadingMessage || "请求处理中...",
      duration: 0
    });
  }

  return config;
});

http.interceptors.response.use(
  (response) => {
    closeLoadingMessage(response.config);

    if (response.config?.showSuccessMessage) {
      ElMessage.success(response.config.successMessage || "请求成功");
    }

    return response.data;
  },
  (error) => {
    closeLoadingMessage(error.config);
    const message = error.response?.data?.message || "请求失败，请稍后再试";
    ElMessage.error(message);
    return Promise.reject(new Error(message));
  }
);

export default http;
