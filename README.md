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

## 功能模块操作指南

建议演示顺序为：管理员先在后台准备票务、活动、优惠券和公告；游客再到前台预约、领取优惠券、报名活动和支付订单；最后核销员完成入园核销。

### 1. 登录和角色切换

1. 启动前后端后，打开前端地址，例如 `http://localhost:5173`。
2. 使用 `visitor / visitor123` 或 `family01 / visitor123` 登录游客端。
3. 使用 `admin / admin123` 登录后台管理端。
4. 使用 `checker / checker123` 登录核销端。

不同角色看到的菜单不同。游客主要操作预约和订单；管理员主要维护基础数据和运营内容；核销员负责凭证核销。

### 2. 后台票务库存

管理员进入后台后，可以先检查票务相关数据。

1. 进入票务或库存管理页面。
2. 查看不同日期、场次、票种的库存。
3. 根据需要调整库存数量、启停状态或票价。
4. 保存后，游客端门票预约页面会读取最新可预约数据。

演示重点：门票订单创建时会扣减库存；取消待支付订单会恢复库存；支付成功后的订单可生成入园凭证。

### 3. 后台活动管理

活动分为免费活动和收费活动。

1. 管理员进入活动管理页面。
2. 新建或编辑活动，填写活动名称、时间、地点、名额、状态等信息。
3. 设置是否收费：
   - 免费活动：游客点击报名后直接生成报名记录。
   - 收费活动：游客点击报名后先创建活动订单，支付成功后报名生效。
4. 收费活动需要填写价格。
5. 收费活动还可以设置适用券类型：
   - `ACTIVITY`：普通收费活动券。
   - `ACTIVITY_PARENT_CHILD`：亲子活动券。
   - `ACTIVITY_NIGHT`：夜游活动券。
6. 保存并启用活动后，游客端活动列表会显示该活动。

演示重点：夜游券只能用于夜游活动，亲子券只能用于亲子活动，普通活动券可以用于普通收费活动。活动订单不占用门票库存，只占用活动名额。

### 4. 后台营销管理：优惠券活动

优惠券由后台发布，游客在前台领取，订单中使用。

1. 管理员进入营销管理页面。
2. 切换到优惠券活动页签。
3. 点击新建，填写优惠券名称、优惠类型、优惠值、使用门槛、库存、有效期。
4. 选择适用范围：
   - `TICKET`：门票预约订单可用。
   - `ACTIVITY`：普通收费活动订单可用。
   - `ACTIVITY_PARENT_CHILD`：亲子活动订单可用。
   - `ACTIVITY_NIGHT`：夜游活动订单可用。
5. 设置启用状态。
6. 保存后，游客端会在可领取优惠券区域看到该券。

优惠券发放方式是用户主动领取：游客点击领取后，系统写入 `user_coupon`，该券进入游客的“我的优惠券”。同一用户同一张券只能领取一次。

优惠券使用时以后端校验为准。后端会检查优惠券是否属于当前用户、是否未使用、是否过期、是否满足门槛、是否匹配订单类型和活动类型。

### 5. 后台营销管理：公告发布

公告用于前台内容投放，不参与订单价格计算。

1. 管理员进入营销管理页面。
2. 切换到公告发布页签。
3. 新建公告，填写标题、内容、优先级和展示位置。
4. 选择展示位置：
   - `ALL`：首页和会员中心都显示。
   - `HOME`：只在首页显示。
   - `MEMBER`：只在会员中心消息通知显示。
5. 设置发布状态：
   - 发布：前台可见。
   - 草稿：前台不可见。
   - 下线：前台不可见。
6. 保存后，到游客端首页或会员中心验证展示效果。

演示重点：首页公告和会员中心消息通知读取的是同一套后端公告数据，不是前端写死的本地静态内容。

### 6. 游客端领取优惠券

1. 使用游客账号登录。
2. 进入首页或会员中心。
3. 查看可领取优惠券。
4. 点击领取。
5. 进入会员中心的“我的优惠券”，确认优惠券已经到账。

如果再次领取同一张券，系统会阻止重复领取。

### 7. 游客端门票预约

1. 使用游客账号登录。
2. 进入门票预约页面。
3. 选择预约日期、场次、票种和数量。
4. 如果当前用户有可用门票券，页面会展示可选择优惠券。
5. 选择优惠券并提交订单。
6. 进入我的订单，找到待支付订单。
7. 点击模拟支付。
8. 支付成功后，订单可以生成入园凭证。

演示重点：门票订单只能使用 `TICKET` 范围的优惠券。未达门槛、已过期、已使用或不属于当前用户的券都不能抵扣。

### 8. 游客端活动报名和活动订单

1. 使用游客账号登录。
2. 进入活动页面。
3. 选择一个免费活动，点击报名，系统直接生成报名记录。
4. 选择一个收费活动，点击报名，系统进入活动订单确认流程。
5. 如果用户已领取匹配的活动券，确认页会展示可用优惠券。
6. 选择优惠券后提交订单。
7. 在我的订单中支付该活动订单。
8. 支付成功后，系统自动生成活动报名记录。

演示重点：收费活动的报名不是点击后立即成功，而是支付成功后才生效。夜游活动应使用 `ACTIVITY_NIGHT` 券，亲子活动应使用 `ACTIVITY_PARENT_CHILD` 券。

### 9. 游客端我的订单和会员中心

我的订单用于查看门票订单和活动订单。

1. 进入我的订单。
2. 查看待支付、已支付、已取消等订单状态。
3. 待支付订单可以支付或取消。
4. 已支付门票订单可以查看入园凭证。
5. 已支付活动订单会对应活动报名成功。

会员中心用于查看个人信息、我的优惠券和消息通知。

1. 进入会员中心。
2. 查看我的优惠券，确认领取和使用状态。
3. 查看消息通知，确认后台发布的公告是否同步展示。

### 10. 核销端凭证核销

1. 使用 `checker / checker123` 登录核销端。
2. 获取游客已支付门票订单的入园凭证。
3. 通过扫码或手动输入凭证码进行核销。
4. 核销成功后，凭证和订单状态会回写。
5. 重复核销时，系统会拦截。

演示重点：核销是门票预约闭环的最后一步，流程为预约下单、支付、生成凭证、核销入园。

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
