<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { APPLICATION_STATUS, createDeviceApplicationApi, listApplicationsApi } from "../../services/applications";
import { listDevicesApi } from "../../services/resources";
import { getCurrentUser } from "../../services/session";

const currentUser = computed(() => getCurrentUser());
const applicationsSource = ref([]);
const devicesSource = ref([]);
const showApply = ref(false);
const submitting = ref(false);

const form = ref({
  deviceName: "",
  category: "",
  quantity: 1,
  expectedBudget: "",
  reason: ""
});

const myDeviceApps = computed(() => {
  const uid = currentUser.value?.id;
  if (!uid) return [];
  return applicationsSource.value
    .filter((x) => x.type === "device_apply" && x.createdByUserId === uid)
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
});

function statusText(status) {
  if (status === APPLICATION_STATUS.APPROVED) return "已通过";
  if (status === APPLICATION_STATUS.REJECTED) return "已驳回";
  return "待审核";
}

function statusClass(status) {
  if (status === APPLICATION_STATUS.APPROVED) return "bg-emerald-100 text-emerald-700";
  if (status === APPLICATION_STATUS.REJECTED) return "bg-rose-100 text-rose-700";
  return "bg-amber-100 text-amber-700";
}

function parseDeviceDetail(detail) {
  const txt = String(detail || "");
  const rows = txt.split("\n");
  const get = (k) => rows.find((r) => r.startsWith(`${k}：`) || r.startsWith(`${k}:`))?.split(/：|:/).slice(1).join(":").trim() || "";
  return {
    category: get("类别"),
    quantity: get("数量"),
    budget: get("预算")
  };
}

function showRejectReason(app) {
  ElMessageBox.alert(app.reviewNote || "管理员未填写驳回理由", "驳回理由", { confirmButtonText: "我知道了", type: "warning" });
}

async function loadData() {
  const [apps, devices] = await Promise.all([listApplicationsApi(), listDevicesApi()]);
  applicationsSource.value = apps || [];
  devicesSource.value = devices || [];
}

async function submitApply() {
  if (!currentUser.value?.id) return ElMessage.warning("请先登录教师账号");
  const payload = {
    applicantId: currentUser.value.id,
    deviceName: form.value.deviceName.trim(),
    category: form.value.category.trim(),
    quantity: Number(form.value.quantity || 0),
    expectedBudget: form.value.expectedBudget ? Number(form.value.expectedBudget) : null,
    reason: form.value.reason.trim()
  };
  if (!payload.deviceName || !payload.category || !payload.reason || payload.quantity <= 0) {
    return ElMessage.warning("请完整填写设备名称、类别、数量和申请理由");
  }
  submitting.value = true;
  try {
    await createDeviceApplicationApi(payload);
    ElMessage.success("设备申请已提交");
    showApply.value = false;
    form.value = { deviceName: "", category: "", quantity: 1, expectedBudget: "", reason: "" };
    await loadData();
  } finally {
    submitting.value = false;
  }
}

onMounted(loadData);
</script>

<template>
  <div class="flex h-full w-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-4">
      <h2 class="text-xl font-semibold text-slate-900">设备管理</h2>
      <p class="mt-1 text-sm text-slate-500">教师端可发起设备申请并跟踪审核，查看已入库设备状态。</p>
    </div>

    <div class="flex min-h-0 flex-1 flex-col gap-4 p-6">
      <div class="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <div class="rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
          <div class="text-xs text-slate-500">设备总数</div>
          <div class="mt-1 text-2xl font-semibold text-slate-900">{{ devicesSource.length }}</div>
        </div>
        <div class="rounded-xl border border-amber-100 bg-amber-50/40 p-3 shadow-sm">
          <div class="text-xs text-amber-700">我的待审申请</div>
          <div class="mt-1 text-2xl font-semibold text-amber-900">{{ myDeviceApps.filter((x) => x.status === APPLICATION_STATUS.SUBMITTED).length }}</div>
        </div>
        <div class="rounded-xl border border-emerald-100 bg-emerald-50/40 p-3 shadow-sm">
          <div class="text-xs text-emerald-700">我的已通过申请</div>
          <div class="mt-1 text-2xl font-semibold text-emerald-900">{{ myDeviceApps.filter((x) => x.status === APPLICATION_STATUS.APPROVED).length }}</div>
        </div>
        <div class="rounded-xl border border-rose-100 bg-rose-50/40 p-3 shadow-sm">
          <div class="text-xs text-rose-700">我的已驳回申请</div>
          <div class="mt-1 text-2xl font-semibold text-rose-900">{{ myDeviceApps.filter((x) => x.status === APPLICATION_STATUS.REJECTED).length }}</div>
        </div>
      </div>

      <div class="flex items-center justify-between rounded-lg border border-slate-200 bg-slate-50 px-4 py-3">
        <div class="text-sm text-slate-600">提交后由管理员审核，通过后系统会自动入库到设备台账。</div>
        <button class="btn-primary" @click="showApply = true">发起设备申请</button>
      </div>

      <div class="grid min-h-0 flex-1 gap-4 lg:grid-cols-2">
        <div class="min-h-0 overflow-auto rounded-lg border border-slate-200 bg-white shadow-sm">
          <div class="border-b border-slate-100 px-4 py-3 text-sm font-semibold text-slate-800">我的设备申请记录</div>
          <table class="w-full border-collapse text-sm">
            <thead class="bg-slate-100">
              <tr>
                <th class="px-4 py-3 text-left">申请ID</th>
                <th class="px-4 py-3 text-left">设备名称</th>
                <th class="px-4 py-3 text-left">数量</th>
                <th class="px-4 py-3 text-left">状态</th>
                <th class="px-4 py-3 text-left">操作</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="app in myDeviceApps" :key="app.id">
                <td class="px-4 py-3 text-slate-600">{{ app.id }}</td>
                <td class="px-4 py-3 text-slate-800">{{ app.title }}</td>
                <td class="px-4 py-3 text-slate-700">{{ parseDeviceDetail(app.detail).quantity || "-" }}</td>
                <td class="px-4 py-3">
                  <span class="rounded-full px-2 py-0.5 text-xs font-medium" :class="statusClass(app.status)">{{ statusText(app.status) }}</span>
                </td>
                <td class="px-4 py-3">
                  <button v-if="app.status === APPLICATION_STATUS.REJECTED" class="btn-secondary border-rose-200 px-3 py-1 text-xs text-rose-700" @click="showRejectReason(app)">
                    驳回理由
                  </button>
                </td>
              </tr>
              <tr v-if="myDeviceApps.length === 0">
                <td colspan="5" class="px-6 py-10 text-center text-slate-500">暂无设备申请记录</td>
              </tr>
            </tbody>
          </table>
        </div>

        <div class="min-h-0 overflow-auto rounded-lg border border-slate-200 bg-white shadow-sm">
          <div class="border-b border-slate-100 px-4 py-3 text-sm font-semibold text-slate-800">设备台账（全量）</div>
          <table class="w-full border-collapse text-sm">
            <thead class="bg-slate-100">
              <tr>
                <th class="px-4 py-3 text-left">编码</th>
                <th class="px-4 py-3 text-left">名称</th>
                <th class="px-4 py-3 text-left">类别</th>
                <th class="px-4 py-3 text-left">位置</th>
                <th class="px-4 py-3 text-left">状态</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr v-for="d in devicesSource" :key="d.id">
                <td class="px-4 py-3 text-slate-600">{{ d.deviceCode }}</td>
                <td class="px-4 py-3 text-slate-800">{{ d.name }}</td>
                <td class="px-4 py-3 text-slate-700">{{ d.category || "-" }}</td>
                <td class="px-4 py-3 text-slate-700">{{ d.location || "-" }}</td>
                <td class="px-4 py-3 text-slate-700">{{ d.status }}</td>
              </tr>
              <tr v-if="devicesSource.length === 0">
                <td colspan="5" class="px-6 py-10 text-center text-slate-500">暂无设备数据</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>

  <el-dialog v-model="showApply" title="发起设备申请" width="640px" destroy-on-close>
    <div class="space-y-4">
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="mb-1 block text-sm text-slate-700">设备名称</label>
          <input v-model="form.deviceName" class="input" placeholder="例如：示波器" />
        </div>
        <div>
          <label class="mb-1 block text-sm text-slate-700">设备类别</label>
          <input v-model="form.category" class="input" placeholder="例如：电子测量" />
        </div>
      </div>
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="mb-1 block text-sm text-slate-700">数量</label>
          <input v-model.number="form.quantity" type="number" min="1" class="input" />
        </div>
        <div>
          <label class="mb-1 block text-sm text-slate-700">预算（可选）</label>
          <input v-model="form.expectedBudget" type="number" min="0" class="input" />
        </div>
      </div>
      <div>
        <label class="mb-1 block text-sm text-slate-700">申请理由</label>
        <textarea v-model="form.reason" class="input min-h-[120px] resize-y" placeholder="请说明使用场景、必要性、预期效益"></textarea>
      </div>
    </div>
    <template #footer>
      <div class="flex justify-end gap-2">
        <button class="btn-secondary" @click="showApply = false">取消</button>
        <button class="btn-primary" :disabled="submitting" @click="submitApply">提交</button>
      </div>
    </template>
  </el-dialog>
</template>
