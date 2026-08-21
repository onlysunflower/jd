# 快速启动

## 1. 初始化数据库

在 MySQL 中执行：

```bash
mysql -u root -p < database/schema.sql
mysql -u root -p < database/data.sql
```

如果你的 MySQL 密码不是 `123456`，请修改：

```text
backend/src/main/resources/application.yml
```

## 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：

```text
http://localhost:8080
```

## 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认地址：

```text
http://localhost:5173
```

## 4. 演示账号

| 角色 | 账号 | 密码 |
| --- | --- | --- |
| 普通用户 | user001 | 123456 |
| 商家 | merchant001 | 123456 |
| 客服管理员 | service_admin | 123456 |
| 商品审核员 | product_admin | 123456 |
| 超级管理员 | super_admin | 123456 |

## 5. 推荐演示流程

1. 使用 `merchant001` 登录，发布一个新商品。
2. 使用 `product_admin` 或 `super_admin` 登录，在管理员后台审核通过商品。
3. 使用 `user001` 登录，购买商品并模拟支付。
4. 使用 `merchant001` 登录，给订单发货。
5. 使用 `user001` 登录，申请退货退款。
6. 使用 `merchant001` 登录，可以同意或拒绝退款。
7. 如果商家拒绝，使用 `user001` 申请平台介入。
8. 使用 `service_admin` 或 `super_admin` 登录，完成售后仲裁。
