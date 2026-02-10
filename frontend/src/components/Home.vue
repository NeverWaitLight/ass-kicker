<template>
  <section class="hero">
    <a-row :gutter="[24, 24]" align="middle">
      <a-col :xs="24" :lg="14">
        <a-typography-title :level="1" class="hero-title">
          现代化全栈起步模板
        </a-typography-title>
        <a-typography-paragraph class="hero-text">
          以响应式后端与高效前端为核心，提供稳定的开发基线，让团队可以专注在业务价值。
        </a-typography-paragraph>
        <a-space size="middle">
          <a-tag color="geekblue">WebFlux</a-tag>
          <a-tag color="cyan">Java 21</a-tag>
          <a-tag color="purple">Vue 3</a-tag>
          <a-tag color="gold">Ant Design Vue</a-tag>
        </a-space>
      </a-col>
      <a-col :xs="24" :lg="10">
        <a-card class="hero-card" bordered>
          <a-typography-title :level="4">核心能力</a-typography-title>
          <a-space direction="vertical" size="small" class="hero-list">
            <div>快速启动后端服务</div>
            <div>统一的组件与视觉规范</div>
            <div>清晰的前后端分层</div>
            <div>可扩展的工程结构</div>
          </a-space>
        </a-card>
      </a-col>
    </a-row>
  </section>

  <section class="features">
    <a-row :gutter="[24, 24]">
      <a-col :xs="24" :md="8">
        <a-card class="feature-card" bordered>
          <a-typography-title :level="4">后端</a-typography-title>
          <a-typography-paragraph>
            使用Spring WebFlux构建响应式服务，优化高并发场景。
          </a-typography-paragraph>
        </a-card>
      </a-col>
      <a-col :xs="24" :md="8">
        <a-card class="feature-card" bordered>
          <a-typography-title :level="4">前端</a-typography-title>
          <a-typography-paragraph>
            Vue 3 + Vite提供即时热更新与轻量构建体验。
          </a-typography-paragraph>
        </a-card>
      </a-col>
      <a-col :xs="24" :md="8">
        <a-card class="feature-card" bordered>
          <a-typography-title :level="4">协作</a-typography-title>
          <a-typography-paragraph>
            明确的目录结构与统一配置降低团队沟通成本。
          </a-typography-paragraph>
        </a-card>
      </a-col>
    </a-row>
  </section>
</template>
