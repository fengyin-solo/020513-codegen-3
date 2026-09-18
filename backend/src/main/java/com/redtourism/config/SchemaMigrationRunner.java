package com.redtourism.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 幂等的数据库结构补丁：schema.sql 仅在 MySQL 数据卷首次初始化时执行，
 * 已存在数据卷的部署环境不会重新建表，因此启动时按 information_schema 补齐
 * 酒店订单状态流转所需的列与 order_status_log 表。
 */
@Slf4j
@Component
public class SchemaMigrationRunner implements ApplicationRunner {

    @Autowired
    private JdbcTemplate jdbc;

    private static final List<String> ORDER_COLUMNS = Arrays.asList(
            "ALTER TABLE order_info ADD COLUMN confirmed_time DATETIME NULL COMMENT '酒店确认时间'",
            "ALTER TABLE order_info ADD COLUMN checked_in_time DATETIME NULL COMMENT '实际入住时间'",
            "ALTER TABLE order_info ADD COLUMN finished_time DATETIME NULL COMMENT '离店/结束时间'",
            "ALTER TABLE order_info ADD COLUMN cancelled_time DATETIME NULL COMMENT '取消时间'",
            "ALTER TABLE order_info ADD COLUMN reschedule_count INT NOT NULL DEFAULT 0 COMMENT '累计改期次数'",
            "ALTER TABLE order_info ADD COLUMN last_reschedule_date DATE NULL COMMENT '最近一次改期发生的自然日'",
            "ALTER TABLE order_info ADD COLUMN checkin_day_reschedule_date DATE NULL COMMENT '原入住日当天发生过改期的自然日'"
    );

    @Override
    public void run(ApplicationArguments args) {
        ensureOrderColumns();
        ensureStatusLogTable();
    }

    private boolean columnExists(String column) {
        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(1) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = 'order_info' AND column_name = ?",
                Integer.class, column);
        return cnt != null && cnt > 0;
    }

    private void ensureOrderColumns() {
        for (String sql : ORDER_COLUMNS) {
            String column = sql.substring(sql.indexOf("ADD COLUMN ") + 11, sql.indexOf(' ', sql.indexOf("ADD COLUMN ") + 11));
            try {
                if (!columnExists(column)) {
                    jdbc.execute(sql);
                    log.info("结构补丁：order_info 新增列 {}", column);
                }
            } catch (Exception e) {
                log.warn("结构补丁跳过 order_info.{}：{}", column, e.getMessage());
            }
        }
        // 存量已支付的老酒店订单（本版本以前不会产生酒店支付单）无需迁移；索引按需创建
        try {
            jdbc.execute("ALTER TABLE order_info ADD INDEX idx_hotel_status (order_type, status, check_in_date, check_out_date)");
        } catch (Exception ignored) {
            // 索引已存在则忽略（MySQL 不支持 ADD INDEX IF NOT EXISTS）
        }
    }

    private void ensureStatusLogTable() {
        try {
            jdbc.update("CREATE TABLE IF NOT EXISTS order_status_log (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                    "order_id BIGINT NOT NULL," +
                    "from_status VARCHAR(20) NULL," +
                    "to_status VARCHAR(20) NOT NULL," +
                    "trigger_type VARCHAR(20) NOT NULL," +
                    "trigger_id BIGINT NULL," +
                    "trigger_name VARCHAR(100) NULL," +
                    "remark VARCHAR(500) NULL," +
                    "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "INDEX idx_order (order_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        } catch (Exception e) {
            log.warn("order_status_log 表创建检查失败：{}", e.getMessage());
        }
    }
}
