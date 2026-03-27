# 招投标管理系统

[![GitHub Repo](https://img.shields.io/badge/GitHub-bid--system-blue.svg)](https://github.com/ga800118-llx/bid-system)

## 项目简介

招投标信息管理系统，支持招标需求文档上传、AI 自动提取关键信息（项目名称、编号、开标时间、拦标价等）。

## 技术栈

**后端：** Java Spring Boot 3.2 + MyBatis Plus + JWT + PDFBox + POI  
**前端：** Vue3 + Vite + Element Plus + Axios  
**数据库：** MySQL（douge_bid）

## 快速启动

### 后端

\\\ash
cd backend
mvn spring-boot:run
\\\

### 前端

\\\ash
cd frontend
npm install
npm run dev
\\\

## 文档规范

所有项目文档统一存放在 docs/ 目录：

`
docs/
├── README.md                       # 文档总览
├── 需求文档/                        # 甲方需求、需求确认书、原始文件
│   └── 01_上传原始文件/             # 原始 PDF/Word（只读）
├── 开发文档/                        # 技术方案、接口设计、数据库设计
├── 测试文档/                        # 测试用例、测试报告
└── 部署文档/                       # 安装部署、运维手册
`

**命名规范：** 功能模块_日期_版本.md（如 登录模块_20260327_v1.md）

## Git 提交规范

- eat: 新功能
- ix: 修复 Bug
- docs: 文档变更
- efactor: 重构
- 	est: 测试相关

## 远程仓库

https://github.com/ga800118-llx/bid-system