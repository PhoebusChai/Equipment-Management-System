<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { createDeviceApi, listDevicesApi, listLabsApi } from "../../services/resources";

const devicesSource = ref([]);
const labsSource = ref([]);
const keyword = ref("");
const showCreate = ref(false);
const creating = ref({
  labId: null,
  deviceCode: "",
  name: "",
  category: "",
  location: "",
  status: "AVAILABLE"
});

const filtered = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return devicesSource.value.filter((x) => {
    if (!kw) return true;
    return (
      String(x.deviceCode || "").toLowerCase().includes(kw) ||
      String(x.name || "").toLowerCase().includes(kw) ||
      String(x.category || "").toLowerCase().includes(kw) ||
      String(x.location || "").toLowerCase().includes(kw)
    );
  });
});

function labName(labId) {
  return labsSource.value.find((x) => x.id === labId)?.name || `实验室#${labId || "-"}`;
}

async function loadData() {
  const [devices, labs] = await Promise.all([listDevicesApi(), listLabsApi()]);
  devicesSource.value = devices || [];
  labsSource.value = labs || [];
}

async function submitCreate() {
  if (!creating.value.labId || !creating.value.deviceCode.trim() || !creating.value.name.trim()) {
    return ElMessage.warning("请填写实验室、设备编码和设备名称");
  }
  await createDeviceApi({
    labId: creating.value.labId,
    deviceCode: creating.value.deviceCode.trim(),
    name: creating.value.name.trim(),
    category: creating.value.category.trim(),
    location: creating.value.location.trim(),
    status: creating.value.status
  });
  ElMessage.success("设备创建成功");
  showCreate.value = false;
  creating.value = { labId: null, deviceCode: "", name: "", category: "", location: "", status: "AVAILABLE" };
  await loadData();
}

onMounted(loadData);
</script>

<template>
  <div class="flex h-full w-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-4">
      <h2 class="text-xl font-semibold text-slate-900">设备信息管理</h2>
      <p class="mt-1 text-sm text-slate-500">管理员维护设备台账，支持设备新增与信息检索。</p>
    </div>
    <div class="flex min-h-0 flex-1 flex-col gap-4 p-6">
      <div class="flex items-center gap-3">
        <input v-model="keyword" class="input flex-1" placeholder="搜索设备编码/名称/类别/位置..." />
        <button class="btn-secondary" @click="loadData">刷新</button>
        <button class="btn-primary" @click="showCreate = true">新增设备</button>
      </div>

      <div class="min-h-0 flex-1 overflow-auto rounded-lg border border-slate-200 bg-white shadow-sm">
        <table class="w-full border-collapse text-sm">
          <thead class="bg-slate-100">
            <tr>
              <th class="px-4 py-3 text-left">设备编码</th>
              <th class="px-4 py-3 text-left">设备名称</th>
              <th class="px-4 py-3 text-left">类别</th>
              <th class="px-4 py-3 text-left">所属实验室</th>
              <th class="px-4 py-3 text-left">位置</th>
              <th class="px-4 py-3 text-left">状态</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr v-for="d in filtered" :key="d.id">
              <td class="px-4 py-3 text-slate-600">{{ d.deviceCode }}</td>
              <td class="px-4 py-3 text-slate-900">{{ d.name }}</td>
              <td class="px-4 py-3 text-slate-700">{{ d.category || "-" }}</td>
              <td class="px-4 py-3 text-slate-700">{{ labName(d.labId) }}</td>
              <td class="px-4 py-3 text-slate-700">{{ d.location || "-" }}</td>
              <td class="px-4 py-3 text-slate-700">{{ d.status }}</td>
            </tr>
            <tr v-if="filtered.length === 0">
              <td colspan="6" class="px-6 py-10 text-center text-slate-500">暂无设备数据</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>

  <el-dialog v-model="showCreate" title="新增设备" width="640px" destroy-on-close>
    <div class="space-y-4">
      <div>
        <label class="mb-1 block text-sm text-slate-700">所属实验室</label>
        <select v-model.number="creating.labId" class="input">
          <option :value="null">请选择实验室</option>
          <option v-for="lab in labsSource" :key="lab.id" :value="lab.id">{{ lab.name }}</option>
        </select>
      </div>
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="mb-1 block text-sm text-slate-700">设备编码</label>
          <input v-model="creating.deviceCode" class="input" />
        </div>
        <div>
          <label class="mb-1 block text-sm text-slate-700">设备名称</label>
          <input v-model="creating.name" class="input" />
        </div>
      </div>
      <div class="grid grid-cols-2 gap-3">
        <div>
          <label class="mb-1 block text-sm text-slate-700">类别</label>
          <input v-model="creating.category" class="input" />
        </div>
        <div>
          <label class="mb-1 block text-sm text-slate-700">位置</label>
          <input v-model="creating.location" class="input" />
        </div>
      </div>
      <div>
        <label class="mb-1 block text-sm text-slate-700">状态</label>
        <select v-model="creating.status" class="input">
          <option value="AVAILABLE">可用</option>
          <option value="IN_USE">占用中</option>
          <option value="MAINTENANCE">维护中</option>
        </select>
      </div>
    </div>
    <template #footer>
      <div class="flex justify-end gap-2">
        <button class="btn-secondary" @click="showCreate = false">取消</button>
        <button class="btn-primary" @click="submitCreate">创建</button>
      </div>
    </template>
  </el-dialog>
</template>
