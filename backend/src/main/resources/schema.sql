SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

CREATE DATABASE IF NOT EXISTS red_tourism DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE red_tourism;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    nickname VARCHAR(50),
    avatar VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status INT NOT NULL DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scenic_spot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    name_en VARCHAR(200),
    name_ja VARCHAR(200),
    description TEXT,
    description_en TEXT,
    description_ja TEXT,
    location VARCHAR(255),
    region VARCHAR(50),
    theme VARCHAR(50),
    open_time VARCHAR(100),
    ticket_price DECIMAL(10,2),
    traffic_info TEXT,
    history_background TEXT,
    revolution_event TEXT,
    person_story TEXT,
    cover_image VARCHAR(255),
    status INT DEFAULT 1,
    view_count BIGINT DEFAULT 0,
    favorite_count BIGINT DEFAULT 0,
    avg_rating DOUBLE DEFAULT 0,
    comment_count BIGINT DEFAULT 0,
    staff_id BIGINT COMMENT '所属工作人员ID（STAFF）',
    longitude DOUBLE,
    latitude DOUBLE,
    ticket_reservation VARCHAR(500),
    ticket_reservation_en VARCHAR(1000),
    ticket_reservation_ja VARCHAR(1000),
    suggested_duration VARCHAR(100),
    suggested_duration_en VARCHAR(200),
    suggested_duration_ja VARCHAR(200),
    items_to_bring TEXT,
    items_to_bring_en TEXT,
    items_to_bring_ja TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_staff_id (staff_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS scenic_spot_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    spot_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    sort_order INT DEFAULT 0,
    INDEX idx_spot_id (spot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS route (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    name_en VARCHAR(200),
    name_ja VARCHAR(200),
    description TEXT,
    description_en TEXT,
    description_ja TEXT,
    days INT,
    theme VARCHAR(50),
    cover_image VARCHAR(255),
    traffic_suggestion TEXT,
    hotel_suggestion TEXT,
    budget DECIMAL(10,2),
    view_count BIGINT DEFAULT 0,
    favorite_count BIGINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS route_spot (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    route_id BIGINT NOT NULL,
    spot_id BIGINT NOT NULL,
    day_number INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    description VARCHAR(500),
    INDEX idx_route_id (route_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS culture_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort_order INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS culture_content (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    title_en VARCHAR(300),
    title_ja VARCHAR(300),
    content LONGTEXT,
    content_en LONGTEXT,
    content_ja LONGTEXT,
    category_id BIGINT,
    cover_image VARCHAR(255),
    author VARCHAR(50),
    view_count BIGINT DEFAULT 0,
    favorite_count BIGINT DEFAULT 0,
    like_count BIGINT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS hotel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    location VARCHAR(255),
    cover_image VARCHAR(255),
    price DECIMAL(10,2),
    has_breakfast INT DEFAULT 0,
    has_room_service INT DEFAULT 0,
    phone VARCHAR(20),
    status INT DEFAULT 1,
    rating DOUBLE DEFAULT 0,
    longitude DOUBLE,
    latitude DOUBLE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS food (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    price DECIMAL(10,2),
    cover_image VARCHAR(255),
    store_id BIGINT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS food_store (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(255),
    category VARCHAR(50),
    hygiene_level VARCHAR(20),
    phone VARCHAR(20),
    cover_image VARCHAR(255),
    longitude DOUBLE,
    latitude DOUBLE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    content TEXT,
    images VARCHAR(1000),
    rating INT DEFAULT 5,
    reply_content TEXT,
    reply_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_target (user_id, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS like_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_target (user_id, target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    order_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    amount DECIMAL(10,2),
    status VARCHAR(20) DEFAULT 'PENDING',
    pay_method VARCHAR(20),
    pay_time DATETIME,
    target_name VARCHAR(100),
    quantity INT DEFAULT 1,
    check_in_date DATE,
    check_out_date DATE,
    -- 酒店订单状态流转：待确认 PENDING_CONFIRM -> 已确认 CONFIRMED -> 入住中 CHECKED_IN -> 已结束 FINISHED / 已取消 CANCELLED
    confirmed_time DATETIME COMMENT '酒店确认时间',
    checked_in_time DATETIME COMMENT '实际入住时间',
    finished_time DATETIME COMMENT '离店/结束时间',
    cancelled_time DATETIME COMMENT '取消时间',
    reschedule_count INT NOT NULL DEFAULT 0 COMMENT '累计改期次数',
    last_reschedule_date DATE COMMENT '最近一次改期操作发生的自然日（入住日并发只接受最早一次）',
    checkin_day_reschedule_date DATE COMMENT '在原入住日当天发生过改期的自然日（当天后续改期一律拒绝）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_hotel_status (order_type, status, check_in_date, check_out_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单状态流转记录：每一次状态变更都保留变更时间并注明触发人
CREATE TABLE IF NOT EXISTS order_status_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(20) COMMENT '变更前状态（下单为 NULL）',
    to_status VARCHAR(20) NOT NULL COMMENT '变更后状态',
    trigger_type VARCHAR(20) NOT NULL COMMENT '触发类型：USER 用户 / ADMIN 后台 / SYSTEM 系统（定时任务）',
    trigger_id BIGINT COMMENT '触发人用户ID（系统触发为空）',
    trigger_name VARCHAR(100) COMMENT '触发人名称（用户名或“系统”）',
    remark VARCHAR(500) COMMENT '备注（如改期日期、重算房价、拒绝原因）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    content TEXT,
    is_read INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS faq (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    question VARCHAR(500) NOT NULL,
    answer TEXT NOT NULL,
    sort_order INT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户自定义线路
CREATE TABLE IF NOT EXISTS user_custom_route (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    days INT DEFAULT 1,
    spot_data TEXT COMMENT 'JSON格式景点列表',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT 'DRAFT|SUBMITTED|APPROVED|REJECTED',
    reject_reason TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE COMMENT '角色代码 USER/ADMIN/STAFF',
    name VARCHAR(50) NOT NULL COMMENT '角色名称',
    description VARCHAR(200),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 角色-菜单权限表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_code VARCHAR(30) NOT NULL,
    menu_key VARCHAR(80) NOT NULL COMMENT '菜单/按钮标识',
    menu_name VARCHAR(100) COMMENT '菜单名称',
    enabled INT DEFAULT 1 COMMENT '1=启用 0=禁用',
    UNIQUE KEY uk_role_menu (role_code, menu_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 景点更正建议
CREATE TABLE IF NOT EXISTS spot_suggestion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    spot_id BIGINT NOT NULL,
    spot_name VARCHAR(100),
    field_name VARCHAR(50) NOT NULL COMMENT '修改的字段',
    old_value TEXT,
    new_value TEXT NOT NULL,
    reason TEXT,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING|APPROVED|REJECTED',
    reject_reason TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 人工客服对话
CREATE TABLE IF NOT EXISTS service_chat (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sender VARCHAR(10) NOT NULL COMMENT 'USER/ADMIN',
    content TEXT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 问题反馈
CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    contact VARCHAR(100),
    category VARCHAR(30) DEFAULT 'GENERAL' COMMENT 'BUG|SUGGESTION|COMPLAINT|GENERAL',
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING|PROCESSING|RESOLVED',
    reply TEXT,
    reply_time DATETIME,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

