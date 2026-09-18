package com.redtourism.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.redtourism.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 行级排他锁读取订单，用于“入住日当天并发改期只接受最早一次”等临界区判定。
     * 必须在事务内调用。
     */
    @Select("SELECT * FROM order_info WHERE id = #{id} FOR UPDATE")
    OrderInfo selectByIdForUpdate(@Param("id") Long id);

    /**
     * 查询需要按入住日自动推进状态的酒店订单（已确认 / 入住中）。
     */
    @Select("SELECT * FROM order_info WHERE order_type = 'HOTEL' AND status IN ('CONFIRMED','CHECKED_IN') " +
            "AND check_in_date IS NOT NULL AND check_out_date IS NOT NULL")
    List<OrderInfo> selectAutoTransitionHotelOrders();
}
