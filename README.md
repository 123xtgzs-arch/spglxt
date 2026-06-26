# 次元方舟视频管理系统 (Java 原生态版)

基于 **Java 原生态（Spring Boot 3 + JPA）** 开发的视频管理系统，对接苹果CMS (MacCMS V10) 数据库。

> **架构说明：** 与 TW 视频系统 `ysname` 相同——Spring Boot 编译为 **GraalVM 原生 Linux 程序** `spglxt`，宝塔以 **Go 项目** 方式守护进程；**服务器无需安装 Java**。

这是原 Python Flask 版本的 Java 重构版本，提供企业级的架构和性能。

## 🏗️ 生产部署架构（与 ysname 相同）

```
宝塔 Go 项目
        │
        ▼
   spglxt（GraalVM 原生 Linux 程序，~90MB）
        │  读取 config/.env、config/*.json
        ▼
   Spring Boot 视频管理系统（定时采集、API、页面）
```

**发布包结构（与 ysname 一致）：**

```
spglxt/
├── spglxt          # Linux 原生可执行文件，服务器不需要 Java
└── config/         # .env、collect_sites.json 等
```

| 组件 | 说明 |
|------|------|
| **Java 原生态** | Spring Boot 3.0.6 源码，Maven 构建 |
| **GraalVM 原生镜像** | `-Pnative` 编译为单个 `spglxt` 二进制 |
| **宝塔 Go 项目** | 启动文件填 `spglxt`，端口 `8080` |
| **服务器要求** | MySQL、Redis（**不需要** JDK / Go） |

### 打包方式

运行 **`build-release.bat`** 选择：

| 选项 | 说明 | 适用场景 |
|------|------|----------|
| **1** | Windows 本地 GraalVM → `spglxt.exe` | 本机测试，不能上传 Linux |
| **2** | Go 启动器（内嵌 JAR） | 服务器需 JDK 17，备选方案 |
| **3** | Docker 编译 Linux `spglxt` | Windows 有 Docker 时推荐 |

**无 Docker 的 Windows 环境：** 将整个项目上传到宝塔，SSH 执行 `deploy/build-native-linux.sh` 在服务器上编译。

产物：`release/spglxt/` + **`spglxt.zip`** → 上传宝塔解压。

详细步骤见 [deploy/宝塔部署说明.md](deploy/宝塔部署说明.md)

## 🎯 技术栈

### 后端框架
- **Spring Boot**: 3.0.6
- **Spring Data JPA**: 数据持久化
- **Spring Security**: 安全认证
- **Spring Session**: 会话管理
- **MySQL**: 数据库
- **Redis**: 缓存和会话存储
- **JWT**: 令牌认证
- **Lombok**: 简化代码
- **Hutool**: 工具类库

### 前端
- HTML5 + CSS3 + JavaScript
- Thymeleaf 模板引擎

### 构建工具
- Maven 3.8+

## 📋 功能特性

- ✅ **视频管理** - CRUD操作、批量处理、多条件筛选
- ✅ **分类管理** - 树形结构、无限级分类
- ✅ **JWT认证** - 无状态Token认证
- ✅ **统计分析** - 总数、今日新增、待审核统计
- ✅ **API接口** - RESTful API设计
- ✅ **分页查询** - 高效的数据分页
- ✅ **Redis缓存** - 提升系统性能
- ✅ **日志记录** - 完整的操作日志

## 🛠️ 环境要求

### 必需
- **JDK**: 17 或更高版本
- **Maven**: 3.8+
- **MySQL**: 5.7+
- **Redis**: 5.0+
- **苹果CMS**: V10 数据库

### 推荐
- IDE: IntelliJ IDEA / Eclipse
- 操作系统: Windows / Linux / macOS

## 🚀 快速开始

### 1. 克隆项目

```bash
cd java_project
```

### 2. 配置数据库

编辑 `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/your_database?useUnicode=true&characterEncoding=utf8mb4
    username: your_username
    password: your_password
```

### 3. 配置Redis

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password:  # 如果有密码,填写这里
```

### 4. 修改管理员账户

```yaml
app:
  admin:
    username: admin
    password: admin123
```

### 5. 构建项目

```bash
mvn clean package
```

### 6. 运行应用

**方式一: Maven命令**
```bash
mvn spring-boot:run
```

**方式二: JAR包**
```bash
java -jar target/spglxt-video-system.jar
```

**方式三: IDE运行**
直接运行 `VideoSystemApplication.java` 主类

### 7. 访问系统

- 系统地址: http://localhost:8080
- 默认账号: admin
- 默认密码: admin123

## 📁 项目结构

```
java_project/
├── src/main/java/com/spglxt/
│   ├── VideoSystemApplication.java    # 主应用类
│   ├── common/                        # 通用类
│   │   └── Result.java                # 统一返回结果
│   ├── config/                        # 配置类
│   │   ├── JwtProperties.java         # JWT配置
│   │   └── SecurityConfig.java        # Security配置
│   ├── controller/                    # 控制器层
│   │   ├── AuthController.java        # 认证控制器
│   │   ├── VideoController.java       # 视频控制器
│   │   └── TypeController.java        # 分类控制器
│   ├── entity/                        # 实体类
│   │   ├── Video.java                 # 视频实体
│   │   └── Type.java                  # 分类实体
│   ├── repository/                    # 数据访问层
│   │   ├── VideoRepository.java       # 视频Repository
│   │   └── TypeRepository.java        # 分类Repository
│   ├── service/                       # 服务层
│   │   ├── VideoService.java          # 视频服务
│   │   └── TypeService.java           # 分类服务
│   └── security/                      # 安全相关
│       ├── JwtUtil.java               # JWT工具类
│       ├── JwtAuthenticationFilter.java  # JWT过滤器
│       └── CustomUserDetailsService.java # 用户详情服务
├── src/main/resources/
│   ├── application.yml                # 主配置文件
│   ├── application-dev.yml            # 开发环境配置
│   ├── application-prod.yml           # 生产环境配置
│   ├── static/                        # 静态资源
│   └── templates/                     # Thymeleaf模板
├── pom.xml                            # Maven配置
└── README.md                          # 项目说明
```

## 🔌 API接口

### 认证接口

| 接口 | 方法 | 说明 |
|-----|------|-----|
| `/api/auth/login` | POST | 用户登录 |
| `/api/auth/logout` | POST | 用户登出 |
| `/api/auth/refresh` | POST | 刷新令牌 |
| `/api/auth/user` | GET | 获取当前用户 |

### 视频接口

| 接口 | 方法 | 说明 |
|-----|------|-----|
| `/api/video/list` | GET | 视频列表(分页) |
| `/api/video/{id}` | GET | 视频详情 |
| `/api/video` | POST | 添加视频 |
| `/api/video/{id}` | PUT | 更新视频 |
| `/api/video/{id}` | DELETE | 删除视频 |
| `/api/video/batch` | DELETE | 批量删除 |
| `/api/video/batch/audit` | PUT | 批量审核 |
| `/api/video/stats` | GET | 统计数据 |

### 分类接口

| 接口 | 方法 | 说明 |
|-----|------|-----|
| `/api/type/tree` | GET | 分类树 |
| `/api/type/list` | GET | 分类列表 |
| `/api/type/{id}` | GET | 分类详情 |
| `/api/type` | POST | 添加分类 |
| `/api/type/{id}` | PUT | 更新分类 |
| `/api/type/{id}` | DELETE | 删除分类 |
| `/api/type/{id}/count` | GET | 统计视频数 |

### API使用示例

**1. 登录获取Token**

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```

返回:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "username": "admin"
  }
}
```

**2. 查询视频列表**

```bash
curl -X GET "http://localhost:8080/api/video/list?page=1&pageSize=20" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

**3. 获取分类树**

```bash
curl -X GET "http://localhost:8080/api/type/tree" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## 🏭 生产部署

与 TW `ysname` 相同：**GraalVM 原生 `spglxt` + `config/`**，宝塔 Go 项目启动，**服务器无需 Java**。

| 方式 | 命令 / 操作 |
|------|-------------|
| Windows + Docker | `build-release.bat` → 选 2 |
| GitHub Actions | 推送代码 → Actions → 下载 `spglxt.zip` |
| 宝塔 SSH 编译 | `./deploy/build-native-linux.sh` |

详细步骤见 [deploy/宝塔部署说明.md](deploy/宝塔部署说明.md)

开发调试仍可用 JAR：

```bash
mvn clean package -DskipTests
java -jar target/spglxt-video-system.jar
```

### Nginx 反向代理（可选）

```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## ⚙️ 配置说明

### application.yml 关键配置

```yaml
# 服务器端口
server:
  port: 8080

# 数据库配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/database
    username: root
    password: password

# Redis配置
  data:
    redis:
      host: localhost
      port: 6379

# JWT配置
app:
  jwt:
    secret: your-secret-key
    expiration: 604800000  # 7天
    header: Authorization
    token-prefix: Bearer 

# 管理员配置
  admin:
    username: admin
    password: admin123
```

## 🔧 开发说明

### 添加新功能

1. **实体层**: 在 `entity` 包中创建实体类
2. **数据层**: 在 `repository` 包中创建Repository接口
3. **服务层**: 在 `service` 包中创建Service类
4. **控制器**: 在 `controller` 包中创建Controller类

### 代码规范

- 使用Lombok简化代码
- 遵循RESTful API设计规范
- 统一使用Result类返回结果
- 异常统一处理
- 添加必要的注释

## 🆚 与Python版本对比

| 特性 | Python版本 | Java版本 |
|-----|-----------|---------|
| 框架 | Flask | Spring Boot |
| ORM | SQLAlchemy | Spring Data JPA |
| 认证 | Session | JWT |
| 性能 | 中等 | 优秀 |
| 部署 | Gunicorn | 独立JAR |
| 类型安全 | 弱类型 | 强类型 |
| 企业级特性 | 较少 | 完善 |

## 📝 常见问题

### 1. 数据库连接失败
检查 `application.yml` 中的数据库配置是否正确。

### 2. Redis连接失败
确保Redis服务已启动并配置正确。

### 3. JWT令牌无效
检查 `app.jwt.secret` 配置,确保前后端使用相同的密钥。

### 4. 跨域问题
在 `SecurityConfig` 中已配置CORS,如仍有问题,检查前端请求头。

## 📄 许可证

MIT License

## 👥 作者

原Python版本转Java Spring Boot版本

---

## 🔗 相关链接

- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [JWT](https://jwt.io/)
- [苹果CMS官网](https://www.maccms.com/)

---

如有问题,欢迎提Issue!
