# ZooReserve

动物园游客预约管理系统全量骨架，采用 Vue 3 + Spring Boot 3 + MySQL 的前后端分离结构。

## Modules

- `frontend`: 游客端、管理员后台、核销员端。
- `backend`: REST API、JWT/RBAC 骨架、MyBatis-Plus 实体与 Mapper、Swagger。
- `backend/src/main/resources/db/schema.sql`: MySQL 8 初始化脚本。

## Local Commands

PowerShell 中请使用 `npm.cmd`：

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
