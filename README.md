# 招投标管理系统

## 项目结构

```
bid-system/
├── backend/          # Spring Boot 后端
│   ├── pom.xml
│   ├── src/main/java/com/bid/system/
│   │   ├── BidSystemApplication.java   # 启动类
│   │   ├── config/                     # 配置类
│   │   ├── controller/                 # 控制器
│   │   ├── service/                     # 业务逻辑
│   │   ├── mapper/                      # 数据访问
│   │   ├── entity/                      # 实体类
│   │   ├── dto/                         # 数据传输对象
│   │   └── util/                        # 工具类
│   └── uploads/                         # 文件存储目录
└── frontend/         # Vue3 前端
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.js
        ├── App.vue
        ├── api/                         # API 调用
        ├── router/                      # 路由配置
        └── views/                       # 页面组件
```

## 运行方式

### 后端（Java 17+）

```bash
cd backend
# 确认 MySQL 中 douge_bid 数据库存在（users 和 projects 表已创建）
mvn spring-boot:run
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端访问: http://localhost:3000
后端接口: http://localhost:8080

## 数据库

使用 `douge_bid` 数据库（MySQL），已有以下表：

- `users` - 用户表
- `projects` - 项目表（14项提取字段）

## 功能说明

- 用户注册登录（JWT认证）
- 招标文件上传（PDF/Word），自动提取14项结构化信息
- 按项目名称搜索
- 项目详情查看 + 原始文件下载
- 管理用户可上传文件和删除项目
- 普通用户可搜索和查看

## 14项提取字段

1. 项目名称
2. 项目编号
3. 招标代理机构
4. 发标单位
5. 开标时间
6. 投诉质疑时间
7. 拦标价（最高投标限价）
8. 下限价（投标成本警戒线）
9. 合同付款方式
10. 专家的组成
11. 价格分的评分方式
12. 主观分分值
13. 投标保证金
14. 履约保证金
