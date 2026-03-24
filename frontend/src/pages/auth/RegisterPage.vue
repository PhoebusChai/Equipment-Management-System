<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { registerApi } from "../../services/auth";

const router = useRouter();
const loading = ref(false);
const form = reactive({
  roleCode: "STUDENT",
  name: "",
  email: "",
  password: "",
  confirmPassword: ""
});

async function onSubmit() {
  if (!form.name || !form.email || !form.password || !form.confirmPassword) {
    ElMessage.warning("请完整填写注册信息");
    return;
  }
  if (form.password !== form.confirmPassword) {
    ElMessage.error("两次输入密码不一致");
    return;
  }
  loading.value = true;
  try {
    await registerApi({
      email: form.email,
      password: form.password,
      realName: form.name,
      roleCode: form.roleCode
    });
    ElMessage.success("注册成功，已自动登录");
    router.push(form.roleCode === "TEACHER" ? "/teacher/dashboard" : "/student/dashboard");
  } catch (e) {
    ElMessage.error(e.message || "注册失败");
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="flex h-full flex-col">
    <h2 class="text-2xl font-semibold text-slate-900">注册</h2>
    <p class="mt-1 text-sm text-slate-500">创建系统账号，开始使用预约与管理功能</p>

    <form class="mt-6 space-y-4" @submit.prevent="onSubmit">
      <div>
        <label class="mb-1 block text-sm text-slate-600">角色</label>
        <select v-model="form.roleCode" class="input">
          <option value="STUDENT">学生</option>
          <option value="TEACHER">教师</option>
        </select>
      </div>

      <div>
        <label class="mb-1 block text-sm text-slate-600">姓名</label>
        <input v-model="form.name" type="text" class="input" placeholder="请输入姓名" />
      </div>

      <div>
        <label class="mb-1 block text-sm text-slate-600">邮箱</label>
        <input v-model="form.email" type="email" class="input" placeholder="name@example.com" />
      </div>

      <div>
        <label class="mb-1 block text-sm text-slate-600">密码</label>
        <input v-model="form.password" type="password" class="input" placeholder="请设置密码" />
      </div>

      <div>
        <label class="mb-1 block text-sm text-slate-600">确认密码</label>
        <input v-model="form.confirmPassword" type="password" class="input" placeholder="请再次输入密码" />
      </div>

      <button class="btn-primary w-full" type="submit" :disabled="loading">
        {{ loading ? "注册中..." : "注册" }}
      </button>
    </form>

    <p class="mt-auto pt-5 text-center text-sm text-slate-600">
      已有账号？
      <router-link to="/auth/login" class="text-brand-600 hover:text-brand-700">返回登录</router-link>
    </p>
  </div>
</template>
