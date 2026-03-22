<script setup>
import DataTable from "../../components/DataTable.vue";

// 模拟：老师名下的实验室
const labColumns = [
  { key: "name", label: "实验室" },
  { key: "building", label: "楼宇" },
  { key: "capacity", label: "容量" },
  { key: "status", label: "当前状态" }
];

const labRows = [
  { name: "计算机实验室A301", building: "实验楼A", capacity: "40 人", status: "可预约" },
  { name: "生物实验室B201", building: "实验楼B", capacity: "30 人", status: "维护中（暂停预约）" }
];

// 模拟：实验室内重点设备
const deviceColumns = [
  { key: "name", label: "设备名称" },
  { key: "lab", label: "所属实验室" },
  { key: "code", label: "资产编号" },
  { key: "status", label: "状态" }
];

const deviceRows = [
  { name: "高性能计算机 #01", lab: "计算机实验室A301", code: "A-2024-001", status: "正常" },
  { name: "显微镜 BIO-02", lab: "生物实验室B201", code: "B-2023-112", status: "待保养" },
  { name: "试剂柜", lab: "生物实验室B201", code: "B-2022-077", status: "使用受限" }
];
</script>

<template>
  <div class="flex h-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-4">
      <h2 class="text-xl font-semibold text-slate-900">实验室与设备管理</h2>
      <p class="mt-1 text-sm text-slate-500">快速查看本人负责的实验室与关键设备台账。</p>
    </div>

    <div class="flex-1 overflow-auto p-6 space-y-6">
      <div class="flex flex-wrap gap-3">
        <button class="btn-secondary">新建实验室申请</button>
        <button class="btn-secondary">申请新增设备</button>
        <button class="btn-secondary">上传设备使用文档</button>
        <button class="btn-secondary">维护 / 保养计划</button>
      </div>

      <div class="space-y-4">
        <div>
          <h3 class="mb-3 text-sm font-medium text-slate-700">我负责的实验室</h3>
          <DataTable :columns="labColumns" :rows="labRows" />
        </div>

        <div>
          <h3 class="mb-3 text-sm font-medium text-slate-700">重点设备台账</h3>
          <DataTable :columns="deviceColumns" :rows="deviceRows" />
        </div>
      </div>
    </div>
  </div>
</template>
