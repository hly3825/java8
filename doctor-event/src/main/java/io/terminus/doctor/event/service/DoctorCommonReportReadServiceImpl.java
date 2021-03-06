package io.terminus.doctor.event.service;

import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.common.utils.BeanMapper;
import io.terminus.common.utils.NumberUtils;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.JsonMapperUtil;
import io.terminus.doctor.event.dao.DoctorBoarMonthlyReportDao;
import io.terminus.doctor.event.dao.DoctorDailyGroupDao;
import io.terminus.doctor.event.dao.DoctorDailyReportDao;
import io.terminus.doctor.event.dao.DoctorKpiDao;
import io.terminus.doctor.event.dao.DoctorParityMonthlyReportDao;
import io.terminus.doctor.event.dao.DoctorRangeReportDao;
import io.terminus.doctor.event.dto.report.common.DoctorCliqueReportDto;
import io.terminus.doctor.event.dto.report.common.DoctorCommonReportDto;
import io.terminus.doctor.event.dto.report.common.DoctorCommonReportTrendDto;
import io.terminus.doctor.event.dto.report.common.DoctorGroupLiveStockDetailDto;
import io.terminus.doctor.event.dto.report.daily.DoctorFarmLiveStockDto;
import io.terminus.doctor.event.enums.ReportRangeType;
import io.terminus.doctor.event.model.DoctorBoarMonthlyReport;
import io.terminus.doctor.event.model.DoctorDailyReport;
import io.terminus.doctor.event.model.DoctorDailyReportSum;
import io.terminus.doctor.event.model.DoctorGroupChangeSum;
import io.terminus.doctor.event.model.DoctorGroupStock;
import io.terminus.doctor.event.model.DoctorParityMonthlyReport;
import io.terminus.doctor.event.model.DoctorRangeReport;
import io.terminus.doctor.event.util.EventUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.terminus.common.utils.Arguments.isNull;
import static io.terminus.common.utils.Arguments.notNull;

/**
 * Desc: ??????????????????????????????
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 2016-08-11
 */
@Slf4j
@Service
@RpcProvider
public class DoctorCommonReportReadServiceImpl implements DoctorCommonReportReadService {

    private static final JsonMapperUtil JSON_MAPPER = JsonMapperUtil.nonEmptyMapper();
    private static final int MONTH_INDEX = 12;
    private static final int WEEK_INDEX = 20;
    private DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM");
    private DateTimeFormatter date = DateTimeFormat.forPattern("yyyy-MM-dd");

    private final DoctorParityMonthlyReportDao doctorParityMonthlyReportDao;
    private final DoctorBoarMonthlyReportDao doctorBoarMonthlyReportDao;
    private final DoctorKpiDao doctorKpiDao;
    private final DoctorRangeReportDao doctorRangeReportDao;
    private final DoctorDailyReportDao doctorDailyReportDao;
    private final DoctorDailyGroupDao doctorDailyGroupDao;

    @Autowired
    public DoctorCommonReportReadServiceImpl(
                                             DoctorParityMonthlyReportDao doctorParityMonthlyReportDao,
                                             DoctorBoarMonthlyReportDao doctorBoarMonthlyReportDao,
                                             DoctorKpiDao doctorKpiDao,
                                             DoctorRangeReportDao doctorRangeReportDao,
                                             DoctorDailyReportDao doctorDailyReportDao,
                                             DoctorDailyGroupDao doctorDailyGroupDao) {
        this.doctorParityMonthlyReportDao = doctorParityMonthlyReportDao;
        this.doctorBoarMonthlyReportDao = doctorBoarMonthlyReportDao;
        this.doctorKpiDao = doctorKpiDao;
        this.doctorRangeReportDao = doctorRangeReportDao;
        this.doctorDailyReportDao = doctorDailyReportDao;
        this.doctorDailyGroupDao = doctorDailyGroupDao;
    }

    @Override
    public Response<DoctorCommonReportTrendDto> findMonthlyReportTrendByFarmIdAndSumAt(Long farmId, String sumAt, Integer index) {
        try {
            Date date;

            //yyyy-MM-dd ??????, ????????????????????????, yyyy-MM ??????, ????????????????????????
            if (DateUtil.isYYYYMMDD(sumAt)) {
                date = DateUtil.toDate(sumAt);
            } else {
                date = getLastDay(DateUtil.toYYYYMM(sumAt));
            }

            String monthStr = DateUtil.getYearMonth(date);

            //???????????????????????????, ??????????????????
            if (new DateTime(date).isAfter(DateUtil.getDateEnd(DateTime.now()))) {
                return Response.ok(failReportTrend(monthStr));
            }

            //??????????????????, ???????????????, ?????????????????????
            DoctorRangeReport report = doctorRangeReportDao.findByRangeReport(farmId, ReportRangeType.MONTH.getValue(), monthStr);
            if (report == null) {
                return Response.ok(failReportTrend(monthStr));
            }


            //???????????????
            return Response.ok(new DoctorCommonReportTrendDto(getDoctorCommonReportDto(report), getMonthlyReportByIndex(farmId, date, index), getParityMonthlyReportByIndex(farmId, date), getBoarMonthlyReportByIndex(farmId, date)));
        } catch (Exception e) {
            log.error("find monthly report by farmId and sumAt failed, farmId:{}, sumAt:{}, cause:{}",
                    farmId, sumAt, Throwables.getStackTraceAsString(e));
            return Response.ok(failReportTrend(DateUtil.getDateStr(new Date())));
        }
    }

    @Override
    public Response<List<DoctorCommonReportTrendDto>> findMonthlyReportTrendByFarmIdAndDuration(Long farmId, String startDate, String endDate) {
        try {
            List<DoctorCommonReportTrendDto> list = Lists.newArrayList();
            DateTime startTime = DateTime.parse(startDate, formatter);
            DateTime endTime = DateTime.parse(endDate, formatter);

            while (!startTime.isAfter(endTime)) {
                String monthStr = startTime.toString(formatter);
                startTime = startTime.plusMonths(1);
                DoctorRangeReport report = doctorRangeReportDao.findByRangeReport(farmId, ReportRangeType.MONTH.getValue(), monthStr);
                if (report == null) {
                    list.add(failReportTrend(monthStr));
                    continue;
                }
                list.add(new DoctorCommonReportTrendDto(getDoctorCommonReportDto(report), null, null, null));
            }
            return Response.ok(list);
        } catch (Exception e) {
            log.error("find monthly report by farmId and duration failed, farmId:{}, startDate:{}, endDate:{}, cause:{}",
                    farmId, startDate, endDate, Throwables.getStackTraceAsString(e));
            return Response.fail("find.monthly.report.by.farmId.and.duration.failed");
        }
    }

    @Override
    public Response<DoctorCommonReportTrendDto> findWeeklyReportTrendByFarmIdAndSumAt(Long farmId, Integer year, Integer week, Integer index) {
        String weekStr = DateUtil.getYearWeek(MoreObjects.firstNonNull(year, DateTime.now().getWeekyear()), MoreObjects.firstNonNull(week, DateTime.now().getWeekOfWeekyear())); //?????????????????????
        log.info("find weekly report, week is : {}", weekStr);
        try {
            //???????????????????????????, ??????????????????
            if (weekStr.compareTo(DateUtil.getYearWeek(new Date())) == 1) {
                return Response.ok(failReportTrend(weekStr));
            }

            DoctorCommonReportDto reportDto;

            //??????????????????, ???????????????, ?????????????????????
            DoctorRangeReport report = doctorRangeReportDao.findByRangeReport(farmId, ReportRangeType.WEEK.getValue(), weekStr);
            if (report == null) {
                return Response.ok(failReportTrend(weekStr));
            }

            DoctorCommonReportTrendDto reportTrendDto = new DoctorCommonReportTrendDto();
            reportTrendDto.setReport(getDoctorCommonReportDto(report));
            reportTrendDto.setReports(getWeeklyReportByIndex(farmId, report.getSumFrom(), index));
            return Response.ok(reportTrendDto);
        } catch (Exception e) {
            log.error("find weekly report by farmId and sumAt failed, farmId:{}, week:{}, cause:{}",
                    farmId, week, Throwables.getStackTraceAsString(e));
            return Response.ok(failReportTrend(weekStr));
        }
    }

    @Override
    public Response<List<DoctorCommonReportTrendDto>> findWeeklyReportTrendByFarmIdAndDuration(Long farmId, Integer year, Integer startWeek, Integer endWeek) {
        try {
            List<DoctorCommonReportTrendDto> list = Lists.newArrayList();
            while (startWeek <= endWeek) {
                String weekStr = DateUtil.getYearWeek(MoreObjects.firstNonNull(year, DateTime.now().getWeekyear()), MoreObjects.firstNonNull(startWeek, DateTime.now().getWeekOfWeekyear())); //?????????????????????
                startWeek++;
                DoctorRangeReport report = doctorRangeReportDao.findByRangeReport(farmId, ReportRangeType.WEEK.getValue(), weekStr);
                if (report == null) {
                    list.add(failReportTrend(weekStr));
                    continue;
                }
                list.add(new DoctorCommonReportTrendDto(getDoctorCommonReportDto(report), null, null, null));
            }
            return Response.ok(list);
        } catch (Exception e) {
            log.error("find weekly report trend by farmId and duration, farmId:{}, year:{}, startWeek:{}, endWeek:{}, cause:{}"
                    , farmId, year, startWeek, endWeek, Throwables.getStackTraceAsString(e));
            return Response.fail("find.weekly.report.trend.farmId.and.duration.failed");
        }
    }

    @Override
    public Response<List<DoctorCommonReportDto>> findMonthlyReports(@NotNull(message = "date.not.null") String sumAt) {
        try {
            //???????????????????????????, ??????????????????
            if (sumAt.substring(0,6).compareTo(DateUtil.getYearMonth(new Date())) == 1) {
                return Response.fail("find.monthly.report.data.failed");
            }
            List<DoctorCommonReportDto> commonReportDtos = Lists.newArrayList();
            List<DoctorRangeReport> reports = doctorRangeReportDao.findFarmBySumAt(ReportRangeType.MONTH.getValue(), sumAt);
            reports.forEach(report -> {
                commonReportDtos.add(getDoctorCommonReportDto(report));
            });
            return Response.ok(commonReportDtos);
        } catch (Exception e) {
            log.error("find.monthly.report.data.failed, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("find.monthly.report.data.failed");
        }
    }


    @Override
    public Response<List<DoctorGroupLiveStockDetailDto>> findEveryGroupInfo(@NotNull(message = "date.not.null") String sumAt) {
        try {
            //???????????????????????????, ??????????????????
            if (new DateTime(DateUtil.toDate(sumAt)).isAfter(DateUtil.getDateEnd(DateTime.now()))) {
                return Response.fail("find every group info failed");
            }
            Map<Integer, String> pigTypeMap = Maps.newHashMap();
            for(PigType pigType : PigType.values()) {
                pigTypeMap.put(pigType.getValue(), pigType.getDesc());
            }
            return Response.ok(doctorKpiDao.getEveryGroupInfo(sumAt).stream().map(map -> {
                DoctorGroupLiveStockDetailDto detailDto = JSON_MAPPER.getMapper().setDateFormat(new SimpleDateFormat("yyyy-MM-dd"))
                        .convertValue(map, DoctorGroupLiveStockDetailDto.class);
                detailDto.setSumAt(DateUtil.toDate(sumAt));
                detailDto.setType(detailDto.getType() != null ? pigTypeMap.get(Integer.parseInt(detailDto.getType())) : null);
                return detailDto;
            }).collect(Collectors.toList()));

        } catch (Exception e) {
            log.error("find.every.group.info.failed, cause:{}", Throwables.getStackTraceAsString(e));
            return Response.fail("find every group info failed");
        }
    }

    private DoctorCommonReportDto getDoctorCommonReportDto(DoctorRangeReport report) {
        DoctorCommonReportDto doctorCommonReportDto = new DoctorCommonReportDto();
        Long farmId = report.getFarmId();
//        Date startAt = getSumAtStart(report.getSumAt(), report.getType());
//        Date endAt = getSumAtEnd(report.getSumAt(), report.getType());
        Date startAt = report.getSumFrom();
        Date endAt = report.getSumTo().after(new Date()) ? new Date() : report.getSumTo();
        doctorCommonReportDto.setFarmId(report.getFarmId());
        doctorCommonReportDto.setDate(report.getSumAt());
        DoctorDailyReportSum dailyReportSum = doctorDailyReportDao.findDailyReportSum(farmId, startAt, endAt);
        DoctorGroupChangeSum groupChangeSum = doctorDailyGroupDao.getGroupChangeSum(farmId, startAt, endAt);
        if (isNull(groupChangeSum)) {
            groupChangeSum = new DoctorGroupChangeSum();
        }

        doctorCommonReportDto.setChangeReport(dailyReportSum);
        doctorCommonReportDto.setGroupChangeReport(groupChangeSum);
        doctorCommonReportDto.setIndicatorReport(report);

        return doctorCommonReportDto;
    }

    private Date getSumAtStart(String sumAt, Integer type) {
        if(Objects.equals(ReportRangeType.WEEK.getValue(), type)){
            Integer year = Integer.valueOf(sumAt.split("-")[0]);
            Integer week = Integer.valueOf(sumAt.split("-")[1]);
            return DateUtil.withWeekOfYear(year, week).minusDays(6).toDate();
        }
        if(Objects.equals(ReportRangeType.MONTH.getValue(), type)){
            return DateUtil.toYYYYMM(sumAt);
        }
        return null;
    }

    private Date getSumAtEnd(String sumAt, Integer type) {
        if(Objects.equals(ReportRangeType.WEEK.getValue(), type)){
            Integer year = Integer.valueOf(sumAt.split("-")[0]);
            Integer week = Integer.valueOf(sumAt.split("-")[1]);
            return DateUtil.withWeekOfYear(year, week).toDate();
        }
        if(Objects.equals(ReportRangeType.MONTH.getValue(), type)){
            return DateUtil.getMonthEnd(new DateTime(DateUtil.toYYYYMM(sumAt))).toDate();
        }
        return null;
    }

    //?????????????????????
    @Override
    public Response<Map<String, Integer>> findBarnLiveStock(Long barnId, Date date, Integer index) {
        try {
            Integer currentLiveStock = doctorKpiDao.getBarnLiveStock(barnId);
            Map<String, Integer> liveStockMap = Maps.newLinkedHashMap();
            liveStockMap.put(DateUtil.toDateString(date), currentLiveStock);
            int i = 1;
            while (i != index) {
                Integer liveStock = currentLiveStock
                        - doctorKpiDao.getBarnChangeCount(barnId, date, i)
                        - doctorKpiDao.getOutTrasGroup(barnId, date, i);
                liveStockMap.put(DateUtil.toDateString(new DateTime(date).minusWeeks(i).toDate()), liveStock);
                i++;
            }
            return Response.ok(liveStockMap);
        }catch (Exception e) {
            log.error("find barn live stock failed, barnId:{}, date:{}, index:{}, cause:{}", barnId, date.toString(), index, Throwables.getStackTraceAsString(e));
            return Response.fail("find.barn.live.stock");
        }
    }

    @Override
    public Response<DoctorFarmLiveStockDto> findFarmCurrentLiveStock(Long farmId) {
        try {
            return Response.ok(buildFarmLiveStockByFarmId(farmId));
        } catch (Exception e) {
            log.error("find farm current live stock failed, farmId:{}, cause:{}",
                    farmId, Throwables.getStackTraceAsString(e));
            return Response.fail("find.farm.current.live.stock.failed");
        }
    }

    @Override
    public Response<List<DoctorFarmLiveStockDto>> findFarmsLiveStock(List<Long> farmIdList) {
        try {
            return Response.ok(farmIdList.stream().map(this::buildFarmLiveStockByFarmId).collect(Collectors.toList()));
        } catch (Exception e) {
            log.error("find farms live stock failed, farmIdList:{}, cause:{}",
                    farmIdList, Throwables.getStackTraceAsString(e));
            return Response.fail("find.farms.live.stock.failed");
        }
    }

    @Override
    public Response<List<DoctorCliqueReportDto>> getTransverseCliqueReport(Long orgId, List<Long> farmIds,
                                                                           Map<Long, String> farmIdToName,
                                                                           String startDate, String endDate) {
        try {
            Date startTime = DateUtil.toDate(startDate);
            Date endTime = DateUtil.toDate(endDate);

            //????????????
            if (endTime.after(new Date())){
                endTime = new Date();
            }
            if (endTime.before(startTime)) {
                return Response.ok(Lists.newArrayList());
            }

            int dayDiff = DateUtil.getDeltaDays(startTime, endTime) + 1;
            List<DoctorCliqueReportDto> list = farmIdToName.keySet().stream().map(farmId -> {
                DoctorCliqueReportDto dto1 = doctorDailyReportDao.getTransverseCliqueReport(farmId, startDate, endDate);
                DoctorCliqueReportDto dto2 = doctorDailyGroupDao.getTransverseCliqueReport(farmId, startDate, endDate);

                //????????????
                fillCommonCliqueReport(dto1, dto2, dayDiff);
                dto1.setFarmName(farmIdToName.get(farmId));

                //????????????
                fillCliqueRangeReport(dto1, farmId, null, DateUtil.getYearMonth(startTime));

                return dto1;
            }).collect(Collectors.toList());

            //??????????????????
            DoctorCliqueReportDto clique = buildCliqueReport(farmIds, new DateTime(startDate), new DateTime(endDate));
            fillCliqueRangeReport(clique, null, orgId, DateUtil.getYearMonth(startTime));
            list.add(clique);

            return Response.ok(list);
        } catch (Exception e) {
            log.error("get transverse clique report failed, farmIdToName:{}, startDate:{}, endDate:{},cause:{}"
                    , farmIdToName, startDate, endDate, Throwables.getStackTraceAsString(e));
            return Response.fail("get.transverse.clique.report.failed");
        }
    }

    @Override
    public Response<List<DoctorCliqueReportDto>> getPortraitCliqueReport(Long orgId, List<Long> farmIds, String startDate, String endDate) {
        try {
            DateTime startTime = DateTime.parse(startDate, date).withDayOfMonth(1);
            DateTime endTime = DateTime.parse(endDate, date).withDayOfMonth(1);

            //????????????????????????
            DateTime monthStartTime = startTime;
            DateTime monthEndTime = DateUtil.getMonthEnd(monthStartTime).isAfter(DateTime.now())
                    ? DateTime.now() : DateUtil.getMonthEnd(monthStartTime);

            List<DoctorCliqueReportDto> list = Lists.newArrayList();
            while (!monthStartTime.isAfter(endTime)) {
                //??????????????????
                DoctorCliqueReportDto clique = buildCliqueReport(farmIds, monthStartTime, monthEndTime);
                fillCliqueRangeReport(clique, null, orgId, DateUtil.getYearMonth(monthStartTime.toDate()));
                list.add(clique);
                //????????????
                monthStartTime = monthStartTime.plusMonths(1);
                monthEndTime = DateUtil.getMonthEnd(monthStartTime).isAfter(DateTime.now())
                        ? DateTime.now() : DateUtil.getMonthEnd(monthStartTime);
            }
            return Response.ok(list);
        } catch (Exception e) {
            log.error("get portrait clique report failed, farmIds:{}, startDate:{}, endDate:{},cause:{}"
                    , farmIds, startDate, endDate, Throwables.getStackTraceAsString(e));
            return Response.fail("get.portrait.clique.report.failed");
        }
    }

    /**
     * ??????????????????(??????id??????????????????????????????,??????id??????????????????????????????)
     * @param dto ????????????
     * @param farmId ??????id
     * @param orgId ??????id
     * @param sumAt ????????????
     * @return ????????????
     */
    private DoctorCliqueReportDto fillCliqueRangeReport(DoctorCliqueReportDto dto, Long farmId, Long orgId, String sumAt) {
        DoctorRangeReport rangeReport;
        if (notNull(farmId)) {
            rangeReport = doctorRangeReportDao.findByRangeReport(farmId, ReportRangeType.MONTH.getValue(), sumAt);
        } else {
            rangeReport = doctorRangeReportDao.findByOrgIdAndSumAt(orgId, sumAt);
        }

        BeanMapper.copy(rangeReport, dto);
        dto.setLiveFarrowRate(EventUtil.minusDouble(1D, rangeReport.getDeadFarrowRate()));
        dto.setLiveFattenRate(EventUtil.minusDouble(1D, rangeReport.getDeadFattenRate()));
        dto.setLiveNurseryRate(EventUtil.minusDouble(1D, rangeReport.getDeadNurseryRate()));
        return dto;
    }

    /**
     * ??????????????????????????????????????????????????????
     * @param farmIds ??????ids
     * @param monthStartTime ??????
     * @param monthEndTime ??????
     * @return ????????????
     */
    private DoctorCliqueReportDto buildCliqueReport(List<Long> farmIds, DateTime monthStartTime, DateTime monthEndTime) {
        String monthStartStr = monthStartTime.toString(date);
        String monthEndStr = monthEndTime.toString(date);
        int dayDiff = DateUtil.getDeltaDays(monthStartTime.toDate(), monthEndTime.toDate()) + 1;
        DoctorCliqueReportDto dto1 = doctorDailyReportDao.getPortraitCliqueReport(farmIds, monthStartStr, monthEndStr);
        DoctorCliqueReportDto dto2 = doctorDailyGroupDao.getPortraitCliqueReport(farmIds, monthStartStr, monthEndStr);
        fillCommonCliqueReport(dto1, dto2, dayDiff);
        dto1.setMonth(DateUtil.getYearMonth(monthStartTime.toDate()));
        return dto1;
    }

    /**
     * ?????????????????????????????????
     * @param dto1 ?????????
     * @param dto2 ????????????
     * @param dayDiff ?????????
     * @return ????????????
     */
    private DoctorCliqueReportDto fillCommonCliqueReport(DoctorCliqueReportDto dto1, DoctorCliqueReportDto dto2, Integer dayDiff) {
        if (isNull(dto1)) {
            return new DoctorCliqueReportDto();
        }
        //????????????
        dto1.setMateCount(dto1.getMateHb() + dto1.getMateDn()
                + dto1.getMateFq() + dto1.getMateLc() + dto1.getMateYx());

        //????????????
        dto1.setPregCount(dto1.getPregPositive() + dto1.getPregNegative()
                + dto1.getPregFanqing() + dto1.getPregLiuchan());
        dto1.setAvgSowLiveStock(Integer.parseInt(NumberUtils.divide(dto1.getAvgSowLiveStock(), dayDiff, 0)));

        //????????????
        if (isNull(dto1.getFarrowNest()) || dto1.getFarrowNest() == 0) {
            dto1.setFarrowNest(0);
            dto1.setAvgFarrowLive(0.00D);
            dto1.setAvgFarrowHealth(0.00D);
            dto1.setAvgFarrowWeak(0.00D);
        } else {
            dto1.setAvgFarrowLive(get2(dto1.getFarrowLive(), dto1.getFarrowNest()));
            dto1.setAvgFarrowHealth(get2(dto1.getFarrowHealth(), dto1.getFarrowNest()));
            dto1.setAvgFarrowWeak(get2(dto1.getFarrowWeak(), dto1.getFarrowNest()));
        }

        //????????????
        if (isNull(dto1.getWeanNest()) || dto1.getWeanNest() == 0) {
            dto1.setWeanNest(0);
            dto1.setNestAvgWean(0.00D);
        } else {
            dto1.setNestAvgWean(get2(dto1.getWeanCount(), dto1.getWeanNest()));
        }

        //??????
        if (notNull(dto2)) {
            dto1.setHpSale(dto2.getHpSale());
            dto1.setCfSale(dto2.getCfSale());
            dto1.setYfSale(dto2.getYfSale());
        }
        return dto1;
    }

    /**
     * ??????????????????????????????
     * @param total ??????
     * @param quantity ?????????
     * @return
     */
    private Double get2(Integer total, Integer quantity) {
        return Double.parseDouble(NumberUtils.divide(total, quantity, 2));
    }

    /**
     * ??????????????????????????????
     * @param total ??????
     * @param quantity ?????????
     * @return
     */
    private Double get2(Double total, Integer quantity) {
        return Double.parseDouble(String.format("%.2f", total / quantity));
    }

    /**
     * ????????????id????????????
     * @param farmId ??????id
     * @return ????????????
     */
    private DoctorFarmLiveStockDto buildFarmLiveStockByFarmId(Long farmId) {
        DoctorDailyReport dailyReport = doctorDailyReportDao.findByFarmIdAndSumAt(farmId, new Date());
        DoctorGroupStock groupStock = doctorDailyGroupDao.getGroupStock(farmId, new Date());
        if (isNull(groupStock)) {
            groupStock = new DoctorGroupStock();
        }
        return DoctorFarmLiveStockDto.builder()
                .farmId(farmId)
                .boar(dailyReport.getBoarEnd())
                .sow(dailyReport.getSowEnd())
                .farrow(MoreObjects.firstNonNull(groupStock.getFarrowEnd(), 0))
                .fatten(MoreObjects.firstNonNull(groupStock.getFattenEnd(), 0))
                .houbei(MoreObjects.firstNonNull(groupStock.getHoubeiEnd(), 0))
                .nursery(MoreObjects.firstNonNull(groupStock.getNurseryEnd(),0))
                .build();
    }

    //???????????????
    private List<DoctorCommonReportDto> getMonthlyReportByIndex(Long farmId, Date date, Integer index) {
        return DateUtil.getBeforeMonthEnds(date, MoreObjects.firstNonNull(index, MONTH_INDEX)).stream()
                .map(month -> {
                    String sumAt = DateUtil.getYearMonth(month);

                    if (DateTime.now().getDayOfMonth() == 1 && DateUtil.inSameYearMonth(month, new Date())) {
                        DoctorCommonReportDto reportDto = new DoctorCommonReportDto();
                        reportDto.setDate(sumAt);
                        return reportDto;
                    }
                    DoctorRangeReport report = doctorRangeReportDao.findByRangeReport(farmId, ReportRangeType.MONTH.getValue(), sumAt);
                    if (report == null) {
                        return failReportDto(sumAt);
                    }
                    DoctorCommonReportDto dto = getDoctorCommonReportDto(report);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    //??????????????????
    private List<DoctorCommonReportDto> getWeeklyReportByIndex(Long farmId, Date date, Integer index) {
        return DateUtil.getBeforeWeekEnds(date, MoreObjects.firstNonNull(index, WEEK_INDEX)).stream()
                .map(week -> {
                    String sumAt = DateUtil.getYearWeek(week);
                    DoctorRangeReport report = doctorRangeReportDao.findByRangeReport(farmId, ReportRangeType.WEEK.getValue(), sumAt);
                    if (report == null) {
                        return failReportDto(sumAt);
                    }
                    DoctorCommonReportDto dto = getDoctorCommonReportDto(report);
                    return dto;
                })
                .collect(Collectors.toList());
    }


    private List<DoctorParityMonthlyReport> getParityMonthlyReportByIndex(Long farmId, Date date){
        return doctorParityMonthlyReportDao.findDoctorParityMonthlyReports(farmId, new DateTime(date).toString(DateUtil.YYYYMM));
    }

    private List<DoctorBoarMonthlyReport> getBoarMonthlyReportByIndex(Long farmId, Date date){
        return doctorBoarMonthlyReportDao.findDoctorBoarMonthlyReports(farmId, new DateTime(date).toString(DateUtil.YYYYMM));
    }

    //?????????????????????
    private static DoctorCommonReportDto failReportDto(String date) {
        DoctorCommonReportDto dto = new DoctorCommonReportDto();
        dto.setFail(true);
        dto.setDate(date);
        return dto;
    }

    //?????????????????????
    private static DoctorCommonReportTrendDto failReportTrend(String date) {
        return new DoctorCommonReportTrendDto(failReportDto(date), Lists.newArrayList(), Lists.newArrayList(), Lists.newArrayList());
    }

    //????????????
    private static Date getLastDay(Date date) {
        DateTime datetime = new DateTime(date);
        DateTime now = DateTime.now();
        //??????
        if (DateUtil.inSameYearMonth(datetime.toDate(), now.toDate())) {
            return now.withTimeAtStartOfDay().toDate();
        }
        return datetime.withDayOfMonth(1).plusMonths(1).plusDays(-1).toDate();
    }

    private static String getWeekStr(DateTime date) {
        return "???" + date.getWeekOfWeekyear() + "???(" + date.toString(DateUtil.DATE) + ")";
    }

}
