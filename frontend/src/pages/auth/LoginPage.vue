<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";

const router = useRouter();
const loading = ref(false);
const form = reactive({
  email: "",
  password: "",
  remember: true
});

// 本地模拟的三个账号：学生、老师、管理员
const mockUsers = [
  {
    role: "student",
    email: "student@example.com",
    password: "123456",
    redirect: "/student/dashboard"
  },
  {
    role: "teacher",
    email: "teacher@example.com",
    password: "123456",
    redirect: "/teacher/dashboard"
  },
  {
    role: "admin",
    email: "admin@example.com",
    password: "123456",
    redirect: "/admin/dashboard"
  }
];

function onSubmit() {
  if (!form.email || !form.password) {
    ElMessage.warning("请填写邮箱和密码");
    return;
  }

  loading.value = true;

  setTimeout(() => {
    const user = mockUsers.find(
      (u) => u.email === form.email.trim() && u.password === form.password
    );

    if (!user) {
      loading.value = false;
      ElMessage.error("账号或密码错误（仅本地模拟账号可用）");
      return;
    }

    // 简单本地“登录态”（演示用：始终写入，方便路由守卫与页面读取）
    const userInfo = { email: user.email, role: user.role };
    window.localStorage.setItem("ems_current_user", JSON.stringify(userInfo));

    loading.value = false;
    ElMessage.success(`以${user.role === "student" ? "学生" : user.role === "teacher" ? "老师" : "管理员"}身份登录成功`);
    router.push(user.redirect);
  }, 500);
}
</script>

<template>
  <div class="flex h-full flex-col">
    <h2 class="text-2xl font-semibold text-slate-900">登录</h2>
    <p class="mt-1 text-sm text-slate-500">欢迎回来，请登录你的账号</p>

    <form class="mt-6 space-y-4" @submit.prevent="onSubmit">
      <div>
        <label class="mb-1 block text-sm text-slate-600">邮箱</label>
        <input v-model="form.email" type="email" class="input" placeholder="name@example.com" />
      </div>

      <div>
        <label class="mb-1 block text-sm text-slate-600">密码</label>
        <input v-model="form.password" type="password" class="input" placeholder="请输入密码" />
      </div>

      <div class="flex items-center justify-between text-sm">
        <label class="flex items-center gap-2 text-slate-600">
          <input v-model="form.remember" type="checkbox" />
          记住我
        </label>
        <router-link to="/auth/forgot-password" class="text-brand-600 hover:text-brand-700">忘记密码？</router-link>
      </div>

      <button class="btn-primary w-full" type="submit" :disabled="loading">
        {{ loading ? "登录中..." : "登录" }}
      </button>
    </form>

    <p class="mt-auto pt-5 text-center text-sm text-slate-600">
      还没有账号？
      <router-link to="/auth/register" class="text-brand-600 hover:text-brand-700">立即注册</router-link>
    </p>
  </div>
</template>
