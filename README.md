# ZooReserve 动物园预约管理系统

ZooReserve 是一个前后端分离的动物园预约管理实验项目，覆盖游客预约、活动报名、订单支付、优惠券、公告、后台管理和核销流程。项目目标是展示完整业务闭环，而不是接入真实支付或生产级高并发能力。

## 功能概览

- 游客端：门票预约、活动预约、我的订单、入园凭证、会员中心、优惠券领取。
- 后台端：票务库存、订单管理、活动管理、营销管理、动物展区、核销和系统账号。
- 核销端：扫码核销、人工核销、订单状态回写。
- 营销闭环：后台发布优惠券/公告，前台领取优惠券并在门票或活动订单中使用。
- 活动闭环：免费活动直接报名；收费活动创建活动订单，支付成功后报名生效。

## 技术栈

- 前端：Vue 3、Vite、TypeScript、Pinia、Vue Router、Element Plus、Lucide Icons、Vitest。
- 后端：Spring Boot 3、Spring Security、JWT、JdbcTemplate、MySQL、H2 Test、Springdoc OpenAPI。
- 数据库：MySQL 8，测试环境使用 H2。

## 目录结构

```text
ZooReserve/
  backend/   Spring Boot 后端服务
  frontend/  Vue 前端应用
  docs/      项目补充文档
```

常用数据库脚本：

- `backend/src/main/resources/db/schema.sql`：完整初始化脚本。
- `backend/src/main/resources/db/data.sql`：演示数据。
- `backend/src/main/resources/db/upgrade-closure.sql`：本地库增量升级和演示数据修复脚本。

## 本地启动

### 1. 初始化或升级数据库

默认本地数据库为 `zoo_reserve`，账号密码为 `root/root`。

首次初始化：

```powershell
mysql --default-character-set=utf8mb4 -uroot -proot -e "source C:/Users/stones/Documents/Codex/ZooReserve/backend/src/main/resources/db/schema.sql"
mysql --default-character-set=utf8mb4 -uroot -proot -e "source C:/Users/stones/Documents/Codex/ZooReserve/backend/src/main/resources/db/data.sql"
```

已有数据库时，优先运行升级脚本：

```powershell
mysql --default-character-set=utf8mb4 -uroot -proot -e "source C:/Users/stones/Documents/Codex/ZooReserve/backend/src/main/resources/db/upgrade-closure.sql"
```

### 2. 启动后端

```powershell
cd C:\Users\stones\Documents\Codex\ZooReserve\backend
mvn spring-boot:run
```

后端默认地址：

- API: `http://localhost:8080`
- Swagger: `http://localhost:8080/swagger-ui.html`

如果提示 `Port 8080 was already in use`，先找出并停止占用端口的进程，或改后端端口。

```powershell
netstat -ano | findstr :8080
Stop-Process -Id <PID> -Force
```

### 3. 启动前端

PowerShell 下建议使用 `npm.cmd`，避免执行策略限制。

```powershell
cd C:\Users\stones\Documents\Codex\ZooReserve\frontend
npm.cmd install
npm.cmd run dev
```

前端默认地址通常为 `http://localhost:5173`。如果端口被占用，Vite 会自动切到 `5174` 等后续端口。

## 演示账号

| 角色 | 用户名 | 密码 | 入口 |
| --- | --- | --- | --- |
| 游客 | `visitor` | `visitor123` | 前台游客端 |
| 游客 | `family01` | `visitor123` | 前台游客端 |
| 管理员 | `admin` | `admin123` | 后台管理 |
| 核销员 | `checker` | `checker123` | 核销端 |

## 业务闭环

### 门票订单

1. 游客选择日期、场次和票种。
2. 前端展示可用门票券。
3. 创建订单时后端校验库存、券归属、券状态、有效期、门槛和适用范围。
4. 待支付订单可取消并恢复库存/优惠券。
5. 模拟支付成功后订单可生成入园凭证并核销。

### 活动订单

1. 后台活动可配置是否收费、价格和适用券类型。
2. 免费活动直接报名。
3. 收费活动创建 `ACTIVITY` 类型订单。
4. 活动券按范围筛选：
   - `ACTIVITY`：全部收费活动通用券。
   - `ACTIVITY_PARENT_CHILD`：亲子课堂券。
   - `ACTIVITY_NIGHT`：夜游活动券。
5. 支付成功后写入活动报名记录。

### 营销管理

营销管理分为两个页签：

- 优惠券活动：管理名称、优惠类型、优惠值、门槛、库存、有效期、适用范围和启停状态。
- 公告发布：管理标题、内容、展示位置、优先级和发布状态。

公告展示位置：

- `ALL`：首页和会员中心都展示。
- `HOME`：首页展示。
- `MEMBER`：会员中心消息展示。

## 测试命令

后端：

```powershell
cd C:\Users\stones\Documents\Codex\ZooReserve\backend
mvn test
```

前端类型检查：

```powershell
cd C:\Users\stones\Documents\Codex\ZooReserve\frontend
npm.cmd run type-check
```

前端单元测试：

```powershell
cd C:\Users\stones\Documents\Codex\ZooReserve\frontend
npm.cmd test
```

## 当前边界

- 支付为模拟支付，不接入微信/支付宝真实网关。
- 项目重点是实验展示和业务闭环，未按生产环境做完整风控、审计和高并发压测。
- 后台权限、库存、订单、优惠券等核心校验已经在后端兜底，前端筛选只用于提升体验。

