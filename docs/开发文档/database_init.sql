-- ================================================
-- 招投标管理系统 数据库初始化脚本
-- 数据库：douge_bid
-- ================================================

CREATE DATABASE IF NOT EXISTS douge_bid DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE douge_bid;

-- ----------------------------
-- 1. 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    username VARCHAR(100) NOT NULL UNIQUE COMMENT '用户名（邮箱）',
    password VARCHAR(64) NOT NULL COMMENT 'MD5密码',
    role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色：user/admin',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 2. 项目表
-- ----------------------------
CREATE TABLE IF NOT EXISTS projects (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    project_name VARCHAR(255) COMMENT '项目名称',
    project_code VARCHAR(50) COMMENT '项目编号',
    bidding_agency VARCHAR(100) COMMENT '招标代理机构',
    client_unit VARCHAR(100) COMMENT '发标单位（采购人）',
    bid_open_time DATETIME COMMENT '开标时间',
    complaint_deadline DATETIME COMMENT '质疑截止时间',
    ceiling_price DECIMAL(15,2) COMMENT '拦标价（最高投标限价）',
    floor_price DECIMAL(15,2) COMMENT '下限价（投标成本警戒线）',
    contract_payment VARCHAR(300) COMMENT '合同付款方式',
    expert_composition VARCHAR(200) COMMENT '评审专家组成',
    price_score_method VARCHAR(300) COMMENT '价格分计算方法',
    subjective_score_details VARCHAR(300) COMMENT '主观分评审细则',
    bid_bond DECIMAL(15,2) COMMENT '投标保证金',
    performance_bond DECIMAL(15,4) COMMENT '履约保证金（比例或金额）',
    file_path VARCHAR(500) COMMENT '原始文件存储路径',
    file_original_name VARCHAR(255) COMMENT '原始文件名',
    uploader_id BIGINT COMMENT 'FK->users.id，上传人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_project_name (project_name),
    INDEX idx_uploader (uploader_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目表';

-- ----------------------------
-- 3. 初始化管理员账号
-- 用户名：admin@bid.com
-- 密码：admin123（MD5：e10adc3949ba59abbe56e057f20f883e）
-- ----------------------------
INSERT INTO users (username, password, role) VALUES
('admin@bid.com', 'e10adc3949ba59abbe56e057f20f883e', 'admin');