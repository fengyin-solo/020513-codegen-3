DROP TABLE IF EXISTS order_status_log;
DROP TABLE IF EXISTS order_info;
DROP TABLE IF EXISTS message;
DROP TABLE IF EXISTS hotel;

CREATE TABLE hotel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    description CLOB,
    location VARCHAR(255),
    cover_image VARCHAR(255),
    price DECIMAL(10,2),
    has_breakfast INT,
    has_room_service INT,
    phone VARCHAR(50),
    status INT DEFAULT 1,
    rating DOUBLE,
    longitude DOUBLE,
    latitude DOUBLE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_info (
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
    confirmed_time DATETIME,
    checked_in_time DATETIME,
    finished_time DATETIME,
    cancelled_time DATETIME,
    reschedule_count INT NOT NULL DEFAULT 0,
    last_reschedule_date DATE,
    checkin_day_reschedule_date DATE,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_status_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    trigger_type VARCHAR(20) NOT NULL,
    trigger_id BIGINT,
    trigger_name VARCHAR(100),
    remark VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200),
    content CLOB,
    is_read INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
