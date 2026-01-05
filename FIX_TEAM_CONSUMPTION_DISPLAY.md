# 修复下级消费金额显示错误

## 问题描述
小程序我分享的下级，他的消费金额显示错误。

## 问题原因
在团队成员列表中，显示的"消费金额"使用了错误的字段：
- **原错误**：显示 `directPerformance`（直推业绩，即该用户的下级的消费总额）
- **应显示**：`selfConsumption`（该用户自己的个人消费金额）

## 修复内容

### 1. 后端修改（WxAppController.java）

#### 修改接口 `/wxapp/user/direct-children`（获取直推用户列表）
- 在 `WxTeamMember` 中添加 `selfConsumption` 字段
- 从 `UmsUser` 中读取并设置 `selfConsumption` 值

#### 修改接口 `/wxapp/user/team`（获取所有团队成员）
- 同样在返回的 `WxTeamMember` 中添加 `selfConsumption` 字段

#### 更新 WxTeamMember DTO
```java
@Data
class WxTeamMember {
    private String userId;
    private String nickName;
    private String avatarUrl;
    private Integer level;
    private java.time.LocalDateTime createdTime;
    private java.math.BigDecimal selfConsumption;   // 个人消费金额（新增）
    private java.math.BigDecimal directPerformance; // 直推业绩（保留，用于其他统计）
}
```

### 2. 小程序前端修改（pages/user/my-team/index.wxml）

#### "我分享的"标签页
```xml
<view class="member-performance">
  <text class="performance-value">¥{{item.selfConsumption || '0.00'}}</text>
  <text class="performance-label">消费金额</text>
</view>
```

#### "全部团队"标签页
```xml
<view class="member-performance">
  <text class="performance-value">¥{{item.selfConsumption || '0.00'}}</text>
  <text class="performance-label">消费金额</text>
</view>
```

## 字段说明

### selfConsumption（个人消费）
- **含义**：该用户自己购买商品的消费总额
- **更新时机**：用户订单确认收货时
- **用途**：显示用户个人的消费金额

### directPerformance（直推业绩）
- **含义**：该用户的直接下级（一级下线）的消费总额
- **更新时机**：下级订单确认收货时
- **用途**：计算该用户可获得的分成收益

### teamPerformance（团队业绩）
- **含义**：该用户的所有下级（包括直接和间接）的消费总额
- **更新时机**：任何团队成员订单确认收货时
- **用途**：团队业绩统计、等级晋升等

## 修复效果
现在团队成员列表中的"消费金额"会正确显示每个成员自己的消费金额，而不是他们的下级业绩。

## 修复日期
2026年1月4日
