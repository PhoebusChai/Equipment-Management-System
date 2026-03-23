<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import { ElDialog, ElMessage } from "element-plus";
import {
  createApplication,
  createRepair,
  findUserByEmail,
  getCurrentUser,
  getResource,
  listDevices,
  listLabs,
  updateResourceStatus
} from "../../mock/db";
import { useMockDb } from "../../composables/useMockDb";

useMockDb();

const router = useRouter();

const keyword = ref("");
const typeFilter = ref("all");
const statusFilter = ref("all");
const page = ref(1);
const pageSize = ref(8);
const pageTab = ref("ledger");

const currentUser = computed(() => getCurrentUser());
const currentDbUser = computed(() => (currentUser.value ? findUserByEmail(currentUser.value.email) : null));

const statusTextMap = {
  available: "可用",
  booked: "已预约",
  maintenance: "维护中"
};

function toStatusText(status) {
  return statusTextMap[status] || "未知";
}

function toStatusClass(status) {
  if (status === "available") return "bg-emerald-100 text-emerald-700";
  if (status === "booked") return "bg-amber-100 text-amber-700";
  if (status === "maintenance") return "bg-rose-100 text-rose-700";
  return "bg-slate-100 text-slate-700";
}

const labs = computed(() => listLabs());
const devices = computed(() => listDevices());

const mergedRows = computed(() => {
  const labNameMap = labs.value.reduce((acc, lab) => {
    acc[lab.id] = lab.name;
    return acc;
  }, {});

  const labRows = labs.value.map((lab) => ({
    id: `lab-${lab.id}`,
    resourceType: "lab",
    resourceId: lab.id,
    type: "lab",
    name: lab.name,
    location: lab.building || "-",
    extra: `${lab.capacity || "-"} 人`,
    status: lab.status || "available"
  }));

  const deviceRows = devices.value.map((device) => ({
    id: `device-${device.id}`,
    resourceType: "device",
    resourceId: device.id,
    type: "device",
    name: device.name,
    location: labNameMap[device.labId] || device.location || "-",
    extra: device.assetCode || "-",
    status: device.status || "available"
  }));

  return [...labRows, ...deviceRows];
});

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return mergedRows.value.filter((row) => {
    const matchKeyword =
      !kw ||
      String(row.name).toLowerCase().includes(kw) ||
      String(row.location).toLowerCase().includes(kw) ||
      String(row.extra).toLowerCase().includes(kw);
    const matchType = typeFilter.value === "all" || row.type === typeFilter.value;
    const matchStatus = statusFilter.value === "all" || row.status === statusFilter.value;
    return matchKeyword && matchType && matchStatus;
  });
});

const total = computed(() => filteredRows.value.length);
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)));

const pagedRows = computed(() => {
  const currentPage = Math.min(page.value, totalPages.value);
  const start = (currentPage - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

const pageInfo = computed(() => {
  if (total.value === 0) return "0 / 0";
  const currentPage = Math.min(page.value, totalPages.value);
  const start = (currentPage - 1) * pageSize.value + 1;
  const end = Math.min(currentPage * pageSize.value, total.value);
  return `${start}-${end} / ${total.value}`;
});

function resetPage() {
  page.value = 1;
}

function prevPage() {
  page.value = Math.max(1, page.value - 1);
}

function nextPage() {
  page.value = Math.min(totalPages.value, page.value + 1);
}

const opBtn =
  "inline-flex shrink-0 items-center justify-center rounded-lg border border-slate-200/90 bg-white px-2.5 py-1.5 text-xs font-medium text-slate-700 shadow-sm transition hover:border-brand-300 hover:bg-brand-50/40 hover:text-brand-800 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:border-slate-200 disabled:hover:bg-white disabled:hover:text-slate-700";

/** 资源详情弹窗 */
const detailVisible = ref(false);
const detailTitle = ref("资源详情");
const detailSubtitle = ref("");
const detailFields = ref([]);

function viewResourceDetail(row) {
  const r = getResource(row.resourceType, row.resourceId);
  if (!r) {
    ElMessage.error("记录不存在或已删除");
    return;
  }
  detailTitle.value = row.resourceType === "lab" ? "实验室详情" : "设备详情";
  detailSubtitle.value = r.name || "";
  if (row.resourceType === "lab") {
    detailFields.value = [
      { label: "名称", value: r.name || "—" },
      { label: "类型", value: "实验室" },
      { label: "楼宇", value: r.building || "—" },
      { label: "容量", value: r.capacity != null ? `${r.capacity} 人` : "—" },
      { label: "状态", value: toStatusText(r.status) },
      { label: "院系", value: r.college || "—" },
      { label: "开放说明", value: r.opening || "—" },
      { label: "负责人", value: r.manager || "—" },
      { label: "简介", value: r.description || "—" }
    ];
  } else {
    detailFields.value = [
      { label: "名称", value: r.name || "—" },
      { label: "类型", value: "设备" },
      { label: "资产编号", value: r.assetCode || "—" },
      { label: "位置", value: r.location || "—" },
      { label: "状态", value: toStatusText(r.status) },
      { label: "分类", value: r.category || "—" }
    ];
  }
  detailVisible.value = true;
}

function closeDetailDialog() {
  detailVisible.value = false;
}

function onDetailDialogClosed() {
  detailFields.value = [];
}

/** 登记报修弹窗 */
const repairVisible = ref(false);
const repairTargetRow = ref(null);
const repairDescription = ref("");

function openRepairForResource(row) {
  if (!currentDbUser.value) {
    ElMessage.error("请先登录教师账号");
    return;
  }
  repairTargetRow.value = row;
  repairDescription.value = "";
  repairVisible.value = true;
}

function closeRepairDialog() {
  repairVisible.value = false;
  repairTargetRow.value = null;
  repairDescription.value = "";
}

function onRepairDialogClosed() {
  repairTargetRow.value = null;
  repairDescription.value = "";
}

function submitRepairFromDialog() {
  const row = repairTargetRow.value;
  if (!row || !currentDbUser.value) return;
  const desc = repairDescription.value.trim();
  if (!desc) {
    ElMessage.warning("请填写报修描述");
    return;
  }
  createRepair({
    createdByUserId: currentDbUser.value.id,
    handlerUserId: currentDbUser.value.id,
    resourceType: row.resourceType,
    resourceId: row.resourceId,
    resourceName: row.name,
    description: desc
  });
  ElMessage.success("报修单已创建，可在「报修工单」中跟进");
  closeRepairDialog();
}

function markResourceMaintenance(row) {
  if (row.status === "maintenance") {
    ElMessage.warning("该资源已在维护中");
    return;
  }
  try {
    updateResourceStatus(row.resourceType, row.resourceId, "maintenance");
    ElMessage.success("已标记为维护中（暂停预约）");
  } catch (e) {
    ElMessage.error(e.message || "操作失败");
  }
}

function markResourceAvailable(row) {
  if (row.status === "available") {
    ElMessage.info("当前已为可用状态");
    return;
  }
  if (row.status === "booked") {
    ElMessage.warning("当前为已预约状态，请先在「预约监管」中处理预约后再恢复可用");
    return;
  }
  try {
    updateResourceStatus(row.resourceType, row.resourceId, "available");
    ElMessage.success("已恢复为可预约/可用");
  } catch (e) {
    ElMessage.error(e.message || "操作失败");
  }
}

function goBookingSupervision(row) {
  if (row?.name) {
    router.push({ path: "/teacher/schedule", query: { q: row.name } });
  } else {
    router.push("/teacher/schedule");
  }
}

function copyResourceRow(row) {
  const typeLabel = row.type === "lab" ? "实验室" : "设备";
  const text = `${row.name}\t${typeLabel}\t${row.location}\t${row.extra}\t${toStatusText(row.status)}`;
  if (navigator.clipboard?.writeText) {
    navigator.clipboard.writeText(text).then(() => ElMessage.success("已复制到剪贴板")).catch(() => ElMessage.info(text));
  } else {
    ElMessage.info(text);
  }
}

function goRepairsPage(row) {
  if (row?.name) {
    router.push({ path: "/teacher/repairs", query: { q: row.name } });
  } else {
    router.push("/teacher/repairs");
  }
}

const pageTabs = [
  { key: "ledger", label: "资源台账" },
  { key: "lab", label: "新建实验室" },
  { key: "device", label: "新增设备" },
  { key: "doc", label: "上传文档" },
  { key: "maintenance", label: "保养计划" }
];

const createLabForm = ref({
  labName: "",
  building: "",
  capacity: "",
  purpose: ""
});

const createDeviceForm = ref({
  deviceName: "",
  category: "",
  labId: null,
  quantity: 1,
  reason: ""
});

const uploadDocForm = ref({
  docName: "",
  target: "",
  version: "",
  note: ""
});
const uploadFiles = ref([]);

const maintenanceForm = ref({
  targetName: "",
  cycle: "monthly",
  startDate: "",
  note: ""
});

function submitCreateLab() {
  if (!currentDbUser.value) return ElMessage.error("请先登录教师账号");
  const labName = createLabForm.value.labName.trim();
  const building = createLabForm.value.building.trim();
  const purpose = createLabForm.value.purpose.trim();
  if (!labName || !building || !purpose) return ElMessage.warning("请填写完整信息");

  const hasSameName = labs.value.some((l) => String(l.name || "").trim() === labName);
  const hasSameBuilding = labs.value.some((l) => String(l.building || "").trim() === building);
  if (hasSameName || hasSameBuilding) {
    return ElMessage.warning(
      hasSameName
        ? "该实验室名称已存在，请到资源台账中查看后再发起维护/变更申请"
        : "该楼栋已存在实验室，请避免重复创建"
    );
  }

  createApplication({
    type: "lab_apply",
    createdByUserId: currentDbUser.value.id,
    title: `新建实验室申请：${labName}`,
    detail: `实验室名称：${labName}\n楼栋：${building}\n容量：${createLabForm.value.capacity || "-"}\n用途：${purpose}`
  });
  ElMessage.success("实验室申请已提交");
  createLabForm.value = { labName: "", building: "", capacity: "", purpose: "" };
}

function submitCreateDevice() {
  if (!currentDbUser.value) return ElMessage.error("请先登录教师账号");
  const deviceName = createDeviceForm.value.deviceName.trim();
  const category = createDeviceForm.value.category.trim();
  const labId = createDeviceForm.value.labId;
  const reason = createDeviceForm.value.reason.trim();
  if (!deviceName || !category || !labId || !reason) return ElMessage.warning("请填写完整信息（需选择所属实验室）");
  const selectedLab = labs.value.find((l) => l.id === labId);
  const labName = selectedLab?.name || "未选择";

  createApplication({
    type: "device_apply",
    createdByUserId: currentDbUser.value.id,
    title: `新增设备申请：${deviceName}`,
    detail: `所属实验室：${labName}\n设备名称：${deviceName}\n分类：${category}\n数量：${createDeviceForm.value.quantity || 1}\n申请原因：${reason}`
  });
  ElMessage.success("设备申请已提交");
  createDeviceForm.value = { deviceName: "", category: "", labId: null, quantity: 1, reason: "" };
}

function submitUploadDoc() {
  if (!currentDbUser.value) return ElMessage.error("请先登录教师账号");
  const docName = uploadDocForm.value.docName.trim();
  const target = uploadDocForm.value.target.trim();
  if (!docName || !target) return ElMessage.warning("请填写文档名称和适用对象");

  const fileNames = uploadFiles.value.map((f) => f.name);

  createApplication({
    type: "doc_upload",
    createdByUserId: currentDbUser.value.id,
    title: `上传使用文档：${docName}`,
    detail: `文档名称：${docName}\n适用对象：${target}\n版本：${uploadDocForm.value.version || "-"}\n附件：${fileNames.length ? fileNames.join("、") : "未上传附件"}\n说明：${uploadDocForm.value.note || "-"}`
  });
  ElMessage.success("文档已提交，待管理员审核发布");
  uploadDocForm.value = { docName: "", target: "", version: "", note: "" };
  uploadFiles.value = [];
}

function submitMaintenancePlan() {
  if (!currentDbUser.value) return ElMessage.error("请先登录教师账号");
  const targetName = maintenanceForm.value.targetName.trim();
  const startDate = maintenanceForm.value.startDate;
  if (!targetName || !startDate) return ElMessage.warning("请填写维护对象和开始日期");

  const cycleMap = {
    weekly: "每周",
    monthly: "每月",
    quarterly: "每季度"
  };

  createApplication({
    type: "maintenance_plan",
    createdByUserId: currentDbUser.value.id,
    title: `维护计划：${targetName}`,
    detail: `维护对象：${targetName}\n周期：${cycleMap[maintenanceForm.value.cycle]}\n开始日期：${startDate}\n计划说明：${maintenanceForm.value.note || "-"}`
  });
  ElMessage.success("保养/维修计划已提交");
  maintenanceForm.value = { targetName: "", cycle: "monthly", startDate: "", note: "" };
}

function addUploadFiles(files) {
  if (!files.length) return;
  uploadFiles.value = [...uploadFiles.value, ...files];
}

function handleFileChange(event) {
  const files = Array.from(event.target.files || []);
  addUploadFiles(files);
  event.target.value = "";
}

function handleFileDrop(event) {
  const files = Array.from(event.dataTransfer?.files || []);
  addUploadFiles(files);
}

function removeUploadFile(index) {
  uploadFiles.value = uploadFiles.value.filter((_, i) => i !== index);
}
</script>

<template>
  <div class="flex h-full w-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-4">
      <h2 class="text-xl font-semibold text-slate-900">实验室与设备管理</h2>
      <p class="mt-1 text-sm text-slate-500">通过标签页管理资源台账、实验室申请、设备申请、文档上传与保养计划。</p>
    </div>

    <div class="flex min-h-0 flex-1 flex-col gap-4 p-6">
      <div class="flex flex-wrap gap-2">
        <button
          v-for="tab in pageTabs"
          :key="tab.key"
          class="rounded-lg border px-3 py-2 text-sm transition"
          :class="
            pageTab === tab.key
              ? 'border-brand-500 bg-brand-50 text-brand-700'
              : 'border-slate-200 bg-white text-slate-600 hover:bg-slate-50'
          "
          @click="pageTab = tab.key"
        >
          {{ tab.label }}
        </button>
      </div>

      <div v-if="pageTab === 'ledger'" class="flex min-h-0 flex-1 flex-col gap-4">
        <div class="flex items-center gap-3 overflow-x-auto">
          <input
            v-model="keyword"
            class="input !w-auto min-w-[320px] flex-1"
            placeholder="搜索名称/位置/编号..."
            @input="resetPage"
          />
          <select v-model="typeFilter" class="input !w-[140px] shrink-0" @change="resetPage">
            <option value="all">全部类型</option>
            <option value="lab">实验室</option>
            <option value="device">设备</option>
          </select>
          <select v-model="statusFilter" class="input !w-[140px] shrink-0" @change="resetPage">
            <option value="all">全部状态</option>
            <option value="available">可用</option>
            <option value="booked">已预约</option>
            <option value="maintenance">维护中</option>
          </select>
        </div>

        <div class="min-h-0 flex-1 overflow-auto rounded-lg border border-slate-200 bg-white shadow-sm">
          <table class="w-full border-collapse text-sm">
            <thead class="bg-slate-100">
              <tr>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">序号</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">资源名称</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">类型</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">位置 / 所属实验室</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">容量 / 资产编号</th>
                <th class="px-6 py-4 text-left font-semibold text-slate-700">状态</th>
                <th class="min-w-[360px] px-3 py-4 text-left font-semibold text-slate-700">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="(row, idx) in pagedRows" :key="row.id" class="transition hover:bg-slate-50">
                <td class="px-6 py-4 font-medium text-slate-500">{{ (page - 1) * pageSize + idx + 1 }}</td>
                <td class="px-6 py-4 text-slate-700">{{ row.name }}</td>
                <td class="px-6 py-4 text-slate-700">{{ row.type === "lab" ? "实验室" : "设备" }}</td>
                <td class="px-6 py-4 text-slate-700">{{ row.location }}</td>
                <td class="px-6 py-4 text-slate-700">{{ row.extra }}</td>
                <td class="px-6 py-4 text-slate-700">
                  <span class="rounded-full px-2 py-0.5 text-xs font-medium" :class="toStatusClass(row.status)">
                    {{ toStatusText(row.status) }}
                  </span>
                </td>
                <td class="px-3 py-3 align-middle">
                  <div class="flex flex-wrap items-center gap-1.5">
                    <button type="button" :class="opBtn" @click="viewResourceDetail(row)">详情</button>
                    <button type="button" :class="opBtn" @click="goBookingSupervision(row)">预约监管</button>
                    <button type="button" :class="opBtn" @click="markResourceMaintenance(row)">标记维护</button>
                    <button
                      type="button"
                      :class="opBtn"
                      :disabled="row.status !== 'maintenance'"
                      :title="row.status !== 'maintenance' ? '仅维护中状态可一键恢复可用' : ''"
                      @click="markResourceAvailable(row)"
                    >
                      恢复可用
                    </button>
                    <button type="button" :class="opBtn" @click="openRepairForResource(row)">登记报修</button>
                    <button type="button" :class="opBtn" @click="copyResourceRow(row)">复制信息</button>
                    <button type="button" :class="opBtn" @click="goRepairsPage(row)">工单页</button>
                  </div>
                </td>
              </tr>
              <tr v-if="pagedRows.length === 0">
                <td colspan="7" class="px-6 py-10 text-center text-slate-500">暂无符合条件的记录</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-slate-200 bg-slate-50 px-4 py-3">
          <div class="text-sm text-slate-600">
            共 <span class="font-medium text-slate-800">{{ total }}</span> 条，当前显示
            <span class="font-medium text-slate-800">{{ pageInfo }}</span>
          </div>
          <div class="flex items-center gap-2">
            <span class="text-sm text-slate-600">每页</span>
            <select v-model.number="pageSize" class="input !w-[88px]" @change="resetPage">
              <option :value="5">5</option>
              <option :value="8">8</option>
              <option :value="10">10</option>
              <option :value="20">20</option>
            </select>
            <button
              class="inline-flex h-9 items-center justify-center rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-700 shadow-sm transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="page <= 1"
              @click="prevPage"
            >
              上一页
            </button>
            <div class="inline-flex h-9 min-w-[72px] items-center justify-center rounded-lg border border-slate-200 bg-white px-3 text-sm font-medium text-slate-700">
              {{ page }} / {{ totalPages }}
            </div>
            <button
              class="inline-flex h-9 items-center justify-center rounded-lg border border-slate-300 bg-white px-3 text-sm text-slate-700 shadow-sm transition hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-50"
              :disabled="page >= totalPages"
              @click="nextPage"
            >
              下一页
            </button>
          </div>
        </div>
      </div>

      <div v-else class="mx-auto w-full" :class="pageTab === 'doc' ? 'max-w-5xl' : 'max-w-3xl'">
        <div class="overflow-hidden rounded-2xl border border-slate-200/90 bg-white shadow-sm ring-1 ring-slate-100">
          <div class="border-b border-slate-100 bg-gradient-to-r from-slate-50 to-brand-50/30 px-6 py-4">
            <h3 class="text-base font-semibold text-slate-900">
              {{
                pageTab === "lab"
                  ? "新建实验室申请"
                  : pageTab === "device"
                    ? "新增设备申请"
                    : pageTab === "doc"
                      ? "上传设备使用文档"
                      : "保养 / 维修计划"
              }}
            </h3>
            <p class="mt-1 text-xs text-slate-500">
              {{
                pageTab === "lab"
                  ? "填写基础信息后提交，将由管理员审核后建档。"
                  : pageTab === "device"
                    ? "说明采购或新增设备用途，便于资产与预算审批。"
                    : pageTab === "doc"
                      ? "补充说明与附件后提交，供管理员审核发布。"
                      : "制定周期性维护安排，便于实验室与设备持续可用。"
              }}
            </p>
          </div>

          <div class="space-y-6 p-6">
            <div v-if="pageTab === 'lab'" class="space-y-6">
              <div class="rounded-xl border border-slate-100 bg-slate-50/50 p-4">
                <p class="mb-3 text-xs font-semibold uppercase tracking-wider text-slate-400">基本信息</p>
                <div class="space-y-4">
                  <div class="space-y-1.5">
                    <label class="text-sm font-medium text-slate-700">实验室名称 <span class="text-rose-500">*</span></label>
                    <input v-model="createLabForm.labName" class="input" placeholder="例如：人工智能实验室 A101" />
                  </div>
                  <div class="grid gap-4 sm:grid-cols-2">
                    <div class="space-y-1.5">
                      <label class="text-sm font-medium text-slate-700">楼栋 <span class="text-rose-500">*</span></label>
                      <input v-model="createLabForm.building" class="input" placeholder="例如：实验楼 A 栋" />
                    </div>
                    <div class="space-y-1.5">
                      <label class="text-sm font-medium text-slate-700">容量（人）</label>
                      <input v-model="createLabForm.capacity" class="input" type="number" min="1" placeholder="例如：40" />
                    </div>
                  </div>
                </div>
              </div>
              <div class="rounded-xl border border-slate-100 bg-slate-50/50 p-4">
                <p class="mb-3 text-xs font-semibold uppercase tracking-wider text-slate-400">用途说明</p>
                <div class="space-y-1.5">
                  <label class="text-sm font-medium text-slate-700">详细描述 <span class="text-rose-500">*</span></label>
                  <textarea
                    v-model="createLabForm.purpose"
                    class="input min-h-[120px] resize-y"
                    placeholder="例如：用于 AI 课程实验、竞赛训练与开放实验..."
                  ></textarea>
                  <p class="text-xs text-slate-400">建议写明主要课程、使用频率与安全要求。</p>
                </div>
              </div>
              <div class="flex justify-end border-t border-slate-100 pt-5">
                <button type="button" class="btn-primary min-w-[140px] shadow-sm" @click="submitCreateLab">提交实验室申请</button>
              </div>
            </div>

            <div v-else-if="pageTab === 'device'" class="space-y-6">
              <div class="rounded-xl border border-slate-100 bg-slate-50/50 p-4">
                <p class="mb-3 text-xs font-semibold uppercase tracking-wider text-slate-400">设备信息</p>
                <div class="space-y-4">
                  <div class="space-y-1.5">
                    <label class="text-sm font-medium text-slate-700">设备名称 <span class="text-rose-500">*</span></label>
                    <input v-model="createDeviceForm.deviceName" class="input" placeholder="例如：GPU 服务器" />
                  </div>
                  <div class="space-y-1.5">
                    <label class="text-sm font-medium text-slate-700">所属实验室 <span class="text-rose-500">*</span></label>
                    <select v-model="createDeviceForm.labId" class="input">
                      <option :value="null">请选择</option>
                      <option v-for="l in labs" :key="l.id" :value="l.id">
                        {{ l.name }}
                      </option>
                    </select>
                  </div>
                  <div class="grid gap-4 sm:grid-cols-2">
                    <div class="space-y-1.5">
                      <label class="text-sm font-medium text-slate-700">设备分类 <span class="text-rose-500">*</span></label>
                      <input v-model="createDeviceForm.category" class="input" placeholder="例如：计算设备 / 精密仪器" />
                    </div>
                    <div class="space-y-1.5">
                      <label class="text-sm font-medium text-slate-700">申请数量</label>
                      <input v-model.number="createDeviceForm.quantity" class="input" type="number" min="1" />
                    </div>
                  </div>
                </div>
              </div>
              <div class="rounded-xl border border-slate-100 bg-slate-50/50 p-4">
                <p class="mb-3 text-xs font-semibold uppercase tracking-wider text-slate-400">申请原因</p>
                <div class="space-y-1.5">
                  <label class="text-sm font-medium text-slate-700">说明 <span class="text-rose-500">*</span></label>
                  <textarea
                    v-model="createDeviceForm.reason"
                    class="input min-h-[120px] resize-y"
                    placeholder="例如：满足深度学习课程并发训练、科研课题算力需求..."
                  ></textarea>
                </div>
              </div>
              <div class="flex justify-end border-t border-slate-100 pt-5">
                <button type="button" class="btn-primary min-w-[140px] shadow-sm" @click="submitCreateDevice">提交设备申请</button>
              </div>
            </div>

            <div v-else-if="pageTab === 'doc'" class="space-y-5">
              <!-- 宽屏：左表单 + 右上传；窄屏：纵向堆叠 -->
              <div class="grid gap-6 lg:grid-cols-2 lg:items-stretch">
                <div class="flex flex-col rounded-xl border border-slate-200/90 bg-slate-50/60 p-5">
                  <h4 class="mb-4 text-sm font-semibold text-slate-800">文档信息</h4>
                  <div class="flex flex-1 flex-col space-y-4">
                    <div class="space-y-1.5">
                      <label class="text-sm font-medium text-slate-800">文档名称 <span class="text-rose-500">*</span></label>
                      <input v-model="uploadDocForm.docName" class="input" placeholder="例如：显微镜安全操作手册 V1.0" />
                    </div>
                    <div class="grid gap-4 sm:grid-cols-2">
                      <div class="space-y-1.5">
                        <label class="text-sm font-medium text-slate-800">适用对象 <span class="text-rose-500">*</span></label>
                        <input v-model="uploadDocForm.target" class="input" placeholder="例如：生物实验室 B201" />
                      </div>
                      <div class="space-y-1.5">
                        <label class="text-sm font-medium text-slate-800">版本</label>
                        <input v-model="uploadDocForm.version" class="input" placeholder="例如：v1.0" />
                      </div>
                    </div>
                    <div class="flex min-h-0 flex-1 flex-col space-y-1.5">
                      <label class="text-sm font-medium text-slate-800">说明 <span class="font-normal text-slate-500">（选填）</span></label>
                      <textarea
                        v-model="uploadDocForm.note"
                        rows="4"
                        class="input min-h-0 flex-1 resize-y py-2.5 leading-relaxed text-slate-800 placeholder:text-slate-400"
                        placeholder="简要说明文档用途、适用人群或注意事项..."
                      ></textarea>
                    </div>
                  </div>
                </div>

                <div class="flex flex-col rounded-xl border border-slate-200/90 bg-slate-50/60 p-5">
                  <h4 class="mb-2 text-sm font-semibold text-slate-800">附件</h4>
                  <p class="mb-3 text-xs leading-relaxed text-slate-600">支持多选。当前为演示环境，仅保存文件名至申请记录，不上传服务器。</p>
                  <label
                    class="group flex min-h-[200px] flex-1 cursor-pointer flex-col items-center justify-center rounded-xl border-2 border-dashed border-slate-300/90 bg-white px-4 py-6 text-center shadow-inner transition hover:border-brand-400 hover:bg-brand-50/30"
                    @dragover.prevent
                    @drop.prevent="handleFileDrop"
                  >
                    <span class="text-sm font-semibold text-slate-700 group-hover:text-brand-700">点击选择文件</span>
                    <span class="mt-2 max-w-[220px] text-xs text-slate-500">或将文件拖到此处释放</span>
                    <input class="hidden" type="file" multiple @change="handleFileChange" />
                  </label>
                  <div v-if="uploadFiles.length > 0" class="mt-3 max-h-40 space-y-2 overflow-y-auto pr-1">
                    <div
                      v-for="(file, idx) in uploadFiles"
                      :key="`${file.name}-${idx}`"
                      class="flex items-center justify-between gap-3 rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm"
                    >
                      <span class="min-w-0 flex-1 truncate text-slate-800">{{ file.name }}</span>
                      <button
                        type="button"
                        class="shrink-0 rounded-md px-2 py-1 text-xs font-medium text-rose-600 hover:bg-rose-50"
                        @click="removeUploadFile(idx)"
                      >
                        移除
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <div class="flex justify-end border-t border-slate-100 pt-4">
                <button type="button" class="btn-primary min-w-[140px] shadow-sm" @click="submitUploadDoc">提交文档</button>
              </div>
            </div>

            <div v-else class="space-y-6">
              <div class="rounded-xl border border-slate-100 bg-slate-50/50 p-4">
                <p class="mb-3 text-xs font-semibold uppercase tracking-wider text-slate-400">计划对象与时间</p>
                <div class="space-y-4">
                  <div class="space-y-1.5">
                    <label class="text-sm font-medium text-slate-700">维护对象 <span class="text-rose-500">*</span></label>
                    <input v-model="maintenanceForm.targetName" class="input" placeholder="例如：计算机实验室 A301 / 某台设备" />
                  </div>
                  <div class="grid gap-4 sm:grid-cols-2">
                    <div class="space-y-1.5">
                      <label class="text-sm font-medium text-slate-700">维护周期</label>
                      <select v-model="maintenanceForm.cycle" class="input">
                        <option value="weekly">每周</option>
                        <option value="monthly">每月</option>
                        <option value="quarterly">每季度</option>
                      </select>
                    </div>
                    <div class="space-y-1.5">
                      <label class="text-sm font-medium text-slate-700">开始日期 <span class="text-rose-500">*</span></label>
                      <input v-model="maintenanceForm.startDate" class="input" type="date" />
                    </div>
                  </div>
                </div>
              </div>
              <div class="rounded-xl border border-slate-100 bg-slate-50/50 p-4">
                <p class="mb-3 text-xs font-semibold uppercase tracking-wider text-slate-400">计划说明</p>
                <div class="space-y-1.5">
                  <label class="text-sm font-medium text-slate-700">详细内容</label>
                  <textarea
                    v-model="maintenanceForm.note"
                    class="input min-h-[120px] resize-y"
                    placeholder="例如：每月第一周周三巡检、耗材更换与设备校准..."
                  ></textarea>
                </div>
              </div>
              <div class="flex justify-end border-t border-slate-100 pt-5">
                <button type="button" class="btn-primary min-w-[140px] shadow-sm" @click="submitMaintenancePlan">提交保养计划</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 资源详情（美化弹窗） -->
  <el-dialog
    v-model="detailVisible"
    :title="detailTitle"
    width="520px"
    align-center
    destroy-on-close
    append-to-body
    class="ems-resource-detail-dialog"
    @closed="onDetailDialogClosed"
  >
    <div class="-mt-1 border-b border-slate-100 pb-4">
      <p class="text-sm font-medium text-slate-800">{{ detailSubtitle }}</p>
      <p class="mt-1 text-xs text-slate-500">以下为当前资源在系统中的登记信息</p>
    </div>
    <div class="mt-4 overflow-hidden rounded-xl border border-slate-200/80 bg-slate-50/40">
      <div
        v-for="(f, i) in detailFields"
        :key="i"
        class="grid grid-cols-[92px_minmax(0,1fr)] gap-x-3 gap-y-1 border-b border-slate-200/60 bg-white px-4 py-3 last:border-b-0"
      >
        <span class="pt-0.5 text-xs font-semibold text-slate-500">{{ f.label }}</span>
        <span class="text-sm leading-relaxed text-slate-900">{{ f.value }}</span>
      </div>
    </div>
    <template #footer>
      <div class="flex justify-end gap-2 border-t border-slate-100 pt-2">
        <button type="button" class="btn-primary px-6" @click="closeDetailDialog">关闭</button>
      </div>
    </template>
  </el-dialog>

  <!-- 登记报修（美化弹窗） -->
  <el-dialog
    v-model="repairVisible"
    title="登记报修"
    width="480px"
    align-center
    destroy-on-close
    append-to-body
    class="ems-repair-dialog"
    @closed="onRepairDialogClosed"
  >
    <div v-if="repairTargetRow" class="space-y-4">
      <div class="flex flex-wrap items-center gap-2 rounded-xl border border-slate-200 bg-gradient-to-r from-slate-50 to-brand-50/20 px-4 py-3">
        <span class="text-xs font-medium text-slate-500">报修对象</span>
        <span class="text-sm font-semibold text-slate-900">{{ repairTargetRow.name }}</span>
        <span
          class="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-medium text-blue-800"
        >
          {{ repairTargetRow.type === "lab" ? "实验室" : "设备" }}
        </span>
      </div>
      <div class="space-y-1.5">
        <label class="text-sm font-medium text-slate-800">故障 / 需求描述 <span class="text-rose-500">*</span></label>
        <textarea
          v-model="repairDescription"
          class="input min-h-[120px] w-full resize-y py-2.5 leading-relaxed"
          placeholder="例如：设备无法开机、需更换配件、实验室空调异响、需消杀…"
        />
      </div>
    </div>
    <template #footer>
      <div class="flex justify-end gap-2 border-t border-slate-100 pt-2">
        <button type="button" class="btn-secondary" @click="closeRepairDialog">取消</button>
        <button type="button" class="btn-primary" @click="submitRepairFromDialog">提交报修</button>
      </div>
    </template>
  </el-dialog>
</template>
