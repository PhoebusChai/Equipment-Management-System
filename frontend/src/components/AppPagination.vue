<script setup>
import { computed } from "vue";

const props = defineProps({
  currentPage: { type: Number, required: true },
  pageSize: { type: Number, required: true },
  total: { type: Number, required: true },
  pageSizeOptions: { type: Array, default: () => [3, 5, 10] }
});

const emit = defineEmits(["update:currentPage", "update:pageSize"]);

const totalPages = computed(() => {
  if (props.total <= 0) return 1;
  return Math.max(1, Math.ceil(props.total / props.pageSize));
});
const showSizeSelector = computed(() => props.pageSizeOptions.length > 1);

const pageNumbers = computed(() => {
  const pages = [];
  for (let i = 1; i <= totalPages.value; i += 1) pages.push(i);
  return pages;
});

function goPage(page) {
  const target = Math.min(Math.max(page, 1), totalPages.value);
  emit("update:currentPage", target);
}

function onChangeSize(event) {
  const size = Number(event.target.value);
  emit("update:pageSize", size);
  emit("update:currentPage", 1);
}
</script>

<template>
  <div class="flex flex-wrap items-center justify-end gap-2 text-xs text-slate-600">
    <span>共 {{ total }} 条</span>

    <select v-if="showSizeSelector" class="rounded border border-slate-200 bg-white px-2 py-1 text-xs" :value="pageSize" @change="onChangeSize">
      <option v-for="size in pageSizeOptions" :key="size" :value="size">{{ size }} / 页</option>
    </select>

    <button
      class="rounded border border-slate-200 bg-white px-2 py-1 text-xs transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
      :disabled="currentPage <= 1"
      @click="goPage(currentPage - 1)"
    >
      上一页
    </button>

    <button
      v-for="page in pageNumbers"
      :key="page"
      class="rounded border px-2 py-1 text-xs transition"
      :class="page === currentPage ? 'border-brand-600 bg-brand-600 text-white' : 'border-slate-200 bg-white text-slate-700 hover:bg-slate-50'"
      @click="goPage(page)"
    >
      {{ page }}
    </button>

    <button
      class="rounded border border-slate-200 bg-white px-2 py-1 text-xs transition hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
      :disabled="currentPage >= totalPages"
      @click="goPage(currentPage + 1)"
    >
      下一页
    </button>
  </div>
</template>
