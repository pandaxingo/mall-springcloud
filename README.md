# mall-springcloud

一个基于 Spring Cloud 的商城微服务项目，包含商品、用户、认证、购物车、订单、搜索、文件上传、网关、注册中心以及 Vue 前端页面。项目适合用于学习 Spring Cloud 微服务拆分、服务注册与发现、网关路由、JWT 登录认证、MyBatis-Plus 数据访问、RabbitMQ 消息通信、Redis 缓存和 Elasticsearch 商品搜索等场景。

## 项目功能

- 商品管理：品牌、分类、规格、SKU、库存等基础商品数据
- 用户中心：用户注册、登录相关能力
- 认证中心：JWT 令牌签发与校验
- 购物车：基于 Redis 的购物车数据处理
- 订单中心：订单、收货地址、秒杀库存、支付回调等业务
- 搜索服务：基于 Elasticsearch 的商品检索
- 文件上传：本地文件上传，并预留 FastDFS、OSS 相关依赖
- API 网关：统一入口、路由转发、跨域配置、登录过滤
- 前端页面：Vue 2 + Element UI 商城前端
- 接口文档：后端服务集成 Swagger UI

## 技术栈

### 后端

- Java 8
- Spring Boot 2.2.5.RELEASE
- Spring Cloud Hoxton.SR3
- Spring Cloud Alibaba 2.2.0.RELEASE
- Spring Cloud Gateway
- Netflix Eureka
- OpenFeign
- MyBatis / MyBatis-Plus
- MySQL
- Redis
- RabbitMQ
- Elasticsearch
- Swagger 2
- Lombok

### 前端

- Vue 2.6
- Vue CLI 4
- Vue Router
- Vuex
- Element UI
- Axios
- Swiper

## 项目结构

```text
mall-springcloud
+-- cloud-registry-server        # Eureka 注册中心，端口 10086
+-- cloud-gateway-server         # Spring Cloud Gateway 网关，端口 10010
+-- cloud-server-common          # 公共工具、统一响应、分页等通用代码
+-- cloud-web-server             # 商品服务聚合模块
|   +-- cloud-bs-interface       # 商品服务接口/公共模型
|   +-- cloud-bs-service         # 商品服务，端口 8081
+-- cloud-server-fileload        # 文件上传服务，端口 8082
+-- cloud-server-search          # 商品搜索服务，端口 8083
+-- cloud-server-user            # 用户服务聚合模块
|   +-- cloud-user-interface     # 用户服务接口/公共模型
|   +-- cloud-user-service       # 用户服务，端口 8085
+-- cloud-server-auth            # 认证服务聚合模块
|   +-- cloud-auth-common        # 认证公共模块
|   +-- cloud-auth-service       # 认证服务，端口 8087
+-- cloud-server-cart            # 购物车服务，端口 8088
+-- cloud-server-order           # 订单服务聚合模块
|   +-- cloud-order-interface    # 订单服务接口/公共模型
|   +-- cloud-order-service      # 订单服务，端口 8089
+-- cloud-doc-front
|   +-- bs-webpc                 # Vue 前端项目，开发端口 9002
|   +-- mysql/bs-wxw.sql         # 数据库初始化脚本
+-- brand-images                 # 品牌图片本地目录
+-- sku-images                   # SKU 图片本地目录
+-- upload-images                # 上传图片本地目录
```

## 环境要求

- JDK 8
- Maven 3.x
- Node.js / npm
- MySQL 5.7+ 或 8.x
- Redis
- RabbitMQ
- Elasticsearch

默认配置中使用了以下本地服务：

| 服务 | 默认配置 |
| --- | --- |
| MySQL | `localhost:3306`，数据库 `bs-wxw` |
| Redis | `localhost:6379` |
| RabbitMQ | `localhost`，虚拟主机 `/wxw` |
| Elasticsearch | `localhost:9300`，集群名 `elasticsearch` |
| Eureka | `http://bs.wxw.com:10086/eureka` |

> 注意：部分配置文件中写有开发机绝对路径、数据库密码、Redis 密码和支付沙箱配置。运行前请根据自己的本机环境修改对应 `application.yml`，生产环境不要直接使用仓库中的示例密钥或口令。

## 本地启动

### 1. 配置本地域名

服务注册中心和网关配置默认使用 `bs.wxw.com`，本地运行时可以在 hosts 文件中添加：

```text
127.0.0.1 bs.wxw.com
```

也可以直接把各模块 `application.yml` 中的 Eureka 地址改为 `http://localhost:10086/eureka`。

### 2. 初始化数据库

创建数据库：

```sql
CREATE DATABASE `bs-wxw` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

导入脚本：

```bash
mysql -uroot -p bs-wxw < cloud-doc-front/mysql/bs-wxw.sql
```

如果你的 MySQL 用户名、密码或端口不同，请同步修改以下模块中的 `spring.datasource` 配置：

- `cloud-web-server/cloud-bs-service`
- `cloud-server-user/cloud-user-service`
- `cloud-server-search`
- `cloud-server-order/cloud-order-service`

### 3. 启动基础依赖

请先启动：

- MySQL
- Redis
- RabbitMQ，并创建虚拟主机 `/wxw`
- Elasticsearch

RabbitMQ 虚拟主机可在管理后台创建，也可使用命令：

```bash
rabbitmqctl add_vhost /wxw
rabbitmqctl set_permissions -p /wxw guest ".*" ".*" ".*"
```

### 4. 编译后端

在项目根目录执行：

```bash
mvn clean install
```

如果只想跳过测试编译：

```bash
mvn clean install -DskipTests
```

### 5. 按顺序启动后端服务

建议按下面顺序启动：

1. `cloud-registry-server`：`RegistryMain10086`
2. `cloud-gateway-server`：`GatewayMain10010`
3. `cloud-web-server/cloud-bs-service`：`BS2020Main`
4. `cloud-server-user/cloud-user-service`：`UserMain8085`
5. `cloud-server-auth/cloud-auth-service`：`AuthMain8087`
6. `cloud-server-fileload`：`FileLoadMiain8082`
7. `cloud-server-search`：`SearchMain8003`
8. `cloud-server-cart`：`CartMain8088`
9. `cloud-server-order/cloud-order-service`：`OrderMian8089`

也可以使用 Maven 启动单个模块，例如：

```bash
mvn -pl cloud-registry-server spring-boot:run
mvn -pl cloud-gateway-server spring-boot:run
mvn -pl cloud-web-server/cloud-bs-service spring-boot:run
```

启动成功后可访问：

- Eureka 控制台：`http://bs.wxw.com:10086`
- 网关入口：`http://bs.wxw.com:10010`
- Swagger UI：`http://bs.wxw.com:10010/swagger-ui.html`

### 6. 启动前端

进入前端目录：

```bash
cd cloud-doc-front/bs-webpc
npm install
npm run serve
```

开发环境默认端口为：

```text
http://localhost:9002
```

构建生产包：

```bash
npm run build
```

## 网关路由

网关统一监听 `10010` 端口，主要路由如下：

| 路径 | 目标服务 |
| --- | --- |
| `/api/item/**` | 商品服务 `cloud-bs-service` |
| `/api/upload/**` | 文件上传服务 `cloud-server-fileload` |
| `/api/search/**` | 搜索服务 `cloud-bs-search` |
| `/api/user/**` | 用户服务 `cloud-user-service` |
| `/api/auth/**` | 认证服务 `cloud-auth-service` |
| `/api/cart/**` | 购物车服务 `cloud-server-cart` |
| `/api/order/**` | 订单服务 `cloud-order-service` |

## 开发提示

- RSA 公钥和私钥路径在认证、网关、购物车、订单等模块中有配置，迁移项目目录后需要同步调整。
- 图片上传默认使用本地目录，相关路径在 `app.upload`、`app.brand-image`、`app.sku-image` 下配置。
- 搜索服务依赖 Elasticsearch 和 RabbitMQ，商品变更消息会用于同步搜索索引。
- 前端已有 `package-lock.json`，推荐优先使用 `npm install` 保持依赖版本一致。
- 如果接口访问失败，先检查 Eureka 控制台中对应服务是否注册成功。

## 许可证

当前仓库未声明开源许可证。如需公开发布或供他人复用，建议补充明确的 License 文件。
