<template>
  <router-view />
</template>

<script setup>
</script>

<style>
html {
  background: var(--ds-color-bg-page);
}

body {
  margin: 0;
  font-family: 'Helvetica Neue', Arial, sans-serif;
  color: var(--ds-color-text-primary);
  background: var(--ds-color-bg-page);
}

* {
  box-sizing: border-box;
}

body[arco-theme="dark"] {
  color-scheme: dark;
}

body[arco-theme="dark"] .arco-modal,
body[arco-theme="dark"] .arco-drawer,
body[arco-theme="dark"] .arco-popover-popup-content,
body[arco-theme="dark"] .arco-trigger-popup,
body[arco-theme="dark"] .arco-select-dropdown,
body[arco-theme="dark"] .arco-cascader-popup,
body[arco-theme="dark"] .arco-picker-popup {
  color: var(--ds-color-text-primary);
  background-color: var(--ds-color-bg-card);
}

body[arco-theme="dark"] .arco-table,
body[arco-theme="dark"] .arco-table-container,
body[arco-theme="dark"] .arco-table-td {
  color: var(--ds-color-text-primary);
  background-color: var(--ds-color-bg-card);
}

body[arco-theme="dark"] .arco-table-th {
  color: var(--ds-color-text-regular);
  background-color: var(--ds-color-bg-soft);
}

body[arco-theme="dark"] .arco-card,
body[arco-theme="dark"] .arco-list,
body[arco-theme="dark"] .arco-upload,
body[arco-theme="dark"] .arco-tabs-content {
  color: var(--ds-color-text-primary);
  background-color: var(--ds-color-bg-card);
}
</style>
