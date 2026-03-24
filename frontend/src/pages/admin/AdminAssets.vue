<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { approveApplicationApi, APPLICATION_STATUS, listApplicationsApi, rejectApplicationApi } from "../../services/applications";
import { listUsersApi } from "../../services/users";
import { getCurrentUser } from "../../services/session";

const currentUser = computed(() => getCurrentUser());
const currentDbUser = computed(() => currentUser.value || null);
const keyword = ref("");
const filterStatus = ref("all");
const noteMap = ref({});
const applicationsSource = ref([]);
const usersSource = ref([]);
const detailVisible = ref(false);
const detailApp = ref(null);

function statusText(status) {
  if (status === APPLICATION_STATUS.SUBMITTED) return "待审核";
  if (status === APPLICATION_STATUS.APPROVED) return "已通过";
  if (status === APPLICATION_STATUS.REJECTED) return "已驳回";
  return status;
}

function statusPill(status) {
  if (status === APPLICATION_STATUS.SUBMITTED) return "bg-amber-100 text-amber-700";
  if (status === APPLICATION_STATUS.APPROVED) return "bg-green-100 text-green-700";
  if (status === APPLICATION_STATUS.REJECTED) return "bg-rose-100 text-rose-700";
  return "bg-slate-100 text-slate-600";
}

function parseDeviceDetail(detail) {
  const txt = String(detail || "");
  const rows = txt.split("\n");
  const get = (k) => rows.find((r) => r.startsWith(`${k}：`) || r.startsWith(`${k}:`))?.split(/：|:/).slice(1).join(":").trim() || "";
  return {
    category: get("类别") || "-",
    quantity: get("数量") || "-",
    budget: get("预算") || "-",
    reason: get("理由") || txt || "-"
  };
}

const deviceApplications = computed(() => {
  let list = applicationsSource.value.filter((a) => a.type === "device_apply");
  if (filterStatus.value !== "all") {
    list = list.filter((a) => a.status === filterStatus.value);
  }
  const kw = keyword.value.trim().toLowerCase();
  if (kw) {
    list = list.filter((a) => {
      const meta = parseDeviceDetail(a.detail);
      return (
        String(a.title || "").toLowerCase().includes(kw) ||
        String(meta.category || "").toLowerCase().includes(kw) ||
        String(meta.reason || "").toLowerCase().includes(kw)
      );
    });
  }
  list.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
  return list;
});

const stats = computed(() => ({
  total: deviceApplications.value.length,
  submitted: deviceApplications.value.filter((x) => x.status === APPLICATION_STATUS.SUBMITTED).length,
  approved: deviceApplications.value.filter((x) => x.status === APPLICATION_STATUS.APPROVED).length,
  rejected: deviceApplications.value.filter((x) => x.status === APPLICATION_STATUS.REJECTED).length
}));

function applicantName(app) {
  const u = usersSource.value.find((x) => x.id === app.createdByUserId);
  return u?.realName || `用户#${app.createdByUserId || "-"}`;
}

function getNote(appId) {
  return noteMap.value[appId] || "";
}

function setNote(appId, val) {
  noteMap.value = { ...noteMap.value, [appId]: val };
}

async function loadApplications() {
  applicationsSource.value = await listApplicationsApi();
}

async function approve(app) {
  if (!currentDbUser.value) return;
  await approveApplicationApi("device", app.id, {
    reviewerId: currentDbUser.value.id,
    reviewComment: getNote(app.id)
  });
  await loadApplications();
  ElMessage.success("已通过申请");
}

async function reject(app) {
  if (!currentDbUser.value) return;
  await rejectApplicationApi("device", app.id, {
    reviewerId: currentDbUser.value.id,
    reviewComment: getNote(app.id)
  });
  await loadApplications();
  ElMessage.success("已驳回申请");
}

function openDetail(app) {
  detailApp.value = app;
  detailVisible.value = true;
}

function pad2(n) {
  return String(n).padStart(2, "0");
}

function formatDate(iso) {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "-";
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}`;
}

onMounted(async () => {
  await loadApplications();
  usersSource.value = await listUsersApi();
});
</script>

<template>
  <div class="flex h-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-4">
      <h2 class="text-xl font-semibold text-slate-900">设备申请管理</h2>
      <p class="mt-1 text-sm text-slate-500">基于当前申请流程审核教师提交的设备采购/增配申请。</p>
    </div>

    <div class="flex-1 overflow-auto p-6 space-y-6">
      <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div class="rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
          <div class="text-xs text-slate-500">当前筛选总数</div>
          <div class="mt-1 text-2xl font-semibold text-slate-900">{{ stats.total }}</div>
        </div>
        <div class="rounded-xl border border-amber-100 bg-amber-50/40 p-3 shadow-sm">
          <div class="text-xs text-amber-700">待审核</div>
          <div class="mt-1 text-2xl font-semibold text-amber-900">{{ stats.submitted }}</div>
        </div>
        <div class="rounded-xl border border-emerald-100 bg-emerald-50/40 p-3 shadow-sm">
          <div class="text-xs text-emerald-700">已通过</div>
          <div class="mt-1 text-2xl font-semibold text-emerald-900">{{ stats.approved }}</div>
        </div>
        <div class="rounded-xl border border-rose-100 bg-rose-50/40 p-3 shadow-sm">
          <div class="text-xs text-rose-700">已驳回</div>
          <div class="mt-1 text-2xl font-semibold text-rose-900">{{ stats.rejected }}</div>
        </div>
      </div>

      <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">
        <div class="grid w-full max-w-full grid-cols-[minmax(0,1fr)_110px] items-center gap-3">
          <input v-model="keyword" class="input min-w-0 w-full" placeholder="搜索设备名称/类别/理由..." />
          <select v-model="filterStatus" class="input w-[110px]">
            <option value="all">全部状态</option>
            <option :value="APPLICATION_STATUS.SUBMITTED">待审核</option>
            <option :value="APPLICATION_STATUS.APPROVED">已通过</option>
            <option :value="APPLICATION_STATUS.REJECTED">已驳回</option>
          </select>
        </div>
      </div>

      <div v-if="!currentDbUser" class="rounded-xl border border-slate-200 bg-slate-50 p-6 text-sm text-slate-600">
        未登录或当前账号无效，请重新登录管理员账号。
      </div>

      <div v-else class="space-y-3">
          <div v-for="app in deviceApplications" :key="app.id" class="rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
            <div class="flex items-start justify-between gap-4">
              <div class="min-w-0 flex-1">
                <div class="flex flex-wrap items-center gap-2">
                  <div class="truncate text-base font-semibold text-slate-900">{{ app.title }}</div>
                  <span class="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-700">设备申请</span>
                  <span class="rounded-full px-2 py-0.5 text-xs font-medium" :class="statusPill(app.status)">{{ statusText(app.status) }}</span>
                </div>
                <div class="mt-2 grid grid-cols-2 gap-3 rounded-lg border border-slate-100 bg-slate-50 p-3 text-sm text-slate-700">
                  <div><span class="font-medium text-slate-800">类别：</span>{{ parseDeviceDetail(app.detail).category }}</div>
                  <div><span class="font-medium text-slate-800">数量：</span>{{ parseDeviceDetail(app.detail).quantity }}</div>
                  <div><span class="font-medium text-slate-800">预算：</span>{{ parseDeviceDetail(app.detail).budget }}</div>
                  <div class="col-span-2"><span class="font-medium text-slate-800">理由：</span>{{ parseDeviceDetail(app.detail).reason }}</div>
                </div>
                <div class="mt-2 text-xs text-slate-500">申请人：{{ applicantName(app) }}</div>
                <div class="mt-2 text-xs text-slate-500">提交时间：{{ formatDate(app.createdAt) }}</div>

                <div class="mt-3">
                  <label class="block text-xs font-medium text-slate-600">审核意见（可选）</label>
                  <input
                    class="input mt-1"
                    :value="getNote(app.id)"
                    placeholder="例如：材料齐全/需补充附件/不符合报废条件..."
                    @input="setNote(app.id, $event.target.value)"
                  />
                </div>

                <div v-if="app.reviewNote" class="mt-3 rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
                  <span class="font-medium text-slate-800">已审核意见：</span>{{ app.reviewNote }}
                </div>
              </div>

              <div class="flex flex-col gap-2">
                <button class="btn-secondary px-3 py-2 text-sm" @click="openDetail(app)">详情</button>
                <button v-if="app.status === APPLICATION_STATUS.SUBMITTED" class="btn-primary px-3 py-2 text-sm" @click="approve(app)">通过</button>
                <button
                  v-if="app.status === APPLICATION_STATUS.SUBMITTED"
                  class="btn-secondary px-3 py-2 text-sm border-rose-200 text-rose-700 hover:bg-rose-50"
                  @click="reject(app)"
                >
                  驳回
                </button>
              </div>
            </div>
          </div>

          <div v-if="deviceApplications.length === 0" class="rounded-xl border border-dashed border-slate-200 bg-slate-50 p-10 text-center text-sm text-slate-500">
            暂无符合条件的申请单
          </div>
      </div>
    </div>
  </div>

  <el-dialog v-model="detailVisible" title="申请详情" width="640px" destroy-on-close>
    <div v-if="detailApp" class="space-y-3 text-sm">
      <div class="grid grid-cols-2 gap-3">
        <div><span class="text-slate-500">申请ID：</span>{{ detailApp.id }}</div>
        <div><span class="text-slate-500">申请类型：</span>设备申请</div>
        <div><span class="text-slate-500">申请人：</span>{{ applicantName(detailApp) }}</div>
        <div><span class="text-slate-500">状态：</span>{{ statusText(detailApp.status) }}</div>
        <div class="col-span-2"><span class="text-slate-500">标题：</span>{{ detailApp.title || "-" }}</div>
      </div>
      <div class="rounded-lg border border-slate-200 bg-slate-50 p-3">
        <div class="mb-2 text-xs font-semibold text-slate-500">申请内容</div>
        <div class="grid grid-cols-2 gap-3 text-sm text-slate-700">
          <div><span class="font-medium text-slate-800">类别：</span>{{ parseDeviceDetail(detailApp.detail).category }}</div>
          <div><span class="font-medium text-slate-800">数量：</span>{{ parseDeviceDetail(detailApp.detail).quantity }}</div>
          <div><span class="font-medium text-slate-800">预算：</span>{{ parseDeviceDetail(detailApp.detail).budget }}</div>
          <div class="col-span-2"><span class="font-medium text-slate-800">理由：</span>{{ parseDeviceDetail(detailApp.detail).reason }}</div>
        </div>
      </div>
    </div>
    <template #footer>
      <div class="flex justify-end">
        <button class="btn-primary" @click="detailVisible = false">关闭</button>
      </div>
    </template>
  </el-dialog>
</template>
