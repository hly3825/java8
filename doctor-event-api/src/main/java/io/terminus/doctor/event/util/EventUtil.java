package io.terminus.doctor.event.util;

import com.google.common.base.MoreObjects;
import org.joda.time.DateTime;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Date;

/**
 * Desc:
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/5/29
 */

public class EventUtil {
    private static final DecimalFormat DECIMAL_FMT_2 = new DecimalFormat("0.00");

    public static double getWeight(double avgWeight, int quantity) {
        return avgWeight * quantity;
    }

    public static double getWeight(Double avgWeight, Integer quantity) {
        return MoreObjects.firstNonNull(avgWeight, 0D) * MoreObjects.firstNonNull(quantity, 0);
    }

    public static double getAvgWeight(Double weight, Integer quantity) {
        if (quantity == null || quantity == 0) {
            return 0D;
        }
        return MoreObjects.firstNonNull(weight, 0.0D) / quantity;
    }

    /**
     * 重新计算均重
     * @param oldWeight 旧猪均重
     * @param newWeight 新进猪均重
     * @param allQty    猪只总数
     * @return 均重
     */
    public static double getAvgWeight(double oldWeight, double newWeight, int allQty) {
        return getAvgWeight(oldWeight + newWeight, allQty);
    }

    public static int plusInt(Integer aq, Integer bq) {
        return MoreObjects.firstNonNull(aq, 0) + MoreObjects.firstNonNull(bq, 0);
    }

    public static int minusInt(Integer aq, Integer bq) {
        return MoreObjects.firstNonNull(aq, 0) - MoreObjects.firstNonNull(bq, 0);
    }

    public static double plusDouble(Double aq, Double bq) {
        return MoreObjects.firstNonNull(aq, 0D) + MoreObjects.firstNonNull(bq, 0D);
    }

    public static double minusDouble(Double aq, Double bq) {
        return MoreObjects.firstNonNull(aq, 0D) - MoreObjects.firstNonNull(bq, 0D);
    }

    public static long minusLong(Long aq, Long bq){
        return MoreObjects.firstNonNull(aq, 0L) - MoreObjects.firstNonNull(bq, 0L);
    }

    public static int minusQuantity(int aq, int bq) {
        return aq - bq;
    }

    /**
     * 重新计算下日龄(四舍五入)
     * @param oldAge 旧猪日龄(事件发生时的日龄)
     * @param oldQty 旧猪只数
     * @param newAge 新进猪日龄(事件发生时的日龄)
     * @param newQty 新进猪只数
     * @return 日龄
     */
    public static int getAvgDayAge(int oldAge, int oldQty, int newAge, int newQty) {
        int allAge = oldAge * oldQty + newAge * newQty;
        return new BigDecimal(allAge).divide(new BigDecimal(oldQty + newQty), BigDecimal.ROUND_HALF_UP).intValue();
    }

    /**
     * 计算日龄,保留两位小数(四舍五入)
     * @param oldAge
     * @param oldQty
     * @param newAge
     * @param newQty
     * @return 平均日龄
     */
    public static double getAvgDayAge(Double oldAge, Integer oldQty, Double newAge, Integer newQty) {

        double allAge = MoreObjects.firstNonNull(oldAge, 0).doubleValue() * MoreObjects.firstNonNull(oldQty, 0)
                + MoreObjects.firstNonNull(newAge, 0).doubleValue() * MoreObjects.firstNonNull(newQty, 0);
        int allQty = MoreObjects.firstNonNull(oldQty, 0) + MoreObjects.firstNonNull(newQty, 0);
        if (allQty == 0) {
            return 0D;
        }
        return new BigDecimal(allAge).divide(new BigDecimal(allQty), 2, BigDecimal.ROUND_HALF_UP).intValue();
    }

    /**
     * 获取均重
     * @param oldWeight
     * @param oldQty
     * @param newWeight
     * @param newQty
     * @return
     */
    public static double getAvgWeight(Double oldWeight, Integer oldQty, Double newWeight, Integer newQty) {

        double allWeight = MoreObjects.firstNonNull(oldWeight, 0).doubleValue() * MoreObjects.firstNonNull(oldQty, 0)
                + MoreObjects.firstNonNull(newWeight, 0).doubleValue() * MoreObjects.firstNonNull(newQty, 0);
        int allQty = MoreObjects.firstNonNull(oldQty, 0) + MoreObjects.firstNonNull(newQty, 0);
        if (allQty == 0) {
            return 0D;
        }
        return new BigDecimal(allWeight).divide(new BigDecimal(allQty), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    /**
     * 重新计算出生日期(日期-日龄 + 1)
     * @param date  日期
     * @param avgDayAge  日龄
     * @return 出生日期
     */
    public static Date getBirthDate(Date date, int avgDayAge) {
        return new DateTime(date).minusDays(avgDayAge - 1).withTimeAtStartOfDay().toDate();
    }

    public static long getPrice(long amount, int quantity) {
        if (quantity == 0) {
            return 0L;
        }
        return amount / quantity;
    }

    public static long getAmount(Long price, Double weight) {
        return (long) (MoreObjects.firstNonNull(price, 0L) * get2(MoreObjects.firstNonNull(weight, 0D)));
    }

    public static double get2(double number) {
        return new BigDecimal(number).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public static double get4(double number) {
        return new BigDecimal(number).setScale(4, BigDecimal.ROUND_HALF_UP).doubleValue();
    }
}
