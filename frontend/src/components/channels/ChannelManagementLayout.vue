<template>
  <section class="channel-layout">
    <div class="channel-header">
      <div>
        <h2><slot name="title">通道管理</slot></h2>
        <p><slot name="subtitle">管理所有通道配置与状态</slot></p>
      </div>
      <div class="channel-actions">
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
  padding: 24px;
  background: var(--surface);
  border-radius: 16px;
  box-shadow: 0 16px 32px var(--shadow-soft);
  border: 1px solid var(--border-subtle);
  color: var(--text-on-surface);
}

.channel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.channel-header h2 {
  margin: 0;
}

.channel-header p {
  margin: 4px 0 0;
  color: var(--text-muted);
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
