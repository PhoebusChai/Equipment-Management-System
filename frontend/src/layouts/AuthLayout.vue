<template>
  <div class="flex min-h-screen items-center justify-center bg-app px-4 py-10">
    <div class="grid w-full max-w-5xl overflow-hidden rounded-xl2 border border-slate-200 bg-white shadow-card lg:grid-cols-2">
      <section class="hidden bg-gradient-to-br from-brand-600 to-brand-800 p-10 text-white lg:block">
        <div class="flex h-full flex-col">
          <div>
            <div class="inline-flex h-10 w-10 items-center justify-center rounded-lg bg-white/20 text-lg font-bold">E</div>
            <h1 class="mt-6 text-3xl font-semibold leading-tight">实验室与设备管理系统</h1>
            <p class="mt-4 text-sm text-blue-100">
              支持学生、教师、管理员三端协同，统一管理实验室、设备预约、报修和数据分析。
            </p>
          </div>
        </div>
      </section>

      <section class="min-h-[640px] p-6 sm:p-10">
        <router-view />
      </section>
    </div>
  </div>
</template>
