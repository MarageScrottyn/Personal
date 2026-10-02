<template>
  <div class="lingtai" ref="container"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import * as THREE from 'three'

const container = ref(null)

/* ================= 层级配置 ================= */
const layers = [
  { name: '太极', radius: 0.6, speed: 0.3, dir: 1, count: 0 },
  { name: '四象', radius: 1.0, speed: 0.25, dir: -1, count: 4 },
  { name: '五行', radius: 1.5, speed: 0.2, dir: 1, count: 5 },
  { name: '八卦', radius: 2.1, speed: 0.15, dir: -1, count: 8 },
]

/* ================= Three 实例 ================= */
let scene, camera, renderer
let groups = [] // 用于旋转
let animId

/* ================= 初始化 ================= */
onMounted(() => {
  init()
  createRings()
  animate()
})

onUnmounted(() => {
  cancelAnimationFrame(animId)
  renderer?.dispose()
})

/* ================= 初始化场景 ================= */
function init() {
  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x050510) // 深空蓝，告别纯黑

  camera = new THREE.PerspectiveCamera(45, 1, 0.1, 100)
  camera.position.set(0, 0, 6) // 拉远一点
  camera.lookAt(0, 0, 0)

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(500, 500)
  container.value.appendChild(renderer.domElement)

  // 加一盏环境光（防止全黑）
  const light = new THREE.AmbientLight(0xffffff, 1)
  scene.add(light)
}

/* ================= 创建层级 ================= */
function createRings() {
  const colors = [
    0xff4444, // 红
    0x44ff88, // 绿
    0x44ccff, // 蓝
    0xffdd44, // 金
  ]

  layers.forEach((layer, index) => {
    const group = new THREE.Group()
    const color = colors[index % colors.length]

    /* 1. 实心圆环（宽环） */
    const ringShape = new THREE.Shape()
    ringShape.absarc(0, 0, layer.radius, 0, Math.PI * 2, false)

    const holePath = new THREE.Path()
    holePath.absarc(0, 0, layer.radius * 0.75, 0, Math.PI * 2, true)
    ringShape.holes.push(holePath)

    const ringGeo = new THREE.ShapeGeometry(ringShape)
    const ringMat = new THREE.MeshBasicMaterial({
      color: color,
      transparent: true,
      opacity: 0.7,
      side: THREE.DoubleSide,
    })
    const ring = new THREE.Mesh(ringGeo, ringMat)
    ring.rotation.x = -Math.PI / 2 // 平铺
    group.add(ring)

    /* 2. 节点（四象/五行/八卦） */
    if (layer.count > 0) {
      for (let i = 0; i < layer.count; i++) {
        const angle = (i / layer.count) * Math.PI * 2
        const x = Math.cos(angle) * layer.radius
        const y = Math.sin(angle) * layer.radius

        const dotGeo = new THREE.SphereGeometry(0.06, 16, 16)
        const dotMat = new THREE.MeshBasicMaterial({ color: 0xffffff })
        const dot = new THREE.Mesh(dotGeo, dotMat)
        dot.position.set(x, y, 0)
        group.add(dot)
      }
    }

    /* 3. 太极特殊处理（中心球） */
    if (layer.name === '太极') {
      const taijiGeo = new THREE.SphereGeometry(layer.radius, 32, 32)
      const taijiMat = new THREE.MeshBasicMaterial({ color: 0xffffff })
      const taiji = new THREE.Mesh(taijiGeo, taijiMat)
      group.add(taiji)
    }

    scene.add(group)
    groups.push({ mesh: group, speed: layer.speed * layer.dir })
  })
}

/* ================= 动画循环 ================= */
function animate() {
  animId = requestAnimationFrame(animate)

  groups.forEach(g => {
    g.mesh.rotation.z += 0.005 * g.speed
  })

  renderer.render(scene, camera)
}
</script>

<style scoped>
.lingtai {
  width: 100vw;
  height: 100vh;
  background: radial-gradient(circle, #0a0a0a, #000);
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>