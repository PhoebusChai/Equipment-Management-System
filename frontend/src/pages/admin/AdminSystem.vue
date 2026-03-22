<script setup>
import { computed, ref } from "vue";
import { ElMessage } from "element-plus";
import { createUser, listUsers, updateUser } from "../../mock/db";
import { useMockDb } from "../../composables/useMockDb";

useMockDb();

const keyword = ref("");
const showCreate = ref(false);
const creating = ref({
  email: "",
  realName: "",
  role: "student"
});

const users = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  const list = listUsers();
  if (!kw) return list;
  return list.filter((u) => {
    const hay = `${u.email} ${u.realName || ""} ${u.role}`.toLowerCase();
    return hay.includes(kw);
  });
});

const roleText = (role) => {
  if (role === "student") return "学生";
  if (role === "teacher") return "教师";
  if (role === "admin") return "管理员";
  return role;
};

const statusPill = (status) => (status === "active" ? "bg-green-100 text-green-700" : "bg-slate-100 text-slate-600");
const statusText = (status) => (status === "active" ? "正常" : "禁用");

function openCreate() {
  creating.value = { email: "", realName: "", role: "student" };
  showCreate.value = true;
}

function submitCreate() {
  const email = creating.value.email.trim();
  if (!email) return ElMessage.warning("请输入邮箱");
  try {
    createUser({
      email,
      realName: creating.value.realName.trim() || email,
      role: creating.value.role,
      status: "active",
      password: "123456"
    });
    ElMessage.success("已新增用户（默认密码 123456）");
    showCreate.value = false;
  } catch (e) {
    ElMessage.error(e.message || "创建失败");
  }
}

function toggleDisable(user) {
  const next = user.status === "active" ? "disabled" : "active";
  updateUser(user.id, { status: next });
  ElMessage.success(next === "disabled" ? "已禁用该用户" : "已启用该用户");
}

function resetPassword(user) {
  updateUser(user.id, { password: "123456" });
  ElMessage.success("已重置密码为 123456");
}

function importDemoUsers() {
  const base = Date.now().toString().slice(-5);
  const demo = [
    { role: "student", email: `s${base}01@example.com`, realName: "学生 示例01" },
    { role: "student", email: `s${base}02@example.com`, realName: "学生 示例02" },
    { role: "teacher", email: `t${base}01@example.com`, realName: "教师 示例01" }
  ];

  let ok = 0;
  for (const u of demo) {
    try {
      createUser({ ...u, password: "123456", status: "active" });
      ok += 1;
    } catch {
      // ignore duplicate
    }
  }
  ElMessage.success(`已导入 ${ok} 个示例账号`);
}
</script>

<template>
  <div class="flex h-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-4">
      <h2 class="text-xl font-semibold text-slate-900">系统管理</h2>
      <p class="mt-1 text-sm text-slate-500">用户导入与查询、实验室申请审核、设备申请审核。</p>
    </div>

    <div class="flex-1 overflow-auto p-6 space-y-6">
      <div class="flex flex-wrap gap-3">
        <button class="btn-secondary" @click="importDemoUsers">导入示例账号</button>
        <button class="btn-secondary" @click="openCreate">新增用户</button>
        <div class="flex-1"></div>
        <input v-model="keyword" class="input min-w-[260px]" placeholder="搜索邮箱/姓名/角色..." />
      </div>

      <div>
        <h3 class="mb-3 text-sm font-medium text-slate-700">用户列表</h3>
        <div class="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
          <table class="w-full border-collapse text-sm">
            <thead class="bg-slate-100">
              <tr>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">邮箱</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">姓名</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">角色</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">状态</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="u in users" :key="u.id" class="transition hover:bg-slate-50">
                <td class="px-6 py-4 text-slate-800">{{ u.email }}</td>
                <td class="px-6 py-4 text-slate-800">{{ u.realName }}</td>
                <td class="px-6 py-4 text-slate-700">{{ roleText(u.role) }}</td>
                <td class="px-6 py-4">
                  <span class="rounded-full px-2 py-0.5 text-xs font-medium" :class="statusPill(u.status)">
                    {{ statusText(u.status) }}
                  </span>
                </td>
                <td class="px-6 py-4">
                  <div class="flex flex-wrap gap-2">
                    <button class="btn-secondary px-3 py-1.5 text-xs" @click="resetPassword(u)">重置密码</button>
                    <button
                      class="btn-secondary px-3 py-1.5 text-xs"
                      :class="u.status === 'active' ? 'border-rose-200 text-rose-700 hover:bg-rose-50' : ''"
                      @click="toggleDisable(u)"
                    >
                      {{ u.status === "active" ? "禁用" : "启用" }}
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="users.length === 0">
                <td class="px-6 py-10 text-center text-slate-500" colspan="5">暂无用户</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>

  <div v-if="showCreate" class="fixed inset-0 z-40 flex items-center justify-center bg-slate-900/30 px-4" @click.self="showCreate = false">
    <div class="w-full max-w-lg rounded-xl border border-slate-200 bg-white shadow-xl">
      <div class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
        <div class="text-base font-semibold text-slate-900">新增用户</div>
        <button class="rounded-md px-2 py-1 text-sm text-slate-500 hover:bg-slate-100" @click="showCreate = false">关闭</button>
      </div>
      <div class="space-y-4 p-5">
        <div>
          <label class="mb-1 block text-sm text-slate-600">邮箱</label>
          <input v-model="creating.email" class="input" placeholder="name@example.com" />
        </div>
        <div>
          <label class="mb-1 block text-sm text-slate-600">姓名</label>
          <input v-model="creating.realName" class="input" placeholder="例如：张三" />
        </div>
        <div>
          <label class="mb-1 block text-sm text-slate-600">角色</label>
          <select v-model="creating.role" class="input">
            <option value="student">学生</option>
            <option value="teacher">教师</option>
            <option value="admin">管理员</option>
          </select>
        </div>
      </div>
      <div class="flex justify-end gap-3 border-t border-slate-200 px-5 py-4">
        <button class="btn-secondary" @click="showCreate = false">取消</button>
        <button class="btn-primary" @click="submitCreate">创建（默认密码 123456）</button>
      </div>
    </div>
  </div>
</template>
