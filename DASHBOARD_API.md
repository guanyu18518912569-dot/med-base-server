# 管理后台首页统计功能说明

## 功能概述

管理后台首页统计功能提供完整的数据分析和可视化展示，包括：
- 订单统计（今日/本周/本月/总计）
- 销售趋势分析
- 热销商品排行
- 分类销售分布
- 订单状态统计
- 实时数据监控

## 接口说明

### 1. 获取首页统计概览
**接口**: `GET /admin/dashboard/overview`

**说明**: 获取今日、本周、本月、总体的核心统计指标

**请求头**:
```
Token: {登录token}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "today": {
      "totalOrders": 15,
      "paidOrders": 12,
      "completedOrders": 8,
      "totalAmount": 125000,
      "totalAmountYuan": "1250.00",
      "avgOrderAmount": "104.17"
    },
    "week": {
      "totalOrders": 85,
      "paidOrders": 72,
      "completedOrders": 58,
      "totalAmount": 856000,
      "totalAmountYuan": "8560.00",
      "avgOrderAmount": "118.89"
    },
    "month": {
      "totalOrders": 320,
      "paidOrders": 285,
      "completedOrders": 245,
      "totalAmount": 3245000,
      "totalAmountYuan": "32450.00",
      "avgOrderAmount": "113.86"
    },
    "total": {
      "totalOrders": 1250,
      "paidOrders": 1100,
      "completedOrders": 980,
      "totalAmount": 12500000,
      "totalAmountYuan": "125000.00",
      "avgOrderAmount": "113.64"
    },
    "totalUsers": 5680,
    "todayNewUsers": 25
  }
}
```

### 2. 获取订单状态统计
**接口**: `GET /admin/dashboard/order-status`

**说明**: 统计各状态订单数量

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "toPay": 45,        // 待付款
    "toDeliver": 28,    // 待发货
    "toReceive": 35,    // 待收货
    "completed": 980,   // 已完成
    "cancelled": 150,   // 已取消
    "afterSale": 12     // 售后中
  }
}
```

### 3. 获取销售趋势
**接口**: `GET /admin/dashboard/sales-trend?days=7`

**参数**:
- `days`: 天数，默认7天，可选7、15、30等

**说明**: 获取近N天的销售趋势数据，用于图表展示

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "days": 7,
    "trend": [
      {
        "date": "2026-01-01",
        "amount": 125000,
        "amountYuan": "1250.00",
        "orderCount": 12
      },
      {
        "date": "2026-01-02",
        "amount": 145000,
        "amountYuan": "1450.00",
        "orderCount": 15
      }
      // ... 更多日期数据
    ]
  }
}
```

### 4. 获取热销商品Top10
**接口**: `GET /admin/dashboard/top-products?limit=10`

**参数**:
- `limit`: 返回数量，默认10

**说明**: 获取最近30天热销商品排行

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "period": "最近30天",
    "products": [
      {
        "spuId": "SPU001",
        "goodsName": "商品A",
        "goodsImage": "https://example.com/image.jpg",
        "salesVolume": 256,
        "totalAmount": 2560000,
        "totalAmountYuan": "25600.00"
      }
      // ... 更多商品
    ]
  }
}
```

### 5. 获取分类销售分布
**接口**: `GET /admin/dashboard/category-distribution`

**说明**: 获取各分类的销售额占比，用于饼图展示

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "period": "最近30天",
    "totalAmount": 5600000,
    "totalAmountYuan": "56000.00",
    "distribution": [
      {
        "categoryId": 1,
        "categoryName": "分类A",
        "amount": 2240000,
        "amountYuan": "22400.00",
        "percentage": 40.0
      },
      {
        "categoryId": 2,
        "categoryName": "分类B",
        "amount": 1680000,
        "amountYuan": "16800.00",
        "percentage": 30.0
      }
      // ... 更多分类
    ]
  }
}
```

### 6. 获取实时统计
**接口**: `GET /admin/dashboard/realtime`

**说明**: 获取实时数据，用于首页动态刷新

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "todayOrders": 15,
    "todaySales": 125000,
    "todaySalesYuan": "1250.00",
    "pendingOrders": 28,
    "updateTime": "2026-01-04 15:30:45"
  }
}
```

### 7. 获取完整首页数据（推荐）
**接口**: `GET /admin/dashboard/all?trendDays=7`

**参数**:
- `trendDays`: 趋势天数，默认7

**说明**: 一次性返回所有首页需要的数据，减少请求次数

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "overview": { /* 概览数据 */ },
    "orderStatus": { /* 订单状态统计 */ },
    "salesTrend": { /* 销售趋势 */ },
    "topProducts": { /* 热销商品 */ },
    "categoryDistribution": { /* 分类分布 */ }
  }
}
```

## 数据来源

所有统计数据均从以下表实时查询：
- `oms_order`: 订单主表
- `oms_order_item`: 订单明细表
- `ums_user`: 用户表
- `pms_spu`: 商品表
- `pms_category`: 分类表

## 性能优化建议

如果订单量较大（>10万），建议：

1. **创建统计表**
   执行 `sql/order_statistics.sql` 创建统计汇总表

2. **定时任务**
   配置定时任务每天凌晨计算前一天的统计数据

3. **缓存优化**
   对首页数据添加Redis缓存，设置5-10分钟过期时间

4. **索引优化**
   确保以下字段有索引：
   - `oms_order.status`
   - `oms_order.created_time`
   - `oms_order.pay_time`
   - `oms_order.finish_time`
   - `oms_order.deleted`

## 前端集成示例

### Vue 3 + TypeScript 示例

```typescript
// services/dashboard.ts
import request from './request';

export const getDashboardOverview = () => {
  return request.get('/admin/dashboard/overview');
};

export const getAllDashboardData = (trendDays: number = 7) => {
  return request.get('/admin/dashboard/all', { params: { trendDays } });
};

export const getRealtimeStats = () => {
  return request.get('/admin/dashboard/realtime');
};
```

```vue
<!-- pages/dashboard/index.vue -->
<template>
  <div class="dashboard">
    <!-- 概览卡片 -->
    <div class="overview-cards">
      <el-card>
        <h3>今日订单</h3>
        <div class="value">{{ dashboardData.overview?.today?.totalOrders || 0 }}</div>
      </el-card>
      <el-card>
        <h3>今日销售额</h3>
        <div class="value">¥{{ dashboardData.overview?.today?.totalAmountYuan || '0.00' }}</div>
      </el-card>
      <!-- 更多卡片... -->
    </div>

    <!-- 销售趋势图表 -->
    <el-card>
      <h3>销售趋势</h3>
      <div ref="trendChart" style="height: 300px;"></div>
    </el-card>

    <!-- 热销商品 -->
    <el-card>
      <h3>热销商品 Top10</h3>
      <el-table :data="dashboardData.topProducts?.products || []">
        <el-table-column prop="goodsName" label="商品名称" />
        <el-table-column prop="salesVolume" label="销量" />
        <el-table-column prop="totalAmountYuan" label="销售额" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import * as echarts from 'echarts';
import { getAllDashboardData, getRealtimeStats } from '@/services/dashboard';

const dashboardData = ref<any>({});
const trendChart = ref<HTMLElement>();

const loadDashboardData = async () => {
  const res = await getAllDashboardData(7);
  if (res.code === 200) {
    dashboardData.value = res.data;
    renderTrendChart();
  }
};

const renderTrendChart = () => {
  if (!trendChart.value) return;
  
  const chart = echarts.init(trendChart.value);
  const trendData = dashboardData.value.salesTrend?.trend || [];
  
  chart.setOption({
    xAxis: {
      type: 'category',
      data: trendData.map(item => item.date)
    },
    yAxis: {
      type: 'value'
    },
    series: [{
      data: trendData.map(item => item.amountYuan),
      type: 'line',
      smooth: true
    }]
  });
};

// 定时刷新实时数据
const refreshRealtimeData = async () => {
  const res = await getRealtimeStats();
  if (res.code === 200) {
    // 更新实时数据
  }
};

onMounted(() => {
  loadDashboardData();
  
  // 每30秒刷新实时数据
  setInterval(refreshRealtimeData, 30000);
});
</script>
```

## 注意事项

1. 所有接口都需要登录Token认证（`@UserLoginToken`注解）
2. 金额单位：数据库存储为"分"，接口返回同时提供"分"和"元"两种格式
3. 时间范围：默认统计已完成订单（status=3），销售趋势统计已付款订单（status>=1）
4. 删除标记：所有查询都过滤了已删除数据（deleted=0）

## 扩展功能

如需添加更多统计维度，可以扩展 `DashboardService`：

```java
// 示例：添加按地区统计
public Map<String, Object> getRegionDistribution() {
    // 实现按收货地区统计的逻辑
}
```

## 相关文件

- 控制器：`src/main/java/med/base/server/controller/DashboardController.java`
- 服务类：`src/main/java/med/base/server/service/DashboardService.java`
- SQL脚本：`sql/order_statistics.sql`（可选的统计表）
- 已有销售统计：`src/main/java/med/base/server/controller/SalesStatisticsController.java`

## 常见问题

Q: 首页数据加载很慢怎么办？
A: 建议使用 `/admin/dashboard/all` 接口一次性获取所有数据，并在前端添加loading状态。如果订单量大，考虑引入统计表和缓存。

Q: 如何自定义统计时间范围？
A: 可以参考 `SalesStatisticsController` 的接口，支持自定义 startDate 和 endDate 参数。

Q: 实时数据更新频率如何控制？
A: 建议前端30秒轮询一次 `/admin/dashboard/realtime` 接口，避免频繁请求影响性能。
