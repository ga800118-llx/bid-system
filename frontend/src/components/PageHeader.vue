<template>
  <span class="page-header-placeholder" aria-hidden="true"></span>
</template>

<script setup>
defineProps({
  title: { type: String, default: '' }
})
</script>

<style scoped>
.page-header-placeholder { display: none; }
</style>
