<template>
  <div class="flex h-full flex-col bg-white">
    <div class="border-b border-slate-200 px-6 py-5">
      <h2 class="text-2xl font-semibold text-slate-900">消息中心</h2>
      <p class="mt-1 text-sm text-slate-600">系统通知、实验室公告、设备公告</p>
    </div>

    <div class="flex-1 overflow-auto p-6">
      <div class="mx-auto max-w-3xl space-y-3">
        <div class="rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm shadow-sm transition hover:shadow-md">
          <div class="flex items-center gap-2">
            <span class="rounded bg-brand-600 px-2 py-0.5 text-xs font-medium text-white">系统</span>
            <span class="text-slate-600">计算机实验室A301 将于周五 18:00 维护</span>
          </div>
        </div>
        <div class="rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm shadow-sm transition hover:shadow-md">
          <div class="flex items-center gap-2">
            <span class="rounded bg-green-600 px-2 py-0.5 text-xs font-medium text-white">预约</span>
            <span class="text-slate-600">你的设备预约已通过审批</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
