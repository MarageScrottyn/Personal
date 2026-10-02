<template>
  <div class="brain-control-panel">
    <div class="panel-header">
      <h2>终控中心</h2>
      <p class="subtitle">大脑神经网络实时监控与控制</p>
    </div>

    <div class="view-controls">
      <button class="action-btn" @click="resetView">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
          <polyline points="23 4 23 10 17 10" />
          <path d="M20.49 15a9 9 0 0 0-2.12-9.36L23 10" />
          <polyline points="1 11 7 11 3 17" />
          <path d="M3.51 9a9 9 0 0 1 2.12-9.36L1 10" />
        </svg>
        重置视角
      </button>
      <button class="action-btn" @click="toggleSimulation">
        <svg v-if="!simulating" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
          <polyline points="5 3 19 12 5 21 5 3" />
        </svg>
        <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
          <rect x="6" y="4" width="4" height="16" />
          <rect x="14" y="4" width="4" height="16" />
        </svg>
        {{ simulating ? '停止模拟' : '启动模拟' }}
      </button>
    </div>

    <div class="brain-container">
      <div class="brain-3d" ref="graphContainer">
        <svg ref="graphSvg" class="graph-svg"></svg>
        <div class="controls-hint">
          <span>鼠标左键拖动旋转 | 滚轮缩放 | 双击节点固定/释放</span>
        </div>
      </div>

      <div class="info-panel">
        <div class="info-card">
          <h3>实时数据</h3>
          <div class="data-grid">
            <div class="data-item">
              <div class="data-value">{{ activeNodeCount }}</div>
              <div class="data-label">活跃节点</div>
            </div>
            <div class="data-item">
              <div class="data-value">{{ totalConnections }}</div>
              <div class="data-label">连接数</div>
            </div>
            <div class="data-item">
              <div class="data-value">{{ nodeCount }}</div>
              <div class="data-label">总节点数</div>
            </div>
            <div class="data-item">
              <div class="data-value">{{ fps }}</div>
              <div class="data-label">FPS</div>
            </div>
          </div>
        </div>

        <div class="info-card">
          <h3>节点区域</h3>
          <div class="lobe-list">
            <div v-for="(lobe, idx) in lobes" :key="idx" class="lobe-item">
              <div class="lobe-color" :style="{ background: lobe.color }"></div>
              <div class="lobe-info">
                <div class="lobe-name">{{ lobe.name }}</div>
                <div class="lobe-function">{{ lobe.count }}个节点</div>
              </div>
            </div>
          </div>
        </div>

        <div class="info-card">
          <h3>选中节点</h3>
          <div v-if="selectedNode" class="selected-info">
            <div class="selected-name">{{ selectedNode.label }}</div>
            <div class="selected-group">区域: {{ selectedNode.group }}</div>
            <div class="selected-connections">连接数: {{ selectedNode.connections || 0 }}</div>
            <button class="mini-btn" @click="unpinSelectedNode">
              {{ selectedNode.fx ? '释放节点' : '固定节点' }}
            </button>
          </div>
          <div v-else class="no-selection">点击节点查看详情</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as d3 from 'd3'

const graphContainer = ref(null)
const graphSvg = ref(null)
const simulating = ref(false)
const selectedNode = ref(null)

const lobes = [
  { name: '额叶', color: '#6366f1', count: 0 },
  { name: '顶叶', color: '#8b5cf6', count: 0 },
  { name: '颞叶', color: '#a855f7', count: 0 },
  { name: '枕叶', color: '#d946ef', count: 0 },
  { name: '边缘系统', color: '#3b82f6', count: 0 },
  { name: '丘脑', color: '#06b6d4', count: 0 },
  { name: '纹状体', color: '#14b8a6', count: 0 },
  { name: '脑桥', color: '#22c55e', count: 0 }
]

let svg = null
let simulation = null
let nodesData = []
let linksData = []
let width = 0
let height = 0
let rotation = { x: 0.3, y: 0.6 }
let scale = 1.2
let isDragging = false
let lastMouse = null
let animationFrame = null
let lastFrameTime = performance.now()
let frameCount = 0
let fpsValue = 60

const activeNodeCount = computed(() => nodesData.filter(n => n.active).length)
const totalConnections = computed(() => linksData.length)
const nodeCount = computed(() => nodesData.length)
const fps = computed(() => fpsValue)

function generateGraphData() {
  const nodes = []
  const links = []
  const labels = ['认知', '记忆', '情感', '语言', '视觉', '听觉', '运动', '逻辑',
    '感知', '推理', '决策', '学习', '注意力', '想象', '创造', '直觉',
    '意识', '潜意识', '反思', '灵感', '共情', '恐惧', '快乐', '悲伤',
    '焦虑', '平静', '愤怒', '惊讶', '厌恶', '信任', '期待', '控制']

  let nodeIndex = 0
  
  const brainRegions = [
    { lobe: 0, name: '额叶', positions: { x: 0, y: -120, z: 0 }, radius: 60, count: 10, hemisphere: 'left' },
    { lobe: 0, name: '额叶', positions: { x: 0, y: -120, z: 0 }, radius: 60, count: 10, hemisphere: 'right' },
    { lobe: 1, name: '顶叶', positions: { x: 0, y: 0, z: 0 }, radius: 55, count: 8, hemisphere: 'left' },
    { lobe: 1, name: '顶叶', positions: { x: 0, y: 0, z: 0 }, radius: 55, count: 8, hemisphere: 'right' },
    { lobe: 2, name: '颞叶', positions: { x: 100, y: 30, z: 0 }, radius: 45, count: 7, hemisphere: 'left' },
    { lobe: 2, name: '颞叶', positions: { x: -100, y: 30, z: 0 }, radius: 45, count: 7, hemisphere: 'right' },
    { lobe: 3, name: '枕叶', positions: { x: 0, y: 120, z: 0 }, radius: 40, count: 6, hemisphere: 'left' },
    { lobe: 3, name: '枕叶', positions: { x: 0, y: 120, z: 0 }, radius: 40, count: 6, hemisphere: 'right' },
    { lobe: 4, name: '边缘系统', positions: { x: 0, y: -20, z: -10 }, radius: 30, count: 5, hemisphere: 'left' },
    { lobe: 4, name: '边缘系统', positions: { x: 0, y: -20, z: 10 }, radius: 30, count: 5, hemisphere: 'right' },
    { lobe: 5, name: '丘脑', positions: { x: 0, y: 0, z: 0 }, radius: 25, count: 4, hemisphere: 'center' },
    { lobe: 6, name: '纹状体', positions: { x: 40, y: -30, z: 0 }, radius: 20, count: 4, hemisphere: 'left' },
    { lobe: 6, name: '纹状体', positions: { x: -40, y: -30, z: 0 }, radius: 20, count: 4, hemisphere: 'right' },
    { lobe: 7, name: '脑桥', positions: { x: 0, y: 80, z: 20 }, radius: 25, count: 5, hemisphere: 'center' }
  ]

  lobes.forEach(lobe => lobe.count = 0)

  brainRegions.forEach(region => {
    lobes[region.lobe].count += region.count
    
    for (let i = 0; i < region.count; i++) {
      const hemisphereSign = region.hemisphere === 'left' ? 1 : region.hemisphere === 'right' ? -1 : 0
      const baseX = region.positions.x + (region.hemisphere !== 'center' ? 55 * hemisphereSign : 0)
      
      const theta = Math.random() * Math.PI * 2
      const phi = Math.acos(2 * Math.random() - 1)
      const r = Math.pow(Math.random(), 0.5) * region.radius
      
      const x = baseX + r * Math.sin(phi) * Math.cos(theta) * 0.9
      const y = region.positions.y + r * Math.sin(phi) * Math.sin(theta) * 0.6
      const z = region.positions.z + r * Math.cos(phi) * 0.8 + (Math.random() - 0.5) * 30
      
      nodes.push({
        id: nodeIndex,
        label: labels[nodeIndex % labels.length] + (Math.floor(nodeIndex / labels.length) > 0 ? `-${nodeIndex}` : ''),
        group: lobes[region.lobe].name,
        color: lobes[region.lobe].color,
        x,
        y,
        z,
        vx: 0,
        vy: 0,
        vz: 0,
        active: Math.random() > 0.5,
        connections: 0,
        region: region.lobe,
        hemisphere: region.hemisphere
      })
      nodeIndex++
    }
  })

  for (let i = 0; i < nodes.length; i++) {
    const connectCount = 4 + Math.floor(Math.random() * 5)
    for (let c = 0; c < connectCount; c++) {
      let j
      if (Math.random() > 0.3) {
        const sameRegion = nodes.filter(n => n.id !== i && n.region === nodes[i].region)
        if (sameRegion.length > 0) {
          j = sameRegion[Math.floor(Math.random() * sameRegion.length)].id
        } else {
          j = Math.floor(Math.random() * nodes.length)
        }
      } else {
        j = Math.floor(Math.random() * nodes.length)
      }
      
      if (i !== j) {
        const exists = links.some(l => 
          (l.source === i && l.target === j) || 
          (l.source === j && l.target === i)
        )
        if (!exists) {
          const sameRegion = nodes[i].region === nodes[j].region
          const sameHemisphere = nodes[i].hemisphere === nodes[j].hemisphere || nodes[i].hemisphere === 'center' || nodes[j].hemisphere === 'center'
          const strength = sameRegion ? 0.6 : sameHemisphere ? 0.35 : 0.2
          
          links.push({
            source: i,
            target: j,
            strength,
            active: Math.random() > (sameRegion ? 0.3 : 0.6)
          })
          nodes[i].connections++
          nodes[j].connections++
        }
      }
    }
  }

  return { nodes, links }
}

function project(x, y, z) {
  const rX = rotation.x * Math.PI * 2
  const rY = rotation.y * Math.PI * 2
  
  let x1 = x * Math.cos(rY) - z * Math.sin(rY)
  let z1 = z * Math.cos(rY) + x * Math.sin(rY)
  let y1 = y * Math.cos(rX) - z1 * Math.sin(rX)
  let z2 = z1 * Math.cos(rX) + y * Math.sin(rX)
  
  const fov = 500
  const k = fov / (fov + z2)
  
  return {
    x: width / 2 + x1 * k * scale,
    y: height / 2 + y1 * k * scale,
    z: z2,
    k
  }
}

function createSimulation() {
  const { nodes, links } = generateGraphData()
  nodesData = nodes
  linksData = links

  const initialPositions = {}
  nodes.forEach(d => {
    initialPositions[d.id] = { x: d.x, y: d.y, z: d.z }
  })

  simulation = d3.forceSimulation(nodesData)
    .force('link', d3.forceLink(linksData).id(d => d.id).distance(d => 40 + d.strength * 50).strength(d => d.strength * 0.6))
    .force('charge', d3.forceManyBody().strength(-60).distanceMax(300))
    .force('center', d3.forceCenter(0, 0))
    .force('collide', d3.forceCollide().radius(20).strength(0.9))
    .force('x', d3.forceX(d => initialPositions[d.id].x).strength(0.4))
    .force('y', d3.forceY(d => initialPositions[d.id].y).strength(0.4))
    .velocityDecay(0.95)
    .alphaTarget(0)
    .stop()

  nodesData.forEach(d => {
    d.z = initialPositions[d.id].z
  })
}

function render() {
  svg.selectAll('*').remove()

  const defs = svg.append('defs')
  
  const gradients = lobes.map((lobe, i) => {
    const grad = defs.append('radialGradient')
      .attr('id', `grad-${i}`)
    grad.append('stop')
      .attr('offset', '0%')
      .attr('stop-color', lobe.color)
      .attr('stop-opacity', 0.8)
    grad.append('stop')
      .attr('offset', '100%')
      .attr('stop-color', lobe.color)
      .attr('stop-opacity', 0.4)
    return grad
  })

  const glow = defs.append('filter')
    .attr('id', 'glow')
  glow.append('feGaussianBlur')
    .attr('stdDeviation', '3')
    .attr('result', 'coloredBlur')
  glow.append('feMerge')
    .append('feMergeNode')
    .attr('in', 'coloredBlur')
  glow.append('feMerge')
    .append('feMergeNode')
    .attr('in', 'SourceGraphic')

  const linksGroup = svg.append('g').attr('class', 'links')
  const nodesGroup = svg.append('g').attr('class', 'nodes')

  const linkElements = linksGroup.selectAll('line')
    .data(linksData)
    .enter()
    .append('line')
    .attr('class', 'link')
    .attr('stroke', d => d.active ? '#22c55e' : 'rgba(99, 102, 241, 0.4)')
    .attr('stroke-width', d => d.strength * 2 + 1)

  const nodeElements = nodesGroup.selectAll('g')
    .data(nodesData)
    .enter()
    .append('g')
    .attr('class', 'node')
    .style('cursor', 'grab')

  nodeElements.append('circle')
    .attr('class', 'node-circle')
    .attr('r', d => 8 + (d.active ? 4 : 0))
    .attr('fill', d => d.color)
    .attr('filter', d => d.active ? 'url(#glow)' : null)

  nodeElements.append('circle')
    .attr('class', 'node-glow')
    .attr('r', d => 14)
    .attr('fill', 'transparent')
    .attr('stroke', d => d.color)
    .attr('stroke-width', 2)
    .style('opacity', 0)

  nodeElements.append('text')
    .attr('class', 'node-label')
    .attr('dy', -18)
    .attr('text-anchor', 'middle')
    .attr('fill', 'var(--text-primary)')
    .text(d => d.label)
    .style('pointer-events', 'none')
    .style('opacity', 0)

  nodeElements
    .on('mouseenter', function(event, d) {
      selectedNode.value = d
      d3.select(this).select('.node-label').style('opacity', 1)
      d3.select(this).select('.node-glow').style('opacity', 0.5)
    })
    .on('mouseleave', function(event, d) {
      if (!selectedNode.value || selectedNode.value.id !== d.id) {
        d3.select(this).select('.node-label').style('opacity', 0)
        d3.select(this).select('.node-glow').style('opacity', 0)
      }
    })
    .on('click', function(event, d) {
      event.stopPropagation()
      if (d.fx != null) {
        d.fx = null
        d.fy = null
        d.fz = null
      } else {
        d.fx = d.x
        d.fy = d.y
        d.fz = d.z
      }
      selectedNode.value = d
    })
    .call(d3.drag()
      .on('start', function(event, d) {
        d3.select(this).style('cursor', 'grabbing')
        d.fx = d.x
        d.fy = d.y
        d.fz = d.z
      })
      .on('drag', function(event, d) {
        const rect = svg.node().getBoundingClientRect()
        const dx = event.x - rect.left - width / 2
        const dy = event.y - rect.top - height / 2
        
        const fov = 500
        const k = (fov + (d.z || 0)) / fov
        
        d.fx = dx / scale / k
        d.fy = dy / scale / k
      })
      .on('end', function(event, d) {
        d3.select(this).style('cursor', 'grab')
      })
    )

  svg.on('mousedown', (event) => {
    if (event.target.closest('.node')) return
    isDragging = true
    lastMouse = { x: event.clientX, y: event.clientY }
  })

  svg.on('mousemove', (event) => {
    if (!isDragging) return
    const dx = (event.clientX - lastMouse.x) / width
    const dy = (event.clientY - lastMouse.y) / height
    rotation.y += dx
    rotation.x += dy
    lastMouse = { x: event.clientX, y: event.clientY }
  })

  svg.on('mouseup', () => {
    isDragging = false
  })

  svg.on('mouseleave', () => {
    isDragging = false
  })

  svg.on('wheel', (event) => {
    event.preventDefault()
    const zoomFactor = event.deltaY > 0 ? 0.95 : 1.05
    scale = Math.max(0.3, Math.min(3, scale * zoomFactor))
  })

  svg.on('dblclick', (event) => {
    if (event.target.closest('.node')) return
    resetView()
  })

  function tick() {
    frameCount++
    const now = performance.now()
    if (now - lastFrameTime >= 1000) {
      fpsValue = frameCount
      frameCount = 0
      lastFrameTime = now
    }

    const projectedNodes = nodesData.map(d => {
      const p = project(d.x || 0, d.y || 0, d.z || 0)
      return { ...d, px: p.x, py: p.y, pz: p.z, k: p.k }
    }).sort((a, b) => a.pz - b.pz)

    nodeElements
      .data(projectedNodes, d => d.id)
      .attr('transform', d => `translate(${d.px}, ${d.py})`)
      .style('opacity', d => 0.5 + d.k * 0.5)
      .select('.node-circle')
      .attr('r', d => (8 + (d.active ? 4 : 0)) * Math.max(0.5, d.k))
      .attr('fill', d => {
        if (selectedNode.value && selectedNode.value.id === d.id) {
          return '#ffffff'
        }
        return d.active ? d.color : d.color
      })

    nodeElements
      .select('.node-glow')
      .attr('r', d => 14 * Math.max(0.5, d.k))
      .style('opacity', d => selectedNode.value && selectedNode.value.id === d.id ? 0.8 : 0)

    nodeElements
      .select('.node-label')
      .style('opacity', d => selectedNode.value && selectedNode.value.id === d.id ? 1 : 0)

    linkElements
      .attr('x1', d => {
        const s = nodesData.find(n => n.id === (typeof d.source === 'object' ? d.source.id : d.source))
        if (!s) return 0
        const p = project(s.x || 0, s.y || 0, s.z || 0)
        return p.x
      })
      .attr('y1', d => {
        const s = nodesData.find(n => n.id === (typeof d.source === 'object' ? d.source.id : d.source))
        if (!s) return 0
        const p = project(s.x || 0, s.y || 0, s.z || 0)
        return p.y
      })
      .attr('x2', d => {
        const t = nodesData.find(n => n.id === (typeof d.target === 'object' ? d.target.id : d.target))
        if (!t) return 0
        const p = project(t.x || 0, t.y || 0, t.z || 0)
        return p.x
      })
      .attr('y2', d => {
        const t = nodesData.find(n => n.id === (typeof d.target === 'object' ? d.target.id : d.target))
        if (!t) return 0
        const p = project(t.x || 0, t.y || 0, t.z || 0)
        return p.y
      })
      .attr('stroke', d => d.active ? '#22c55e' : 'rgba(99, 102, 241, 0.4)')
      .style('opacity', d => {
        const s = nodesData.find(n => n.id === (typeof d.source === 'object' ? d.source.id : d.source))
        const t = nodesData.find(n => n.id === (typeof d.target === 'object' ? d.target.id : d.target))
        if (!s || !t) return 0
        const p1 = project(s.x || 0, s.y || 0, s.z || 0)
        const p2 = project(t.x || 0, t.y || 0, t.z || 0)
        const avgK = (p1.k + p2.k) / 2
        return 0.2 + avgK * 0.5
      })

    if (simulating.value) {
      simulation.tick()
    }
    animationFrame = requestAnimationFrame(tick)
  }
  animationFrame = requestAnimationFrame(tick)
}

function initSvg() {
  if (!graphContainer.value || !graphSvg.value) return
  
  const rect = graphContainer.value.getBoundingClientRect()
  width = rect.width
  height = rect.height

  svg = d3.select(graphSvg.value)
    .attr('width', width)
    .attr('height', height)
    .style('background', 'linear-gradient(135deg, var(--bg-secondary) 0%, var(--bg-primary) 100%)')

  createSimulation()
  render()
}

function toggleSimulation() {
  simulating.value = !simulating.value
  if (simulating.value) {
    simulation.alphaTarget(0.1).restart()
  } else {
    simulation.alphaTarget(0)
  }
}

function resetView() {
  rotation = { x: 0.5, y: 0.5 }
  scale = 1
  selectedNode.value = null
  createSimulation()
  svg.selectAll('*').remove()
  render()
}

function unpinSelectedNode() {
  if (selectedNode.value) {
    const node = nodesData.find(n => n.id === selectedNode.value.id)
    if (node) {
      if (node.fx != null) {
        node.fx = null
        node.fy = null
        node.fz = null
      } else {
        node.fx = node.x
        node.fy = node.y
        node.fz = node.z
      }
    }
  }
}

let resizeObserver = null

onMounted(async () => {
  await nextTick()
  
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      if (graphContainer.value && graphSvg.value) {
        const rect = graphContainer.value.getBoundingClientRect()
        width = rect.width
        height = rect.height
        d3.select(graphSvg.value).attr('width', width).attr('height', height)
      }
    })
    resizeObserver.observe(graphContainer.value)
  }

  initSvg()
})

onUnmounted(() => {
  if (animationFrame) cancelAnimationFrame(animationFrame)
  if (resizeObserver) resizeObserver.disconnect()
})
</script>

<style scoped>
.brain-control-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg-primary);
  padding: 20px;
  gap: 20px;
}

.panel-header {
  text-align: center;
}

.panel-header h2 {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
}

.panel-header .subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.view-controls {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 0 20px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  background: var(--bg-secondary);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all var(--transition-fast);
}

.action-btn:hover {
  background: var(--bg-tertiary);
}

.brain-container {
  flex: 1;
  display: flex;
  gap: 24px;
  overflow: hidden;
}

.brain-3d {
  flex: 1;
  position: relative;
  background: var(--bg-secondary);
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-default);
  overflow: hidden;
}

.graph-svg {
  width: 100%;
  height: 100%;
  min-height: 400px;
  display: block;
}

.controls-hint {
  position: absolute;
  bottom: 12px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.6);
  padding: 6px 16px;
  border-radius: var(--radius-md);
  color: rgba(255, 255, 255, 0.8);
  font-size: 12px;
  backdrop-filter: blur(4px);
}

.node {
  cursor: grab;
}

.node:active {
  cursor: grabbing;
}

.node-circle {
  transition: all 0.15s ease;
}

.link {
  pointer-events: none;
}

.info-panel {
  width: 280px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

.info-card {
  background: var(--bg-secondary);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  padding: 16px;
}

.info-card h3 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 12px 0;
}

.data-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.data-item {
  text-align: center;
  padding: 10px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.data-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--accent-primary);
  margin-bottom: 4px;
}

.data-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.lobe-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.lobe-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background: var(--bg-tertiary);
  border-radius: var(--radius-md);
}

.lobe-color {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.lobe-info {
  flex: 1;
  min-width: 0;
}

.lobe-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--text-primary);
}

.lobe-function {
  font-size: 11px;
  color: var(--text-secondary);
}

.selected-info {
  text-align: center;
}

.selected-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.selected-group,
.selected-connections {
  font-size: 13px;
  color: var(--text-secondary);
  margin-bottom: 4px;
}

.mini-btn {
  margin-top: 12px;
  padding: 6px 16px;
  background: var(--accent-primary);
  border: none;
  border-radius: var(--radius-md);
  color: white;
  font-size: 13px;
  cursor: pointer;
  transition: background var(--transition-fast);
}

.mini-btn:hover {
  background: var(--accent-primary-hover);
}

.no-selection {
  text-align: center;
  color: var(--text-secondary);
  font-size: 14px;
  padding: 20px 0;
}
</style>
