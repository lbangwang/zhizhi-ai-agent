<template>
  <footer class="site-footer" :class="{ compact }">
    <p class="copyright">
      © {{ year }} {{ brandName }}
      <span class="divider" aria-hidden="true">·</span>
      <span class="rights">保留所有权利</span>
    </p>
  </footer>
</template>

<script setup>
import { BRAND_NAME } from '../constants/apps.js'

defineProps({
  compact: {
    type: Boolean,
    default: false,
  },
})

const brandName = BRAND_NAME
const year = new Date().getFullYear()
</script>

<style scoped>
.site-footer {
  width: 100%;
  flex-shrink: 0;
  padding: 16px 12px;
  text-align: center;
}

.site-footer.compact {
  padding: 8px 12px 4px;
}

.copyright {
  margin: 0;
  font-size: 12px;
  line-height: 1.6;
  letter-spacing: 0.02em;
  color: var(--color-text-muted);
  text-shadow: 0 1px 0 rgba(255, 255, 255, 0.35);
}

.divider {
  margin: 0 6px;
  opacity: 0.55;
}

.rights {
  opacity: 0.9;
}

@media (max-width: 480px) {
  .copyright {
    font-size: 11px;
  }

  .site-footer:not(.compact) {
    padding: 12px 10px;
  }
}
</style>
