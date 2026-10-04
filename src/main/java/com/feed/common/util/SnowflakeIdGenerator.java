package com.feed.common.util;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import lombok.extern.slf4j.Slf4j;

/**
 * 雪花算法 ID 生成器（Twitter Snowflake）
 *
 * 64 位 Long 结构：
 * 0 | 41 位时间戳(相对 epoch) | 5 位机房id | 5 位机器id | 12 位序列号
 *
 * - 全局趋势递增（高位是时间戳），适合做数据库主键和 B+ 树索引
 * - 不依赖数据库自增，分库分表/批量导入不会撞 id
 * - 单机每毫秒最多生成 4096 个，序号耗尽则等待下一毫秒
 */
@Slf4j
public class SnowflakeIdGenerator implements IdentifierGenerator {

    /** 起始时间戳：2024-01-01 00:00:00（一旦上线不可修改） */
    private static final long EPOCH = 1704038400000L;

    /** 各部分位数 */
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    /** 最大值：31 / 31 / 4095 */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /** 左移位数 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long workerId;
    private final long datacenterId;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long workerId, long datacenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("workerId 超出范围 0~31: " + workerId);
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException("datacenterId 超出范围 0~31: " + datacenterId);
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        log.info("SnowflakeIdGenerator 初始化：workerId={}, datacenterId={}", workerId, datacenterId);
    }

    /**
     * MyBatis-Plus 主键生成入口：@TableId(type = IdType.ASSIGN_ID) 的实体会调用
     */
    @Override
    public Long nextId(Object entity) {
        return nextId();
    }

    /**
     * 生成全局唯一 id（synchronized 保证单实例线程安全）
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        // 时钟回拨保护：NTP 校时导致时间倒退时直接拒绝，避免生成重复 id
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            throw new IllegalStateException("时钟回拨，拒绝生成 id，回拨毫秒=" + offset);
        }

        if (timestamp == lastTimestamp) {
            // 同一毫秒内：序列号 +1，溢出则自旋到下一毫秒
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
