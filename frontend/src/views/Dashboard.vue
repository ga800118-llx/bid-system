<template>
  <div class="dashboard">
    <PageHeader title="仪表盘" />>
    <div class="content">
      <div class="stat-cards">
        <div class="stat-card blue">
          <div class="stat-value">{{ stats.total }}</div>
          <div class="stat-label">总项目数</div>
        </div>
        <div class="stat-card sky">
          <div class="stat-value">{{ stats.monthlyNew }}</div>
          <div class="stat-label">本月新增</div>
        </div>
        <div class="stat-card orange">
          <div class="stat-value">{{ stats.pendingBidOpen }}</div>
          <div class="stat-label">待开标</div>
        </div>
        <div class="stat-card green">
          <div class="stat-value">{{ stats.agencyCount }}</div>
          <div class="stat-label">代理机构数</div>
        </div>
      </div>
      <div class="charts-row">
        <div class="chart-card">
          <h3>月度上传趋势</h3>
          <div ref="monthlyChart" class="chart"></div>
        </div>
        <div class="chart-card">
          <h3>代理机构 TOP5</h3>
          <div ref="agencyChart" class="chart"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'

const baseURL = '/api'
const token = localStorage.getItem('token')
const stats = ref({ total: 0, monthlyNew: 0, pendingBidOpen: 0, agencyCount: 0 })
const monthlyChart = ref(null)
const agencyChart = ref(null)

async function fetchData() {
  try {
    const [sr, mr, ar] = await Promise.all([
      axios.get(baseURL + '/dashboard/stats', { headers: { Authorization: 'Bearer ' + token } }),
      axios.get(baseURL + '/dashboard/monthly', { headers: { Authorization: 'Bearer ' + token } }),
      axios.get(baseURL + '/dashboard/by-agency', { headers: { Authorization: 'Bearer ' + token } })
    ])
    if (sr.data.code === 200) stats.value = sr.data.data
    if (mr.data.code === 200) drawMonthly(mr.data.data)
    if (ar.data.code === 200) drawAgency(ar.data.data)
  } catch (e) { console.error(e) }
}

function drawMonthly(data) {
  if (!monthlyChart.value) return
  const chart = echarts.init(monthlyChart.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.months },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: data.counts, itemStyle: { color: '#165DFF' }, barWidth: '50%' }],
    grid: { left: 50, right: 20, top: 20, bottom: 30 }
  })
}

function drawAgency(data) {
  if (!agencyChart.value) return
  const chart = echarts.init(agencyChart.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: data.agencies.reverse() },
    series: [{ type: 'bar', data: data.counts.reverse(), itemStyle: { color: '#00B42A' }, barWidth: '50%' }],
    grid: { left: 120, right: 20, top: 20, bottom: 30 }
  })
}

onMounted(fetchData)
</script>

<style scoped>
.dashboard { min-height: 100vh; background: #f0f2f5; }
.page-header { background: #fff; padding: 16px 24px; border-bottom: 1px solid #e8e8e8; }
.page-header h2 { margin: 0; font-size: 18px; font-weight: 600; color: #1d1d1d; }
.content { padding: 24px; max-width: 1200px; margin: 0 auto; }
.stat-cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { background: #fff; border-radius: 8px; padding: 24px; text-align: center; }
.stat-card.blue { border-top: 3px solid #165DFF; }
.stat-card.sky { border-top: 3px solid #0EA5E9; }
.stat-card.orange { border-top: 3px solid #FF7D00; }
.stat-card.green { border-top: 3px solid #00B42A; }
.stat-value { font-size: 36px; font-weight: 700; color: #1d1d1d; margin-bottom: 8px; }
.stat-label { font-size: 14px; color: #666; }
.charts-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.chart-card { background: #fff; border-radius: 8px; padding: 20px; }
.chart-card h3 { margin: 0 0 16px 0; font-size: 15px; font-weight: 600; color: #1d1d1d; }
.chart { height: 300px; width: 100%; }
@media (max-width: 900px) { .stat-cards { grid-template-columns: repeat(2, 1fr); } .charts-row { grid-template-columns: 1fr; } }
</style>