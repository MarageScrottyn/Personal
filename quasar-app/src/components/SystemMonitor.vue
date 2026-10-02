<template>
  <div class="system-monitor">
    <div class="monitor-grid">
      <!-- CPU -->
      <div class="monitor-item" @click="toggleExpand('cpu')">
        <div class="monitor-icon">
          <CpuIcon :size="18" />
        </div>
        <div class="monitor-info">
          <span class="monitor-label">CPU</span>
          <span class="monitor-value">{{ formatValue(cpuUsage, '%') }}</span>
        </div>
        <div class="monitor-bar">
          <div 
            class="monitor-bar-fill" 
            :style="{ width: cpuUsage + '%', backgroundColor: getBarColor(cpuUsage) }"
          ></div>
        </div>
      </div>

      <!-- Memory -->
      <div class="monitor-item" @click="toggleExpand('memory')">
        <div class="monitor-icon">
          <MemoryStickIcon :size="18" />
        </div>
        <div class="monitor-info">
          <span class="monitor-label">内存</span>
          <span class="monitor-value">{{ formatMemory(memoryUsed) }}</span>
        </div>
        <div class="monitor-bar">
          <div 
            class="monitor-bar-fill" 
            :style="{ width: memoryUsage + '%', backgroundColor: getBarColor(memoryUsage) }"
          ></div>
        </div>
      </div>

      <!-- Battery -->
      <div class="monitor-item" v-if="batteryInfo" @click="toggleExpand('battery')">
        <div class="monitor-icon">
          <component :is="getBatteryIcon()" :size="18" />
        </div>
        <div class="monitor-info">
          <span class="monitor-label">{{ batteryInfo.acOnline ? '充电中' : '电池' }}</span>
          <span class="monitor-value">{{ batteryInfo.batteryLifePercent }}%</span>
        </div>
        <div class="monitor-bar">
          <div 
            class="monitor-bar-fill" 
            :style="{ 
              width: batteryInfo.batteryLifePercent + '%', 
              backgroundColor: getBatteryColor(batteryInfo.batteryLifePercent) 
            }"
          ></div>
        </div>
      </div>

      <!-- Volume -->
      <div class="monitor-item" v-if="volumeInfo" @click="toggleExpand('volume')">
        <div class="monitor-icon">
          <component :is="getVolumeIcon()" :size="18" />
        </div>
        <div class="monitor-info">
          <span class="monitor-label">音量</span>
          <span class="monitor-value">{{ formatValue(volumeInfo.level, '%') }}</span>
        </div>
        <div class="monitor-bar">
          <div 
            class="monitor-bar-fill" 
            :style="{ width: volumeInfo.level + '%', backgroundColor: '#4CAF50' }"
          ></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>import { ref, onMounted, onUnmounted, computed } from 'vue';
import { CpuIcon, MemoryStickIcon, BatteryIcon, BatteryChargingIcon, Volume2Icon, VolumeXIcon } from 'lucide-vue-next';
const cpuUsage = ref(0);
const memoryUsage = ref(0);
const memoryUsed = ref(0);
const memoryTotal = ref(0);
const batteryInfo = ref(null);
const volumeInfo = ref(null);
let updateInterval = null;
const formatValue = (value, unit = '') => {
 if (value === null || value === undefined)
 return '0' + unit;
 return value.toFixed(1) + unit;
};
const formatMemory = (bytes) => {
 if (!bytes)
 return '0 GB';
 const gb = bytes / (1024 * 1024 * 1024);
 return gb.toFixed(1) + ' GB';
};
const getBarColor = (usage) => {
 if (usage >= 90)
 return '#ef4444';
 if (usage >= 70)
 return '#f59e0b';
 return '#4CAF50';
};
const getBatteryColor = (percent) => {
 if (percent <= 20)
 return '#ef4444';
 if (percent <= 50)
 return '#f59e0b';
 return '#4CAF50';
};
const getBatteryIcon = () => {
 if (!batteryInfo.value)
 return BatteryIcon;
 return batteryInfo.value.acOnline ? BatteryChargingIcon : BatteryIcon;
};
const getVolumeIcon = () => {
 if (!volumeInfo.value)
 return Volume2Icon;
 return volumeInfo.value.muted ? VolumeXIcon : Volume2Icon;
};
const toggleExpand = (type) => {
 console.log('Toggle expand:', type);
};
const updateStats = async () => {
 try {
 const stats = await window.electronAPI?.nativeService?.getAllStats();
 if (stats) {
 if (stats.cpu) {
 cpuUsage.value = parseFloat(stats.cpu.usage) || 0;
 }
 if (stats.memory) {
 memoryUsage.value = parseFloat(stats.memory.usage) || 0;
 memoryUsed.value = stats.memory.used || 0;
 memoryTotal.value = stats.memory.total || 0;
 }
 if (stats.battery && !stats.battery.error) {
 batteryInfo.value = {
 acOnline: stats.battery.acOnline,
 batteryPresent: stats.battery.batteryPresent,
 batteryLifePercent: stats.battery.batteryLifePercent || 0,
 batteryLifeTime: stats.battery.batteryLifeTime,
 batteryFullLifeTime: stats.battery.batteryFullLifeTime
 };
 }
 if (stats.volume && !stats.volume.error) {
 volumeInfo.value = {
 level: parseFloat(stats.volume.level) || 0,
 muted: stats.volume.muted || false
 };
 }
 }
 }
 catch (error) {
 console.error('Failed to update stats:', error);
 }
};
onMounted(() => {
 updateStats();
 updateInterval = setInterval(updateStats, 2000);
});
onUnmounted(() => {
 if (updateInterval) {
 clearInterval(updateInterval);
 }
});
</script>

<style scoped>
.system-monitor {
  padding: 12px;
}

.monitor-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.monitor-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.monitor-item:hover {
  background: rgba(255, 255, 255, 0.1);
}

.monitor-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  color: #fff;
}

.monitor-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.monitor-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.monitor-value {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.monitor-bar {
  width: 60px;
  height: 4px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  overflow: hidden;
}

.monitor-bar-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s ease;
}
</style>