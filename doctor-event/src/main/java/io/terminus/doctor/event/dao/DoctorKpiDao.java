package io.terminus.doctor.event.dao;

import com.google.common.collect.ImmutableMap;
import io.terminus.common.utils.Arguments;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.event.dto.report.common.DoctorLiveStockChangeCommonReport;
import io.terminus.doctor.event.dto.report.common.DoctorStockStructureCommonReport;
import io.terminus.doctor.event.handler.sow.DoctorSowMatingHandler;
import io.terminus.doctor.event.util.EventUtil;
import org.joda.time.DateTime;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Desc: 猪场月报表Dao类
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 2016-08-11
 */
@Repository
public class DoctorKpiDao {

    private final SqlSessionTemplate sqlSession;

    @Autowired
    public DoctorKpiDao(SqlSessionTemplate sqlSession) {
        this.sqlSession = sqlSession;
    }

    private static String sqlId(String id) {
        return "DoctorKpi." + id;
    }

    /**
     * 获取某一猪群的死淘率
     */
    public double getDeadRateByGroupId(Long groupId) {
        return EventUtil.get2(sqlSession.selectOne(sqlId("getDeadRateByGroupId"), groupId));
    }

    /**
     * 预产胎数
     */
    public int getPreDelivery(Long farmId, Date startAt, Date endAt) {
        if (!Objects.isNull(startAt)) {
            startAt = new DateTime(startAt).minusDays(DoctorSowMatingHandler.MATING_PREG_DAYS).toDate();
        }
        if (!Objects.isNull(endAt)) {
            endAt = new DateTime(endAt).minusDays(DoctorSowMatingHandler.MATING_PREG_DAYS).toDate();
        }
        return this.sqlSession.selectOne(sqlId("preDeliveryCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 分娩窝数
     */
    public int getDelivery(Long farmId, Date startAt, Date endAt) {
        return this.sqlSession.selectOne(sqlId("deliveryCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 分娩初生重
     */
    public double getFarrowWeightAvg(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get2(this.sqlSession.selectOne(sqlId("farrowWeightAvg"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 分娩初生重总重
     */
    public double getFarrowWeight(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get2(this.sqlSession.selectOne(sqlId("farrowWeight"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 产活仔数
     */
    public int getDeliveryLive(Long farmId, Date startAt, Date endAt) {
        return this.sqlSession.selectOne(sqlId("deliveryLiveCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 产健仔数
     */
    public int getDeliveryHealth(Long farmId, Date startAt, Date endAt) {
        return this.sqlSession.selectOne(sqlId("deliveryHealthCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 产弱仔数
     */
    public int getDeliveryWeak(Long farmId, Date startAt, Date endAt) {
        return this.sqlSession.selectOne(sqlId("deliveryWeakCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 产死仔数
     */
    public int getDeliveryDead(Long farmId, Date startAt, Date endAt) {
        return this.sqlSession.selectOne(sqlId("deliveryDeadCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 产畸形数
     */
    public int getDeliveryJx(Long farmId, Date startAt, Date endAt) {
        return this.sqlSession.selectOne(sqlId("deliveryJxCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 产木乃伊数
     */
    public int getDeliveryMny(Long farmId, Date startAt, Date endAt) {
        return this.sqlSession.selectOne(sqlId("deliveryMnyCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 产黑胎数
     */
    public int getDeliveryBlack(Long farmId, Date startAt, Date endAt) {
        return this.sqlSession.selectOne(sqlId("deliveryBlackCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 死黑木畸
     */
    public int getDeliveryDeadBlackMuJi(Long farmId, Date startAt, Date endAt) {
        return this.sqlSession.selectOne(sqlId("getDeliveryDeadBlackMuJi"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 总产仔数
     */
    public int getDeliveryAll(Long farmId, Date startAt, Date endAt) {
        return this.sqlSession.selectOne(sqlId("deliveryAllCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 窝均健仔数
     */
    public double getDeliveryHealthAvg(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get2(this.sqlSession.selectOne(sqlId("deliveryHealthCountsAvg"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 窝均弱仔数
     */
    public double getDeliveryWeakAvg(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get2(this.sqlSession.selectOne(sqlId("deliveryWeakCountsAvg"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 窝均活仔数
     */
    public double getDeliveryLiveAvg(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get2(this.sqlSession.selectOne(sqlId("deliveryLiveCountsAvg"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 窝均产仔数
     */
    public double getDeliveryAllAvg(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get2(this.sqlSession.selectOne(sqlId("deliveryAllCountsAvg"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 断奶母猪数
     */
    public int getWeanSow(Long farmId, Date startAt, Date endAt) {
        return this.sqlSession.selectOne(sqlId("weanSowCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 断奶仔猪数
     */
    public int getWeanPiglet(Long farmId, Date startAt, Date endAt) {
        return this.sqlSession.selectOne(sqlId("weanPigletCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 断奶仔猪均重
     */
    public double getWeanPigletWeightAvg(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get2(this.sqlSession.selectOne(sqlId("weanPigletWeightAvg"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 窝均断奶数
     */
    public double getWeanPigletCountsAvg(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get2(this.sqlSession.selectOne(sqlId("weanPigletCountsAvg"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 断奶日龄
     */
    public double getWeanDayAgeAvg(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get2(this.sqlSession.selectOne(sqlId("getWeanDayAgeAvg"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 产房仔猪转场
     */
    public int getFarrowChgFarmCount(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getFarrowChgFarmCount"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 产房仔猪转保育
     */
    public int getFarrowToNursery(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getFarrowToNurseyCount"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 产房仔猪销售
     */
    public int getFarrowSaleCount(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getFarrowSaleCount"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 育肥猪转后备猪
     */
    public int getMonthlyLiveStockChangeToHoubei(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getMonthlyLiveStockChangeToHoubei"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }
    /**
     * 保育猪转场
     */
    public int getMonthlyLiveStockChangeGroupNuseryNumber(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getMonthlyLiveStockChangeGroupNuseryNumber"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }
    /**
     * 育肥转场
     */
    public int getMonthlyLiveStockChangeGroupFattenNumber(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getMonthlyLiveStockChangeGroupFattenNumber"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }
    /**
     * 后备转场
     */
    public int getMonthlyLiveStockChangeGroupHoubeiNumber(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getMonthlyLiveStockChangeGroupHoubeiNumber"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 保育猪转场
     */
    public int getMonthlyLiveStockChangeGroupNuseryOtherNumber(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getMonthlyLiveStockChangeGroupNuseryOtherNumber"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }
    /**
     * 育肥转场
     */
    public int getMonthlyLiveStockChangeGroupFattenOtherNumber(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getMonthlyLiveStockChangeGroupFattenOtherNumber"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }
    /**
     * 后备转场
     */
    public int getMonthlyLiveStockChangeGroupHoubeiOtherNumber(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getMonthlyLiveStockChangeGroupHoubeiOtherNumber"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 销售情况: 母猪
     */
    public int getSaleSow(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getSaleSow"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 销售情况: 公猪
     */
    public int getSaleBoar(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getSaleBoar"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 销售情况: 保育猪（产房+保育）
     */
    public int getSaleNursery(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getSaleNursery"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 销售情况: 育肥猪
     */
    public int getSaleFatten(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getSaleFatten"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 销售情况: 后备猪
     */
    public int getSaleHoubei(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getSaleHoubei"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 进场情况: 母猪
     */
    public int getInSow(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getInSow"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 其他数量减少情况: 母猪
     */
    public int getOtherOutSow(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getOtherOutSow"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 进场情况: 公猪
     */
    public int getInBoar(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getInBoar"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 其他数量减少情况: 公猪
     */
    public int getOtherOutBoar(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getOtherOutBoar"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 死亡情况: 母猪
     */
    public int getDeadSow(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getDeadSow"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 淘汰情况: 母猪
     */
    public int getWeedOutSow(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getWeedOutSow"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 死亡情况: 公猪
     */
    public int getDeadBoar(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getDeadBoar"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 淘汰情况: 公猪
     */
    public int getWeedOutBoar(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getWeedOutBoar"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 死亡情况: 产房仔猪
     */
    public int getDeadFarrow(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getDeadFarrow"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 淘汰情况: 产房仔猪
     */
    public int getWeedOutFarrow(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getWeedOutFarrow"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 死亡情况: 保育猪
     */
    public int getDeadNursery(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getDeadNursery"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 淘汰情况: 保育猪
     */
    public int getWeedOutNursery(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getWeedOutNursery"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 死亡情况: 育肥猪
     */
    public int getDeadFatten(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getDeadFatten"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 淘汰情况: 育肥猪
     */
    public int getWeedOutFatten(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getWeedOutFatten"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 死亡情况: 后备猪
     */
    public int getDeadHoubei(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getDeadHoubei"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 淘汰情况: 后备猪
     */
    public int getWeedOutHoubei(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getWeedOutHoubei"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 死淘情况: 产房死淘率
     */
    public double getDeadFarrowRate(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get4(sqlSession.selectOne(sqlId("getDeadFarrowRate"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 死淘情况: 保育死淘率
     */
    public double getDeadNurseryRate(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get4(sqlSession.selectOne(sqlId("getDeadNurseryRate"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 死淘情况: 育肥死淘率
     */
    public double getDeadFattenRate(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get4(sqlSession.selectOne(sqlId("getDeadFattenRate"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 配种情况:配后备
     */
    public int firstMatingCounts(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("firstMatingCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 配种情况:配流产
     */
    public int abortionMatingCounts(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("abortionMatingCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 配种情况:配断奶
     */
    public int weanMatingCounts(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("weanMatingCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 配种情况:配阴性
     */
    public int yinMatingCounts(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("yinMatingCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 配种情况:配返情
     */
    public int fanQMatingCounts(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("fanQMatingCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 配种情况:估算受胎率
     */
    public double assessPregnancyRate(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get4(sqlSession.selectOne(sqlId("assessPregnancyRate"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 配种情况:实际受胎率
     */
    public double realPregnancyRate(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get4(sqlSession.selectOne(sqlId("realPregnancyRate"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 配种情况:估算分娩率
     */
    public double assessFarrowingRate(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get4(sqlSession.selectOne(sqlId("assessFarrowingRate"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 配种情况:实际配种分娩率
     */
    public double realFarrowingRate(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get4(sqlSession.selectOne(sqlId("realFarrowingRate"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 妊娠检查情况:妊娠检查阳性
     */
    public int checkYangCounts(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("checkYangCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 妊娠检查情况:返情
     */
    public int checkFanQCounts(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("checkFanQCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 妊娠检查情况:妊娠检查阴性
     */
    public int checkYingCounts(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("checkYingCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 妊娠检查情况:流产
     */
    public int checkAbortionCounts(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("checkAbortionCounts"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * NPD
     */
    public double npd(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get2(sqlSession.selectOne(sqlId("npd"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * psy
     */
    public double psy(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get2(sqlSession.selectOne(sqlId("psy"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 某一猪群即将关闭之前的存栏
     *
     * @param groupId 猪群id
     * @return 存栏数量
     */
    public int farrowGroupQuantityWhenClose(Long groupId) {
        return sqlSession.selectOne(sqlId("farrowGroupQuantityWhenClose"), ImmutableMap.of("groupId", groupId));
    }

    /**
     * 实时存栏: 获取某天的产房仔猪存栏
     *
     * @param farmId 猪场id
     * @param date   日期
     * @return 存栏数量
     */
    public int realTimeLiveStockFarrow(Long farmId, Date date) {
        return sqlSession.selectOne(sqlId("realTimeLiveStockFarrow"), ImmutableMap.of("farmId", farmId, "date", date));
    }

    /**
     * 实时存栏: 获取某天的保育猪存栏
     *
     * @param farmId 猪场id
     * @param date   日期
     * @return 存栏数量
     */
    public int realTimeLiveStockNursery(Long farmId, Date date) {
        return sqlSession.selectOne(sqlId("realTimeLiveStockNursery"), ImmutableMap.of("farmId", farmId, "date", date));
    }

    /**
     * 实时存栏: 获取某天的育肥猪存栏
     *
     * @param farmId 猪场id
     * @param date   日期
     * @return 存栏数量
     */
    public int realTimeLiveStockFatten(Long farmId, Date date) {
        return sqlSession.selectOne(sqlId("realTimeLiveStockFatten"), ImmutableMap.of("farmId", farmId, "date", date));
    }

    /**
     * 实时存栏: 获取某天的后备母猪存栏
     *
     * @param farmId 猪场id
     * @param date   日期
     * @return 存栏数量
     */
    public int realTimeLiveStockHoubeiSow(Long farmId, Date date) {
        return sqlSession.selectOne(sqlId("realTimeLiveStockHoubeiSow"), ImmutableMap.of("farmId", farmId, "date", date));
    }

    /**
     * 实时存栏: 获取某天的后备公猪存栏
     *
     * @param farmId 猪场id
     * @param date   日期
     * @return 存栏数量
     * @deprecated 已经没有后备公猪了，统称后备猪
     */
    @Deprecated
    public int realTimeLiveStockHoubeiBoar(Long farmId, Date date) {
        return sqlSession.selectOne(sqlId("realTimeLiveStockHoubeiBoar"), ImmutableMap.of("farmId", farmId, "date", date));
    }


    /**
     * 实时存栏: 获取某天公猪存栏
     *
     * @param farmId 猪场id
     * @param date   日期
     * @return 存栏数量
     */
    public int realTimeLiveStockBoar(Long farmId, Date date) {
        return sqlSession.selectOne(sqlId("realTimeLiveStockBoar"), ImmutableMap.of("farmId", farmId, "date", date));
    }

    /**
     * 实时存栏: 获取某天母猪存栏
     *
     * @param farmId 猪场id
     * @param date   日期
     * @return 存栏数量
     */
    public int realTimeLiveStockSow(Long farmId, Date date) {
        return sqlSession.selectOne(sqlId("realTimeLiveStockSow"), ImmutableMap.of("farmId", farmId, "date", date));
    }

    /**
     * 实时存栏: 获取某天在产房的母猪存栏
     *
     * @param farmId 猪场id
     * @param date   日期
     * @return 存栏数量
     */
    public int realTimeLiveStockFarrowSow(Long farmId, Date date) {
        return sqlSession.selectOne(sqlId("realTimeLiveStockFarrowSow"), ImmutableMap.of("farmId", farmId, "date", date));
    }

    /**
     *
     * @param farmId
     * @param date
     * @return
     */
    public int realTimeLiveStockPHSow(Long farmId, Date date) {
        return sqlSession.selectOne(sqlId("realTimeLiveStockPHSow"), ImmutableMap.of("farmId", farmId, "date", date));
    }

    /**
     * 猪群销售: 基础价格10kg的均价
     */
    public long getGroupSaleBasePrice10(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getGroupSaleBasePrice10"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 猪群销售: 基础价格15kg的均价
     */
    public long getGroupSaleBasePrice15(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getGroupSaleBasePrice15"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 猪群销售: 育肥猪均价
     */
    public long getGroupSaleFattenPrice(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getGroupSaleFattenPrice"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 公猪生产月报: 配种次数
     */
    public int getBoarMateCount(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getBoarMateCount"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 公猪生产月报: 首配母猪头数
     */
    public int getBoarSowFirstMateCount(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getBoarSowFirstMateCount"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 公猪生产月报: 受胎头数
     */
    public int getBoarSowPregCount(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getBoarSowPregCount"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 公猪生产月报: 产仔母猪数
     */
    public int getBoarSowFarrowCount(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getBoarSowFarrowCount"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 公猪生产月报: 平均产仔数
     */
    public double getBoarSowFarrowAvgCount(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get2(sqlSession.selectOne(sqlId("getBoarSowFarrowAvgCount"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 公猪生产月报: 平均产活仔数
     */
    public double getBoarSowFarrowLiveAvgCount(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get2(sqlSession.selectOne(sqlId("getBoarSowFarrowLiveAvgCount"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 公猪生产月报: 受胎率
     */
    public double getBoarSowPregRate(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get4(sqlSession.selectOne(sqlId("getBoarSowPregRate"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 公猪生产月报: 分娩率
     */
    public double getBoarSowFarrowRate(Long farmId, Date startAt, Date endAt) {
        return EventUtil.get4(sqlSession.selectOne(sqlId("getBoarSowFarrowRate"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt)));
    }

    /**
     * 存栏变动月报: 期初
     */
    public DoctorLiveStockChangeCommonReport getMonthlyLiveStockChangeBegin(Long farmId, Date startAt) {
        DoctorLiveStockChangeCommonReport report = new DoctorLiveStockChangeCommonReport();
        report.setHoubeiBegin(realTimeLiveStockHoubeiSow(farmId, startAt) + realTimeLiveStockHoubeiBoar(farmId, startAt));
        report.setFarrowSowBegin(realTimeLiveStockFarrowSow(farmId, startAt));
        report.setPeiHuaiBegin(realTimeLiveStockSow(farmId, startAt) - report.getFarrowSowBegin());
        report.setFarrowBegin(realTimeLiveStockFarrow(farmId, startAt));
        report.setNurseryBegin(realTimeLiveStockNursery(farmId, startAt));
        report.setFattenBegin(realTimeLiveStockFatten(farmId, startAt));
        return report;
    }

    /**
     * 存栏变动月报: 转入(后备,产房仔猪,保育,育肥)
     */
    public DoctorLiveStockChangeCommonReport getMonthlyLiveStockChangeIn(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne("getMonthlyLiveStockChangeIn", ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 存栏变动月报: 进场
     */
    public int getMonthlyLiveStockChangeSowIn(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne("getMonthlyLiveStockChangeSowIn", ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }


    /**
     * 存栏变动月报: 断奶转入
     */
    public int getMonthlyLiveStockChangeWeanIn(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne("getMonthlyLiveStockChangeWeanIn", ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 存栏变动月报: 配怀转产房
     */
    public int getMonthlyLiveStockChangeToFarrow(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne("getMonthlyLiveStockChangeToFarrow", ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 存栏变动月报: 后备转种猪
     */
    public int getMonthlyLiveStockChangeToSeed(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne("getMonthlyLiveStockChangeToSeed", ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }


    /**
     * 存栏变动月报: 产房转保育
     */
    public int getMonthlyLiveStockChangeToNursery(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne("getMonthlyLiveStockChangeToNursery", ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 存栏变动月报: 保育转育肥
     */
    public int getMonthlyLiveStockChangeToFatten(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne("getMonthlyLiveStockChangeToFatten", ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 存栏变动月报: 死淘(后备,产房仔猪,保育,育肥)
     */
    public DoctorLiveStockChangeCommonReport getMonthlyLiveStockChangeGroupDead(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne("getMonthlyLiveStockChangeGroupDead", ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 存栏变动月报: 死淘(配怀,产房母猪)
     */
    public DoctorLiveStockChangeCommonReport getMonthlyLiveStockChangeSowDead(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne("getMonthlyLiveStockChangeSowDead", ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 存栏变动月报: 销售(后备,产房仔猪,保育,育肥)
     */
    public DoctorLiveStockChangeCommonReport getMonthlyLiveStockChangeSale(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne("getMonthlyLiveStockChangeSale", ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 存栏变动月报: 饲料数量
     */
    public DoctorLiveStockChangeCommonReport getMonthlyLiveStockChangeFeedCount(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne("getMonthlyLiveStockChangeFeedCount", ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 存栏变动月报: 物料金额(饲料, 药品, 疫苗, 易耗品
     */
    public DoctorLiveStockChangeCommonReport getMonthlyLiveStockChangeMaterielAmount(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne("getMonthlyLiveStockChangeMaterielAmount", ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 胎次分布月报
     * @param farmId
     * @param startAt
     * @param endAt
     * @return
     */
    public List<DoctorStockStructureCommonReport> getMonthlyParityStock(Long farmId, Date startAt, Date endAt){
        return sqlSession.selectList(sqlId("getParityStockMonthly"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 品类分布月报
     * @param farmId
     * @param startAt
     * @param endAt
     * @return
     */
    public List<DoctorStockStructureCommonReport> getMonthlyBreedStock(Long farmId, Date startAt, Date endAt){
        return sqlSession.selectList(sqlId("getBreedStockMonthly"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 断奶七天配种率
     */
    public double getMateInSeven(Long farmId, Date startAt, Date endAt) {
        Double d = sqlSession.selectOne(sqlId("getMateInSeven"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
        return d == null ? 0D : EventUtil.get4(d);
    }

    /**
     * 根据日期获取当时猪群的情况
     */
    public List<Map<String, Object>> getEveryGroupInfo(String date) {
        return sqlSession.selectList(sqlId("getEveryGroupInfo"), ImmutableMap.of("date", date));
    }

    /**
     * 猪群实时存栏
     * @param groupId
     * @param date
     * @return
     */
    public int realTimeLivetockGroup(Long groupId, Date date){
        return sqlSession.selectOne(sqlId("realTimeLivetockGroup"), ImmutableMap.of("groupId", groupId, "date", date));
    }

    /**
     * 具体某个猪群内部转入
     * @param groupId
     * @param startAt
     * @param endAt
     * @return
     */
    public int getGroupInnerIn(Long groupId, Date startAt, Date endAt){
        return sqlSession.selectOne(sqlId("getGroupInnerIn"), ImmutableMap.of("groupId", groupId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 猪群时间段内分娩转入
     * @param groupId 猪群id
     * @param startAt 开始时间
     * @param endAt 结束时间
     * @return 分娩转入数量
     */
    public int getGroupFarrowIn(Long groupId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getGroupFarrowIn"), ImmutableMap.of("groupId", groupId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 猪群时间段内断奶数量
     * @param groupId 猪群id
     * @param startAt 开始时间
     * @param endAt 结束时间
     * @return 分娩转入数量
     */
    public int getGroupDayWeanCount(Long groupId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getGroupDayWeanCount"), ImmutableMap.of("groupId", groupId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 具体某个猪群外部转入
     * @param groupId
     * @param startAt
     * @param endAt
     * @return
     */
    public int getGroupOuterIn(Long groupId, Date startAt, Date endAt){
        return sqlSession.selectOne(sqlId("getGroupOuterIn"), ImmutableMap.of("groupId", groupId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 具体某个猪群销售
     * @param groupId
     * @param startAt
     * @param endAt
     * @return
     */
    public int getGroupSale(Long groupId, Date startAt, Date endAt){
        return sqlSession.selectOne(sqlId("getGroupSale"), ImmutableMap.of("groupId", groupId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 具体某个猪群死淘
     * @param groupId
     * @param startAt
     * @param endAt
     * @return
     */
    public int getGroupDead(Long groupId, Date startAt, Date endAt){
        return sqlSession.selectOne(sqlId("getGroupDead"), ImmutableMap.of("groupId", groupId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 具体某个猪群死淘
     * @param groupId
     * @param startAt
     * @param endAt
     * @return
     */
    public int getGroupWeedOut(Long groupId, Date startAt, Date endAt){
        return sqlSession.selectOne(sqlId("getGroupWeedOut"), ImmutableMap.of("groupId", groupId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 具体某个猪群其他变动
     * @param groupId
     * @param startAt
     * @param endAt
     * @return
     */
    public int getGroupOtherChange(Long groupId, Date startAt, Date endAt){
        return sqlSession.selectOne(sqlId("getGroupOtherChange"), ImmutableMap.of("groupId", groupId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 具体某个猪群转场
     * @param groupId
     * @param startAt
     * @param endAt
     * @return
     */
    public int getGroupChgFarm(Long groupId, Date startAt, Date endAt){
        return sqlSession.selectOne(sqlId("getGroupChgFarm"), ImmutableMap.of("groupId", groupId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 具体某个猪群内转
     * @param groupId
     * @param startAt
     * @param endAt
     * @return
     */
    public int getGroupInnerOut(Long groupId, Date startAt, Date endAt){
        return sqlSession.selectOne(sqlId("getGroupInnerOut"), ImmutableMap.of("groupId", groupId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 具体某个猪群外传
     * @param groupId
     * @param startAt
     * @param endAt
     * @return
     */
    public int getGroupOuterOut(Long groupId, Date startAt, Date endAt){
        return sqlSession.selectOne(sqlId("getGroupOuterOut"), ImmutableMap.of("groupId", groupId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 具体某个猪群外传
     * @param groupId
     * @param startAt
     * @param endAt
     * @return
     */
    public int getGroupTrunSeed(Long groupId, Date startAt, Date endAt){
        return sqlSession.selectOne(sqlId("getGroupTurnSeed"), ImmutableMap.of("groupId", groupId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 具体某个猪群断奶数量
     * @param groupId
     * @param date
     * @return
     */
    public int getGroupWean(Long groupId, Date date){
        return sqlSession.selectOne(sqlId("getGroupWean"), ImmutableMap.of("groupId", groupId, "date", date));
    }

    /**
     * 具体某个猪群未断奶数量
     * @param groupId
     * @param date
     * @return
     */
    public int getGroupUnWean(Long groupId, Date date){
        return sqlSession.selectOne(sqlId("getGroupUnWean"), ImmutableMap.of("groupId", groupId, "date", date));
    }

    /**
     * 配怀母猪进场
     * @param farmId
     * @param startAt
     * @param endAt
     * @return
     */
    public int getSowPhInFarm(Long farmId, Date startAt, Date endAt){
        return sqlSession.selectOne(sqlId("getSowPhInFarm"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 配怀母猪死亡
     * @param farmId
     * @param startAt
     * @param endAt
     * @return
     */
    public int getSowPhDead(Long farmId, Date startAt, Date endAt){
        return sqlSession.selectOne(sqlId("getSowPhDead"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 配怀母猪淘汰
     * @param farmId
     * @param startAt
     * @param endAt
     * @return
     */
    public int getSowPhWeedOut(Long farmId, Date startAt, Date endAt){
        return sqlSession.selectOne(sqlId("getSowPhWeedOut"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 产房母猪死亡
     * @param farmId
     * @param startAt
     * @param endAt
     * @return
     */
    public int getSowCfDead(Long farmId, Date startAt, Date endAt){
        return sqlSession.selectOne(sqlId("getSowCfDead"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 产房母猪淘汰
     * @param farmId
     * @param startAt
     * @param endAt
     * @return
     */
    public int getSowCfWeedOut(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getSowCfWeedOut"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }
    /**
     * @param barnId
     * @param date
     * @param index
     * @return
     */
    public Integer getBarnChangeCount(Long barnId, Date date, Integer index){
        return sqlSession.selectOne(sqlId("getBarnChangeCount"), ImmutableMap.of("barnId", barnId, "date", date, "index", index));
    }

    /**
     * 获取猪舍存栏
     * @param barnId 猪舍id
     * @return 猪舍存栏
     */
    public Integer getBarnLiveStock(Long barnId){
        return sqlSession.selectOne(sqlId("getBarnLiveStock"), barnId);
    }

    public Integer getOutTrasGroup(Long barnId,Date date, Integer index) {
        return sqlSession.selectOne(sqlId("getOutTrasGroup"), ImmutableMap.of("barnId", barnId, "date", date, "index", index));
    }

    public Integer getPigChgFarm(Long farmId, Integer type, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getPigChgFarm"), ImmutableMap.of("farmId", farmId, "type", type, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 产房母猪销售
     * @param farmId
     * @param startAt
     * @param endAt
     * @return
     */
    public Integer getSowCfSale(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getSowCfSale"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 配怀母猪销售
     * @param farmId
     * @param startAt
     * @param endAt
     * @return
     */
    public Integer getSowPhSale(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getSowPhSale"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 产房母猪其他离场
     * @param farmId
     * @param startAt
     * @param endAt
     * @return
     */
    public Integer getSowCfOtherOut(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getSowCfOtherOut"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 产房母猪转场
     * @param farmId
     * @param startAt
     * @param endAt
     * @return
     */
    public Integer getSowCfChgFarm(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getSowCfChgFarm"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt));
    }

    /**
     * 保育转育肥、转后备
     * @param groupId
     * @param pigType
     * @param startAt
     * @param endAt
     * @return
     */
    public Integer getNurSeryOuterOut(Long groupId, Integer pigType, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getNurseryOuterOutByType"), ImmutableMap.of("groupId", groupId, "pigType", pigType, "startAt", startAt, "endAt", endAt));
    }

    public Double getNurserFeedConversion(Long farmId, Date startAt, Date endAt) {
        return getFeedConversion(farmId, startAt, endAt, PigType.NURSERY_PIGLET.getValue());
    }

    public Double getFattenFeedConversion(Long farmId, Date startAt, Date endAt) {
        return getFeedConversion(farmId, startAt, endAt, PigType.FATTEN_PIG.getValue());
    }

    /**
     * 获取某日的已配种母猪数
     * @param farmId 猪场id
     * @param startAt 日期
     * @return 已配种母猪数
     */
    public Integer getSowMatingCount(Long farmId, Date startAt) {
        return sqlSession.selectOne(sqlId("getSowMatingCount"), ImmutableMap.of("farmId", farmId, "startAt", startAt));
    }

    /**
     * 获取某日的空怀母猪数
     * @param farmId 猪场id
     * @param startAt 日期
     * @return 空怀母猪数
     */
    public Integer getSowKonghuaiCount(Long farmId, Date startAt) {
        return sqlSession.selectOne(sqlId("getSowKonghuaiCount"), ImmutableMap.of("farmId", farmId, "startAt", startAt));
    }

    /**
     * 获取某日的怀孕母猪数
     * @param farmId 猪场id
     * @param startAt 日期
     * @return 怀孕母猪数
     */
    public Integer getSowPregnantCount(Long farmId, Date startAt) {
        return sqlSession.selectOne(sqlId("getSowPregnantCount"), ImmutableMap.of("farmId", farmId, "startAt", startAt));
    }

    /**
     * 获取配怀后备转入
     * @param farmId 猪场id
     * @param startAt 开始时间
     * @param endAt 结束时间
     * @return 后备转入数量
     */
    public Integer getSowPhReserveIn(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getSowPhReserveIn"), ImmutableMap.of("farmId", farmId
                , "startAt", startAt, "endAt", endAt));
    }

    /**
     * 获取转场转入产房
     * @param farmId 猪场id
     * @param startAt 开始时间
     * @param endAt 结束时间
     * @return 转入数量
     */
    public Integer getSowCfInFarmIn(Long farmId, Date startAt, Date endAt) {
        return sqlSession.selectOne(sqlId("getSowCfInFarmIn"), ImmutableMap.of("farmId", farmId
                , "startAt", startAt, "endAt", endAt));
    }

    private Double getFeedConversion(Long farmId, Date startAt, Date endAt, int type) {
        Double feed = sqlSession.selectOne(sqlId("getFeedConsume"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt, "type", type));
        Double weight = sqlSession.selectOne(sqlId("getWeightGain"), ImmutableMap.of("farmId", farmId, "startAt", startAt, "endAt", endAt, "type", type));
        if(Arguments.isNull(feed) || Arguments.isNull(weight) || Objects.equals(0d, weight)){
            return null;
        }
        return feed/weight;
    }

}
