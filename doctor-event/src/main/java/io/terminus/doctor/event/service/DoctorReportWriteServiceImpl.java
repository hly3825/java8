package io.terminus.doctor.event.service;

import com.google.common.base.Stopwatch;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.exception.ServiceException;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.dao.DoctorPigDailyDao;
import io.terminus.doctor.event.dao.DoctorPigEventDao;
import io.terminus.doctor.event.dao.DoctorPigTrackDao;
import io.terminus.doctor.event.dao.DoctorReportNpdDao;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.enums.PregCheckResult;
import io.terminus.doctor.event.enums.ReportTime;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorReportNpd;
import io.terminus.doctor.user.model.DoctorFarm;
import io.terminus.doctor.user.service.DoctorFarmReadService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by sunbo@terminus.io on 2017/12/21.
 */
@Slf4j
@Service
@RpcProvider
public class DoctorReportWriteServiceImpl implements DoctorReportWriteService {

    @Autowired
    private DoctorPigTrackDao doctorPigTrackDao;
    @Autowired
    private DoctorPigEventDao doctorPigEventDao;
    @Autowired
    private DoctorPigReportReadService doctorPigReportReadService;
    @Autowired
    private DoctorPigDailyDao doctorPigDailyDao;
    @Autowired
    private DoctorReportNpdDao doctorReportNpdDao;

    @RpcConsumer
    private DoctorFarmReadService doctorFarmReadService;


    @Override
    public void flushNPD(List<Long> farmIds, Date countDate, ReportTime reportTime) {

        if (reportTime == ReportTime.DAY)
            throw new ServiceException("report.time.day.not.support");

        DoctorPigReportReadService.DateDuration dateDuration = doctorPigReportReadService.getDuration(countDate, reportTime);

        flushNPD(farmIds, dateDuration.getStart(), dateDuration.getEnd());
    }

    @Override
    public void flushNPD(List<Long> farmIds, Date start) {
        Date startAtMonth = DateUtil.monthStart(start);//日期所在月的第一天
        Date end = DateUtil.monthEnd(new Date());


        flushNPD(farmIds, startAtMonth, end);


    }

    public void flushNPD(List<Long> farmIds, Date startDate, Date endDate) {

        Stopwatch stopwatch = Stopwatch.createStarted();

        Map<Long/*farmID*/, Map<String/*year-month*/, Integer/*怀孕天数*/>> farmPregnancy = new ConcurrentHashMap<>();
        Map<Long/*farmID*/, Map<String/*year-month*/, Integer/*哺乳天数*/>> farmLactation = new ConcurrentHashMap<>();
        Map<Long/*farmID*/, Map<String/*year-month*/, Integer/*非生产天数*/>> farmNPD = new ConcurrentHashMap<>();


        Map<String, Object> params = new HashMap<>();
        params.put("farmIds", farmIds);
        params.put("beginDate", startDate);
        params.put("endDate", endDate);
        List<Long> pigs = doctorPigEventDao.findPigAtEvent(startDate, endDate, farmIds);
        pigs.stream().forEach((pigId) -> {

            List<DoctorPigEvent> pigAllEvent = doctorPigEventDao.findByPigId(pigId);

            List<DoctorPigEvent> filterMultiPreCheckEvents = filterMultiPregnancyCheckEvent(pigAllEvent);

            for (int i = 0; i < filterMultiPreCheckEvents.size(); i++) {
                if (i == filterMultiPreCheckEvents.size() - 1)
                    break;

                DoctorPigEvent currentEvent = filterMultiPreCheckEvents.get(i);
                DoctorPigEvent nextEvent = filterMultiPreCheckEvents.get(i + 1);

                if (nextEvent.getType().equals(PigEvent.CHG_FARM.getKey()) || nextEvent.getType().equals(PigEvent.REMOVAL.getKey()))
                    break;


                int days = DateUtil.getDeltaDays(currentEvent.getEventAt(), nextEvent.getEventAt());//天数
                int month = new DateTime(nextEvent.getEventAt()).getMonthOfYear();
                int year = new DateTime(nextEvent.getEventAt()).getYear();

                String yearAndMonthKey = year + "-" + month;

                if (nextEvent.getType().equals(PigEvent.FARROWING.getKey())) {

                    log.debug("猪【{}】的本次事件为【{}】【{}】，下次事件为【{}】【{}】，间隔为【{}】，计入{}月的怀孕期", pigId,
                            PigEvent.from(currentEvent.getType()).getName(),
                            DateUtil.toDateString(currentEvent.getEventAt()),
                            PigEvent.from(nextEvent.getType()).getName(),
                            DateUtil.toDateString(nextEvent.getEventAt()),
                            days,
                            month);

                    if (farmPregnancy.containsKey(nextEvent.getFarmId())) {
                        Map<String, Integer> monthPregnancy = farmPregnancy.get(nextEvent.getFarmId());
                        if (monthPregnancy.containsKey(yearAndMonthKey))
                            monthPregnancy.put(yearAndMonthKey, monthPregnancy.get(yearAndMonthKey) + days);
                        else
                            monthPregnancy.put(yearAndMonthKey, days);
                    } else {
                        Map<String, Integer> monthPregnancy = new HashMap<>();
                        monthPregnancy.put(yearAndMonthKey, days);
                        farmPregnancy.put(nextEvent.getFarmId(), monthPregnancy);
                    }
                } else if (nextEvent.getType().equals(PigEvent.WEAN.getKey())) {

                    log.debug("猪【{}】的本次事件为【{}】【{}】，下次事件为【{}】【{}】，间隔为【{}】，计入{}月的哺乳期", pigId,
                            PigEvent.from(currentEvent.getType()).getName(),
                            DateUtil.toDateString(currentEvent.getEventAt()),
                            PigEvent.from(nextEvent.getType()).getName(),
                            DateUtil.toDateString(nextEvent.getEventAt()),
                            days,
                            month);

                    if (farmLactation.containsKey(nextEvent.getFarmId())) {
                        Map<String, Integer> monthLactation = farmLactation.get(nextEvent.getFarmId());
                        if (monthLactation.containsKey(yearAndMonthKey))
                            monthLactation.put(yearAndMonthKey, monthLactation.get(yearAndMonthKey) + days);
                        else
                            monthLactation.put(yearAndMonthKey, days);
                    } else {
                        Map<String, Integer> monthLactation = new HashMap<>();
                        monthLactation.put(yearAndMonthKey, days);
                        farmLactation.put(nextEvent.getFarmId(), monthLactation);
                    }
                } else {

                    log.debug("猪【{}】的本次事件为【{}】【{}】，下次事件为【{}】【{}】，间隔为【{}】，计入{}月的NPD", pigId,
                            PigEvent.from(currentEvent.getType()).getName(),
                            DateUtil.toDateString(currentEvent.getEventAt()),
                            PigEvent.from(nextEvent.getType()).getName(),
                            DateUtil.toDateString(nextEvent.getEventAt()),
                            days,
                            month);

                    if (farmNPD.containsKey(nextEvent.getFarmId())) {
                        Map<String, Integer> monthNPD = farmNPD.get(nextEvent.getFarmId());
                        if (monthNPD.containsKey(yearAndMonthKey))
                            monthNPD.put(yearAndMonthKey, monthNPD.get(yearAndMonthKey) + days);
                        else
                            monthNPD.put(yearAndMonthKey, days);
                    } else {
                        Map<String, Integer> monthNPD = new HashMap<>();
                        monthNPD.put(yearAndMonthKey, days);
                        farmNPD.put(nextEvent.getFarmId(), monthNPD);
                    }
                }
            }

        });


        Date last = DateUtils.addMonths(endDate, 1);

        farmIds.forEach(f -> {

            DoctorFarm farm = RespHelper.orServEx(doctorFarmReadService.findFarmById(f));

            for (Date i = startDate; i.before(last); i = DateUtils.addMonths(i, 1)) {

                Date monthEndDate = DateUtil.monthEnd(i);

                int dayCount = DateUtil.getDeltaDays(i, monthEndDate) + 1;

                DoctorReportNpd npd = doctorReportNpdDao.findByFarmAndSumAt(f, i).orElseGet(() -> new DoctorReportNpd());
                npd.setFarmId(f);
                npd.setDays(dayCount);

                Integer sowCount = doctorPigDailyDao.countSow(f, i, monthEndDate);
                npd.setSowCount(sowCount);

                npd.setSumAt(i);

                int year = new DateTime(i).getYear();
                int month = new DateTime(i).getMonthOfYear();
                String monthAndYearKey = year + "-" + month;

                if (!farmNPD.containsKey(f))
                    npd.setNpd(0);
                else {
                    Map<String, Integer> monthNPD = farmNPD.get(f);
                    if (!monthNPD.containsKey(monthAndYearKey))
                        npd.setNpd(0);
                    else
                        npd.setNpd(monthNPD.get(monthAndYearKey));
                }
                if (!farmPregnancy.containsKey(f))
                    npd.setPregnancy(0);
                else {
                    Map<String, Integer> monthPregnancy = farmPregnancy.get(f);
                    if (!monthPregnancy.containsKey(monthAndYearKey))
                        npd.setPregnancy(0);
                    else
                        npd.setPregnancy(monthPregnancy.get(monthAndYearKey));
                }
                if (!farmLactation.containsKey(f))
                    npd.setLactation(0);
                else {
                    Map<String, Integer> monthLactation = farmLactation.get(f);
                    if (!monthLactation.containsKey(monthAndYearKey))
                        npd.setLactation(0);
                    else
                        npd.setLactation(monthLactation.get(monthAndYearKey));
                }

                npd.setOrgId(null == farm ? null : farm.getOrgId());
                if (null == npd.getId())
                    doctorReportNpdDao.create(npd);
                else doctorReportNpdDao.update(npd);
            }
        });

        log.debug("use {}", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }


    /**
     * 非生产天数最小以猪场作为维度
     *
     * @param pigIds
     */
    @Deprecated
    public void flushNPD(List<Long> pigIds) {

        Map<Date, Integer> monthlyNPD = new HashMap<>();
        Map<Date, Integer> monthlyPD = new HashMap<>();

        pigIds.forEach(p -> {
            List<DoctorPigEvent> events = doctorPigEventDao.queryAllEventsByPigIdForASC(p);

            //过滤多余的妊娠检查事件
            List<DoctorPigEvent> filterMultiPreCheckEvents = new ArrayList<>();
            for (int i = 0; i < events.size(); i++) {
                if (i == events.size() - 1) {//最后一笔
                    filterMultiPreCheckEvents.add(events.get(i));
                    break;
                }

                DoctorPigEvent nextEvent = events.get(i + 1);
                if (nextEvent.getType().equals(PigEvent.PREG_CHECK.getKey())) {//下一笔还是妊娠检查事件
                    //如果下一笔还是同一个月的
                    if (new DateTime(nextEvent.getEventAt()).getMonthOfYear() ==
                            new DateTime(events.get(i).getEventAt()).getMonthOfYear())
                        continue;//放弃这一笔的妊娠检查事件
                }

                filterMultiPreCheckEvents.add(events.get(i));
            }


            for (int i = 0; i < filterMultiPreCheckEvents.size(); i++) {
                if (i == filterMultiPreCheckEvents.size() - 1)
                    break;

                DoctorPigEvent currentEvent = filterMultiPreCheckEvents.get(i);
                DoctorPigEvent nextEvent = filterMultiPreCheckEvents.get(i + 1);

                if (nextEvent.getType().equals(PigEvent.FARROWING.getKey()) || nextEvent.getType().equals(PigEvent.WEAN)) {
                    if (monthlyPD.containsKey(nextEvent.getEventAt()))
                        monthlyPD.put(nextEvent.getEventAt(), monthlyNPD.get(nextEvent.getEventAt()) + DateUtil.getDeltaDays(currentEvent.getEventAt(), nextEvent.getEventAt()));
                    else
                        monthlyPD.put(nextEvent.getEventAt(), DateUtil.getDeltaDays(currentEvent.getEventAt(), nextEvent.getEventAt()));
                } else {
                    if (monthlyNPD.containsKey(nextEvent.getEventAt()))
                        monthlyNPD.put(nextEvent.getEventAt(), monthlyNPD.get(nextEvent.getEventAt()) + DateUtil.getDeltaDays(currentEvent.getEventAt(), nextEvent.getEventAt()));
                    else
                        monthlyNPD.put(nextEvent.getEventAt(), DateUtil.getDeltaDays(currentEvent.getEventAt(), nextEvent.getEventAt()));

                    if (nextEvent.getType().equals(PigEvent.CHG_FARM.getKey()) || nextEvent.getType().equals(PigEvent.REMOVAL.getKey()))
                        break;
                }

            }
        });

        //

    }


    /**
     * 过滤多余的事件
     * 先按照事件发生日期排序，正序
     * 如果是妊娠检查，结果为阳性事件一律去除
     * 如果是妊娠检查，结果为反情，流产，阴性，一个月内保留最后一个妊娠检查事件，其余去除
     * 如果是体况，疾病，拼窝，仔猪变动，转舍事件，过滤
     * 其他事件类型不影响
     *
     * @return
     */
    public List<DoctorPigEvent> filterMultiPregnancyCheckEvent(List<DoctorPigEvent> pigEvents) {

        List<DoctorPigEvent> sortedByEventDate = pigEvents.stream()
                .sorted((e1, e2) -> e1.getEventAt().compareTo(e2.getEventAt()))
                .filter(e -> !e.getType().equals(PigEvent.CONDITION.getKey())
                        && !e.getType().equals(PigEvent.DISEASE.getKey())
                        && !e.getType().equals(PigEvent.FOSTERS_BY.getKey())
                        && !e.getType().equals(PigEvent.PIGLETS_CHG.getKey())
                        && !e.getType().equals(PigEvent.CHG_LOCATION.getKey())
                        && !e.getType().equals(PigEvent.TO_MATING.getKey())
                        && !e.getType().equals(PigEvent.TO_FARROWING.getKey())
                        && !e.getType().equals(PigEvent.TO_PREG.getKey())
                        && !e.getType().equals(PigEvent.VACCINATION.getKey()))
                .collect(Collectors.toList());
//        List<DoctorPigEvent> sortedByEventDate = pigEvents.stream()
//                .sorted((e1, e2) -> e1.getEventAt().compareTo(e2.getEventAt()))
//                .collect(Collectors.toList());

        List<DoctorPigEvent> filterMultiPreCheckEvents = new ArrayList<>();
        for (int i = 0; i < sortedByEventDate.size(); i++) { //过滤单月内多余的妊娠检查事件
            if (i == sortedByEventDate.size() - 1) {//最后一笔
                DoctorPigEvent lastEvent = sortedByEventDate.get(i);
                if (lastEvent.getType().equals(PigEvent.PREG_CHECK.getKey()) && lastEvent.getPregCheckResult().equals(PregCheckResult.YANG.getKey())) {
                } else {
                    filterMultiPreCheckEvents.add(sortedByEventDate.get(i));
                }
                break;
            }

            DoctorPigEvent currentEvent = sortedByEventDate.get(i);

            if (currentEvent.getType().equals(PigEvent.PREG_CHECK.getKey())) {

                //如果是阳性，过滤
                if (currentEvent.getPregCheckResult().equals(PregCheckResult.YANG.getKey()))
                    continue;


                boolean remove = false;
                for (int j = i + 1; j < sortedByEventDate.size(); j++) {

                    if (sortedByEventDate.get(j).getType().equals(PigEvent.PREG_CHECK.getKey()))//下一笔还是妊娠检查事件
                        //如果下一笔还是同一个月的
                        if (new DateTime(sortedByEventDate.get(j).getEventAt()).getMonthOfYear() ==
                                new DateTime(sortedByEventDate.get(i).getEventAt()).getMonthOfYear()) {
                            remove = true;
                            continue;//放弃这一笔的妊娠检查事件
                        }
                }
                if (remove)
                    continue;

            }

            filterMultiPreCheckEvents.add(currentEvent);
        }
        return Collections.unmodifiableList(filterMultiPreCheckEvents);
    }
}