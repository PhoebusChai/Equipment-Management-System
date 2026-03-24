<script setup>
import { computed, onMounted, ref } from "vue";
import AppPagination from "../../components/AppPagination.vue";
import { listAdminReviewsApi } from "../../services/reviews";

const keyword = ref("");
const typeFilter = ref("all");
const ratingFilter = ref("all");
const rows = ref([]);
const currentPage = ref(1);
const pageSize = ref(10);

async function loadData() {
  rows.value = await listAdminReviewsApi({
    resourceType: typeFilter.value === "all" ? undefined : typeFilter.value,
    rating: ratingFilter.value === "all" ? undefined : Number(ratingFilter.value)
  });
}

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  let list = rows.value.slice();
  if (kw) {
    list = list.filter((x) =>
      String(x.resourceName || "").toLowerCase().includes(kw) ||
      String(x.userName || "").toLowerCase().includes(kw) ||
      String(x.content || "").toLowerCase().includes(kw)
    );
  }
  return list;
});

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredRows.value.slice(start, start + pageSize.value);
});

onMounted(loadData);
</script>

<template>
  <div class="flex h-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-4">
      <h2 class="text-xl font-semibold text-slate-900">评论管理</h2>
      <p class="mt-1 text-sm text-slate-500">查看并管理实验室与设备评论数据。</p>
    </div>

    <div class="flex min-h-0 flex-1 flex-col gap-4 p-6">
      <div class="rounded-xl border border-slate-200 bg-slate-50 p-4">
        <div class="grid w-full max-w-full grid-cols-[120px_120px_minmax(0,1fr)] items-center gap-3">
          <select v-model="typeFilter" class="input w-[120px]" @change="loadData">
            <option value="all">全部类型</option>
            <option value="lab">实验室</option>
            <option value="device">设备</option>
          </select>
          <select v-model="ratingFilter" class="input w-[120px]" @change="loadData">
            <option value="all">全部评分</option>
            <option v-for="s in [5,4,3,2,1]" :key="s" :value="String(s)">{{ s }}分</option>
          </select>
          <input v-model="keyword" class="input min-w-0 w-full" placeholder="搜索资源/用户/评论内容..." />
        </div>
      </div>

      <div class="min-h-0 flex-1 overflow-auto rounded-xl border border-slate-200 bg-white shadow-sm">
        <table class="w-full border-collapse text-sm">
          <thead class="bg-slate-100">
            <tr>
              <th class="px-4 py-3 text-left">资源名称</th>
              <th class="px-4 py-3 text-left">类型</th>
              <th class="px-4 py-3 text-left">评价人</th>
              <th class="px-4 py-3 text-left">评分</th>
              <th class="px-4 py-3 text-left">评价内容</th>
              <th class="px-4 py-3 text-left">时间</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-slate-100">
            <tr v-for="r in pagedRows" :key="r.id" class="transition hover:bg-slate-50">
              <td class="px-4 py-3 text-slate-900">{{ r.resourceName }}</td>
              <td class="px-4 py-3 text-slate-700">{{ r.resourceType === "lab" ? "实验室" : "设备" }}</td>
              <td class="px-4 py-3 text-slate-700">{{ r.userName }}</td>
              <td class="px-4 py-3 text-slate-700">{{ r.rating }}</td>
              <td class="max-w-[360px] truncate px-4 py-3 text-slate-700" :title="r.content">{{ r.content || "-" }}</td>
              <td class="px-4 py-3 text-slate-600">{{ r.createdAt?.replace("T", " ") || "-" }}</td>
            </tr>
            <tr v-if="pagedRows.length === 0">
              <td colspan="6" class="px-6 py-12 text-center text-slate-500">暂无评论数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <AppPagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="filteredRows.length" :page-size-options="[10,20,50]" />
    </div>
  </div>
</template>
