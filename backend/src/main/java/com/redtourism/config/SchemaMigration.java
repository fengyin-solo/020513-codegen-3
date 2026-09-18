package com.redtourism.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 酒店订单状态流转相关的幂等表结构迁移。
 * schema.sql 仅在 MySQL 容器首次初始化时执行，已有数据卷需要通过此处补齐新列/新表。
 */
@Slf4j
@Component
public class SchemaMigration implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        try {
            addColumnIfMissing("order_info", "reschedule_count",
                    "ALTER TABLE order_info ADD COLUMN reschedule_count INT DEFAULT 0 COMMENT '改期次数'");
            addColumnIfMissing("order_info", "version",
                    "ALTER TABLE order_info ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号'");
            jdbcTemplate.update("UPDATE order_info SET reschedule_count = 0 WHERE reschedule_count IS NULL");
            jdbcTemplate.update("UPDATE order_info SET version = 0 WHERE version IS NULL");
            createOrderStatusLogIfMissing();
        } catch (Exception e) {
            log.error("订单表结构迁移失败: ", e);
        }
    }

    private void addColumnIfMissing(String table, String column, String ddl) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class, table, column);
        if (count == null || count == 0) {
            log.info("迁移表结构：{}", ddl);
            jdbcTemplate.execute(ddl);
        }
    }

    private void createOrderStatusLogIfMissing() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_status_log'",
                Integer.class);
        if (count == null || count == 0) {
            log.info("迁移表结构：创建 order_status_log 表");
            jdbcTemplate.execute(
                    "CREATE TABLE order_status_log (" +
                    "    id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "    order_id BIGINT NOT NULL," +
                    "    action VARCHAR(20) NOT NULL COMMENT 'CREATE/CONFIRM/RESCHEDULE/CHECKIN/FINISH/CANCEL'," +
                    "    from_status VARCHAR(20)," +
                    "    to_status VARCHAR(20)," +
                    "    operator_type VARCHAR(10) NOT NULL COMMENT 'USER/ADMIN/STAFF/SYSTEM'," +
                    "    operator_id BIGINT," +
                    "    operator_name VARCHAR(50)," +
                    "    detail VARCHAR(500) COMMENT '变更说明'," +
                    "    create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "    INDEX idx_order_id (order_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }
}
