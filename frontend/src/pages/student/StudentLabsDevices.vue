<script setup>
import { computed, ref, watch } from "vue";
import { useRouter } from "vue-router";
import AppPagination from "../../components/AppPagination.vue";
import { labs, statusClass, statusText } from "../../mock/labs";

const filters = ref({
  building: "ALL",
  college: "ALL",
  keyword: ""
});
const router = useRouter();

const buildingOptions = computed(() => ["ALL", ...new Set(labs.map((lab) => lab.building))]);
const collegeOptions = computed(() => ["ALL", ...new Set(labs.map((lab) => lab.college))]);

const filteredLabs = computed(() =>
  labs.filter((lab) => {
    const byBuilding = filters.value.building === "ALL" || lab.building === filters.value.building;
    const byCollege = filters.value.college === "ALL" || lab.college === filters.value.college;
    const keyword = filters.value.keyword.trim();
    const byKeyword = !keyword || lab.name.includes(keyword);
    return byBuilding && byCollege && byKeyword;
  })
);

const currentPage = ref(1);
const pageSize = ref(6);
const pagedLabs = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return filteredLabs.value.slice(start, start + pageSize.value);
});

watch(filteredLabs, (list) => {
  const totalPages = Math.max(1, Math.ceil(list.length / pageSize.value));
  if (currentPage.value > totalPages) currentPage.value = totalPages;
}, { immediate: true });

watch([() => filters.value.building, () => filters.value.college, () => filters.value.keyword], () => {
  currentPage.value = 1;
});

function openDetail(labId) {
  router.push(`/student/labs-devices/${labId}`);
}
</script>

<template>
  <div class="flex h-full flex-col bg-white">
    <!-- 页面标题 -->
    <div class="border-b border-slate-200 px-6 py-4">
      <h2 class="text-xl font-semibold text-slate-800">实验室与设备管理</h2>
      <p class="mt-1 text-sm text-slate-500">实验室筛选、查看与详情联动</p>
    </div>

    <!-- 筛选区域 -->
    <div class="border-b border-slate-200 px-6 py-4">
      <div class="grid gap-3 md:grid-cols-3">
        <select v-model="filters.building" class="input">
          <option v-for="item in buildingOptions" :key="item" :value="item">{{ item === "ALL" ? "全部楼宇" : item }}</option>
        </select>
        <select v-model="filters.college" class="input">
          <option v-for="item in collegeOptions" :key="item" :value="item">{{ item === "ALL" ? "全部学院" : item }}</option>
        </select>
        <input v-model="filters.keyword" class="input" placeholder="输入实验室名称关键字" />
      </div>
    </div>

    <!-- 实验室列表 -->
    <div class="flex-1 overflow-auto px-6 py-4">
      <div class="grid gap-4 lg:grid-cols-2 xl:grid-cols-3">
        <div
          v-for="lab in pagedLabs"
          :key="lab.id"
          class="group flex overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm transition hover:shadow-lg hover:border-slate-300"
        >
          <!-- 实验室图片 -->
          <div class="relative m-3 h-32 w-32 flex-shrink-0 overflow-hidden rounded-lg bg-slate-100">
            <img
              :src="lab.image"
              :alt="lab.name"
              class="h-full w-full object-cover transition duration-500 group-hover:scale-110"
            />
            <div class="absolute inset-0 bg-gradient-to-t from-black/20 to-transparent opacity-0 transition-opacity duration-300 group-hover:opacity-100"></div>
          </div>

          <!-- 实验室信息 -->
          <div class="flex flex-1 flex-col py-3 pr-4">
            <div class="flex-1">
              <div class="flex items-start justify-between gap-2">
                <h3 class="line-clamp-1 text-base font-semibold text-slate-900 transition group-hover:text-brand-600">
                  {{ lab.name }}
                </h3>
                <span 
                  class="flex-shrink-0 rounded-full px-2.5 py-0.5 text-xs font-medium shadow-sm" 
                  :class="statusClass(lab.status)"
                >
                  {{ statusText(lab.status) }}
                </span>
              </div>
              
              <p class="mt-1.5 text-xs text-slate-600">
                <span class="font-medium">{{ lab.building }}</span>
                <span class="mx-1.5 text-slate-400">·</span>
                <span>{{ lab.college }}</span>
              </p>
              
              <div class="mt-3 flex items-center gap-4 text-xs text-slate-500">
                <div class="flex items-center gap-1">
                  <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                  </svg>
                  <span>{{ lab.capacity }}人</span>
                </div>
                <div class="flex items-center gap-1">
                  <svg class="h-3.5 w-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                  </svg>
                  <span>{{ lab.opening }}</span>
                </div>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="mt-4 flex gap-2">
              <button
                class="flex-1 rounded-lg bg-brand-600 px-3 py-2 text-xs font-medium text-white shadow-sm transition hover:bg-brand-700 hover:shadow active:scale-95"
                @click="openDetail(lab.id)"
              >
                查看详情
              </button>
              <button
                class="flex-1 rounded-lg border border-slate-300 bg-white px-3 py-2 text-xs font-medium text-slate-700 shadow-sm transition hover:bg-slate-50 hover:border-slate-400 active:scale-95"
                :disabled="lab.status !== 'AVAILABLE'"
                :class="lab.status !== 'AVAILABLE' ? 'opacity-40 cursor-not-allowed hover:bg-white hover:border-slate-300' : ''"
              >
                立即预约
              </button>
            </div>
          </div>
        </div>
      </div>
      <p v-if="!pagedLabs.length" class="py-16 text-center text-sm text-slate-400">暂无符合条件的实验室</p>
    </div>

    <!-- 分页 -->
    <div class="border-t border-slate-200 px-6 py-3">
      <AppPagination v-model:current-page="currentPage" v-model:page-size="pageSize" :total="filteredLabs.length" :page-size-options="[6]" />
    </div>
  </div>
</template>
