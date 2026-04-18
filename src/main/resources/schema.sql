CREATE TABLE IF NOT EXISTS `user` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL,
    `username` VARCHAR(50) NOT NULL UNIQUE,
    `password` VARCHAR(255) NOT NULL,
    `email` VARCHAR(100),
    `role` VARCHAR(20) DEFAULT 'USER',
    `age` INT,
    `phone` VARCHAR(25),
    `avatar` VARCHAR(255),
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    `version` INT DEFAULT 1
);

-- 权限表
CREATE TABLE IF NOT EXISTS `permission` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `permission_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '权限代码，如 user:upload',
    `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
    `description` VARCHAR(255) COMMENT '权限描述',
    `module` VARCHAR(50) COMMENT '所属模块',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_module (`module`)
) COMMENT='权限表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS `role_permission` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
    `role` VARCHAR(20) NOT NULL COMMENT '角色：USER, ADMIN',
    `permission_id` BIGINT NOT NULL COMMENT '权限ID',
    `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_role_permission (`role`, `permission_id`),
    INDEX idx_role (`role`),
    FOREIGN KEY (`permission_id`) REFERENCES `permission`(`id`) ON DELETE CASCADE
) COMMENT='角色权限关联表';

-- 初始化权限数据
INSERT INTO `permission` (`permission_code`, `permission_name`, `description`, `module`) VALUES
('user:upload', '文件上传', '允许上传文件', 'file'),
('user:delete', '文件删除', '允许删除文件', 'file'),
('user:view', '文件查看', '允许查看文件', 'file')
ON DUPLICATE KEY UPDATE `update_time` = CURRENT_TIMESTAMP;

-- 初始化角色权限关联
-- ADMIN 角色拥有所有权限
INSERT INTO `role_permission` (`role`, `permission_id`)
SELECT 'ADMIN', `id` FROM `permission`
ON DUPLICATE KEY UPDATE `role_permission`.`create_time` = CURRENT_TIMESTAMP;

-- USER 角色只有上传和查看权限
INSERT INTO `role_permission` (`role`, `permission_id`)
SELECT 'USER', `id` FROM `permission` WHERE `permission_code` IN ('user:upload', 'user:view')
ON DUPLICATE KEY UPDATE `role_permission`.`create_time` = CURRENT_TIMESTAMP;