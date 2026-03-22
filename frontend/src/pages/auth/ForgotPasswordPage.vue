<script setup>
import { reactive, ref } from "vue";
import { ElMessage } from "element-plus";

const loading = ref(false);
const form = reactive({
  email: "",
  code: "",
  newPassword: ""
});

function sendCode() {
  if (!form.email) {
    ElMessage.warning("请先输入邮箱");
    return;
  }
  ElMessage.success("验证码已发送到邮箱");
}

function onSubmit() {
  if (!form.email || !form.code || !form.newPassword) {
    ElMessage.warning("请填写完整信息");
    return;
  }
  loading.value = true;
  setTimeout(() => {
    loading.value = false;
    ElMessage.success("密码重置成功，请重新登录");
  }, 800);
}
</script>

<template>
  <div class="flex h-full flex-col">
    <h2 class="text-2xl font-semibold text-slate-900">忘记密码</h2>
    <p class="mt-1 text-sm text-slate-500">通过邮箱验证码重置密码</p>

    <form class="mt-6 space-y-4" @submit.prevent="onSubmit">
      <div>
        <label class="mb-1 block text-sm text-slate-600">邮箱</label>
        <input v-model="form.email" type="email" class="input" placeholder="name@example.com" />
      </div>

      <div>
        <label class="mb-1 block text-sm text-slate-600">验证码</label>
        <div class="flex gap-2">
          <input v-model="form.code" class="input" placeholder="请输入验证码" />
          <button type="button" class="btn-secondary whitespace-nowrap" @click="sendCode">发送验证码</button>
        </div>
      </div>

      <div>
        <label class="mb-1 block text-sm text-slate-600">新密码</label>
        <input v-model="form.newPassword" type="password" class="input" placeholder="请输入新密码" />
      </div>

      <button class="btn-primary w-full" type="submit" :disabled="loading">
        {{ loading ? "提交中..." : "重置密码" }}
      </button>
    </form>

    <p class="mt-auto pt-5 text-center text-sm text-slate-600">
      想起来了？
      <router-link to="/auth/login" class="text-brand-600 hover:text-brand-700">返回登录</router-link>
    </p>
  </div>
</template>
