<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { loginApi } from "../../services/auth";

const router = useRouter();
const loading = ref(false);
const form = reactive({
  email: "",
  password: "",
  remember: true
});

async function onSubmit() {
  if (!form.email || !form.password) {
    ElMessage.warning("请填写邮箱和密码");
    return;
  }

  loading.value = true;

  try {
    const user = await loginApi({ email: form.email.trim(), password: form.password });
    const redirectMap = {
      student: "/student/dashboard",
      teacher: "/teacher/dashboard",
      admin: "/admin/dashboard"
    };
    loading.value = false;
    ElMessage.success(`以${user.role === "student" ? "学生" : user.role === "teacher" ? "老师" : "管理员"}身份登录成功`);
    router.push(redirectMap[user.role] || "/auth/login");
  } catch (e) {
    loading.value = false;
    ElMessage.error(e.message || "账号或密码错误");
  }
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
