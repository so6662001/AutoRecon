# 钢铁行业数字化平台 Demo

## 快速启动

### 方式一: 一键启动(推荐)
```bash
./start-demo.sh
```

### 方式二: 分别启动

#### 后端
```bash
# 对账通 (端口8080)
cd autorecon-server && ./mvnw -pl recon-web spring-boot:run -Dspring-boot.run.profiles=demo

# 提货通 (端口8081)
cd pickup-express-server && ./mvnw -pl pe-web spring-boot:run -Dspring-boot.run.profiles=demo
```

#### 前端
```bash
cd autorecon-web && npm run dev:demo        # 端口3000
cd pickup-express-web && PORT=3001 npm run dev:demo   # 端口3001
```

## Demo账号
| 账号 | 密码 | 角色 |
|------|------|------|
| admin | admin123 | 平台管理员 |
| seller1 | 123456 | 卖方管理员(张三·XX钢铁) |
| buyer1 | 123456 | 买方管理员(李四·YY建设) |
| buyer2 | 123456 | 买方管理员(王五·ZZ贸易) |

## 系统地址
| 系统 | 前端 | API文档 | 数据库控制台 |
|------|------|---------|------------|
| 对账通 | http://localhost:3000 | http://localhost:8080/doc.html | http://localhost:8080/h2-console |
| 提货通 | http://localhost:3001 | http://localhost:8081/doc.html | http://localhost:8081/h2-console |
| Demo入口 | demo-portal/index.html | | |

## 无外部依赖
Demo模式使用H2内存数据库，无需安装MySQL/Redis/RabbitMQ。
