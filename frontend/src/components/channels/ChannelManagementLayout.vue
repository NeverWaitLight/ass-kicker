<template>
  <section class="channel-layout data-list-page">
    <div class="channel-header data-list-page__header">
      <div>
        <h2 class="data-list-page__title"><slot name="title">通道</slot></h2>
        <p class="data-list-page__desc"><slot name="subtitle">管理所有通道配置与状态</slot></p>
      </div>
      <div v-if="$slots.actions" class="channel-actions">
        <slot name="actions" />
      </div>
    </div>
    <div class="channel-body">
      <slot />
    </div>
  </section>
</template>

<style scoped>
.channel-layout {
  border-radius: 16px;
}

.channel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.channel-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.channel-body {
  min-height: 200px;
}

@media (max-width: 768px) {
  .channel-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .channel-actions {
    width: 100%;
    flex-wrap: wrap;
  }
}
</style>
