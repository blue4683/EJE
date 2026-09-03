<script setup>
defineProps({
  label: { type: String, required: true },
  type: { type: String, default: 'text' },
  autocomplete: { type: String, default: undefined },
  error: { type: String, default: null },
  placeholder: { type: String, default: '' },
})

defineEmits(['blur'])
const model = defineModel({ type: String, required: true })
</script>

<template>
  <label class="field">
    <span class="field__label">{{ label }}</span>
    <input
      v-model="model"
      :type="type"
      :autocomplete="autocomplete"
      :placeholder="placeholder"
      :aria-invalid="Boolean(error)"
      @blur="$emit('blur')"
    />
    <span v-if="error" class="field__error" role="alert">{{ error }}</span>
  </label>
</template>

<style scoped>
.field { display: grid; gap: var(--space-2); }
.field__label { font-size: 0.9rem; font-weight: 700; }
input {
  width: 100%;
  padding: 12px 14px;
  color: var(--color-text);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-1);
  outline: none;
}
input:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px var(--color-primary-weak); }
input[aria-invalid='true'] { border-color: var(--color-danger); }
.field__error { color: var(--color-danger); font-size: 0.84rem; }
</style>
