-- 部门与用户管理 - 数据库初始化脚本
-- 执行方式: mysql -u root -p douge_bid < backend/scripts/2026-04-09_init_org.sql

-- ----------------------------
-- 1. org_department 部门表
-- ----------------------------
CREATE TABLE IF NOT EXISTS org_department (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
  name        VARCHAR(100)  NOT NULL COMMENT '部门名称',
  parent_id   BIGINT COMMENT '父部门ID，NULL=根节点',
  sort_order  INT DEFAULT 0 COMMENT '同级排序，数字越小越靠前',
  status      TINYINT DEFAULT 1 COMMENT '1=正常 0=禁用',
  created_at  DATETIME,
  updated_at  DATETIME,
  INDEX idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ----------------------------
-- 2. org_user 用户表
-- ----------------------------
CREATE TABLE IF NOT EXISTS org_user (
  id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username        VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录账号',
  password_hash   VARCHAR(255) NOT NULL COMMENT '密码加密存储',
  real_name       VARCHAR(50) COMMENT '真实姓名',
  dept_id         BIGINT COMMENT '所属部门，NULL=未分配',
  status          TINYINT DEFAULT 1 COMMENT '1=正常 0=禁用',
  last_login_at   DATETIME COMMENT '最后登录时间',
  created_at      DATETIME,
  updated_at      DATETIME,
  INDEX idx_dept (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 3. 初始化根部门
-- ----------------------------
INSERT INTO org_department (id, name, parent_id, sort_order, status, created_at, updated_at)
VALUES (1, '我的公司', NULL, 0, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE name = name;
