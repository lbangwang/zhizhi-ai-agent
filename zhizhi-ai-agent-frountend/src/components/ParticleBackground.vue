<template>
  <canvas ref="canvasRef" class="particle-canvas" aria-hidden="true" />
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'

const props = defineProps({
  /** dense | normal | light */
  density: {
    type: String,
    default: 'normal',
  },
})

const canvasRef = ref(null)
let rafId = 0
let particles = []
let ctx = null
let width = 0
let height = 0
let dpr = 1
let reducedMotion = false

const COLORS = [
  'rgba(31, 111, 139, 0.85)',
  'rgba(95, 168, 192, 0.8)',
  'rgba(47, 122, 107, 0.7)',
  'rgba(143, 191, 178, 0.65)',
  'rgba(255, 255, 255, 0.55)',
]

function particleCount() {
  const area = width * height
  const base = Math.min(90, Math.max(28, Math.floor(area / 14000)))
  if (props.density === 'dense') return Math.floor(base * 1.35)
  if (props.density === 'light') return Math.floor(base * 0.55)
  return base
}

function createParticles() {
  const count = particleCount()
  particles = Array.from({ length: count }, () => ({
    x: Math.random() * width,
    y: Math.random() * height,
    vx: (Math.random() - 0.5) * 0.35,
    vy: (Math.random() - 0.5) * 0.35,
    r: Math.random() * 1.8 + 0.8,
    color: COLORS[Math.floor(Math.random() * COLORS.length)],
    pulse: Math.random() * Math.PI * 2,
  }))
}

function resize() {
  const canvas = canvasRef.value
  if (!canvas) return
  dpr = Math.min(window.devicePixelRatio || 1, 2)
  width = window.innerWidth
  height = window.innerHeight
  canvas.width = Math.floor(width * dpr)
  canvas.height = Math.floor(height * dpr)
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`
  ctx = canvas.getContext('2d')
  ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  createParticles()
}

function draw() {
  if (!ctx) return
  ctx.clearRect(0, 0, width, height)

  const linkDist = Math.min(140, width * 0.12)

  for (let i = 0; i < particles.length; i++) {
    const p = particles[i]
    if (!reducedMotion) {
      p.x += p.vx
      p.y += p.vy
      p.pulse += 0.02
      if (p.x < -10) p.x = width + 10
      if (p.x > width + 10) p.x = -10
      if (p.y < -10) p.y = height + 10
      if (p.y > height + 10) p.y = -10
    }

    const glow = 0.55 + Math.sin(p.pulse) * 0.25
    ctx.beginPath()
    ctx.arc(p.x, p.y, p.r * (0.85 + glow * 0.35), 0, Math.PI * 2)
    ctx.fillStyle = p.color
    ctx.globalAlpha = glow
    ctx.fill()
    ctx.globalAlpha = 1

    for (let j = i + 1; j < particles.length; j++) {
      const q = particles[j]
      const dx = p.x - q.x
      const dy = p.y - q.y
      const dist = Math.hypot(dx, dy)
      if (dist < linkDist) {
        const alpha = (1 - dist / linkDist) * 0.22
        ctx.beginPath()
        ctx.moveTo(p.x, p.y)
        ctx.lineTo(q.x, q.y)
        ctx.strokeStyle = `rgba(31, 111, 139, ${alpha})`
        ctx.lineWidth = 1
        ctx.stroke()
      }
    }
  }

  if (!reducedMotion) {
    rafId = requestAnimationFrame(draw)
  }
}

function onVisibility() {
  if (document.hidden) {
    cancelAnimationFrame(rafId)
  } else if (!reducedMotion) {
    rafId = requestAnimationFrame(draw)
  }
}

onMounted(() => {
  reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  resize()
  draw()
  window.addEventListener('resize', resize)
  document.addEventListener('visibilitychange', onVisibility)
})

onUnmounted(() => {
  cancelAnimationFrame(rafId)
  window.removeEventListener('resize', resize)
  document.removeEventListener('visibilitychange', onVisibility)
})
</script>

<style scoped>
.particle-canvas {
  position: fixed;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}
</style>
