# 贵州红色文化旅游景点信息管理系统

> 基于 SpringBoot 的全栈旅游景点信息管理平台，支持游客端与管理端双入口，具备景点浏览、旅游线路、红色文化传播、酒店美食预订、多语言、地图展示、审核工作流等完整业务功能。

---

## 1. 快速启动

```bash
docker compose up --build -d
```

启动后等待约 20-30 秒，待后端服务完成数据库初始化，即可访问：

| 入口 | 地址 |
|------|------|
| 🌐 游客端 | http://localhost:8083 |
| 🔧 管理端 | http://localhost:8084 |
| 🔌 后端 API | http://localhost:8089/api/ |
| 🗄️ 数据库 | localhost:8088（root / root123） |

---

## 2. 测试账号

| 角色 | 用户名 | 密码 | 登录入口 |
|------|--------|------|---------|
| 管理员 | admin | admin123 | http://localhost:8084 |
| 工作人员 | staff1 | 123456 | http://localhost:8084 |
| 工作人员 | staff2 | 123456 | http://localhost:8084 |
| 游客 | zhangsan | 123456 | http://localhost:8083 |
| 游客 | lisi | 123456 | http://localhost:8083 |

---

## 3. 功能模块

### 游客端（http://localhost:8083）— 21 个页面

| 页面 | 核心功能 |
|------|---------|
| 首页 `index.html` | 景点轮播图、热门景点卡片、推荐线路、红色文化快速入口 |
| 景点列表 `spots.html` | 分页展示、地区/主题/状态多条件筛选、关键词搜索、距离/人气/好评排序 |
| 景点详情 `spot-detail.html` | 图片轮播、基础信息、红色文化解读（历史背景/革命事件/人物故事）、Leaflet 嵌入式地图、相关景点推荐、用户评论、预订门票、问题反馈跳转、**信息更正提交** |
| 线路列表 `routes.html` | 天数（1-7天）/主题分类筛选、收藏 |
| 线路详情 `route-detail.html` | 行程安排、景点地图标注（OpenStreetMap）、交通住宿预算建议、预订、**导出/打印** |
| 红色文化列表 `culture.html` | 分类浏览、关键词搜索、点赞、收藏 |
| 红色文化详情 `culture-detail.html` | 富文本内容、点赞、收藏、分享 |
| 酒店列表/详情 `hotels.html` / `hotel-detail.html` | 酒店信息（早餐/客房服务）、日期房间选择、预订、模拟支付 |
| 美食列表/详情 `foods.html` / `food-detail.html` | 美食分类、门店信息（位置/卫生/品类）、购买、模拟支付 |
| 登录/注册/找回密码 | 账号密码登录（记住我）、手机号注册、旧密码验证找回 |
| 个人中心 `profile.html` | 修改昵称/手机号/头像、修改密码 |
| 我的订单 `orders.html` | 订单列表（按状态筛选）、立即支付、取消、申请退款；**酒店订单完整状态流转**（待确认/已确认/入住中/已结束）、**免费改期/按新间夜重算房价改期**、状态流转时间线（变更时间+触发人） |
| 我的收藏 `favorites.html` | 景点/线路/文化/酒店收藏管理 |
| 消息通知 `messages.html` | 系统消息列表（审核结果/评论回复/反馈回复自动推送） |
| 我的线路 `my-routes.html` | 创建/编辑/删除自定义线路，拖拽添加景点，提交申请官方推荐，查看审核状态与驳回原因 |
| 问题反馈 `feedback.html` | 提交 Bug/建议/投诉（支持从景点详情预填信息），查看历史反馈及管理员回复 |
| 客服中心 `faq.html` | 智能客服（FAQ关键词匹配自动回复）+ **人工客服**（一键切换，5秒轮询实时显示管理员回复） |
| 多语言 | 中/英/日三语切换（导航栏 select，后端 API 支持 `lang=zh/en/ja` 参数） |

### 管理端（http://localhost:8084）— 16 个页面

| 页面 | 核心功能 |
|------|---------|
| 仪表盘 `index.html` | 用户数/景点数/订单数/收入总览，快捷入口卡片 |
| 用户管理 `users.html` | 列表（角色/状态/关键词筛选）、新增/编辑、禁用/启用、重置密码、删除、**CSV 导出** |
| 景点管理 `spots.html` | 景点 CRUD、图片上传管理、开放状态切换、访问量/收藏量/评分统计 |
| 线路管理 `routes.html` | 推荐线路 CRUD、行程景点关联与排序、封面图上传 |
| 文化管理 `culture.html` | 文化内容 CRUD、分类管理、**Quill 富文本编辑器**（支持图片/视频/表格插入） |
| 酒店管理 `hotels.html` | 酒店 CRUD、早餐/客房服务配置 |
| 美食管理 `foods.html` | 美食 CRUD、门店 CRUD |
| 留言管理 `comments.html` | 查看所有评论、回复留言（自动发送通知）、删除违规评论 |
| 订单管理 `orders.html` | 订单列表（类型/状态筛选）、完成/取消/退款/删除，显示具体支付方式，**酒店订单确认（待确认→已确认）**、入住/离店日期与改期次数展示 |
| 订单详情 `order-detail.html` | **后台订单详情页**：订单全字段、各状态变更时间点、**状态流转记录时间线（每步变更时间+触发人）**、确认/取消/退款/完成等操作入口 |
| FAQ 管理 `faqs.html` | 常见问题 CRUD（供智能客服匹配使用） |
| 问题反馈 `feedbacks.html` | 查看用户反馈、回复（自动消息通知）、状态流转（待处理→处理中→已解决）、删除 |
| 线路审核 `route-review.html` | 审核用户申请推荐的自定义线路，通过后纳入官方推荐，驳回填写原因，操作自动通知用户 |
| **景点审核** `spot-review.html` | 审核用户提交的景点信息更正建议，通过后自动更新景点字段，驳回附理由，操作自动通知用户 |
| **权限配置** `permissions.html` | 角色 CRUD（新增/修改/删除角色）、菜单权限精细化分配（启用/禁用各角色对菜单/按钮的访问） |
| **人工客服** `customer-service.html` | 左侧用户会话列表（显示最新消息预览），右侧对话面板，管理员实时回复用户咨询 |
| 管理员登录 `login.html` | 独立管理员身份验证 |

---

## 4. API 端点速查

### 认证
| 端点 | 说明 |
|------|------|
| `GET /api/auth/login?username=&password=` | 登录（支持 remember-me） |
| `GET /api/auth/register?username=&password=&phone=` | 注册 |
| `GET /api/auth/resetPassword?phone=&oldPassword=&newPassword=` | 密码找回 |
| `GET /api/auth/logout` | 退出登录 |
| `GET /api/auth/me` | 获取当前登录用户信息 |

### 景点
| 端点 | 说明 |
|------|------|
| `GET /api/spot/list?page=&size=&region=&theme=&status=&keyword=&orderBy=&lang=` | 列表（lang: zh/en/ja，orderBy: viewCount/rating/favoriteCount/commentCount） |
| `GET /api/spot/detail?id=&lang=` | 详情 |
| `GET /api/spot/carousel` | 首页轮播图 |
| `GET /api/spot/search?keyword=&orderBy=&lang=` | 模糊搜索 |
| `GET /api/spot/related?id=&region=&theme=` | 相关景点推荐 |

### 线路
| 端点 | 说明 |
|------|------|
| `GET /api/route/list?page=&size=&days=&theme=&lang=` | 推荐线路列表 |
| `GET /api/route/detail?id=&lang=` | 线路详情（含行程景点列表） |

### 红色文化
| 端点 | 说明 |
|------|------|
| `GET /api/culture/list?page=&size=&categoryId=&keyword=&lang=` | 文化内容列表 |
| `GET /api/culture/detail?id=&lang=` | 文化内容详情 |
| `GET /api/culture/categories` | 分类列表 |

### 酒店 / 美食
| 端点 | 说明 |
|------|------|
| `GET /api/hotel/list?page=&size=&keyword=&orderBy=` | 酒店列表 |
| `GET /api/hotel/detail?id=` | 酒店详情 |
| `GET /api/food/list?page=&size=&category=&keyword=` | 美食列表 |
| `GET /api/food/detail?id=` | 美食详情（含门店信息） |

### 用户交互
| 端点 | 说明 |
|------|------|
| `GET /api/order/create?orderType=&targetId=&targetName=&amount=&quantity=&checkInDate=&checkOutDate=` | 创建订单（酒店订单必须传入住/离店日期，提交后为待确认） |
| `GET /api/order/pay?orderId=&payMethod=` | 模拟支付（payMethod: WECHAT/BANK_ICBC/BANK_CCB/BANK_ABC/BANK_BOC/BANK_BOCOM/BANK_CMB/BANK_PSBC；仅普通订单） |
| `GET /api/order/cancel?orderId=` | 取消订单（酒店订单待确认可直接取消、已确认仅入住日前可取消） |
| `GET /api/order/reschedule?orderId=&newCheckInDate=&newCheckOutDate=` | 酒店订单改期（可改范围服务端判定；待确认免费、已确认按新间夜重算房价；入住日并发只接受最早一次） |
| `GET /api/order/rescheduleInfo?orderId=` | 查询改期资格（canReschedule/reason/unitPrice/estimatedAmount 等） |
| `GET /api/order/statusLogs?orderId=` | 查询订单状态流转记录（变更时间、触发人、备注） |
| `GET /api/order/refund?orderId=` | 申请退款（普通订单） |
| `GET /api/order/myList?page=&size=&orderType=&status=` | 我的订单列表（status 支持逗号分隔多状态；查询前自动推进到期酒店订单） |
| `GET /api/order/detail?id=` | 我的订单详情（打开时惰性推进入住中/已结束） |
| `GET /api/favorite/add?targetType=&targetId=` | 收藏/取消收藏 |
| `GET /api/favorite/list?targetType=` | 收藏列表 |
| `GET /api/like/add?targetType=&targetId=` | 点赞/取消点赞 |
| `GET /api/comment/add?targetType=&targetId=&content=&rating=` | 发表评论 |
| `GET /api/comment/list?targetType=&targetId=&page=&size=` | 评论列表 |
| `GET /api/message/list?page=&size=` | 消息通知列表 |
| `GET /api/message/unreadCount` | 未读消息数 |

### 自定义线路
| 端点 | 说明 |
|------|------|
| `GET /api/customRoute/list` | 我的自定义线路列表 |
| `GET /api/customRoute/save?name=&description=&days=&spotData=` | 保存线路 |
| `GET /api/customRoute/delete?id=` | 删除线路 |
| `GET /api/customRoute/submitForReview?id=` | 申请官方推荐 |

### 客服 & 反馈
| 端点 | 说明 |
|------|------|
| `GET /api/faq/list` | FAQ 列表 |
| `GET /api/faq/ask?question=` | 智能客服问答 |
| `GET /api/chat/send?content=` | 向人工客服发送消息（需登录） |
| `GET /api/chat/history` | 查看与客服的对话记录 |
| `GET /api/feedback/submit?category=&title=&content=` | 提交问题反馈 |
| `GET /api/feedback/my` | 我的反馈记录 |

### 景点信息更正
| 端点 | 说明 |
|------|------|
| `GET /api/spotSuggestion/submit?spotId=&fieldName=&newValue=&reason=` | 提交更正建议（fieldName: name/description/location/openTime/ticketPrice/trafficInfo） |
| `GET /api/spotSuggestion/my` | 我的更正建议列表 |

### 管理端（需 ADMIN 或 STAFF 角色）
| 端点 | 说明 |
|------|------|
| `GET /api/admin/user/list?page=&size=&role=&status=&keyword=` | 用户列表 |
| `GET /api/admin/user/save?username=&role=&nickname=&phone=&password=` | 新增/编辑用户 |
| `GET /api/admin/user/toggleStatus?id=` | 禁用/启用用户 |
| `GET /api/admin/user/resetPassword?id=&newPassword=` | 重置密码 |
| `GET /api/admin/user/delete?id=` | 删除用户 |
| `GET /api/admin/user/export` | 导出全部用户 CSV |
| `GET /api/admin/spot/list?page=&size=` | 景点列表（STAFF 仅返回本人所属景点） |
| `GET /api/admin/spot/save?...` | 景点新增/编辑（STAFF 新增自动绑定 `staff_id`） |
| `GET /api/admin/spot/delete?id=` | 删除景点（STAFF 仅可删本人景点） |
| `GET /api/admin/spot/toggleStatus?id=` | 切换景点状态（STAFF 仅可操作本人景点） |
| `GET /api/admin/spot/addImage?spotId=&imageUrl=&sortOrder=` | 添加景点图片（STAFF 仅可操作本人景点） |
| `GET /api/admin/spot/deleteImage?imageId=` | 删除景点图片（STAFF 仅可操作本人景点） |
| `GET /api/admin/route/*` | 线路 CRUD |
| `GET /api/admin/culture/*` | 文化内容 CRUD |
| `GET /api/admin/hotel/*` | 酒店 CRUD |
| `GET /api/admin/food/*` | 美食 CRUD |
| `GET /api/admin/comment/*` | 留言管理（list/reply/delete） |
| `GET /api/admin/order/*` | 订单管理（list/detail/statusLogs/confirm/cancel/refund/complete/delete） |
| `GET /api/admin/faq/*` | FAQ CRUD |
| `GET /api/admin/feedback/*` | 反馈管理（list/reply/status/delete） |
| `GET /api/admin/customRoute/list?status=` | 用户提交的线路审核列表 |
| `GET /api/admin/customRoute/approve?id=` | 通过线路审核（纳入推荐） |
| `GET /api/admin/customRoute/reject?id=&reason=` | 驳回线路 |
| `GET /api/admin/customRoute/pendingCount` | 线路待审数量 |
| `GET /api/admin/spotSuggestion/list?status=` | 景点更正审核列表 |
| `GET /api/admin/spotSuggestion/approve?id=` | 通过景点更正（自动更新景点数据） |
| `GET /api/admin/spotSuggestion/reject?id=&reason=` | 驳回景点更正 |
| `GET /api/admin/spotSuggestion/pendingCount` | 景点更正待审数量 |
| `GET /api/admin/chat/sessions` | 客服用户会话列表 |
| `GET /api/admin/chat/history?userId=` | 查看指定用户对话记录 |
| `GET /api/admin/chat/send?userId=&content=` | 管理员发送客服消息 |
| `GET /api/admin/role/list` | 角色列表 |
| `GET /api/admin/role/save?code=&name=&description=` | 新增/编辑角色 |
| `GET /api/admin/role/delete?id=` | 删除角色 |
| `GET /api/admin/role/menu/list?roleCode=` | 角色菜单权限列表 |
| `GET /api/admin/role/menu/save?roleCode=&menuKey=&menuName=` | 新增菜单权限 |
| `GET /api/admin/role/menu/toggle?id=` | 启用/禁用菜单权限 |
| `GET /api/admin/role/menu/delete?id=` | 删除菜单权限 |
| `GET /api/admin/stats` | 系统统计数据 |

---

## 5. 角色设计说明

### 三角色架构（含工作人员隔离）

本系统当前采用 **USER（游客）**、**ADMIN（管理员）**、**STAFF（工作人员）** 三角色并行模型，其中 STAFF 专门用于满足“工作人员仅操作所属景点”的审计要求。

| 角色 | 权限边界 |
|------|---------|
| USER | 游客端浏览、收藏、评论、下单、自定义线路、问题反馈、景点信息更正、客服咨询 |
| ADMIN | 管理端全量权限，可管理全部景点与全系统业务数据 |
| STAFF | 可访问管理端景点管理能力，但仅能操作 `scenic_spot.staff_id = 当前用户ID` 的景点，越权操作统一拒绝 |

景点权限隔离的强约束已落在后端接口层（`/api/admin/spot/*`），包含列表过滤、编辑/删除/状态切换、图片新增/删除的所属校验。

---

## 6. 技术栈

| 层级 | 技术 |
|------|------|
| 后端框架 | Spring Boot 2.7.18 + Spring Security |
| ORM | MyBatis-Plus 3.5.3.1 |
| 数据库 | MySQL 8.0 |
| 运行时 | JDK 1.8（eclipse-temurin:8-jre） |
| 构建 | Maven 3.9 |
| 前端 | 原生 HTML5 + CSS3 + JavaScript（无框架） |
| 富文本 | Quill.js 1.3.6 |
| 地图 | Leaflet 1.9.4 + OpenStreetMap |
| 容器 | Docker + Docker Compose + Nginx alpine |
| 设计约束 | 密码明文存储、HttpSession 认证、全 GET 请求、CORS 全局配置 |

---

## 7. 项目结构

```
label-02051/
├── backend/                          # SpringBoot 后端（~4000 行代码）
│   ├── src/main/java/com/redtourism/
│   │   ├── config/                   # Security / CORS / MybatisPlus / WebMvc 配置
│   │   ├── common/                   # Result 统一响应、全局异常处理、常量
│   │   ├── entity/                   # 23 个实体类
│   │   ├── mapper/                   # 22 个 MyBatis-Plus Mapper
│   │   ├── service/                  # 11 个业务接口 + 实现
│   │   └── controller/               # 20 个 REST Controller（~1900 行）
│   ├── src/main/resources/
│   │   ├── schema.sql                # 建表脚本（23 张表）
│   │   ├── data.sql                  # 初始化数据（景点/线路/文化/酒店/美食/FAQ等）
│   │   └── application.yml           # 应用配置（session 30min、文件上传 10MB）
│   ├── uploads/                      # 图片资源（49 张，含景点/线路/酒店/美食封面）
│   ├── Dockerfile                    # 标准构建（含 mvn package）
│   └── Dockerfile.fast               # 快速构建（直接拷贝预编译 JAR）
├── frontend-user/                    # 游客端前端（21 个页面）
│   ├── css/style.css                 # 全局样式（CSS 变量 + 响应式）
│   ├── js/common.js                  # 公共函数（api/getUser/i18n/分页/Toast等）
│   └── *.html                        # 业务页面
├── frontend-admin/                   # 管理端前端（16 个页面）
│   ├── css/admin.css                 # 管理端样式
│   ├── js/admin-common.js            # 管理端公共函数（requireAdmin/renderSidebar等）
│   └── *.html                        # 管理页面
├── nginx/
│   └── default.conf                  # 反向代理配置（:80→游客端 :81→管理端 /api/→后端）
├── docker-compose.yml                # 三服务编排（MySQL + Backend + Nginx）
└── docs/                             # 项目文档
    ├── Requirements.md
    ├── Roadmap.md
    ├── DesignSpec.md
    ├── AuditReport.md
    └── SelfTestReport.md
```

### 数据库表清单（23 张）

| 表名 | 说明 | 初始数据 |
|------|------|---------|
| `sys_user` | 用户（USER/ADMIN/STAFF） | 6 条 |
| `sys_role` | 角色定义 | 3 条 |
| `sys_role_menu` | 角色菜单权限 | 25 条 |
| `scenic_spot` | 景点信息（含多语言字段） | 8 条 |
| `scenic_spot_image` | 景点图片 | 若干 |
| `route` | 推荐线路 | 5 条 |
| `route_spot` | 线路-景点关联 | 若干 |
| `culture_category` | 红色文化分类 | 若干 |
| `culture_content` | 红色文化内容 | 6 条 |
| `hotel` | 酒店信息 | 5 条 |
| `food` | 美食信息 | 7 条 |
| `food_store` | 美食门店 | 若干 |
| `comment` | 用户评论/留言 | 若干 |
| `favorite` | 收藏记录 | — |
| `like_record` | 点赞记录 | — |
| `order_info` | 订单信息（含酒店确认/入住/离店/取消时间、改期次数） | — |
| `order_status_log` | 订单状态流转记录（变更时间/触发人/备注） | — |
| `message` | 系统消息通知 | — |
| `faq` | 常见问题 | 8 条 |
| `feedback` | 用户问题反馈 | 若干 |
| `user_custom_route` | 用户自定义线路 | 3 条 |
| `spot_suggestion` | 景点信息更正建议 | — |
| `service_chat` | 人工客服对话记录 | — |

---

## 8. 酒店订单状态流转说明

酒店订单（`order_type=HOTEL`）采用与普通订单不同的完整预订状态机，所有状态变更均由服务端统一判定，并写入 `order_status_log`（变更时间 + 触发人 + 备注）。

```
提交订单 ──▶ PENDING_CONFIRM 待确认
                  │ 后台“确认”（ADMIN）
                  ▼
             CONFIRMED 已确认
                  │ 入住日当天（定时任务每10分钟扫描 + 列表/详情惰性推进，SYSTEM）
                  ▼
             CHECKED_IN 入住中
                  │ 到达离店日（同上，SYSTEM）
                  ▼
              FINISHED 已结束（结束记录留存）

  PENDING_CONFIRM / CONFIRMED（入住日之前）── 取消 ──▶ CANCELLED 已取消
```

**关键规则**

- **改期**：仅酒店订单、仅待确认/已确认且未跨过入住日可改。待确认阶段免费（沿用原间夜单价折算）；已确认阶段按新的间夜 × 酒店当前房价 × 房间数重算金额。新入住日不得早于今天、离店必须晚于入住，可改范围全部在服务端判定（`/api/order/rescheduleInfo` 预判、`/api/order/reschedule` 执行）。
- **入住日并发改期**：改期方法在事务内对订单行执行 `SELECT ... FOR UPDATE`，并以 `last_reschedule_date` 记录当天是否已改过；入住日当天只接受最早一次，并发的后续请求直接拒绝。
- **自动推进**：`OrderStatusScheduleTask` 每 10 分钟按 Asia/Shanghai 日期扫描 CONFIRMED/CHECKED_IN 订单；用户/后台打开列表或详情时也会惰性推进，双保险。
- **状态留痕**：提交、确认、入住、离店、取消以及每次改期都在 `order_status_log` 留存一条记录，触发人分为 USER（用户本人）/ADMIN（后台操作人）/SYSTEM（系统定时任务），用户端“状态记录”弹窗与后台订单详情页均可查看。
- 普通订单（景点/线路/美食）仍沿用 待支付 PENDING → 已支付 PAID → 已完成 COMPLETED 流程，取消/退款规则不变。

## 9. 支付说明（Mock 模式）

支付为完整模拟流程，调用 `/api/order/pay` 即立即标记为已支付，无需真实扣款。酒店预订不经过支付环节，提交后直接等待酒店确认。

支持以下支付方式（前端均有对应选项）：

| 方式代码 | 显示名称 |
|---------|---------|
| `WECHAT` | 微信支付 |
| `BANK_ICBC` | 工商银行 |
| `BANK_CCB` | 建设银行 |
| `BANK_ABC` | 农业银行 |
| `BANK_BOC` | 中国银行 |
| `BANK_BOCOM` | 交通银行 |
| `BANK_CMB` | 招商银行 |
| `BANK_PSBC` | 邮储银行 |

**真实对接方式**：替换 `OrderServiceImpl.payOrder()` 方法，在其中按 `payMethod` 路由调用对应支付 SDK（微信支付 SDK / 各银行开放平台 API），保持接口签名不变即可无缝切换。

---

## 10. 注意事项

1. **Docker 构建**：使用多阶段 `Dockerfile`，容器内自动执行 Maven 构建，无需本地预装 Java/Maven 环境，真正一键启动。首次构建因需下载 Maven 依赖耗时约 3-5 分钟，后续有层缓存构建会快很多。
2. **数据持久化**：MySQL 数据通过 Docker named volume `mysql-data` 持久化，`docker compose down` 不会丢失数据；`docker compose down -v` 会清除数据并在下次启动时重新初始化。
3. **图片上传**：支持 10MB 以内图片上传，存储于 `backend/uploads/` 目录，通过 Nginx 静态服务以 `/uploads/` 路径访问。
4. **智能客服**：基于 FAQ 表关键词相似度匹配实现自动回复，非 AI 大模型；人工客服采用前端轮询（5 秒间隔）模拟实时效果。
5. **多语言**：英文（en）和日文（ja）内容需在管理端景点/线路/文化编辑页面手动填写对应语言字段（`name_en`/`name_ja`/`description_en`/`description_ja` 等），初始化数据中已为部分景点提供英/日文示例。
6. **Session 超时**：默认 30 分钟，配置于 `application.yml`；前端每 5 分钟检测一次 session 状态，超时自动弹窗提示并跳转登录页。
