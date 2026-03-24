<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import AppPagination from "../../components/AppPagination.vue";
import { createUserApi, listUsersApi, resetUserPasswordApi, updateUserApi, updateUserStatusApi } from "../../services/users";
import { getCurrentUser } from "../../services/session";

const keyword = ref("");
const roleFilter = ref("all");
const statusFilter = ref("all");
const showCreate = ref(false);
const showEdit = ref(false);
const showDetail = ref(false);
const usersSource = ref([]);
const currentUser = computed(() => getCurrentUser());

const creating = ref({
  email: "",
  realName: "",
  role: "student"
});
const editing = ref({
  id: null,
  realName: "",
  role: "student"
});
const detailUser = ref(null);

const currentPage = ref(1);
const pageSize = ref(8);

const roleText = (role) => {
  if (role === "student") return "学生";
  if (role === "teacher") return "教师";
  if (role === "admin") return "管理员";
  return role;
};
const statusPill = (status) => (status === "active" ? "bg-emerald-100 text-emerald-700" : "bg-slate-100 text-slate-600");
const statusText = (status) => (status === "active" ? "正常" : "禁用");
const fmtDate = (iso) => (iso ? String(iso).replace("T", " ").slice(0, 16) : "-");
const avatarFallback = (name, email) => (String(name || email || "U").trim().charAt(0) || "U").toUpperCase();

const stats = computed(() => ({
  total: usersSource.value.length,
  active: usersSource.value.filter((u) => u.status === "active").length,
  disabled: usersSource.value.filter((u) => u.status !== "active").length,
  admins: usersSource.value.filter((u) => u.role === "admin").length
}));

const filteredUsers = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return usersSource.value.filter((u) => {
    const byRole = roleFilter.value === "all" || u.role === roleFilter.value;
    const byStatus = statusFilter.value === "all" || u.status === statusFilter.value;
    const byKw = !kw || `${u.email} ${u.realName || ""} ${u.role}`.toLowerCase().includes(kw);
    return byRole && byStatus && byKw;
  });
});

const pagedUsers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredUsers.value.slice(start, start + pageSize.value);
});

async function refreshUsers() {
  usersSource.value = await listUsersApi();
}

function resetPage() {
  currentPage.value = 1;
}

function openCreate() {
  creating.value = { email: "", realName: "", role: "student" };
  showCreate.value = true;
}

function openEdit(user) {
  editing.value = { id: user.id, realName: user.realName, role: user.role };
  showEdit.value = true;
}

function openDetail(user) {
  detailUser.value = user;
  showDetail.value = true;
}

async function submitCreate() {
  const email = creating.value.email.trim();
  if (!email) return ElMessage.warning("请输入邮箱");
  try {
    await createUserApi({
      email,
      realName: creating.value.realName.trim() || email,
      role: creating.value.role
    });
    await refreshUsers();
    ElMessage.success("已新增用户（默认密码 123456）");
    showCreate.value = false;
  } catch (e) {
    ElMessage.error(e.message || "创建失败");
  }
}

async function submitEdit() {
  if (!editing.value.id) return;
  if (!editing.value.realName.trim()) return ElMessage.warning("请输入姓名");
  try {
    await updateUserApi(editing.value.id, {
      realName: editing.value.realName.trim(),
      role: editing.value.role
    });
    await refreshUsers();
    ElMessage.success("用户信息已更新");
    showEdit.value = false;
  } catch (e) {
    ElMessage.error(e.message || "更新失败");
  }
}

async function toggleDisable(user) {
  if (user.email === currentUser.value?.email && user.status === "active") {
    return ElMessage.warning("不能禁用当前登录账号");
  }
  const next = user.status === "active" ? "disabled" : "active";
  await ElMessageBox.confirm(`确认${next === "disabled" ? "禁用" : "启用"}用户 ${user.email} 吗？`, "提示", { type: "warning" });
  await updateUserStatusApi(user.id, next);
  await refreshUsers();
  ElMessage.success(next === "disabled" ? "已禁用该用户" : "已启用该用户");
}

async function resetPassword(user) {
  await ElMessageBox.confirm(`确认重置用户 ${user.email} 密码为 123456 吗？`, "提示", { type: "warning" });
  await resetUserPasswordApi(user.id, "123456");
  ElMessage.success("已重置密码为 123456");
}

onMounted(refreshUsers);
</script>

<template>
  <div class="feishu-page flex h-full w-full flex-col">
    <div class="border-b border-slate-200/80 bg-white px-6 py-5">
      <h2 class="text-xl font-semibold text-slate-900">用户管理</h2>
      <p class="mt-1 text-sm text-slate-500">账号生命周期管理：新增、编辑、筛选、状态控制与密码重置。</p>
    </div>

    <div class="flex min-h-0 flex-1 flex-col gap-4 p-6">
      <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div class="feishu-stat-card rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
          <div class="text-xs text-slate-500">总用户</div>
          <div class="mt-1 text-2xl font-semibold text-slate-900">{{ stats.total }}</div>
        </div>
        <div class="feishu-stat-card rounded-xl border border-emerald-100 bg-emerald-50/40 p-3 shadow-sm">
          <div class="text-xs text-emerald-700">正常</div>
          <div class="mt-1 text-2xl font-semibold text-emerald-900">{{ stats.active }}</div>
        </div>
        <div class="feishu-stat-card rounded-xl border border-slate-200 bg-slate-50 p-3 shadow-sm">
          <div class="text-xs text-slate-600">禁用</div>
          <div class="mt-1 text-2xl font-semibold text-slate-800">{{ stats.disabled }}</div>
        </div>
        <div class="feishu-stat-card rounded-xl border border-blue-100 bg-blue-50/40 p-3 shadow-sm">
          <div class="text-xs text-blue-700">管理员</div>
          <div class="mt-1 text-2xl font-semibold text-blue-900">{{ stats.admins }}</div>
        </div>
      </div>

      <div class="feishu-filter rounded-xl border border-slate-200 bg-slate-50/60 p-4">
        <div class="flex items-center gap-3 overflow-x-auto whitespace-nowrap">
          <input v-model="keyword" class="input !w-[280px] shrink-0" placeholder="搜索邮箱/姓名/角色..." @input="resetPage" />
          <button class="btn-secondary shrink-0 px-4" @click="resetPage">搜索</button>
          <button class="btn-primary shrink-0" @click="openCreate">添加用户</button>
          <select v-model="roleFilter" class="input !w-[130px] shrink-0" @change="resetPage">
            <option value="all">全部角色</option>
            <option value="student">学生</option>
            <option value="teacher">教师</option>
            <option value="admin">管理员</option>
          </select>
          <select v-model="statusFilter" class="input !w-[130px] shrink-0" @change="resetPage">
            <option value="all">全部状态</option>
            <option value="active">正常</option>
            <option value="disabled">禁用</option>
          </select>
        </div>
      </div>

      <div class="feishu-table-wrap min-h-0 flex-1 overflow-auto rounded-xl border border-slate-200 bg-white shadow-sm">
        <table class="w-full border-collapse text-sm">
          <thead class="bg-slate-100/90">
            <tr>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">#</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">头像</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">邮箱</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">姓名</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">角色</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">状态</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">创建时间</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr v-for="(u, idx) in pagedUsers" :key="u.id" class="transition hover:bg-[#f7f9fc]">
              <td class="px-4 py-3 text-slate-500">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="px-4 py-3">
                <div class="flex items-center">
                  <img
                    v-if="u.avatarUrl"
                    :src="u.avatarUrl"
                    alt="avatar"
                    class="h-8 w-8 rounded-full border border-slate-200 object-cover"
                  />
                  <div
                    v-else
                    class="flex h-8 w-8 items-center justify-center rounded-full bg-blue-100 text-xs font-semibold text-blue-700"
                  >
                    {{ avatarFallback(u.realName, u.email) }}
                  </div>
                </div>
              </td>
              <td class="px-4 py-3 text-slate-800">{{ u.email }}</td>
              <td class="px-4 py-3 text-slate-800">{{ u.realName }}</td>
              <td class="px-4 py-3 text-slate-700">{{ roleText(u.role) }}</td>
              <td class="px-4 py-3">
                <span class="rounded-full px-2 py-0.5 text-xs font-medium" :class="statusPill(u.status)">
                  {{ statusText(u.status) }}
                </span>
              </td>
              <td class="px-4 py-3 text-slate-600">{{ fmtDate(u.createdAt) }}</td>
              <td class="px-4 py-3">
                <div class="flex flex-wrap gap-2">
                  <button class="btn-secondary px-3 py-1.5 text-xs" @click="openDetail(u)">详情</button>
                  <button class="btn-secondary px-3 py-1.5 text-xs" @click="openEdit(u)">编辑</button>
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
            <tr v-if="pagedUsers.length === 0">
              <td class="px-6 py-10 text-center text-slate-500" colspan="8">暂无用户</td>
            </tr>
          </tbody>
        </table>
      </div>

      <AppPagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="filteredUsers.length" :page-size-options="[8,12,20]" />
    </div>
  </div>

  <div v-if="showCreate" class="fixed inset-0 z-40 flex items-center justify-center bg-slate-900/30 px-4" @click.self="showCreate = false">
    <div class="feishu-modal w-full max-w-lg rounded-xl border border-slate-200 bg-white shadow-xl">
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

  <div v-if="showEdit" class="fixed inset-0 z-40 flex items-center justify-center bg-slate-900/30 px-4" @click.self="showEdit = false">
    <div class="feishu-modal w-full max-w-lg rounded-xl border border-slate-200 bg-white shadow-xl">
      <div class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
        <div class="text-base font-semibold text-slate-900">编辑用户</div>
        <button class="rounded-md px-2 py-1 text-sm text-slate-500 hover:bg-slate-100" @click="showEdit = false">关闭</button>
      </div>
      <div class="space-y-4 p-5">
        <div>
          <label class="mb-1 block text-sm text-slate-600">姓名</label>
          <input v-model="editing.realName" class="input" placeholder="例如：张三" />
        </div>
        <div>
          <label class="mb-1 block text-sm text-slate-600">角色</label>
          <select v-model="editing.role" class="input">
            <option value="student">学生</option>
            <option value="teacher">教师</option>
            <option value="admin">管理员</option>
          </select>
        </div>
      </div>
      <div class="flex justify-end gap-3 border-t border-slate-200 px-5 py-4">
        <button class="btn-secondary" @click="showEdit = false">取消</button>
        <button class="btn-primary" @click="submitEdit">保存</button>
      </div>
    </div>
  </div>

  <div v-if="showDetail && detailUser" class="fixed inset-0 z-40 flex items-center justify-center bg-slate-900/30 px-4" @click.self="showDetail = false">
    <div class="feishu-modal w-full max-w-xl rounded-xl border border-slate-200 bg-white shadow-xl">
      <div class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
        <div class="text-base font-semibold text-slate-900">用户详情</div>
        <button class="rounded-md px-2 py-1 text-sm text-slate-500 hover:bg-slate-100" @click="showDetail = false">关闭</button>
      </div>
      <div class="space-y-4 p-5">
        <div class="flex items-center gap-3">
          <img
            v-if="detailUser.avatarUrl"
            :src="detailUser.avatarUrl"
            alt="avatar"
            class="h-12 w-12 rounded-full border border-slate-200 object-cover"
          />
          <div
            v-else
            class="flex h-12 w-12 items-center justify-center rounded-full bg-blue-100 text-sm font-semibold text-blue-700"
          >
            {{ avatarFallback(detailUser.realName, detailUser.email) }}
          </div>
          <div>
            <div class="text-sm font-semibold text-slate-900">{{ detailUser.realName }}</div>
            <div class="text-xs text-slate-500">{{ detailUser.email }}</div>
          </div>
        </div>
        <div class="grid gap-3 sm:grid-cols-2">
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <div class="text-xs text-slate-500">角色</div>
            <div class="mt-1 text-sm font-medium text-slate-900">{{ roleText(detailUser.role) }}</div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <div class="text-xs text-slate-500">状态</div>
            <div class="mt-1 text-sm font-medium text-slate-900">{{ statusText(detailUser.status) }}</div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 sm:col-span-2">
            <div class="text-xs text-slate-500">创建时间</div>
            <div class="mt-1 text-sm font-medium text-slate-900">{{ fmtDate(detailUser.createdAt) }}</div>
          </div>
        </div>
      </div>
      <div class="flex justify-end border-t border-slate-200 px-5 py-4">
        <button class="btn-primary px-5" @click="showDetail = false">我知道了</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.feishu-page {
  background: #f5f6f8;
}

.feishu-stat-card {
  border-radius: 10px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}

.feishu-filter,
.feishu-table-wrap,
.feishu-modal {
  border-radius: 10px;
}

.feishu-page .btn-primary {
  border-color: #1456f0;
  background: #1456f0;
}

.feishu-page .btn-primary:hover {
  border-color: #0f46cc;
  background: #0f46cc;
}

.feishu-page .btn-secondary {
  border-color: #d0d7e2;
  color: #334155;
  background: #fff;
}

.feishu-page .btn-secondary:hover {
  border-color: #b8c2d1;
  background: #f8fafc;
}
</style>

