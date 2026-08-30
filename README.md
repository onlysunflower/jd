# 京东商城核心业务复刻系统

## 一、技术选型建议

本项目推荐使用：

- 后端：Java + Spring Boot
- 前端：Vue 3 + Vite + Element Plus
- 数据库：MySQL
- 持久层：MyBatis-Plus
- 权限认证：JWT + RBAC 角色权限控制
- 接口调试：Apifox / Postman
- 可选增强：Redis、Spring Validation、Swagger / Knife4j

如果课程没有强制语言，并且只追求最快完成，也可以选择 Python Django。Django 自带后台、ORM、用户权限和表单能力，开发速度很快。但如果想做成更接近真实电商系统、方便展示用户端、商家端、管理员端三类角色，Spring Boot + Vue 3 的结构更清晰，也更符合常见企业级项目风格。

本项目最终建议采用前后端分离架构：

```text
用户端 Vue 页面
商家端 Vue 后台
管理员端 Vue 后台
        |
        | HTTP API / JSON
        v
Spring Boot 后端服务
        |
        v
MySQL 数据库
```

## 二、项目简介

本项目是一个参考京东业务模式设计的电商平台核心流程复刻系统。项目不追求完整实现京东全部功能，而是围绕商品、购物车、订单、售后退款、商家管理、管理员仲裁等核心业务模块进行深入设计。

系统重点体现三类角色之间的业务协作：

- 普通用户：浏览商品、加入购物车、下单、查看订单、申请售后、评价商品
- 商家：管理商品、处理订单、发货、审核退款、处理售后
- 管理员：审核商品与商家、处理售后纠纷、管理用户和商家权限

项目目标不是简单堆页面数量，而是把选定业务流程做完整、做细致，让不同角色在同一个订单或售后流程中产生真实联动。

## 三、核心功能模块

### 1. 用户端模块

用户端主要面向普通消费者，提供基础购物体验。

主要功能：

- 用户注册、登录、退出登录
- 浏览商品列表
- 商品搜索与分类筛选
- 查看商品详情
- 加入购物车
- 修改购物车商品数量
- 选择商品结算
- 管理收货地址
- 提交订单
- 模拟支付
- 查看订单状态
- 取消订单
- 确认收货
- 申请退款 / 退货退款
- 查看售后进度
- 发表商品评价

重点细节：

- 商品库存不足时不能下单
- 商品下架后购物车中显示失效
- 未支付订单超时后自动取消
- 不同订单状态显示不同操作按钮
- 售后处理过程使用时间轴展示

### 2. 商家端模块

商家端主要用于商品、订单、售后处理。

主要功能：

- 商家登录
- 查看店铺概况
- 发布商品
- 编辑商品信息
- 设置商品价格、库存、上下架状态
- 查看商品审核状态
- 查看用户订单
- 订单发货
- 填写物流信息
- 处理退款申请
- 同意退款
- 拒绝退款并填写原因
- 确认退货收货
- 查看用户评价
- 回复评价

重点细节：

- 商品发布后需要管理员审核
- 商品审核通过后才能在用户端展示
- 商品库存不足时给商家提示
- 商家拒绝退款必须填写原因
- 商家超时不处理售后时，用户可以申请管理员介入

### 3. 管理员端模块

管理员端对应平台客服和平台管理人员，负责审核、监管和仲裁。

主要功能：

- 管理员登录
- 用户管理
- 商家管理
- 商家入驻审核
- 商品审核
- 商品违规下架
- 订单查询
- 售后纠纷处理
- 退款仲裁
- 强制退款
- 驳回售后申请
- 查看操作日志
- 分配管理员角色权限

可设计的管理员角色：

- 超级管理员：拥有全部权限
- 客服管理员：处理退款、投诉、售后仲裁
- 商品审核员：审核商品发布、处理违规商品
- 商家管理员：审核商家入驻、冻结商家
- 技术管理员：查看系统日志和异常数据

重点细节：

- 不同管理员角色看到不同菜单
- 关键操作需要填写处理原因
- 管理员操作记录日志
- 售后仲裁结果会影响订单和退款状态

## 四、推荐重点业务流程

### 1. 商品发布与审核流程

```text
商家发布商品
  -> 商品进入待审核状态
  -> 管理员审核商品
  -> 审核通过：商品上架，用户可见
  -> 审核驳回：商家修改后重新提交
```

相关状态：

- 待审核
- 审核通过
- 审核驳回
- 已上架
- 已下架

### 2. 购物下单流程

```text
用户浏览商品
  -> 加入购物车
  -> 选择收货地址
  -> 提交订单
  -> 模拟支付
  -> 商家发货
  -> 用户确认收货
  -> 订单完成
```

相关状态：

- 待付款
- 待发货
- 待收货
- 已完成
- 已取消

### 3. 退款 / 退货退款流程

```text
用户申请退款
  -> 商家审核
  -> 商家同意
  -> 用户寄回商品
  -> 商家确认收货
  -> 系统模拟退款
  -> 售后完成
```

异常流程：

```text
用户申请退款
  -> 商家拒绝
  -> 用户申请平台介入
  -> 管理员查看双方说明
  -> 管理员裁决
  -> 退款成功 / 退款失败
```

超时流程：

```text
用户申请退款
  -> 商家长时间未处理
  -> 系统提醒商家
  -> 用户申请管理员介入
  -> 管理员强制处理
```

售后状态：

- 售后申请中
- 商家审核中
- 商家已同意
- 商家已拒绝
- 等待用户退货
- 等待商家收货
- 平台介入中
- 退款成功
- 退款失败
- 售后关闭

## 五、角色权限设计

| 功能 | 用户 | 商家 | 客服管理员 | 商品审核员 | 超级管理员 |
| --- | --- | --- | --- | --- | --- |
| 浏览商品 | 是 | 是 | 是 | 是 | 是 |
| 加入购物车 | 是 | 否 | 否 | 否 | 否 |
| 提交订单 | 是 | 否 | 否 | 否 | 否 |
| 取消订单 | 是 | 部分 | 部分 | 否 | 是 |
| 商品发布 | 否 | 是 | 否 | 否 | 是 |
| 商品审核 | 否 | 否 | 否 | 是 | 是 |
| 订单发货 | 否 | 是 | 否 | 否 | 是 |
| 申请退款 | 是 | 否 | 否 | 否 | 否 |
| 审核退款 | 否 | 是 | 否 | 否 | 是 |
| 售后仲裁 | 否 | 否 | 是 | 否 | 是 |
| 商家冻结 | 否 | 否 | 否 | 否 | 是 |
| 权限分配 | 否 | 否 | 否 | 否 | 是 |

## 六、数据库表设计建议

建议至少设计以下数据表：

- user：用户表
- role：角色表
- permission：权限表
- user_role：用户角色关联表
- merchant：商家表
- shop：店铺表
- category：商品分类表
- product：商品表
- product_image：商品图片表
- cart_item：购物车表
- address：收货地址表
- order_info：订单主表
- order_item：订单明细表
- payment_record：支付记录表
- refund_request：售后退款表
- refund_log：售后流转日志表
- review：评价表
- complaint：投诉表
- operation_log：后台操作日志表

## 七、接口设计示例

### 用户端接口

```text
POST   /api/auth/login
POST   /api/auth/register
GET    /api/products
GET    /api/products/{id}
GET    /api/products/{id}/reviews
POST   /api/cart/items
PUT    /api/cart/items/{id}
DELETE /api/cart/items/{id}
POST   /api/orders
GET    /api/orders
GET    /api/orders/{id}
POST   /api/orders/{id}/pay
POST   /api/orders/{id}/cancel
POST   /api/orders/{id}/confirm
POST   /api/refunds
GET    /api/refunds/{id}
GET    /api/reviews/tasks
POST   /api/reviews
```

### 商家端接口

```text
POST   /api/merchant/products
PUT    /api/merchant/products/{id}
POST   /api/merchant/products/{id}/submit-review
POST   /api/merchant/products/{id}/off-shelf
GET    /api/merchant/orders
POST   /api/merchant/orders/{id}/ship
GET    /api/merchant/refunds
POST   /api/merchant/refunds/{id}/approve
POST   /api/merchant/refunds/{id}/reject
POST   /api/merchant/refunds/{id}/confirm-return
```

### 管理员端接口

```text
GET    /api/admin/users
GET    /api/admin/merchants
POST   /api/admin/merchants/{id}/approve
POST   /api/admin/merchants/{id}/reject
POST   /api/admin/merchants/{id}/freeze
GET    /api/admin/products/pending
POST   /api/admin/products/{id}/approve
POST   /api/admin/products/{id}/reject
GET    /api/admin/refunds/disputes
POST   /api/admin/refunds/{id}/arbitrate
GET    /api/admin/logs
```

## 八、页面设计建议

### 用户端页面

- 首页 / 商品列表页
- 商品详情页
- 购物车页
- 结算页
- 我的订单页
- 订单详情页
- 退款申请页
- 售后详情页
- 售后与评价页（待评价、已评价、商品评价弹窗）
- 收货地址管理页

### 商家端页面

- 商家工作台
- 商品管理页
- 商品发布 / 编辑页
- 订单管理页
- 发货处理页
- 售后管理页
- 评价管理页
- 店铺信息页

### 管理员端页面

- 管理员仪表盘
- 用户管理页
- 商家审核页
- 商家管理页
- 商品审核页
- 订单查询页
- 售后仲裁页
- 权限管理页
- 操作日志页

## 九、前端界面风格建议

整体风格可以参考京东：

- 主色：京东红 `#E2231A`
- 辅助色：深灰、浅灰、白色
- 用户端：更偏商城风格，突出商品图片、价格、购买按钮
- 商家端：更偏后台管理风格，使用表格、筛选器、状态标签
- 管理员端：强调信息密度和操作效率，适合使用侧边栏布局

建议使用 Element Plus 组件：

- Table：订单、商品、售后列表
- Form：商品发布、退款申请、审核表单
- Dialog：确认操作、填写拒绝原因
- Steps / Timeline：订单和售后进度
- Tag：订单状态、售后状态、审核状态
- Tabs：区分不同订单状态

## 十、开发优先级

建议按照以下顺序实现：

1. 用户、角色、登录认证
2. 商品分类与商品展示
3. 商家商品发布与管理员审核
4. 购物车与提交订单
5. 模拟支付与订单状态流转
6. 商家发货与用户确认收货
7. 退款 / 退货退款流程
8. 管理员售后仲裁
9. 用户评价与商家回复
10. 操作日志与权限细化

最小可交付版本可以只完成：

1. 登录注册
2. 商品浏览
3. 购物车
4. 下单与模拟支付
5. 商家发货
6. 用户退款
7. 商家审核退款
8. 管理员介入仲裁

## 十一、项目运行方式

### 数据库升级

当前工程已加入 SKU、优惠券、商家入驻、结算提现和评价治理。已有旧数据库时，先备份后在 MySQL 中执行：

```text
database/migration_business_v2.sql
```

全新部署或不需要保留旧测试数据时，依次执行 `database/schema.sql`、`database/data.sql`。`schema.sql` 会删除并重建业务表，请勿在需要保留数据的库上直接执行。

### 后端运行

```bash
cd backend
mvn spring-boot:run
```

默认后端地址：

```text
http://localhost:8080
```

### 前端运行

```bash
cd frontend
npm install
npm run dev
```

默认前端地址：

```text
http://localhost:5173
```

## 十二、演示账号建议

可以在初始化数据中准备以下账号：

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 普通用户 | user001 | 123456 |
| 商家 | merchant001 | 123456 |
| 客服管理员 | service_admin | 123456 |
| 商品审核员 | product_admin | 123456 |
| 超级管理员 | super_admin | 123456 |
| 待审核入驻账号 | merchant_apply | 123456 |

## 十三、项目亮点

- 不是简单商城页面，而是完整设计用户、商家、管理员三方业务联动
- 订单状态和售后状态清晰，适合展示复杂业务流程
- 支持商家超时不处理、用户申请平台介入、管理员仲裁等真实业务场景
- 使用 RBAC 权限模型，不同后台角色拥有不同菜单和操作权限
- 使用操作日志记录关键后台行为，增强系统真实性
- 支付、物流等非重点模块使用模拟逻辑，降低实现成本

## 十四、后续可扩展功能

- 优惠券、满减、秒杀等促销模块
- 商品推荐模块
- 物流轨迹模拟
- 商家评分体系
- 用户投诉处罚机制
- 数据统计看板
- Redis 缓存热门商品
- WebSocket 订单消息通知

## 十五、项目定位

本项目适合作为课程设计或综合实训项目。实现时应重点关注业务流程的完整性、角色权限的合理性、页面操作的便利性和系统结构的清晰性。相比功能数量，项目更强调把订单、售后、商品审核等核心流程做深做扎实。

## 十六、当前工程结构

本仓库已经按前后端分离方式生成基础工程：

```text
backend/               Spring Boot 后端
frontend/              Vue 3 + Element Plus 前端
database/schema.sql    MySQL 建表脚本
database/data.sql      演示数据和演示账号
QUICK_START.md         快速启动说明
```

后端当前采用轻量 token 认证，适合课程项目演示。真实生产系统应替换为更完整的 Spring Security + JWT 方案，并对密码进行加密存储。
