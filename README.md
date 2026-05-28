# ZooReserve 动物园游客预约管理系统

这是一个前后端分离的全量骨架项目，面向“动物园游客预约管理系统”的真实上线演进。

## 结构

- `frontend/`: Vue 3 + Vite + TypeScript，包含游客端、管理员后台、核销员端。
- `backend/`: Spring Boot 3，包含 JWT/RBAC 骨架、Mock 业务接口、MyBatis-Plus 实体与 Mapper、Swagger 配置。
- `backend/src/main/resources/db/schema.sql`: MySQL 8 初始化脚本。
- `docs/`: 项目说明文档。

## 本地运行

PowerShell 中使用 `npm.cmd`，避免 `npm.ps1` 执行策略限制。

```powershell
cd frontend
npm.cmd install
npm.cmd run dev
```

```powershell
cd backend
mvn test
mvn spring-boot:run
```

前端默认地址：`http://localhost:5173`

后端默认地址：`http://localhost:8080`

Swagger：`http://localhost:8080/swagger-ui.html`

## 首版账号约定

登录接口为 Mock 骨架，支持三类角色：

- `VISITOR`: 游客
- `ADMIN`: 管理员
- `CHECKER`: 核销员

## 当前边界

首版目标是可运行的全量业务基础版。真实商户支付、短信通道、对象存储、消息队列可通过现有适配器边界接入；本地开发默认使用模拟支付与本地数据库流程。
