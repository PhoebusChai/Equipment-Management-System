<script setup>
import StatCard from "../../components/StatCard.vue";
import DataTable from "../../components/DataTable.vue";

const stats = [
  { title: "我的信用分", value: 98, desc: "预约守约率优秀" },
  { title: "当前预约", value: 2, desc: "1个实验室 + 1个设备" },
  { title: "紧急预约次数", value: 1, desc: "本月已使用1次" }
];

const columns = [
  { key: "name", label: "资源名称" },
  { key: "type", label: "类型" },
  { key: "time", label: "预约时间" },
  { key: "status", label: "状态" }
];

const rows = [
  { name: "计算机实验室A301", type: "实验室", time: "09:00-11:00", status: "审核通过" },
  { name: "生物显微镜BIO-02", type: "设备", time: "14:00-15:00", status: "预约中" }
];
</script>

<template>
  <div class="flex h-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-5">
      <h1 class="text-2xl font-semibold text-slate-900">学生端总览</h1>
      <p class="mt-1 text-sm text-slate-600">覆盖用户中心、设备查询、预约、消息与反馈功能</p>
    </div>

    <div class="flex-1 overflow-auto p-6">
      <div class="grid gap-4 md:grid-cols-3">
        <StatCard v-for="item in stats" :key="item.title" :title="item.title" :value="item.value" :desc="item.desc" />
      </div>

      <div class="mt-6">
        <DataTable :columns="columns" :rows="rows" />
      </div>
    </div>
  </div>
</template>
