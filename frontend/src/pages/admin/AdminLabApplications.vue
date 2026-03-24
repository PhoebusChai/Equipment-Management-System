<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import AppPagination from "../../components/AppPagination.vue";
import { APPLICATION_STATUS, approveApplicationApi, listApplicationsApi, rejectApplicationApi } from "../../services/applications";
import { getCurrentUser } from "../../services/session";
import { listUsersApi } from "../../services/users";

const currentUser = computed(() => getCurrentUser());
const keyword = ref("");
const statusFilter = ref("all");
const noteMap = ref({});
const source = ref([]);
const usersSource = ref([]);
const selectedIds = ref([]);
const loading = ref(false);
const acting = ref(false);
const currentPage = ref(1);
const pageSize = ref(8);
const detailVisible = ref(false);
const detailItem = ref(null);

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return source.value
    .filter((x) => x.type === "lab_apply")
    .filter((x) => (statusFilter.value === "all" ? true : x.status === statusFilter.value))
    .filter((x) => {
      if (!kw) return true;
      return String(x.title || "").toLowerCase().includes(kw) || String(x.detail || "").toLowerCase().includes(kw);
    })
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
});

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

const totalCount = computed(() => filteredRows.value.length);
const submittedCount = computed(() => filteredRows.value.filter((x) => x.status === APPLICATION_STATUS.SUBMITTED).length);
const approvedCount = computed(() => filteredRows.value.filter((x) => x.status === APPLICATION_STATUS.APPROVED).length);
const rejectedCount = computed(() => filteredRows.value.filter((x) => x.status === APPLICATION_STATUS.REJECTED).length);
const selectablePending = computed(() => filteredRows.value.filter((x) => x.status === APPLICATION_STATUS.SUBMITTED).map((x) => x.id));
const allPendingSelected = computed(
  () => selectablePending.value.length > 0 && selectablePending.value.every((id) => selectedIds.value.includes(id))
);

function parseLabApplyMeta(detail) {
  const txt = String(detail || "");
  const rows = txt.split("\n");
  const get = (k) => rows.find((r) => r.startsWith(`${k}：`) || r.startsWith(`${k}:`))?.split(/：|:/).slice(1).join(":").trim() || "";
  return {
    labId: get("labId"),
    openDateStart: get("开放开始"),
    openDateEnd: get("开放结束"),
    dailyStartTime: get("每日开始"),
    dailyEndTime: get("每日结束"),
    slotPreset: get("时段模板")
  };
}

function statusText(status) {
  if (status === APPLICATION_STATUS.SUBMITTED) return "待审核";
  if (status === APPLICATION_STATUS.APPROVED) return "已通过";
  if (status === APPLICATION_STATUS.REJECTED) return "已驳回";
  return status;
}

function statusClass(status) {
  if (status === APPLICATION_STATUS.SUBMITTED) return "bg-amber-100 text-amber-700";
  if (status === APPLICATION_STATUS.APPROVED) return "bg-emerald-100 text-emerald-700";
  if (status === APPLICATION_STATUS.REJECTED) return "bg-rose-100 text-rose-700";
  return "bg-slate-100 text-slate-700";
}

function getNote(id) {
  return noteMap.value[id] || "";
}

function setNote(id, val) {
  noteMap.value = { ...noteMap.value, [id]: val };
}

function formatDateTime(iso) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "-";
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")} ${String(
    d.getHours()
  ).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
}

function applicantName(app) {
  const u = usersSource.value.find((x) => x.id === app.createdByUserId);
  return u?.realName || `用户#${app.createdByUserId || "-"}`;
}

function toggleSelectAllPending() {
  if (allPendingSelected.value) {
    selectedIds.value = selectedIds.value.filter((id) => !selectablePending.value.includes(id));
  } else {
    selectedIds.value = [...new Set([...selectedIds.value, ...selectablePending.value])];
  }
}

function toggleSelectOne(id, checked) {
  if (checked) {
    selectedIds.value = [...new Set([...selectedIds.value, id])];
  } else {
    selectedIds.value = selectedIds.value.filter((x) => x !== id);
  }
}

function clearSelection() {
  selectedIds.value = [];
}

function resetPage() {
  currentPage.value = 1;
}

async function loadData() {
  loading.value = true;
  try {
    const [apps, users] = await Promise.all([listApplicationsApi(), listUsersApi()]);
    source.value = apps;
    usersSource.value = users;
    selectedIds.value = selectedIds.value.filter((id) => source.value.some((x) => x.id === id));
  } finally {
    loading.value = false;
  }
}

function openDetail(item) {
  detailItem.value = item;
  detailVisible.value = true;
}

async function approve(item) {
  if (!currentUser.value?.id) return ElMessage.warning("请先登录管理员账号");
  acting.value = true;
  await approveApplicationApi("lab", item.id, {
    reviewerId: currentUser.value.id,
    reviewComment: getNote(item.id)
  });
  try {
    await loadData();
    ElMessage.success("实验室申请已通过");
  } finally {
    acting.value = false;
  }
}

async function reject(item) {
  if (!currentUser.value?.id) return ElMessage.warning("请先登录管理员账号");
  acting.value = true;
  await rejectApplicationApi("lab", item.id, {
    reviewerId: currentUser.value.id,
    reviewComment: getNote(item.id)
  });
  try {
    await loadData();
    ElMessage.success("实验室申请已驳回");
  } finally {
    acting.value = false;
  }
}

async function batchApprove() {
  if (!currentUser.value?.id) return ElMessage.warning("请先登录管理员账号");
  const ids = selectedIds.value.filter((id) => selectablePending.value.includes(id));
  if (!ids.length) return ElMessage.warning("请先选择待审核申请");
  acting.value = true;
  let ok = 0;
  for (const id of ids) {
    const item = filteredRows.value.find((x) => x.id === id);
    if (!item) continue;
    try {
      await approveApplicationApi("lab", id, {
        reviewerId: currentUser.value.id,
        reviewComment: getNote(id)
      });
      ok += 1;
    } catch {
      // keep processing
    }
  }
  try {
    await loadData();
    ElMessage.success(`批量通过完成：${ok}/${ids.length}`);
    clearSelection();
  } finally {
    acting.value = false;
  }
}

async function batchReject() {
  if (!currentUser.value?.id) return ElMessage.warning("请先登录管理员账号");
  const ids = selectedIds.value.filter((id) => selectablePending.value.includes(id));
  if (!ids.length) return ElMessage.warning("请先选择待审核申请");
  acting.value = true;
  let ok = 0;
  for (const id of ids) {
    const item = filteredRows.value.find((x) => x.id === id);
    if (!item) continue;
    try {
      await rejectApplicationApi("lab", id, {
        reviewerId: currentUser.value.id,
        reviewComment: getNote(id)
      });
      ok += 1;
    } catch {
      // keep processing
    }
  }
  try {
    await loadData();
    ElMessage.success(`批量驳回完成：${ok}/${ids.length}`);
    clearSelection();
  } finally {
    acting.value = false;
  }
}

onMounted(async () => {
  await loadData();
});
</script>

<template>
  <div class="feishu-page flex h-full w-full flex-col">
    <div class="border-b border-slate-200/80 bg-white px-6 py-5">
      <h2 class="text-xl font-semibold text-slate-900">实验室申请管理</h2>
      <p class="mt-1 text-sm text-slate-500">支持申请检索、审批流转、批量处理与审核追踪。</p>
    </div>

    <div class="flex min-h-0 flex-1 flex-col gap-4 p-6">
      <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div class="feishu-stat-card rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
          <div class="text-xs text-slate-500">当前筛选总数</div>
          <div class="mt-1 text-2xl font-semibold text-slate-900">{{ totalCount }}</div>
        </div>
        <div class="feishu-stat-card rounded-xl border border-amber-100 bg-amber-50/40 p-3 shadow-sm">
          <div class="text-xs text-amber-700">待审核</div>
          <div class="mt-1 text-2xl font-semibold text-amber-800">{{ submittedCount }}</div>
        </div>
        <div class="feishu-stat-card rounded-xl border border-emerald-100 bg-emerald-50/40 p-3 shadow-sm">
          <div class="text-xs text-emerald-700">已通过</div>
          <div class="mt-1 text-2xl font-semibold text-emerald-800">{{ approvedCount }}</div>
        </div>
        <div class="feishu-stat-card rounded-xl border border-rose-100 bg-rose-50/40 p-3 shadow-sm">
          <div class="text-xs text-rose-700">已驳回</div>
          <div class="mt-1 text-2xl font-semibold text-rose-800">{{ rejectedCount }}</div>
        </div>
      </div>

      <div class="feishu-filter rounded-xl border border-slate-200 bg-slate-50/60 p-4">
        <div class="flex items-center gap-3 overflow-x-auto whitespace-nowrap">
          <input v-model="keyword" class="input !w-[280px] shrink-0" placeholder="搜索申请标题或详情..." @input="resetPage" />
          <select v-model="statusFilter" class="input !w-[130px] shrink-0" @change="resetPage">
            <option value="all">全部状态</option>
            <option :value="APPLICATION_STATUS.SUBMITTED">待审核</option>
            <option :value="APPLICATION_STATUS.APPROVED">已通过</option>
            <option :value="APPLICATION_STATUS.REJECTED">已驳回</option>
          </select>
          <label class="inline-flex items-center gap-2 text-sm text-slate-700">
            <input type="checkbox" :checked="allPendingSelected" @change="toggleSelectAllPending" />
            全选待审核
          </label>
          <span class="text-sm text-slate-500">已选 {{ selectedIds.length }} 项</span>
          <button class="btn-primary px-3 py-1.5 text-xs" :disabled="acting" @click="batchApprove">批量通过</button>
          <button class="btn-secondary border-rose-200 px-3 py-1.5 text-xs text-rose-700 hover:bg-rose-50" :disabled="acting" @click="batchReject">
            批量驳回
          </button>
          <button class="btn-secondary px-3 py-1.5 text-xs" @click="clearSelection">清空选择</button>
          <button class="btn-secondary" @click="loadData">刷新</button>
        </div>
      </div>

      <div class="feishu-table-wrap min-h-0 flex-1 overflow-auto rounded-xl border border-slate-200 bg-white shadow-sm">
        <table class="w-full border-collapse text-sm">
          <thead class="bg-slate-100/90">
            <tr>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">选择</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">申请ID</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">标题</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">详情</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">状态</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">申请人</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">提交时间</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">审核意见</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr v-for="app in pagedRows" :key="app.id" class="transition hover:bg-[#f7f9fc]">
              <td class="px-4 py-3">
                <input
                  v-if="app.status === APPLICATION_STATUS.SUBMITTED"
                  type="checkbox"
                  :checked="selectedIds.includes(app.id)"
                  @change="toggleSelectOne(app.id, $event.target.checked)"
                />
              </td>
              <td class="px-4 py-3 text-slate-600">{{ app.id }}</td>
              <td class="max-w-[220px] truncate px-4 py-3 text-slate-800">{{ app.title || `实验室申请 #${app.id}` }}</td>
              <td class="max-w-[300px] truncate px-4 py-3 text-slate-600">{{ app.detail || "-" }}</td>
              <td class="px-4 py-3">
                <span class="rounded-full px-2 py-0.5 text-xs font-medium" :class="statusClass(app.status)">
                  {{ statusText(app.status) }}
                </span>
              </td>
              <td class="px-4 py-3 text-slate-600">{{ applicantName(app) }}</td>
              <td class="px-4 py-3 text-slate-600">{{ formatDateTime(app.createdAt) }}</td>
              <td class="px-4 py-3">
                <input
                  class="input"
                  :value="getNote(app.id)"
                  placeholder="请输入审核意见"
                  @input="setNote(app.id, $event.target.value)"
                />
              </td>
              <td class="px-4 py-3">
                <div v-if="app.status === APPLICATION_STATUS.SUBMITTED" class="flex gap-2">
                  <button class="btn-secondary px-3 py-1.5 text-xs" @click="openDetail(app)">详情</button>
                  <button class="btn-primary px-3 py-1.5 text-xs" :disabled="acting" @click="approve(app)">通过</button>
                  <button class="btn-secondary border-rose-200 px-3 py-1.5 text-xs text-rose-700 hover:bg-rose-50" :disabled="acting" @click="reject(app)">
                    驳回
                  </button>
                </div>
                <div v-else class="flex items-center gap-2">
                  <button class="btn-secondary px-3 py-1.5 text-xs" @click="openDetail(app)">详情</button>
                  <span class="text-xs text-slate-400">已处理</span>
                </div>
              </td>
            </tr>
            <tr v-if="pagedRows.length === 0">
              <td class="px-6 py-10 text-center text-slate-500" colspan="9">
                {{ loading ? "正在加载申请数据..." : "暂无实验室申请记录" }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <AppPagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="filteredRows.length" :page-size-options="[8,12,20]" />
    </div>
  </div>

  <el-dialog v-model="detailVisible" title="实验室申请详情" width="680px" destroy-on-close>
    <div v-if="detailItem" class="space-y-4 text-sm">
      <div class="rounded-xl border border-slate-200 bg-gradient-to-r from-slate-50 to-white px-4 py-3">
        <div class="flex flex-wrap items-center justify-between gap-3">
          <div>
            <div class="text-base font-semibold text-slate-900">{{ detailItem.title || `实验室申请 #${detailItem.id}` }}</div>
            <div class="mt-1 text-xs text-slate-500">申请ID：{{ detailItem.id }}</div>
          </div>
          <span class="rounded-full px-2.5 py-1 text-xs font-semibold" :class="statusClass(detailItem.status)">
            {{ statusText(detailItem.status) }}
          </span>
        </div>
      </div>

      <div class="grid gap-3 sm:grid-cols-2">
        <div class="rounded-lg border border-slate-200 bg-white px-3 py-2">
          <div class="text-xs text-slate-500">申请人</div>
          <div class="mt-1 text-sm font-medium text-slate-900">{{ applicantName(detailItem) }}</div>
        </div>
        <div class="rounded-lg border border-slate-200 bg-white px-3 py-2">
          <div class="text-xs text-slate-500">提交时间</div>
          <div class="mt-1 text-sm font-medium text-slate-900">{{ formatDateTime(detailItem.createdAt) }}</div>
        </div>
        <div class="rounded-lg border border-slate-200 bg-white px-3 py-2 sm:col-span-2">
          <div class="text-xs text-slate-500">申请类型</div>
          <div class="mt-1 text-sm font-medium text-slate-900">实验室申请</div>
        </div>
        <div class="rounded-lg border border-slate-200 bg-white px-3 py-2 sm:col-span-2">
          <div class="text-xs text-slate-500">开放时段</div>
          <div class="mt-1 text-sm font-medium text-slate-900">
            {{
              (() => {
                const m = parseLabApplyMeta(detailItem.detail);
                if (!m.openDateStart || !m.openDateEnd || !m.dailyStartTime || !m.dailyEndTime) return "未配置";
                return `${m.openDateStart} ~ ${m.openDateEnd} / 每日 ${m.dailyStartTime}-${m.dailyEndTime}${m.slotPreset ? `（${m.slotPreset}）` : ""}`;
              })()
            }}
          </div>
        </div>
      </div>

      <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">
        <div class="mb-2 flex items-center gap-2">
          <span class="h-1.5 w-1.5 rounded-full bg-brand-500"></span>
          <span class="text-xs font-semibold uppercase tracking-wider text-slate-500">申请内容</span>
        </div>
        <pre class="whitespace-pre-wrap break-words rounded-lg bg-white p-3 text-sm leading-relaxed text-slate-700">{{ detailItem.detail || "-" }}</pre>
      </div>

      <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">
        <div class="mb-2 flex items-center gap-2">
          <span class="h-1.5 w-1.5 rounded-full bg-slate-400"></span>
          <span class="text-xs font-semibold uppercase tracking-wider text-slate-500">当前填写的审核意见</span>
        </div>
        <div class="rounded-lg bg-white px-3 py-2 text-sm text-slate-700">{{ getNote(detailItem.id) || "（无）" }}</div>
      </div>
    </div>
    <template #footer>
      <div class="flex justify-end border-t border-slate-100 pt-3">
        <button class="btn-primary min-w-[92px]" @click="detailVisible = false">关闭</button>
      </div>
    </template>
  </el-dialog>
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
.feishu-table-wrap {
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
