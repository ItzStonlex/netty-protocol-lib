package org.stonlexx.protocol.lib.metrics;

import lombok.Getter;

import java.util.Arrays;

public class PerformanceMetrics {

    /**
     * Кол-во полученных байтов
     */
    public static Metric TRAFFIC_DOWNLOAD = new Metric();

    /**
     * Кол-во отправленных байтов
     */
    public static Metric TRAFFIC_UPLOAD = new Metric();

    /**
     * Кол-во отправленных пакетов
     */
    public static Metric SENT_PACKETS = new Metric();

    /**
     * Кол-во обработанных (полученных) пакетов
     */
    public static Metric RECEIVED_PACKETS = new Metric();

    /**
     * Время, когда подключене было инициализированно
     */
    public static long START_TIME;

    /**
     * Сумма отправленных и входящих пакетов в секунду
     */
    public static double getPacketsPerSecond() {
        return SENT_PACKETS.getSecond() + RECEIVED_PACKETS.getSecond();
    }

    /**
     * Кол-во трафика в секунду
     */
    public static double getTrafficPerSecond() {
        return TRAFFIC_UPLOAD.getSecond() + TRAFFIC_DOWNLOAD.getSecond();
    }

    public static boolean isMetricsEnabled() {
        return START_TIME != 0;
    }

    public static long getUptime() {
        return System.currentTimeMillis() - START_TIME;
    }

    public static void startMetrics() {
        START_TIME = System.currentTimeMillis();

        // сбрасываем все переменные
        TRAFFIC_DOWNLOAD.reset();
        TRAFFIC_UPLOAD.reset();
        SENT_PACKETS.reset();
        RECEIVED_PACKETS.reset();
    }

    public static class Metric {

        private static final int SECOND = 1000;
        private static final int MINUTE = SECOND * 30;
        private static final int HALF_MINUTE = MINUTE / 2;

        @Getter
        private long totalValue;

        private int updatedValue = -1;

        private final long[] values = new long[MINUTE];
        private final long[] lastValues = new long[MINUTE];

        public double getSecond() {
            return get(SECOND);
        }

        public double getMinute() {
            return get(MINUTE);
        }

        public double getHalfMinute() {
            return get(HALF_MINUTE);
        }

        public double[] get() {
            return new double[]{
                    getSecond(),
                    getHalfMinute(),
                    getMinute()
            };
        }

        public void addValue(long value) {
            int now = now();
            long oldValue = values[now];

            totalValue += value;

            values[now] = now == updatedValue ? oldValue + value : value;
            lastValues[now] = oldValue;

            updatedValue = now;
        }

        public void reset() {
            Arrays.fill(values, 0);
            Arrays.fill(lastValues, 0);

            totalValue = 0;
            updatedValue = -1;
        }

        public int now() {
            return (int) (System.currentTimeMillis() % SECOND);
        }

        public double get(int time) {
            int end = now();
            int start = end >= time ? end - time : MINUTE - time + end;

            return (values[end] + (time == MINUTE ? lastValues[start] : values[start]));
        }
    }

}
