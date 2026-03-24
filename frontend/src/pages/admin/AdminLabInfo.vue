<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import AppPagination from "../../components/AppPagination.vue";
import { getCurrentUser } from "../../services/session";
import { createLabApi, getLabApi, listLabsApi } from "../../services/resources";

const currentUser = computed(() => getCurrentUser());
const keyword = ref("");
const statusFilter = ref("all");
const typeFilter = ref("all");
const currentPage = ref(1);
const pageSize = ref(8);
const labsSource = ref([]);
const loading = ref(false);

const showCreate = ref(false);
const creating = ref({
  labCode: "",
  labName: "",
  labType: "COMPUTER",
  building: "",
  college: "",
  capacity: "",
  description: "",
  images: []
});
const imagePreviews = ref([]);

const showDetail = ref(false);
const detail = ref(null);
const detailLoading = ref(false);

const statusTextMap = {
  available: "可用",
  booked: "占用中",
  maintenance: "维护中"
};

const statusClassMap = {
  available: "bg-emerald-100 text-emerald-700",
  booked: "bg-amber-100 text-amber-700",
  maintenance: "bg-rose-100 text-rose-700"
};

const typeTextMap = {
  COMPUTER: "计算机实验室",
  BIOLOGY: "生物实验室"
};

const stats = computed(() => ({
  total: labsSource.value.length,
  available: labsSource.value.filter((x) => x.status === "available").length,
  booked: labsSource.value.filter((x) => x.status === "booked").length,
  maintenance: labsSource.value.filter((x) => x.status === "maintenance").length,
  avgCapacity: labsSource.value.length
    ? Math.round(labsSource.value.reduce((sum, x) => sum + Number(x.capacity || 0), 0) / labsSource.value.length)
    : 0
}));

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return labsSource.value.filter((lab) => {
    const matchKeyword =
      !kw ||
      String(lab.name || "").toLowerCase().includes(kw) ||
      String(lab.labCode || "").toLowerCase().includes(kw) ||
      String(lab.building || "").toLowerCase().includes(kw) ||
      String(lab.college || "").toLowerCase().includes(kw);
    const matchStatus = statusFilter.value === "all" || lab.status === statusFilter.value;
    const matchType = typeFilter.value === "all" || String(lab.type || "").toUpperCase() === typeFilter.value;
    return matchKeyword && matchStatus && matchType;
  });
});

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

function resetPage() {
  currentPage.value = 1;
}

function statusText(status) {
  return statusTextMap[status] || "未知";
}

function statusClass(status) {
  return statusClassMap[status] || "bg-slate-100 text-slate-700";
}

function typeText(type) {
  return typeTextMap[String(type || "").toUpperCase()] || type || "-";
}

async function loadLabs() {
  loading.value = true;
  try {
    labsSource.value = await listLabsApi();
  } finally {
    loading.value = false;
  }
}

function openCreate() {
  creating.value = { labCode: "", labName: "", labType: "COMPUTER", building: "", college: "", capacity: "", description: "", images: [] };
  imagePreviews.value.forEach((x) => URL.revokeObjectURL(x.url));
  imagePreviews.value = [];
  showCreate.value = true;
}

function onImageChange(event) {
  const files = Array.from(event.target.files || []);
  if (!files.length) return;
  const imageFiles = files.filter((f) => f.type.startsWith("image/"));
  const merged = [...creating.value.images, ...imageFiles].slice(0, 12);
  creating.value.images = merged;
  imagePreviews.value.forEach((x) => URL.revokeObjectURL(x.url));
  imagePreviews.value = merged.map((file, idx) => ({
    name: file.name,
    size: Math.round(file.size / 1024),
    url: URL.createObjectURL(file),
    idx
  }));
  event.target.value = "";
}

function removeImage(index) {
  const next = creating.value.images.filter((_, i) => i !== index);
  creating.value.images = next;
  imagePreviews.value.forEach((x) => URL.revokeObjectURL(x.url));
  imagePreviews.value = next.map((file, idx) => ({
    name: file.name,
    size: Math.round(file.size / 1024),
    url: URL.createObjectURL(file),
    idx
  }));
}

async function submitCreate() {
  if (!currentUser.value?.id) return ElMessage.warning("请先登录管理员账号");
  const labCode = creating.value.labCode.trim();
  const labName = creating.value.labName.trim();
  const building = creating.value.building.trim();
  if (!labCode || !labName || !building) return ElMessage.warning("请填写实验室编码、名称和楼宇");
  try {
    await createLabApi({
      labCode,
      name: labName,
      type: creating.value.labType,
      building,
      college: creating.value.college.trim() || null,
      capacity: creating.value.capacity ? Number(creating.value.capacity) : null,
      description: creating.value.description.trim() || null,
      images: creating.value.images
    });
    ElMessage.success("实验室添加成功");
    showCreate.value = false;
    imagePreviews.value.forEach((x) => URL.revokeObjectURL(x.url));
    imagePreviews.value = [];
    await loadLabs();
  } catch {
    // http 拦截器已提示后端错误信息，这里避免未捕获异常导致 Vue 警告
  }
}

async function openDetail(row) {
  detailLoading.value = true;
  showDetail.value = true;
  detail.value = null;
  try {
    detail.value = await getLabApi(row.id);
  } catch {
    ElMessage.error("加载详情失败");
  } finally {
    detailLoading.value = false;
  }
}

function exportCsv() {
  const header = ["实验室编码", "名称", "类型", "楼宇", "学院", "容量", "状态"];
  const body = filteredRows.value.map((x) => [
    x.labCode || "",
    x.name || "",
    typeText(x.type),
    x.building || "",
    x.college || "",
    String(x.capacity || 0),
    statusText(x.status)
  ]);
  const csv = [header, ...body].map((line) => line.map((v) => `"${String(v).replace(/"/g, '""')}"`).join(",")).join("\n");
  const blob = new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `实验室信息_${new Date().toISOString().slice(0, 10)}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}

onMounted(loadLabs);
</script>

<template>
  <div class="feishu-page flex h-full w-full flex-col">
    <div class="border-b border-slate-200/80 bg-white px-6 py-5">
      <h2 class="text-xl font-semibold text-slate-900">实验室信息管理</h2>
      <p class="mt-1 text-sm text-slate-500">实验室信息查询、筛选统计、详情查看与新增实验室。</p>
    </div>

    <div class="flex min-h-0 flex-1 flex-col gap-4 p-6">
      <div class="grid grid-cols-2 gap-3 sm:grid-cols-5">
        <div class="feishu-stat-card rounded-xl border border-slate-200 bg-white p-3 shadow-sm">
          <div class="text-xs text-slate-500">实验室总数</div>
          <div class="mt-1 text-2xl font-semibold text-slate-900">{{ stats.total }}</div>
        </div>
        <div class="feishu-stat-card rounded-xl border border-emerald-100 bg-emerald-50/40 p-3 shadow-sm">
          <div class="text-xs text-emerald-700">可用</div>
          <div class="mt-1 text-2xl font-semibold text-emerald-900">{{ stats.available }}</div>
        </div>
        <div class="feishu-stat-card rounded-xl border border-amber-100 bg-amber-50/40 p-3 shadow-sm">
          <div class="text-xs text-amber-700">占用中</div>
          <div class="mt-1 text-2xl font-semibold text-amber-900">{{ stats.booked }}</div>
        </div>
        <div class="feishu-stat-card rounded-xl border border-rose-100 bg-rose-50/40 p-3 shadow-sm">
          <div class="text-xs text-rose-700">维护中</div>
          <div class="mt-1 text-2xl font-semibold text-rose-900">{{ stats.maintenance }}</div>
        </div>
        <div class="feishu-stat-card rounded-xl border border-blue-100 bg-blue-50/40 p-3 shadow-sm">
          <div class="text-xs text-blue-700">平均容量</div>
          <div class="mt-1 text-2xl font-semibold text-blue-900">{{ stats.avgCapacity }}</div>
        </div>
      </div>

      <div class="feishu-filter rounded-xl border border-slate-200 bg-slate-50/60 p-4">
        <div class="flex items-center gap-3 overflow-x-auto whitespace-nowrap">
          <input v-model="keyword" class="input !w-[280px] shrink-0" placeholder="搜索名称/编码/楼宇/学院..." @input="resetPage" />
          <select v-model="typeFilter" class="input !w-[150px] shrink-0" @change="resetPage">
            <option value="all">全部类型</option>
            <option value="COMPUTER">计算机实验室</option>
            <option value="BIOLOGY">生物实验室</option>
          </select>
          <select v-model="statusFilter" class="input !w-[130px] shrink-0" @change="resetPage">
            <option value="all">全部状态</option>
            <option value="available">可用</option>
            <option value="booked">占用中</option>
            <option value="maintenance">维护中</option>
          </select>
          <button class="btn-secondary shrink-0 px-4" @click="loadLabs">刷新</button>
          <button class="btn-primary shrink-0" @click="openCreate">添加实验室</button>
          <button class="btn-secondary shrink-0" @click="exportCsv">导出</button>
        </div>
      </div>

      <div class="feishu-table-wrap min-h-0 flex-1 overflow-auto rounded-xl border border-slate-200 bg-white shadow-sm">
        <table class="w-full border-collapse text-sm">
          <thead class="bg-slate-100/90">
            <tr>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">#</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">编码</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">名称</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">类型</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">楼宇</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">学院</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">容量</th>
          <th class="px-4 py-3 text-left font-semibold text-slate-700">图片</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">状态</th>
              <th class="px-4 py-3 text-left font-semibold text-slate-700">操作</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr v-for="(lab, idx) in pagedRows" :key="lab.id" class="transition hover:bg-[#f7f9fc]">
              <td class="px-4 py-3 text-slate-500">{{ (currentPage - 1) * pageSize + idx + 1 }}</td>
              <td class="px-4 py-3 text-slate-700">{{ lab.labCode || "-" }}</td>
              <td class="px-4 py-3 text-slate-800">{{ lab.name }}</td>
              <td class="px-4 py-3 text-slate-700">{{ typeText(lab.type) }}</td>
              <td class="px-4 py-3 text-slate-700">{{ lab.building || "-" }}</td>
              <td class="px-4 py-3 text-slate-700">{{ lab.college || "-" }}</td>
              <td class="px-4 py-3 text-slate-700">{{ lab.capacity || 0 }}</td>
              <td class="px-4 py-3 text-slate-700">{{ (lab.imageUrls || []).length }} 张</td>
              <td class="px-4 py-3">
                <span class="rounded-full px-2 py-0.5 text-xs font-medium" :class="statusClass(lab.status)">
                  {{ statusText(lab.status) }}
                </span>
              </td>
              <td class="px-4 py-3">
                <button class="btn-secondary px-3 py-1.5 text-xs" @click="openDetail(lab)">详情</button>
              </td>
            </tr>
            <tr v-if="pagedRows.length === 0">
              <td class="px-6 py-10 text-center text-slate-500" colspan="10">
                {{ loading ? "正在加载实验室数据..." : "暂无实验室数据" }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <AppPagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="filteredRows.length" :page-size-options="[8,12,20]" />
    </div>
  </div>

  <div v-if="showCreate" class="fixed inset-0 z-40 flex items-center justify-center bg-slate-900/30 px-4" @click.self="showCreate = false">
    <div class="feishu-modal w-full max-w-lg rounded-xl border border-slate-200 bg-white shadow-xl">
      <div class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
        <div class="text-base font-semibold text-slate-900">添加实验室</div>
        <button class="rounded-md px-2 py-1 text-sm text-slate-500 hover:bg-slate-100" @click="showCreate = false">关闭</button>
      </div>
      <div class="space-y-4 p-5">
        <div>
          <label class="mb-1 block text-sm text-slate-600">实验室编码</label>
          <input v-model="creating.labCode" class="input" placeholder="例如：LAB-A101" />
        </div>
        <div>
          <label class="mb-1 block text-sm text-slate-600">实验室名称</label>
          <input v-model="creating.labName" class="input" placeholder="例如：人工智能实验室 A101" />
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div>
            <label class="mb-1 block text-sm text-slate-600">实验室类型</label>
            <select v-model="creating.labType" class="input">
              <option value="COMPUTER">计算机实验室</option>
              <option value="BIOLOGY">生物实验室</option>
            </select>
          </div>
          <div>
            <label class="mb-1 block text-sm text-slate-600">容量</label>
            <input v-model="creating.capacity" class="input" type="number" min="1" placeholder="例如：40" />
          </div>
        </div>
        <div>
          <label class="mb-1 block text-sm text-slate-600">学院</label>
          <input v-model="creating.college" class="input" placeholder="例如：计算机学院" />
        </div>
        <div>
          <label class="mb-1 block text-sm text-slate-600">楼宇</label>
          <input v-model="creating.building" class="input" placeholder="例如：实验楼 A 栋" />
        </div>
        <div>
          <label class="mb-1 block text-sm text-slate-600">描述</label>
          <textarea v-model="creating.description" class="input min-h-[100px] resize-y" placeholder="填写实验室简介（可选）"></textarea>
        </div>
        <div>
          <label class="mb-1 block text-sm text-slate-600">实验室图片（支持多图）</label>
          <input type="file" accept="image/*" multiple class="input" @change="onImageChange" />
          <p class="mt-1 text-xs text-slate-500">支持 jpg/jpeg/png/gif/webp/bmp，最多 12 张。</p>
          <div v-if="imagePreviews.length" class="mt-3 grid grid-cols-2 gap-3 sm:grid-cols-3">
            <div v-for="(img, i) in imagePreviews" :key="img.url" class="rounded-lg border border-slate-200 bg-slate-50 p-2">
              <img :src="img.url" alt="preview" class="h-24 w-full rounded object-cover" />
              <div class="mt-1 truncate text-xs text-slate-600">{{ img.name }}</div>
              <div class="text-[11px] text-slate-400">{{ img.size }} KB</div>
              <button class="mt-1 text-xs text-rose-600 hover:underline" @click="removeImage(i)">移除</button>
            </div>
          </div>
        </div>
      </div>
      <div class="flex justify-end gap-3 border-t border-slate-200 px-5 py-4">
        <button class="btn-secondary" @click="showCreate = false">取消</button>
        <button class="btn-primary" @click="submitCreate">立即添加</button>
      </div>
    </div>
  </div>

  <div v-if="showDetail" class="fixed inset-0 z-40 flex items-center justify-center bg-slate-900/30 px-4" @click.self="showDetail = false">
    <div class="feishu-modal w-full max-w-xl rounded-xl border border-slate-200 bg-white shadow-xl">
      <div class="flex items-center justify-between border-b border-slate-200 px-5 py-4">
        <div class="text-base font-semibold text-slate-900">实验室详情</div>
        <button class="rounded-md px-2 py-1 text-sm text-slate-500 hover:bg-slate-100" @click="showDetail = false">关闭</button>
      </div>
      <div class="p-5">
        <div v-if="detailLoading" class="py-8 text-center text-sm text-slate-500">加载中...</div>
        <div v-else-if="detail" class="grid gap-3 sm:grid-cols-2">
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <div class="text-xs text-slate-500">编码</div>
            <div class="mt-1 text-sm font-medium text-slate-900">{{ detail.labCode || "-" }}</div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <div class="text-xs text-slate-500">名称</div>
            <div class="mt-1 text-sm font-medium text-slate-900">{{ detail.name || "-" }}</div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <div class="text-xs text-slate-500">类型</div>
            <div class="mt-1 text-sm font-medium text-slate-900">{{ typeText(detail.type) }}</div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <div class="text-xs text-slate-500">状态</div>
            <div class="mt-1 text-sm font-medium text-slate-900">{{ statusText(detail.status) }}</div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <div class="text-xs text-slate-500">楼宇</div>
            <div class="mt-1 text-sm font-medium text-slate-900">{{ detail.building || "-" }}</div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2">
            <div class="text-xs text-slate-500">学院</div>
            <div class="mt-1 text-sm font-medium text-slate-900">{{ detail.college || "-" }}</div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 sm:col-span-2">
            <div class="text-xs text-slate-500">容量</div>
            <div class="mt-1 text-sm font-medium text-slate-900">{{ detail.capacity || 0 }} 人</div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 sm:col-span-2">
            <div class="text-xs text-slate-500">描述</div>
            <div class="mt-1 text-sm font-medium text-slate-900">{{ detail.description || "-" }}</div>
          </div>
          <div class="rounded-lg border border-slate-200 bg-slate-50 px-3 py-2 sm:col-span-2">
            <div class="text-xs text-slate-500">实验室图片</div>
            <div v-if="detail.imageUrls?.length" class="mt-2 grid grid-cols-2 gap-2 sm:grid-cols-3">
              <a v-for="(url, idx) in detail.imageUrls" :key="idx" :href="url" target="_blank" rel="noreferrer">
                <img :src="url" alt="lab" class="h-24 w-full rounded border border-slate-200 object-cover" />
              </a>
            </div>
            <div v-else class="mt-1 text-sm text-slate-500">暂无图片</div>
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
