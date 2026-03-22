<script setup>
defineProps({
  columns: { type: Array, required: true },
  rows: { type: Array, required: true }
});
</script>

<template>
  <div class="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
    <table class="w-full border-collapse text-sm">
      <thead class="bg-slate-100">
        <tr>
          <th v-for="col in columns" :key="col.key" class="px-6 py-4 text-left font-semibold text-slate-700">
            {{ col.label }}
          </th>
        </tr>
      </thead>
      <tbody class="divide-y divide-slate-100">
        <tr v-for="(row, index) in rows" :key="index" class="transition hover:bg-slate-50">
          <td v-for="col in columns" :key="col.key" class="px-6 py-4 text-slate-700">
            {{ row[col.key] }}
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
