<script setup lang="ts">
import { computed, ref } from 'vue'

defineProps<{ busy: boolean }>()
const emit = defineEmits<{ generate: [size: number] }>()
const sizeText = ref('9')
const error = ref('')
const size = computed(() => Number(sizeText.value))
const isValid = computed(() => /^\d+$/.test(sizeText.value) && Number.isInteger(size.value)
  && size.value >= 2 && size.value <= 16)
const isPrime = computed(() => isValid.value && Array.from(
  { length: Math.floor(Math.sqrt(size.value)) - 1 }, (_, index) => index + 2,
).every(divisor => size.value % divisor !== 0))

function generate() {
  if (!isValid.value) {
    error.value = 'Choose an integer from 2 to 16.'
    return
  }
  error.value = ''
  emit('generate', size.value)
}
</script>

<template>
  <form novalidate @submit.prevent="generate">
    <label for="size">Board size (2-16)</label>
    <input id="size" v-model="sizeText" type="number" min="2" max="16" step="1" :disabled="busy">
    <button type="submit" :disabled="busy">Generate</button>
    <p v-if="error" role="alert">{{ error }}</p>
    <p v-if="isPrime">This prime size uses 1-by-N regions and forms a Latin square.</p>
  </form>
</template>
