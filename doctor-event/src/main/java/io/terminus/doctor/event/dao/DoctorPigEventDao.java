package io.terminus.doctor.event.dao;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import io.terminus.common.model.Paging;
import io.terminus.common.mysql.dao.MyBatisDao;
import io.terminus.common.utils.MapBuilder;
import io.terminus.doctor.common.utils.Params;
import io.terminus.doctor.event.dto.DoctorFarmEarlyEventAtDto;
import io.terminus.doctor.event.dto.DoctorNpdExportDto;
import io.terminus.doctor.event.dto.DoctorPigSalesExportDto;
import io.terminus.doctor.event.dto.DoctorProfitExportDto;
import io.terminus.doctor.event.dto.event.DoctorEventOperator;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.model.DoctorPigEvent;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by yaoqijun.
 * Date:2016-04-25
 * Email:yaoqj@terminus.io
 * Descirbe:
 */
@Repository
public class DoctorPigEventDao extends MyBatisDao<DoctorPigEvent> {

    public List<DoctorPigEvent> getabosum(Map<String, Object> criteria){
        return getSqlSession().selectList(sqlId("getabosum"),criteria);
    }

    public List<DoctorPigEvent> getweansum(Map<String, Object> criteria){
        return getSqlSession().selectList(sqlId("getweansum"),criteria);
    }

    public List<DoctorPigEvent> getfosterssum(Map<String, Object> criteria){
        return getSqlSession().selectList(sqlId("getfosterssum"),criteria);
    }

    public List<DoctorPigEvent> getpigletssum(Map<String, Object> criteria){
        return getSqlSession().selectList(sqlId("getpigletssum"),criteria);
    }

    public DoctorPigEvent findByRelGroupEventId(Long relGroupEventId) {
        return getSqlSession().selectOne(sqlId("findByRelGroupEventId"), relGroupEventId);
    }

    public DoctorPigEvent findByRelPigEventId(Long relPigEventId) {
        return getSqlSession().selectOne(sqlId("findByRelPigEventId"), relPigEventId);
    }

    public void deleteByFarmId(Long farmId) {
        getSqlSession().delete(sqlId("deleteByFarmId"), farmId);
    }

    public DoctorPigEvent queryLastPigEventById(Long pigId) {
        return this.getSqlSession().selectOne(sqlId("queryLastPigEventById"), pigId);
    }

    /**
     * ????????????????????????????????????????????????type??????
     */
    public DoctorPigEvent findLastByTypeAndDate(Long pigId, Date eventAt, Integer type) {
        return getSqlSession().selectOne(sqlId("findLastByTypeAndDate"), ImmutableMap.of("pigId", pigId, "eventAt", eventAt, "type", type));
    }


    /**
     * ???????????????????????????
     *
     * @param pigId ???id
     * @return ????????????
     */
    public DoctorPigEvent queryLastManualPigEventById(Long pigId) {
        return this.getSqlSession().selectOne(sqlId("queryLastManualPigEventById"), pigId);
    }

    public DoctorPigEvent queryLastPigEventByPigIds(List<Long> pigIds) {
        return this.getSqlSession().selectOne(sqlId("queryLastPigEventByPigIds"), pigIds);
    }

    /**
     * ????????????????????????????????????
     *
     * @param pigId ???Id
     * @param types ????????????
     * @return
     */
    public DoctorPigEvent queryLastPigEventInWorkflow(Long pigId, List<Integer> types) {
        return this.getSqlSession().selectOne(sqlId("queryLastPigEventInWorkflow"), MapBuilder.<String, Object>of().put("pigId", pigId).put("types", types).map());
    }


    /**
     * ??????????????????,?????????????????????????????????
     *
     * @param pigId
     * @return
     */
    public DoctorPigEvent queryLastFirstMate(Long pigId, Integer parity) {
        return this.getSqlSession().selectOne(sqlId("queryLastFirstMate"), MapBuilder.<String, Object>of().put("pigId", pigId).put("type", PigEvent.MATING.getKey()).put("parity", parity).put("currentMatingCount", 1).map());
    }

    /**
     * ??????????????????,?????????????????????????????????
     *
     * @param pigId
     * @return
     */
    public DoctorPigEvent queryLastFarrowing(Long pigId) {
        return this.getSqlSession().selectOne(sqlId("queryLastEvent"), MapBuilder.<String, Object>of().put("pigId", pigId).put("type", PigEvent.FARROWING.getKey()).map());
    }

    /**
     * ??????????????????,???????????????????????????????????????
     *
     * @param pigId
     * @return
     */
    public DoctorPigEvent queryLastPregCheck(Long pigId) {
        return this.getSqlSession().selectOne(sqlId("queryLastEvent"), MapBuilder.<String, Object>of().put("pigId", pigId).put("type", PigEvent.PREG_CHECK.getKey()).map());
    }

    /**
     * ??????????????????,????????????????????????
     *
     * @param pigId
     * @return
     * @deprecated ????????????????????????????????????????????????????????????????????????????????????
     */
    public DoctorPigEvent queryLastWean(Long pigId) {
        return this.getSqlSession().selectOne(sqlId("queryLastEvent"), MapBuilder.<String, Object>of().put("pigId", pigId).put("type", PigEvent.WEAN.getKey()).map());
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param pigId
     * @param type
     * @return
     */
    public DoctorPigEvent queryLastEventByType(Long pigId, Integer type) {
        return this.getSqlSession().selectOne(sqlId("queryLastEvent"), MapBuilder.<String, Object>of().put("pigId", pigId).put("type", type).map());
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param pigId
     * @return
     */
    public DoctorPigEvent queryLastEnter(Long pigId) {
        return this.getSqlSession().selectOne(sqlId("queryLastEvent"), MapBuilder.<String, Object>of().put("pigId", pigId).put("type", PigEvent.ENTRY.getKey()).map());
    }

    /**
     * ??????PigId ????????? ????????????
     *
     * @param pigId
     * @return
     */
    public List<DoctorPigEvent> queryAllEventsByPigId(Long pigId) {
        return this.getSqlSession().selectList(sqlId("queryAllEventsByPigId"), pigId);
    }

    /**
     * ??????PigId ????????? ???????????? ????????????
     *
     * @param pigId ???id
     * @return ????????????
     */
    public List<DoctorPigEvent> queryAllEventsByPigIdForASC(Long pigId) {
        return this.getSqlSession().selectList(sqlId("queryAllEventsByPigIdForASC"), pigId);
    }

    /**
     * ??????pigId ??????Event??????????????????
     *
     * @param params ?????????????????????
     * @return
     */
    public Boolean updatePigEventFarmIdByPigId(Map<String, Object> params) {
        return this.getSqlSession().update(sqlId("updatePigEventFarmIdByPigId"), params) >= 0;
    }

    public Long countPigEventTypeDuration(Long farmId, Integer eventType, Date startDate, Date endDate) {
        return this.getSqlSession().selectOne(sqlId("countPigEventTypeDuration"),
                ImmutableMap.of("farmId", farmId, "eventType", eventType,
                        "startDate", startDate, "endDate", endDate));
    }

    public List<Long> queryAllFarmInEvent() {
        return this.getSqlSession().selectList(sqlId("queryAllFarmInEvent"));
    }

    /**
     * ????????????id???Kind??????
     *
     * @param farmId ??????id
     * @param kind   ??????(??????, ??????)
     * @return ??????list
     */
    public List<DoctorPigEvent> findByFarmIdAndKind(Long farmId, Integer kind) {
        return getSqlSession().selectList(sqlId("findByFarmIdAndKind"), ImmutableMap.of("farmId", farmId, "kind", kind));
    }

    /**
     * ????????????id???Kind??????
     *
     * @param farmId     ??????id
     * @param kind       ??????(??????, ??????)
     * @param eventTypes ????????????
     * @return ??????list
     */
    public List<DoctorPigEvent> findByFarmIdAndKindAndEventTypes(Long farmId, Integer kind, List<Integer> eventTypes) {
        return getSqlSession().selectList(sqlId("findByFarmIdAndKindAndEventTypes"), MapBuilder.of()
                .put("farmId", farmId)
                .put("kind", kind)
                .put("eventTypes", eventTypes).map()
        );
    }

    /**
     * ?????????relEventId
     *
     * @param pigEvent relEventId
     */
    public void updateRelEventId(DoctorPigEvent pigEvent) {
        getSqlSession().update(sqlId("updateRelEventId"), pigEvent);
    }

    /**
     * ???????????????(?????????????????????)??????????????????
     *
     * @param pigId    ???id, ????????????
     * @param fromDate ?????????
     * @return
     */
    public DoctorPigEvent findFirstPigEvent(Long pigId, Date fromDate) {
        Map<String, Object> param;
        if (fromDate == null) {
            param = ImmutableMap.of("pigId", pigId);
        } else {
            param = ImmutableMap.of("pigId", pigId, "fromDate", fromDate);
        }
        return sqlSession.selectOne(sqlId("findFirstPigEvent"), param);
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param barnId ??????id
     * @return
     */
    public Long countByBarnId(Long barnId) {
        return sqlSession.selectOne(sqlId("countByBarnId"), barnId);
    }

    /**
     * ???????????????????????????????????????, ???????????? eventAt
     *
     * @param beginDate
     * @param endDate
     * @return ??????????????????5000???
     */
    public List<DoctorPigEvent> findByDateRange(Date beginDate, Date endDate) {
        Map<String, Object> param = new HashMap<>();
        param.put("beginDate", beginDate);
        param.put("endDate", endDate);
        return sqlSession.selectList(sqlId("findByDateRange"), ImmutableMap.copyOf(Params.filterNullOrEmpty(param)));
    }

    public Boolean updates(List<DoctorPigEvent> lists) {
        return Boolean.valueOf(sqlSession.update(sqlId("updates"), lists) == 1);
    }

    /**
     * ?????????????????????????????????
     *
     * @param criteria
     * @return
     */
    public List<DoctorEventOperator> findOperators(Map<String, Object> criteria) {
        return getSqlSession().selectList(sqlId("findOperators"), criteria);
    }

    public List<DoctorPigEvent> findByPigId(Long pigId) {
        return sqlSession.selectList(sqlId("findByPigId"), pigId);
    }


    /**
     * ????????????(????????????????????????)
     *
     * @return
     */
    public List<DoctorPigEvent> addWeanEventAfterFosAndPigLets() {
        return sqlSession.selectList(sqlId("addWeanEventAfterFosAndPigLets"));
    }

    /**
     * ?????????????????????
     *
     * @param criteria
     * @return
     */
    public DoctorPigEvent canRollbackEvent(Map<String, Object> criteria) {
        return getSqlSession().selectOne(sqlId("canRollbackEvent"), criteria);
    }

    /**
     * ???????????????????????????????????????
     */
    public List<DoctorPigEvent> findByFarmIdAndTypeAndDate(long farmId, int type, Date startAt, Date endAt) {
        return getSqlSession().selectList(sqlId("findByFarmIdAndTypeAndDate"),
                ImmutableMap.of("farmId", farmId, "type", type, "startAt", startAt, "endAt", endAt));
    }

    /**
     * ????????????
     *
     * @param doctorPigEvent
     */
    @Deprecated
    public void updatePigEvents(DoctorPigEvent doctorPigEvent) {
        sqlSession.update("updatePigEvents", doctorPigEvent);
    }

    public void updatePigCode(Long pigId, String code) {
        sqlSession.update(sqlId("updatePigCode"), ImmutableMap.of("pigId", pigId, "pigCode", code));
    }

    /**
     * ????????????????????????????????????
     *
     * @param pigId
     * @return
     */
    public Map<String, Object> querySowParityAvg(Long pigId) {
        return sqlSession.selectOne("querySowParityAvg", pigId);
    }

    /**
     * ???????????????????????????
     *
     * @param pigId ???id
     * @param limit ????????????
     * @return ???????????????
     */
    public List<DoctorPigEvent> limitPigEventOrderByEventAt(Long pigId, Integer limit) {
        return getSqlSession().selectList(sqlId("limitPigEventOrderByEventAt"), ImmutableMap.of("pigId", pigId, "limit", MoreObjects.firstNonNull(limit, 1)));
    }

    /**
     * ????????????????????????????????????
     *
     * @param groupId ??????id
     * @return ?????????
     */
    public List<DoctorPigEvent> findByGroupId(Long groupId) {
        return getSqlSession().selectList(sqlId("findByGroupId"), ImmutableMap.of("groupId", groupId));
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param pigId   ???id
     * @param eventId ??????id
     * @return ????????????
     */
    public List<DoctorPigEvent> findFollowEvents(Long pigId, Long eventId) {
        return getSqlSession().selectList(sqlId("findFollowEvents"), ImmutableMap.of("eventId", eventId, "pigId", pigId));
    }

    /**
     * ????????????????????????
     *
     * @param ids    ??????id??????
     * @param status ?????????????????????
     */
    public void updateEventsStatus(List<Long> ids, Integer status) {
        getSqlSession().update(sqlId("updateEventsStatus"), ImmutableMap.of("ids", ids, "status", status));
    }

    /**
     * ??????????????????,??????????????????
     *
     * @param id ??????id
     * @return ??????
     */
    public DoctorPigEvent findEventById(Long id) {
        return getSqlSession().selectOne(sqlId("findEventById"), id);
    }

    /**
     * ??????????????????????????????????????????????????????????????????
     */
    public DoctorPigEvent findLastEventExcludeTypes(Long pigId, List<Integer> types) {
        return getSqlSession().selectOne(sqlId("findLastEventExcludeTypes"), ImmutableMap.of("pigId", pigId, "types", types));
    }

    /*
     * ?????????????????????id
     * @param criteria
     * @return
     */
    public List<Long> findPigIdsBy(Map<String, Object> criteria) {
        return getSqlSession().selectList(sqlId("findPigIdsBy"), criteria);
    }

    /**
     * ???????????????
     *
     * @param farmId   ?????????????????????id
     * @param farmName ???????????????
     */
    public void updateFarmName(Long farmId, String farmName) {
        getSqlSession().update(sqlId("updateFarmName"), ImmutableMap.of("farmId", farmId, "farmName", farmName));
    }

    /**
     * ?????????????????????????????????????????????????????????
     *
     * @param excludeIds ???????????????ids
     * @return ???????????????
     */
    public List<DoctorPigEvent> queryWeansWithoutGroupWean(List<Long> excludeIds, Long farmId, Integer offset, Integer limit) {
        return getSqlSession().selectList(sqlId("queryWeansWithoutGroupWean"), ImmutableMap.of("excludeIds", excludeIds, "farmId", farmId, "offset", offset, "limit", limit));
    }

    /**
     * ???????????????????????????????????????
     *
     * @return ??????????????????
     */
    public List<DoctorPigEvent> queryOldAddWeanEvent() {
        return getSqlSession().selectList(sqlId("queryOldAddWeanEvent"));
    }

    /**
     * ??????????????????????????????????????????????????????
     *
     * @return ??????????????????
     */
    public List<DoctorPigEvent> queryTriggerWeanEvent() {
        return getSqlSession().selectList(sqlId("queryTriggerWeanEvent"));
    }

    /**
     * ??????npd?????????????????????????????????
     *
     * @param maps
     * @return
     */
    public Long countNpdWeanEvent(Map<String, Object> maps) {
        return getSqlSession().selectOne(sqlId("countNpdWeanEvent"), maps);
    }

    /**
     * npd?????????
     *
     * @param maps
     * @param offset
     * @param limit
     * @return
     */
    public Paging<DoctorNpdExportDto> sumNpdWeanEvent(Map<String, Object> maps, Integer offset, Integer limit) {
        maps.put("offset", offset);
        maps.put("limit", limit);

        maps = ImmutableMap.copyOf(Params.filterNullOrEmpty(maps));
        long total = countNpdWeanEvent(maps);
        if (total <= 0) {
            return new Paging<>(0L, Collections.<DoctorNpdExportDto>emptyList());
        }
        List<DoctorNpdExportDto> doctorNpdExportDtos = getSqlSession().selectList(sqlId("sumPngWeanEvent"), maps);
        return new Paging<>(total, doctorNpdExportDtos);
    }

    public Paging<Map<String, Object>> sumNpd(Map<String, Object> maps, Integer offset, Integer limit) {
        maps.put("offset", offset);
        maps.put("limit", limit);
        if(maps.get("startDate") != null && maps.get("endDate") != null){
            String aa = maps.get("startDate").toString();
            String bb = maps.get("endDate").toString();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
            Calendar cal = Calendar.getInstance();
            try {
                Date startDate = sdf.parse(aa);
                cal.setTime(startDate);
                int startYear = cal.get(Calendar.YEAR);
                int startMonth = cal.get(Calendar.MONTH) + 1;
                maps.put("startYear", startYear);
                maps.put("startMonth", startMonth);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            try {
                Date endDate = sdf.parse(bb);
                cal.setTime(endDate);
                int endYear = cal.get(Calendar.YEAR);
                int endMonth = cal.get(Calendar.MONTH) + 1;
                maps.put("endYear", endYear);
                maps.put("endMonth", endMonth);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        maps = ImmutableMap.copyOf(Params.filterNullOrEmpty(maps));
        long total = getSqlSession().selectOne(sqlId("totalNpd"),maps);
        if (total <= 0) {
            return new Paging(0L, Collections.<DoctorNpdExportDto>emptyList());
        }
        List<Map<String, Object>> dates = getSqlSession().selectList(sqlId("sumNpd"), maps);
        return new Paging(total, dates);
    }

    public Long countSaleEvent(Map<String, Object> maps) {
        return getSqlSession().selectOne(sqlId("countSales"), maps);
    }

    /**
     * ????????????
     *
     * @param maps
     * @param offset
     * @param limit
     * @return
     */
    public Paging<DoctorPigSalesExportDto> findSalesEvent(Map<String, Object> maps, Integer offset, Integer limit) {
        maps.put("offset", offset);
        maps.put("limit", limit);
        long total = countSaleEvent(maps);
        if (total <= 0) {
            return new Paging<>(0L, Collections.<DoctorPigSalesExportDto>emptyList());
        }
        List<DoctorPigSalesExportDto> doctorPigSalesExportDtos = getSqlSession().selectList(sqlId("findSalesEvent"), maps);
        return new Paging<>(total, doctorPigSalesExportDtos);
    }

    /**
     * ???????????????????????????????????????
     *
     * @param pigId  ???id
     * @param parity ??????
     * @return ????????????
     */
    public DoctorPigEvent getFarrowEventByParity(Long pigId, Integer parity) {
        //???????????????bug?????????????????????????????????????????????????????????
        //?????????????????????????????????????????????????????????????????????????????????????????????
        //?????????????????????????????????????????????????????????????????????????????????????????????
        List<DoctorPigEvent> events = getSqlSession().selectList(sqlId("getFarrowEventByParity"), ImmutableMap.of("pigId", pigId, "parity", parity));

        return events.stream()
                .sorted((p1, p2) -> {
                    int result = p2.getEventAt().compareTo(p1.getEventAt());
                    if (result == 0)
                        return p2.getId().compareTo(p1.getId());
                    else return result;
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * ???????????????????????????????????????
     *
     * @param pigId  ???id
     * @param parity ??????
     * @return ????????????
     */
    public DoctorPigEvent getWeanEventByParity(Long pigId, Integer parity) {
        return getSqlSession().selectOne(sqlId("getWeanEventByParity"), ImmutableMap.of("pigId", pigId, "parity", parity));
    }

    /**
     * ???????????????????????????????????????????????????
     *
     * @param pigId   ???id
     * @param eventAt ??????
     * @return ??????
     */
    public DoctorPigEvent getLastStatusEventBeforeEventAt(Long pigId, Date eventAt) {
        return getSqlSession().selectOne(sqlId("getLastStatusEventBeforeEventAt"), ImmutableMap.of("pigId", pigId, "eventAt", eventAt));
    }

    /**
     * ?????????????????????????????????id?????????????????????????????????
     *
     * @param pigId   ???id
     * @param eventAt ??????
     * @param id      ??????????????????id
     * @return ??????
     */
    public DoctorPigEvent getLastStatusEventBeforeEventAtExcludeId(Long pigId, Date eventAt, Long id) {
        return getSqlSession().selectOne(sqlId("getLastStatusEventBeforeEventAtExcludeId"), ImmutableMap.of("pigId", pigId, "eventAt", eventAt, "id", id));
    }

    /**
     * ?????????????????????????????????id?????????????????????????????????
     *
     * @param pigId   ???id
     * @param eventAt ??????
     * @param id      ??????????????????id
     * @return ??????
     */
    public DoctorPigEvent getLastStatusEventAfterEventAtExcludeId(Long pigId, Date eventAt, Long id) {
        return getSqlSession().selectOne(sqlId("getLastStatusEventAfterEventAtExcludeId"), ImmutableMap.of("pigId", pigId, "eventAt", eventAt, "id", id));
    }

    /**
     * ??????????????????????????????
     *
     * @param pigId   ???id
     * @param eventAt ??????
     * @return ????????????
     */
    public DoctorPigEvent getFirstMateEvent(Long pigId, Date eventAt) {
        return getSqlSession().selectOne(sqlId("getFirstMateEvent"), ImmutableMap.of("pigId", pigId, "eventAt", eventAt));
    }

    /**
     * ???????????????????????????????????????
     * ????????????
     *
     * @param maps
     * @return
     */
    public List<DoctorProfitExportDto> sumProfitPigType(Map<String, Object> maps) {
        maps = Params.filterNullOrEmpty(maps);
        return getSqlSession().selectList(sqlId("sumProFitPigType"), maps);
    }

    /**
     * ??????????????????????????????
     */
    public void deleteByChgFarm(Long pigId) {
        getSqlSession().delete(sqlId("deleteByChgFarm"), pigId);
    }

    /**
     * ??????????????????????????????
     *
     * @param pigId ???id
     * @param id    ????????????id
     * @return ????????????????????????
     */
    public DoctorPigEvent getLastEventBeforeRemove(Long pigId, Long id) {
        return getSqlSession().selectOne(sqlId("getLastEventBeforeRemove"), ImmutableMap.of("pigId", pigId, "id", id));
    }

    /**
     * ???????????????????????????null
     *
     * @param pigEvent ?????????
     * @return ??????????????????
     */
    public Boolean updateIncludeNull(DoctorPigEvent pigEvent) {
        return getSqlSession().update(sqlId("updateIncludeNull"), pigEvent) == 1;
    }

    /**
     * ?????????????????????
     *
     * @param pigId ???id
     * @return ????????????
     */
    public Integer findLastParity(Long pigId) {
        return getSqlSession().selectOne(sqlId("findLastParity"), pigId);
    }

    /**
     * ?????????????????????????????????????????????
     *
     * @param pigId  ???id
     * @param parity ??????
     * @return ????????????
     */
    public Integer findUnWeanCountByParity(Long pigId, Integer parity) {
        return getSqlSession().selectOne(sqlId("findUnWeanCountByParity"), ImmutableMap.of("pigId", pigId, "parity", parity));
    }

    /**
     * ?????????????????????(????????????:3,4,5,8)
     *
     * @param pigId ???id
     * @return ??????
     * @see PigEvent
     */
    public DoctorPigEvent getLastStatusEvent(Long pigId) {
        return getSqlSession().selectOne(sqlId("getLastStatusEvent"), pigId);
    }

    /**
     * ????????????????????????,??????????????????????????????????????????
     *
     * @param pigId  ???id
     * @param parity ??????
     * @param id     ??????????????????id
     * @return ????????????
     */
    public DoctorPigEvent getFirstMatingBeforePregCheck(Long pigId, Integer parity, Long id) {
        return getSqlSession().selectOne(sqlId("getFirstMatingBeforePregCheck"),
                ImmutableMap.of("pigId", pigId, "parity", parity, "id", id));
    }

    public List<DoctorPigEvent> findEffectMatingCountByPigIdForAsc(Long pigId) {
        return sqlSession.selectList(sqlId("findEffectMatingCountByPigIdForAsc"), pigId);
    }


    /**
     * ????????????????????????????????????????????????eventSource=5
     *
     * @param list ??????id??????
     */
    public void flushChgFarmEventSource(List<Long> list) {
        sqlSession.update(sqlId("flushChgFarmEventSource"), list);
    }

    /**
     * ?????????????????????
     *
     * @param list ??????id??????
     * @return ????????????
     */
    public List<DoctorPigEvent> findByFarmIds(List<Long> list) {
        return sqlSession.selectList(sqlId("findByFarmIds"), list);
    }

    /**
     * ?????????????????????????????????
     *
     * @param pigId ??????id
     * @return ???????????????
     */
    public Integer getSowUnweanCount(Long pigId) {
        return sqlSession.selectOne(sqlId("getSowUnweanCount"), pigId);
    }

    public List<DoctorFarmEarlyEventAtDto> getFarmEarlyEventAt(String startDate) {
        return sqlSession.selectList(sqlId("getFarmEarlyEventAt"), startDate);
    }

    /**
     * ???????????????????????????????????????????????????
     *
     * @param pigId  ???id
     * @param status ?????????
     * @return ????????????
     */
    public Date findEventAtLeadToStatus(Long pigId, Integer status) {
        return getSqlSession().selectOne(sqlId("findEventAtLeadToStatus"),
                ImmutableMap.of("pigId", pigId, "status", status));
    }

    /**
     *
     */
    public Date findMateEventToPigId(Long pigId) {
        return getSqlSession().selectOne(sqlId("findMateEventToPigId"),pigId);
    }

    /**
     * ????????????????????????????????????
     *
     * @return
     */
    @Deprecated
    public List<DoctorPigEvent> findAllFarrowNoNestCode() {
        return sqlSession.selectList(sqlId("findAllFarrowNoNestCode"));
    }

    /**
     * ????????????,????????????????????????
     *
     * @return
     */
    @Deprecated
    public void insertNestCode(Long farmId, String begin, String end) {
        sqlSession.update(sqlId("insertNestCode"), ImmutableMap.of("farmId", farmId
                , "begin", begin, "end", end));
    }

    public List<DoctorPigSalesExportDto> findSales(Map<String, Object> map) {
        return sqlSession.selectList(sqlId("findSales"), map);
    }


    /**
     * ????????????????????????NPD?????????
     * ??????????????????
     * ????????????????????????????????????????????????????????????????????????????????????????????????????????????
     * ?????????farmId???pigId???type???preCheckResult???eventAt??????
     *
     * @param farmId
     * @param start
     * @param end
     * @return
     */
    public List<DoctorPigEvent> findForNPD(Long farmId, Date start, Date end) {

        Map<String, Object> params = new HashMap<>();
        params.put("beginDate", start);
        params.put("endDate", end);
        params.put("farmId", farmId);
        return sqlSession.selectList(sqlId("findForNPD"), params);
    }
    /**
     * ??????????????????????????????id
     */
    public List<Map<String, Object>> selectPIG(Map<String, Object> params) {
        return sqlSession.selectList(sqlId("selectPIG"), params);
    }
    /**
     * ??????npd
     * ysq
     */
    public List<Map<String, Object>> selectNPD(Map<String, Object> params) {
        return sqlSession.selectList(sqlId("selectNPD"), params);
    }
    /**
     *??????????????????????????????????????????????????????
     * ysq
     */
    public List<Map<String, Object>> selectEvent(Map<String, Object> params) {
        return sqlSession.selectList(sqlId("selectEvent"), params);
    }
    /**
     * ??????????????????
     * ysq
     */
    public List<Map<String, Object>> selectType(Map<String, Object> params) {
        return sqlSession.selectList(sqlId("selectType"), params);
    }
    /**
     * ????????????????????????
     * ysq
     * @param params
     * @return
     */
    public List<Map<String, Object>> selectLastEndNPD(Map<String, Object> params) {
        return sqlSession.selectList(sqlId("selectLastEndNPD"), params);
    }

    /**
     * ????????????????????????
     * ysq
     */
    public String selectDate(Date time, long farmId, long pigId){
        Map params = new HashMap();
        params.put("time",time);
        params.put("farmId",farmId);
        params.put("pigId",pigId);
        return  sqlSession.selectOne(sqlId("selectDate"), params);
    }
    /**
     * ??????npd????????????
     * ysq
     */
    public List<Map<String, Object>> selectStartNPD(Map<String, Object> params) {
        return sqlSession.selectList(sqlId("selectStartNPD"), params);
    }

    /**
     * ????????????????????????????????????????????????
     *
     * @return
     */
    public List<Long> findPigAtEvent(Date start, Date end, List<Long> farmIds) {

        Map<String, Object> params = new HashMap<>();
        params.put("beginDate", start);
        params.put("endDate", end);
        params.put("farmIds", farmIds);
        params.put("kind", 1);

        return this.sqlSession.selectList(this.sqlId("findPigAt"), params);
    }

    public List<DoctorFarmEarlyEventAtDto> findEarLyAt() {
        return sqlSession.selectList(sqlId("findEarLyAt"));
    }

    /**
     * ???????????????????????????
     *
     * @param pigId
     * @param matingEventAt
     * @return
     */
    public Integer countParity(Long pigId, Date matingEventAt) {

        int basicParity = 0;

        List<DoctorPigEvent> entryEvents = findByPigAndType(pigId, PigEvent.ENTRY);
        if (!entryEvents.isEmpty() && null != entryEvents.get(0).getParity()) {
            basicParity = entryEvents.get(0).getParity();
        }

        return basicParity + countEvent(pigId, matingEventAt, PigEvent.WEAN);
    }

    /**
     * ??????????????????????????????????????????????????????????????????
     *
     * @param pigId
     * @param eventAt
     * @return
     */
    public Integer countEvent(Long pigId, Date eventAt, PigEvent eventType) {

        if (null == pigId || null == eventAt || null == eventAt)
            return 0;

        Map<String, Object> params = new HashMap<>();
        params.put("beforeAt", eventAt);
        params.put("pigId", pigId);
        params.put("type", eventType.getKey());

        return this.sqlSession.selectOne(this.sqlId("countEvent"), params);
    }


    public List<DoctorPigEvent> findByPigAndType(Long pigId, PigEvent eventType) {

        Map<String, Object> params = new HashMap<>();
        params.put("pigId", pigId);
        params.put("type", eventType.getKey());

        return this.sqlSession.selectList(this.sqlId("findByPigAndType"), params);
    }

    /**
     * ??????????????????????????????
     * @param pigId ???id
     * @return ????????????
     */
    public Integer findWeanToMatingCount(Long pigId) {
        return getSqlSession().selectOne(sqlId("findWeanToMatingCount"), pigId);
    }

    public Boolean flushParityAndBeforeStatusAndAfterStatus(List<DoctorPigEvent> list){
        return getSqlSession().update(sqlId("flushParityAndBeforeStatusAndAfterStatus"), list) == 1;
    }

    /**
     * ????????????????????????????????????????????????
     * @param farmId
     * @return
     */
    @Deprecated
    public List<DoctorPigEvent> queryToMatingForTime(Long farmId) {
        return getSqlSession().selectList(sqlId("queryToMatingForTime"), farmId);
    }

    public List<DoctorPigEvent> queryEventsForDescBy(Long pigId, Integer parity) {
        return getSqlSession().selectList(sqlId("queryEventsForDescBy"),
                ImmutableMap.of("pigId", pigId, "parity", parity));
    }


    /**
     * ???????????????????????????
     * @param pigId
     * @param eventId
     * @return
     */
    public List<DoctorPigEvent> queryBeforeChgFarm(Long pigId, Long eventId){
        return getSqlSession().selectList(sqlId("queryBeforeChgFarm"),
                ImmutableMap.of("pigId", pigId, "eventId", eventId));
    }

    /**
     * ???????????????????????????????????????
     * ????????????????????????????????????
     * ????????????????????????????????????
     * ?????????????????????????????????
     * ???????????????:????????????????????????????????????
     * @see io.terminus.doctor.event.enums.PigEvent
     * @param event
     * @return
     */
    public DoctorPigEvent queryBeforeEvent(DoctorPigEvent event){
        Map<String, Object> params = new HashMap<>();
        params.put("id",event.getId());
        params.put("eventAt", event.getEventAt());
        params.put("pigId", event.getPigId());
        params.put("type",event.getType());
        return this.sqlSession.selectOne(this.sqlId("queryBeforeEvent"), params);
    }

    public Long queryEventId(Long pigId){
        return getSqlSession().selectOne(sqlId("queryEventId"), pigId);
    }



    /**
     * ?????????????????????ID
     * @param criteria
     * @return
     */
    public List<Long> findPigIdsByEvent(Map<String, Object> criteria) {
        return getSqlSession().selectList(sqlId("findPigIdsByEvent"), criteria);
    }

    public Date findFarmSowEventAt(Long pigId, Long farmId) {
        return getSqlSession().selectOne(sqlId("findFarmSowEventAt"),
                ImmutableMap.of("pigId", pigId, "farmId", farmId));
    }

    public List<Map<String,Object>> getInFarmPigId(Long farmId, Date time,String pigCode,Integer breed,Date beginInFarmTime, Date endInFarmTime,Integer parity,Integer pigStatus,String operatorName,Long barnId){
        Map<String, Object> map = new HashMap<>();
        map.put("farmId",farmId);
        map.put("time",time);
        map.put("pigCode",pigCode);
        map.put("breed",breed);
        map.put("beginInFarmTime",beginInFarmTime);
        map.put("endInFarmTime",endInFarmTime);
        map.put("parity",parity);
        map.put("pigStatus",pigStatus);
        map.put("operatorName",operatorName);
        map.put("barnId",barnId);
        return this.sqlSession.selectList(this.sqlId("getInFarmPigId"), map);
    }
    public List<Map<String,Object>> getInFarmPigId1(Long farmId, Date time,String pigCode,Integer breed,Date beginInFarmTime, Date endInFarmTime,Integer parity,Integer pigStatus,String operatorName,Long barnId){
        Map<String, Object> map = new HashMap<>();
        map.put("farmId",farmId);
        map.put("time",time);
        map.put("pigCode",pigCode);
        map.put("breed",breed);
        map.put("beginInFarmTime",beginInFarmTime);
        map.put("endInFarmTime",endInFarmTime);
        map.put("parity",parity);
        map.put("pigStatus",pigStatus);
        map.put("operatorName",operatorName);
        map.put("barnId",barnId);
        return this.sqlSession.selectList(this.sqlId("getInFarmPigId1"), map);
    }
    public List<Map<String,Object>> getInFarmPigId2(Long farmId, Date time,String pigCode,Integer breed,Date beginInFarmTime, Date endInFarmTime){
        Map<String, Object> map = new HashMap<>();
        map.put("farmId",farmId);
        map.put("time",time);
        map.put("pigCode",pigCode);
        map.put("breed",breed);
        map.put("beginInFarmTime",beginInFarmTime);
        map.put("endInFarmTime",endInFarmTime);
        return this.sqlSession.selectList(this.sqlId("getInFarmPigId2"), map);
    }
    public List<Map<String,Object>> getInFarmPigId3(Long farmId, Date time,String pigCode,Integer breed,Date beginInFarmTime, Date endInFarmTime){
        Map<String, Object> map = new HashMap<>();
        map.put("farmId",farmId);
        map.put("time",time);
        map.put("pigCode",pigCode);
        map.put("breed",breed);
        map.put("beginInFarmTime",beginInFarmTime);
        map.put("endInFarmTime",endInFarmTime);
        return this.sqlSession.selectList(this.sqlId("getInFarmPigId3"), map);
    }
    public List<Map<String,Object>> getInFarmBoarId(Long farmId,Date queryDate,String pigCode,Integer breedId,Integer pigStatus, Date beginDate,Date endDate) {
        Map<String, Object> map = new HashMap<>();
        map.put("farmId",farmId);
        map.put("queryDate",queryDate);
        map.put("pigCode",pigCode);
        map.put("breedId",breedId);
        map.put("pigStatus",pigStatus);
        map.put("beginDate",beginDate);
        map.put("endDate",endDate);
        return this.sqlSession.selectList(this.sqlId("getInFarmBoarId"), map);
    }
    public List<Map<String,Object>> getInFarmBoarId1(Long farmId,Integer pigType,Date queryDate,Integer barnId,String pigCode,Integer breedId,String staffName, Date beginDate,Date endDate) {
        Map<String, Object> map = new HashMap<>();
        map.put("farmId",farmId);
        map.put("pigType",pigType);
        map.put("queryDate",queryDate);
        map.put("barnId",barnId);
        map.put("pigCode",pigCode);
        map.put("breedId",breedId);
        map.put("staffName",staffName);
        map.put("beginDate",beginDate);
        map.put("endDate",endDate);
        return this.sqlSession.selectList(this.sqlId("getInFarmBoarId1"), map);
    }
    public List<Map<String,Object>> getInFarmBoarId2(Long farmId,Integer pigType,Date queryDate,Integer barnId,String pigCode,Integer breedId,String staffName, Date beginDate,Date endDate) {
        Map<String, Object> map = new HashMap<>();
        map.put("farmId",farmId);
        map.put("pigType",pigType);
        map.put("queryDate",queryDate);
        map.put("barnId",barnId);
        map.put("pigCode",pigCode);
        map.put("breedId",breedId);
        map.put("staffName",staffName);
        map.put("beginDate",beginDate);
        map.put("endDate",endDate);
        return this.sqlSession.selectList(this.sqlId("getInFarmBoarId2"), map);
    }
    public List<Map<String,Object>> getInFarmBoarId3(Long farmId,Date queryDate,Integer barnId,String pigCode,Integer breedId,String staffName, Date beginDate,Date endDate) {
        Map<String, Object> map = new HashMap<>();
        map.put("farmId", farmId);
        map.put("queryDate", queryDate);
        map.put("barnId", barnId);
        map.put("pigCode", pigCode);
        map.put("breedId", breedId);
        map.put("staffName", staffName);
        map.put("beginDate", beginDate);
        map.put("endDate", endDate);
        return this.sqlSession.selectList(this.sqlId("getInFarmBoarId3"), map);
    }
    /* public Integer isOutFarm(BigInteger id, BigInteger pigId, Date eventAt, Long farmId, Date time){
         Map<String, Object> map = new HashMap<>();
         map.put("id",id);
         map.put("pigId",pigId);
         map.put("eventAt",eventAt);
         map.put("farmId",farmId);
         map.put("time",time);
         return this.sqlSession.selectOne(this.sqlId("isOutFarm"), map);
     }*/
    public BigInteger isBarn(BigInteger id, BigInteger pigId, Date eventAt,  Date time){
        Map<String, Object> map = new HashMap<>();
        map.put("id",id);
        map.put("pigId",pigId);
        map.put("eventAt",eventAt);
        map.put("time",time);
        return this.sqlSession.selectOne(this.sqlId("isBarn"), map);
    }
    public Map<String,Object> findBarn(BigInteger isBarn,BigInteger id, BigInteger pigId, Date eventAt, Date time,String operatorName,Long barnId){
        Map<String, Object> map = new HashMap<>();
        map.put("id",id);
        map.put("pigId",pigId);
        map.put("eventAt",eventAt);
        map.put("time",time);
        map.put("operatorName",operatorName);
        map.put("barnId",barnId);
        map.put("isBarn",isBarn);
        return this.sqlSession.selectOne(this.sqlId("findBarn"), map);
    }
    /* public Map<String,Object> findPigInfo(Long pigId){
         Map<String, Object> map = new HashMap<>();
         map.put("pigId",pigId);
         return this.sqlSession.selectOne(this.sqlId("findPigInfo"), map);
     }*/

    /*public Map<String,Object> frontEventId(BigInteger pigId,Date time){
        Map<String, Object> map = new HashMap<>();
        map.put("pigId",pigId);
        map.put("time",time);
        return this.sqlSession.selectOne(this.sqlId("frontEventId"), map);
    }*/
    public Map<String,Object> frontEvent(Integer parity,BigInteger pigId, Date time ,Integer pigStatus){
        Map<String, Object> map = new HashMap<>();
        map.put("pigId",pigId);
        map.put("parity",parity);
        map.put("time",time);
        map.put("pigStatus",pigStatus);
        return this.sqlSession.selectOne(this.sqlId("frontEvent"), map);
    }
    public int getPregCheckResult(Integer parity,BigInteger pigId, Date time ,Integer pigStatus){
        Map<String, Object> map = new HashMap<>();
        map.put("pigId",pigId);
        map.put("parity",parity);
        map.put("time",time);
        map.put("pigStatus",pigStatus);
        return this.sqlSession.selectOne(this.sqlId("getPregCheckResult"), map);
    }
    public Map<String,Object> findBarns(BigInteger pigId,String operatorName,Long barnId){
        Map<String, Object> map = new HashMap<>();
        map.put("pigId",pigId);
        map.put("operatorName",operatorName);
        map.put("barnId",barnId);
        return this.sqlSession.selectOne(this.sqlId("findBarns"), map);
    }

    public List<Map<String,Object>> getdaizaishu(BigInteger pigId,Date time,Date nearDeliverDate){
        Map<String, Object> map = new HashMap<>();
        map.put("pigId",pigId);
        map.put("time",time);
        map.put("nearDeliverDate",nearDeliverDate);
        return this.sqlSession.selectList(this.sqlId("getdaizaishu"), map);
    }
    public Map<String,Object> afterEvent(BigInteger pigId,Date time){
        Map<String, Object> map = new HashMap<>();
        map.put("pigId",pigId);
        map.put("time",time);
        return this.sqlSession.selectOne(this.sqlId("afterEvent"), map);
    }
    public Map<String,Object> nearDeliver(BigInteger pigId,Date time){
        Map<String, Object> map = new HashMap<>();
        map.put("pigId",pigId);
        map.put("time",time);
        return this.sqlSession.selectOne(this.sqlId("nearDeliver"), map);
    }

    public BigInteger isBoarBarn(BigInteger id, BigInteger pigId, Date eventAt, Date queryDate, Long farmId){
        Map<String, Object> map = new HashMap<>();
        map.put("id",id);
        map.put("pigId",pigId);
        map.put("eventAt",eventAt);
        map.put("queryDate",queryDate);
        map.put("farmId",farmId);
        return this.sqlSession.selectOne(this.sqlId("isBoarBarn"), map);
    }

    public Map<String,Object> findBoarBarn(BigInteger isBoarBarn,BigInteger id,BigInteger pigId,Date eventAt,Date queryDate,String staffName,Integer barnId){
        Map<String, Object> map = new HashMap<>();
        map.put("isBoarBarn",isBoarBarn);
        map.put("id",id);
        map.put("pigId",pigId);
        map.put("eventAt",eventAt);
        map.put("queryDate",queryDate);
        map.put("staffName",staffName);
        map.put("barnId",barnId);
        return this.sqlSession.selectOne(this.sqlId("findBoarBarn"), map);
    }
    public Map<String,Object> findBoarBarns(BigInteger pigId,String staffName,Integer barnId){
        Map<String, Object> map = new HashMap<>();
        map.put("pigId",pigId);
        map.put("staffName",staffName);
        map.put("barnId",barnId);
        return this.sqlSession.selectOne(this.sqlId("findBoarBarns"), map);
    }
   public Map<String,Object> findBoarBarn1(BigInteger pigId,String staffName,Integer barnId,Long farmId,Date queryDate){
       Map<String, Object> map = new HashMap<>();
       map.put("pigId",pigId);
       map.put("staffName",staffName);
       map.put("barnId",barnId);
       map.put("farmId", farmId);
       map.put("queryDate", queryDate);
       return this.sqlSession.selectOne(this.sqlId("findBoarBarn1"), map);
   }
    public Integer checkBarn(Long barnId){
        Map<String, Object> map = new HashMap<>();
        map.put("barnId",barnId);
        return this.sqlSession.selectOne(this.sqlId("checkBarn"), map);
    }
    public BigInteger isBoarChgFarm(BigInteger id, BigInteger pigId, Date eventAt, Date queryDate, Long farmId){
        Map<String, Object> map = new HashMap<>();
        map.put("id",id);
        map.put("pigId",pigId);
        map.put("eventAt",eventAt);
        map.put("queryDate",queryDate);
        map.put("farmId",farmId);
        return this.sqlSession.selectOne(this.sqlId("isBoarChgFarm"), map);
    }

    public String findStaffName(Long currentBarnId, String staffName,Integer barnId){
        Map<String, Object> map = new HashMap<>();
        map.put("currentBarnId",currentBarnId);
        map.put("staffName",staffName);
        map.put("barnId",barnId);
        return this.sqlSession.selectOne(this.sqlId("findStaffName"), map);
    }

    // ????????????-???????????????????????????????????????????????????????????????????????????????????????????????? ????????? 2018-09-05???
    public DoctorPigEvent getKongHuaiStatus(Long pigId) {
        return getSqlSession().selectOne(sqlId("getKongHuaiStatus"), pigId);
    }

    public Integer getNpds(int year, int month, long farmId){
        Map<String, Object> map = new HashMap<>();
        map.put("year",year);
        map.put("month",month);
        map.put("farmId",farmId);
        return getSqlSession().selectOne(sqlId("getNpds"), map);
    }

    public Integer getPregnancys(int year, int month, long farmId){
        Map<String, Object> map = new HashMap<>();
        map.put("year",year);
        map.put("month",month);
        map.put("farmId",farmId);
        return getSqlSession().selectOne(sqlId("getPregnancys"), map);
    }
    public Integer getLactations(int year, int month, long farmId){
        Map<String, Object> map = new HashMap<>();
        map.put("year",year);
        map.put("month",month);
        map.put("farmId",farmId);
        return getSqlSession().selectOne(sqlId("getLactations"), map);
    }

    public Map<String,Object> getBranName(Long pigId, Date date) {
        Map<String, Object> map = new HashMap<>();
        map.put("pigId",pigId);
        map.put("date",date);
        return getSqlSession().selectOne(sqlId("getBranName"), map);
    }
}
