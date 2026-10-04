package com.feed.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 时间工具：LocalDateTime 与毫秒时间戳互转（按系统时区，与 MySQL FROM_UNIXTIME 口径一致）
 */
public class TimeUtils {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    public static long toMillis(LocalDateTime time) {
        return time.atZone(ZONE).toInstant().toEpochMilli();
    }

    public static LocalDateTime toLocalDateTime(long millis) {
        return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis), ZONE);
    }
}
