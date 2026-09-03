<script setup>
defineProps({
  coaching: { type: Object, required: true },
})
</script>

<template>
  <article class="coaching">
    <h3>코칭</h3>
    <p>{{ coaching.summary }}</p>
    <strong>{{ coaching.practiceRecommendation }}</strong>
    <ul>
      <li v-for="item in coaching.actionItems" :key="item">{{ item }}</li>
    </ul>
  </article>
</template>

<style scoped>
.coaching { padding: var(--space-4); background: var(--color-primary-weak); border-radius: var(--radius-2); }
.coaching h3 { margin-top: 0; }
.coaching ul { margin-bottom: 0; padding-left: var(--space-6); }
</style>
