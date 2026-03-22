<script setup>
defineProps({
  title: { type: String, required: true },
  value: { type: [String, Number], required: true },
  desc: { type: String, default: "" }
});
</script>

<template>
  <div class="rounded-lg border border-slate-200 bg-slate-50 px-5 py-4 shadow-sm transition hover:shadow-md">
    <p class="text-sm font-medium text-slate-600">{{ title }}</p>
    <p class="mt-2 text-3xl font-bold text-slate-900">{{ value }}</p>
    <p class="mt-1 text-xs text-slate-500">{{ desc }}</p>
  </div>
</template>
