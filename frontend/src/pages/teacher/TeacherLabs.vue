<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import AppPagination from "../../components/AppPagination.vue";
import { APPLICATION_STATUS, createLabApplicationApi, listApplicationsApi } from "../../services/applications";
import { getCurrentUser } from "../../services/session";
import { listLabsApi } from "../../services/resources";

const route = useRoute();
const currentUser = computed(() => getCurrentUser());
const currentSection = computed(() => (route.path.includes("/teacher/labs/applications") ? "labApply" : "labInfo"));
const keyword = ref("");
const typeFilter = ref("all");
const statusFilter = ref("all");
const buildingFilter = ref("all");
const currentPage = ref(1);
const pageSize = ref(8);
const applicationsSource = ref([]);
const labsSource = ref([]);
const showApplyDialog = ref(false);
const applySubmitting = ref(false);
const retryFromRejectedId = ref(null);
const detailVisible = ref(false);
const detailLab = ref(null);

const createLabForm = ref({
  labId: null,
  purpose: "",
  openDateStart: "",
  openDateEnd: "",
  dailyStartTime: "08:00",
  dailyEndTime: "18:00",
  slotPreset: ""
});
const slotPresetOptions = [
  { label: "上午第一段（08:00-10:00）", start: "08:00", end: "10:00" },
  { label: "上午第二段（10:00-12:00）", start: "10:00", end: "12:00" },
  { label: "下午第一段（14:00-16:00）", start: "14:00", end: "16:00" },
  { label: "下午第二段（16:00-18:00）", start: "16:00", end: "18:00" },
  { label: "晚间时段（19:00-21:00）", start: "19:00", end: "21:00" }
];

function normalizeStatus(status) {
  const s = String(status || "").toLowerCase();
  if (s === APPLICATION_STATUS.APPROVED) return APPLICATION_STATUS.APPROVED;
  if (s === APPLICATION_STATUS.REJECTED) return APPLICATION_STATUS.REJECTED;
  return APPLICATION_STATUS.SUBMITTED;
}

function parseLabReason(reason) {
  const txt = String(reason || "");
  const rows = txt.split("\n");
  const building = rows.find((r) => r.startsWith("楼栋："))?.replace("楼栋：", "") || "-";
  const capacity = rows.find((r) => r.startsWith("容量："))?.replace("容量：", "") || "-";
  const purpose = rows.find((r) => r.startsWith("用途："))?.replace("用途：", "") || txt;
  const openDateStart = rows.find((r) => r.startsWith("开放开始："))?.replace("开放开始：", "") || "";
  const openDateEnd = rows.find((r) => r.startsWith("开放结束："))?.replace("开放结束：", "") || "";
  const dailyStartTime = rows.find((r) => r.startsWith("每日开始："))?.replace("每日开始：", "") || "";
  const dailyEndTime = rows.find((r) => r.startsWith("每日结束："))?.replace("每日结束：", "") || "";
  const slotPreset = rows.find((r) => r.startsWith("时段模板："))?.replace("时段模板：", "") || "";
  return { building, capacity, purpose, openDateStart, openDateEnd, dailyStartTime, dailyEndTime, slotPreset };
}

const myApprovedLabs = computed(() => {
  const uid = currentUser.value?.id;
  if (!uid) return [];
  const appRows = applicationsSource.value
    .filter((a) => a.type === "lab_apply" && a.createdByUserId === uid && normalizeStatus(a.status) === APPLICATION_STATUS.APPROVED)
    .map((a) => {
      const parsed = parseLabReason(a.detail);
      const dbLab = labsSource.value.find((l) => String(l.name || "").trim() === String(a.title || "").trim());
      return {
        id: a.id,
        name: a.title,
        type: dbLab?.type || "COMPUTER",
        building: dbLab?.building || parsed.building,
        capacity: dbLab?.capacity || parsed.capacity,
        status: dbLab?.status || "available",
        purpose: parsed.purpose,
        reviewedAt: a.updatedAt || a.createdAt
      };
    });
  return appRows;
});

const filteredApprovedLabs = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  if (!kw) return myApprovedLabs.value;
  return myApprovedLabs.value.filter((x) => {
    const byKw =
      String(x.name || "").toLowerCase().includes(kw) ||
      String(x.building || "").toLowerCase().includes(kw) ||
      String(x.capacity || "").toLowerCase().includes(kw);
    return byKw;
  });
});

const buildingOptions = computed(() => ["all", ...new Set(myApprovedLabs.value.map((x) => x.building).filter(Boolean))]);

const queriedApprovedLabs = computed(() =>
  filteredApprovedLabs.value.filter((x) => {
    const byType = typeFilter.value === "all" || String(x.type || "").toUpperCase() === typeFilter.value;
    const byStatus = statusFilter.value === "all" || String(x.status || "").toLowerCase() === statusFilter.value;
    const byBuilding = buildingFilter.value === "all" || x.building === buildingFilter.value;
    return byType && byStatus && byBuilding;
  })
);

const pagedLabs = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return queriedApprovedLabs.value.slice(start, start + pageSize.value);
});

const myLabApplications = computed(() => {
  const uid = currentUser.value?.id;
  if (!uid) return [];
  return applicationsSource.value
    .filter((a) => a.type === "lab_apply" && a.createdByUserId === uid)
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
});

const selectableLabs = computed(() =>
  (labsSource.value || [])
    .filter((x) => x && x.id != null)
    .map((x) => ({
      ...x,
      name: x.name || x.labName || `实验室#${x.id}`
    }))
);
const selectedLab = computed(() => selectableLabs.value.find((x) => x.id === createLabForm.value.labId) || null);

function applicationTypeText(item) {
  const parsed = parseLabReason(item.detail);
  return labTypeText(parsed.labType || "COMPUTER");
}

function applicationBuildingText(item) {
  return parseLabReason(item.detail).building || "-";
}

function applicationCapacityText(item) {
  return parseLabReason(item.detail).capacity || "-";
}

function resetPage() {
  currentPage.value = 1;
}

function resourceStatusText(status) {
  const s = String(status || "").toLowerCase();
  if (s === "available") return "可用";
  if (s === "booked") return "已预约";
  if (s === "maintenance") return "维护中";
  return s || "-";
}

function resourceStatusClass(status) {
  const s = String(status || "").toLowerCase();
  if (s === "available") return "bg-emerald-100 text-emerald-700";
  if (s === "booked") return "bg-amber-100 text-amber-700";
  if (s === "maintenance") return "bg-rose-100 text-rose-700";
  return "bg-slate-100 text-slate-700";
}

function statusText(status) {
  const s = normalizeStatus(status);
  if (s === APPLICATION_STATUS.APPROVED) return "已通过";
  if (s === APPLICATION_STATUS.REJECTED) return "已驳回";
  return "待审核";
}

function statusClass(status) {
  const s = normalizeStatus(status);
  if (s === APPLICATION_STATUS.APPROVED) return "bg-emerald-100 text-emerald-700";
  if (s === APPLICATION_STATUS.REJECTED) return "bg-rose-100 text-rose-700";
  return "bg-amber-100 text-amber-700";
}

function labTypeText(type) {
  const t = String(type || "").toUpperCase();
  if (t === "BIOLOGY") return "生物实验室";
  return "计算机实验室";
}

function pickSlotPreset() {
  if (!createLabForm.value.slotPreset) return;
  const preset = slotPresetOptions.find((x) => x.label === createLabForm.value.slotPreset);
  if (!preset) return;
  createLabForm.value.dailyStartTime = preset.start;
  createLabForm.value.dailyEndTime = preset.end;
}

async function loadData() {
  const [appsRes, labsRes] = await Promise.allSettled([listApplicationsApi(), listLabsApi()]);
  applicationsSource.value = appsRes.status === "fulfilled" ? appsRes.value || [] : [];
  labsSource.value = labsRes.status === "fulfilled" ? labsRes.value || [] : [];
  if (appsRes.status === "rejected") {
    ElMessage.warning("申请列表加载失败，已仅加载实验室数据");
  }
  if (labsRes.status === "rejected") {
    ElMessage.error("实验室数据加载失败，请点击刷新后重试");
  }
}

async function submitCreateLab() {
  const uid = currentUser.value?.id;
  if (!uid) return ElMessage.error("请先登录教师账号");
  const applicantId = Number(uid);
  if (!Number.isFinite(applicantId) || applicantId <= 0) {
    return ElMessage.error("当前登录用户ID无效，请重新登录");
  }
  if (!createLabForm.value.labId) return ElMessage.warning("请选择实验室");
  const chosen = selectedLab.value;
  if (!chosen) return ElMessage.warning("所选实验室不存在");
  const labName = String(chosen.name || "").trim();
  const building = String(chosen.building || "").trim();
  const purpose = createLabForm.value.purpose.trim();
  const openDateStart = createLabForm.value.openDateStart;
  const openDateEnd = createLabForm.value.openDateEnd;
  const dailyStartTime = createLabForm.value.dailyStartTime;
  const dailyEndTime = createLabForm.value.dailyEndTime;
  if (!labName || !building || !purpose) return ElMessage.warning("请填写完整信息");
  if (!openDateStart || !openDateEnd || !dailyStartTime || !dailyEndTime) return ElMessage.warning("请完整选择开放时段");
  if (openDateStart > openDateEnd) return ElMessage.warning("开放结束日期不能早于开始日期");
  if (dailyStartTime >= dailyEndTime) return ElMessage.warning("每日结束时间必须晚于开始时间");
  const labType = String(chosen.type || "COMPUTER").toUpperCase();
  applySubmitting.value = true;
  try {
    await createLabApplicationApi({
      applicantId,
      labName,
      labType,
      reason:
        `labId：${chosen.id}\n` +
        `楼栋：${building}\n` +
        `容量：${chosen.capacity || "-"}\n` +
        `labType：${labType}\n` +
        `用途：${purpose}`,
      openDateStart,
      openDateEnd,
      dailyStartTime,
      dailyEndTime,
      slotPreset: createLabForm.value.slotPreset || "自定义"
    });
    ElMessage.success("实验室申请已提交");
    showApplyDialog.value = false;
    retryFromRejectedId.value = null;
    createLabForm.value = {
      labId: null,
      purpose: "",
      openDateStart: "",
      openDateEnd: "",
      dailyStartTime: "08:00",
      dailyEndTime: "18:00",
      slotPreset: ""
    };
    await loadData();
  } catch (e) {
    ElMessage.error(e.message || "提交失败，请检查输入后重试");
  } finally {
    applySubmitting.value = false;
  }
}

function showRejectReason(item) {
  ElMessageBox.alert(item.reviewNote || "管理员未填写驳回理由", "驳回理由", {
    confirmButtonText: "我知道了",
    type: "warning"
  });
}

function retryApplication(item) {
  const matched = selectableLabs.value.find((x) => String(x.name || "").trim() === String(item.title || "").trim());
  const parsed = parseLabReason(item.detail);
  retryFromRejectedId.value = item.id;
  createLabForm.value = {
    labId: matched?.id ?? null,
    purpose: parsed.purpose || "",
    openDateStart: parsed.openDateStart || "",
    openDateEnd: parsed.openDateEnd || "",
    dailyStartTime: parsed.dailyStartTime || "08:00",
    dailyEndTime: parsed.dailyEndTime || "18:00",
    slotPreset: parsed.slotPreset || ""
  };
  showApplyDialog.value = true;
}

function openLabDetail(row) {
  detailLab.value = row;
  detailVisible.value = true;
}

function copyLabInfo(row) {
  const text = `${row.name}\t${labTypeText(row.type)}\t${row.building}\t${row.capacity}\t${resourceStatusText(row.status)}`;
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(text).then(() => ElMessage.success("已复制实验室信息")).catch(() => ElMessage.info(text));
  } else {
    ElMessage.info(text);
  }
}

onMounted(loadData);
</script>

<template>
  <div class="flex h-full w-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-4">
      <h2 class="text-xl font-semibold text-slate-900">{{ currentSection === "labInfo" ? "实验室信息管理" : "实验室申请管理" }}</h2>
      <p class="mt-1 text-sm text-slate-500">
        {{ currentSection === "labInfo" ? "展示本人已通过申请的实验室信息。" : "发起、查看与重提实验室开放申请。" }}
      </p>
    </div>

    <div class="flex min-h-0 flex-1 flex-col gap-4 p-6">
      <div v-if="currentSection === 'labInfo'" class="flex min-h-0 flex-1 flex-col gap-4">
        <div class="grid gap-3 sm:grid-cols-3 lg:grid-cols-4">
          <div class="rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
            <div class="text-xs text-slate-500">我的实验室</div>
            <div class="mt-1 text-2xl font-semibold text-slate-900">{{ queriedApprovedLabs.length }}</div>
          </div>
          <div class="rounded-xl border border-emerald-100 bg-emerald-50/40 p-3 shadow-sm">
            <div class="text-xs text-emerald-700">可用</div>
            <div class="mt-1 text-2xl font-semibold text-emerald-800">
              {{ queriedApprovedLabs.filter((x) => String(x.status).toLowerCase() === "available").length }}
            </div>
          </div>
          <div class="rounded-xl border border-amber-100 bg-amber-50/40 p-3 shadow-sm">
            <div class="text-xs text-amber-700">已预约</div>
            <div class="mt-1 text-2xl font-semibold text-amber-800">
              {{ queriedApprovedLabs.filter((x) => String(x.status).toLowerCase() === "booked").length }}
            </div>
          </div>
          <div class="rounded-xl border border-rose-100 bg-rose-50/40 p-3 shadow-sm">
            <div class="text-xs text-rose-700">维护中</div>
            <div class="mt-1 text-2xl font-semibold text-rose-800">
              {{ queriedApprovedLabs.filter((x) => String(x.status).toLowerCase() === "maintenance").length }}
            </div>
          </div>
        </div>

        <div class="flex items-center gap-3 overflow-x-auto">
          <input v-model="keyword" class="input !w-auto min-w-[320px] flex-1" placeholder="搜索实验室名称/楼宇/容量..." @input="resetPage" />
          <select v-model="typeFilter" class="input !w-[140px] shrink-0" @change="resetPage">
            <option value="all">全部类型</option>
            <option value="COMPUTER">计算机实验室</option>
            <option value="BIOLOGY">生物实验室</option>
          </select>
          <select v-model="statusFilter" class="input !w-[140px] shrink-0" @change="resetPage">
            <option value="all">全部状态</option>
            <option value="available">可用</option>
            <option value="booked">已预约</option>
            <option value="maintenance">维护中</option>
          </select>
          <select v-model="buildingFilter" class="input !w-[160px] shrink-0" @change="resetPage">
            <option value="all">全部楼栋</option>
            <option v-for="b in buildingOptions" v-show="b !== 'all'" :key="b" :value="b">{{ b }}</option>
          </select>
        </div>

        <div class="min-h-0 flex-1 overflow-auto rounded-lg border border-slate-200 bg-white shadow-sm">
          <table class="w-full border-collapse text-sm">
            <thead class="bg-slate-100">
              <tr>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">序号</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">实验室名称</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">类型</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">楼宇</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">容量</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">状态</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="(row, idx) in pagedLabs" :key="row.id" class="transition hover:bg-slate-50">
                <td class="px-6 py-4 text-slate-500">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
                <td class="px-6 py-4 text-slate-800">{{ row.name }}</td>
                <td class="px-6 py-4 text-slate-700">{{ labTypeText(row.type) }}</td>
                <td class="px-6 py-4 text-slate-700">{{ row.building }}</td>
                <td class="px-6 py-4 text-slate-700">{{ row.capacity }}</td>
                <td class="px-6 py-4">
                  <span class="rounded-full px-2 py-0.5 text-xs font-medium" :class="resourceStatusClass(row.status)">
                    {{ resourceStatusText(row.status) }}
                  </span>
                </td>
                <td class="px-6 py-4">
                  <div class="flex flex-wrap gap-2">
                    <button class="btn-secondary px-3 py-1.5 text-xs" @click="openLabDetail(row)">详情</button>
                    <button class="btn-secondary px-3 py-1.5 text-xs" @click="copyLabInfo(row)">复制信息</button>
                  </div>
                </td>
              </tr>
              <tr v-if="pagedLabs.length === 0">
                <td colspan="7" class="px-6 py-10 text-center text-slate-500">暂无符合条件的实验室记录</td>
              </tr>
            </tbody>
          </table>
        </div>
        <AppPagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="queriedApprovedLabs.length" :page-size-options="[8,12,20]" />
      </div>

      <div v-else class="flex min-h-0 flex-1 flex-col gap-4">
        <div class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3">
          <div class="text-sm text-slate-600">通过“发起实验室申请”从已有实验室中选择目标进行申请。</div>
          <button class="btn-primary" @click="showApplyDialog = true; if (!selectableLabs.length) loadData()">发起实验室申请</button>
        </div>

        <div class="min-h-0 flex-1 overflow-auto rounded-lg border border-slate-200 bg-white shadow-sm">
          <table class="w-full border-collapse text-sm">
            <thead class="bg-slate-100">
              <tr>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">申请ID</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">实验室名称</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">类型</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">楼栋</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">容量</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">状态</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="item in myLabApplications" :key="item.id" class="transition hover:bg-slate-50">
                <td class="px-6 py-4 text-slate-600">{{ item.id }}</td>
                <td class="px-6 py-4 text-slate-800">{{ item.title }}</td>
                <td class="px-6 py-4 text-slate-700">{{ applicationTypeText(item) }}</td>
                <td class="px-6 py-4 text-slate-700">{{ applicationBuildingText(item) }}</td>
                <td class="px-6 py-4 text-slate-700">{{ applicationCapacityText(item) }}</td>
                <td class="px-6 py-4">
                  <span class="rounded-full px-2 py-0.5 text-xs font-medium" :class="statusClass(item.status)">{{ statusText(item.status) }}</span>
                </td>
                <td class="px-6 py-4">
                  <div v-if="normalizeStatus(item.status) === APPLICATION_STATUS.REJECTED" class="flex flex-wrap gap-2">
                    <button class="btn-secondary px-3 py-1.5 text-xs border-rose-200 text-rose-700 hover:bg-rose-50" @click="showRejectReason(item)">
                      查看驳回理由
                    </button>
                    <button class="btn-primary px-3 py-1.5 text-xs" @click="retryApplication(item)">修改后重提</button>
                  </div>
                  <span v-else class="text-xs text-slate-400">—</span>
                </td>
              </tr>
              <tr v-if="myLabApplications.length === 0">
                <td colspan="7" class="px-6 py-10 text-center text-slate-500">暂无申请记录</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>

  <el-dialog v-model="showApplyDialog" :title="retryFromRejectedId ? '修改并重新发起申请' : '发起实验室申请'" width="640px" destroy-on-close>
    <div class="space-y-4">
      <div v-if="retryFromRejectedId" class="rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
        正在基于被驳回申请 #{{ retryFromRejectedId }} 重新提交，请根据驳回意见修改后再发起。
      </div>
      <div>
        <label class="mb-1 block text-sm text-slate-700">选择已有实验室 <span class="text-rose-500">*</span></label>
        <select v-model.number="createLabForm.labId" class="input">
          <option :value="null">请选择实验室</option>
          <option v-for="lab in selectableLabs" :key="lab.id" :value="lab.id">
            {{ lab.name }}（{{ lab.building || "-" }}）
          </option>
        </select>
        <div v-if="!selectableLabs.length" class="mt-1 text-xs text-amber-600">
          暂无可选实验室，请先点击“刷新”或确认管理员端已维护实验室基础数据。
        </div>
      </div>

      <div v-if="selectedLab" class="grid grid-cols-2 gap-3 rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm">
        <div><span class="text-slate-500">实验室名称：</span>{{ selectedLab.name }}</div>
        <div><span class="text-slate-500">类型：</span>{{ labTypeText(selectedLab.type) }}</div>
        <div><span class="text-slate-500">楼栋：</span>{{ selectedLab.building || "-" }}</div>
        <div><span class="text-slate-500">容量：</span>{{ selectedLab.capacity || "-" }} 人</div>
      </div>

      <div>
        <label class="mb-1 block text-sm text-slate-700">用途说明 <span class="text-rose-500">*</span></label>
        <textarea
          v-model="createLabForm.purpose"
          class="input min-h-[120px] resize-y"
          placeholder="例如：用于课程实验、科研训练与开放共享..."
        ></textarea>
      </div>
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="mb-1 block text-sm text-slate-700">开放开始日期 <span class="text-rose-500">*</span></label>
          <input v-model="createLabForm.openDateStart" class="input" type="date" />
        </div>
        <div>
          <label class="mb-1 block text-sm text-slate-700">开放结束日期 <span class="text-rose-500">*</span></label>
          <input v-model="createLabForm.openDateEnd" class="input" type="date" />
        </div>
      </div>
      <div>
        <label class="mb-1 block text-sm text-slate-700">时段模板</label>
        <select v-model="createLabForm.slotPreset" class="input" @change="pickSlotPreset">
          <option value="">自定义时段</option>
          <option v-for="preset in slotPresetOptions" :key="preset.label" :value="preset.label">{{ preset.label }}</option>
        </select>
      </div>
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="mb-1 block text-sm text-slate-700">每日开始时间 <span class="text-rose-500">*</span></label>
          <input v-model="createLabForm.dailyStartTime" class="input" type="time" />
        </div>
        <div>
          <label class="mb-1 block text-sm text-slate-700">每日结束时间 <span class="text-rose-500">*</span></label>
          <input v-model="createLabForm.dailyEndTime" class="input" type="time" />
        </div>
      </div>
    </div>
    <template #footer>
      <div class="flex justify-end gap-2">
        <button class="btn-secondary" @click="showApplyDialog = false">取消</button>
        <button class="btn-primary" :disabled="applySubmitting" @click="submitCreateLab">提交申请</button>
      </div>
    </template>
  </el-dialog>

  <el-dialog v-model="detailVisible" title="实验室详情" width="560px" destroy-on-close>
    <div v-if="detailLab" class="space-y-3 text-sm">
      <div class="grid grid-cols-2 gap-3">
        <div><span class="text-slate-500">实验室名称：</span>{{ detailLab.name }}</div>
        <div><span class="text-slate-500">实验室类型：</span>{{ labTypeText(detailLab.type) }}</div>
        <div><span class="text-slate-500">楼栋：</span>{{ detailLab.building || "-" }}</div>
        <div><span class="text-slate-500">容量：</span>{{ detailLab.capacity || "-" }} 人</div>
        <div><span class="text-slate-500">状态：</span>{{ resourceStatusText(detailLab.status) }}</div>
        <div><span class="text-slate-500">申请时间：</span>{{ detailLab.reviewedAt || "-" }}</div>
      </div>
      <div class="rounded-lg border border-slate-200 bg-slate-50 p-3">
        <div class="mb-1 text-xs font-semibold text-slate-500">用途说明</div>
        <div class="whitespace-pre-wrap break-words text-sm text-slate-700">{{ detailLab.purpose || "-" }}</div>
      </div>
    </div>
    <template #footer>
      <div class="flex justify-end">
        <button class="btn-primary" @click="detailVisible = false">关闭</button>
      </div>
    </template>
  </el-dialog>
</template>
