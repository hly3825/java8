package io.terminus.doctor.move.manager;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.event.dao.DoctorGroupDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dao.DoctorPigDao;
import io.terminus.doctor.event.dao.DoctorPigEventDao;
import io.terminus.doctor.event.dao.DoctorPigTrackDao;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.move.dto.DoctorImportBasicData;
import io.terminus.doctor.move.dto.DoctorImportGroupEvent;
import io.terminus.doctor.move.dto.DoctorImportPigEvent;
import io.terminus.doctor.move.dto.DoctorMoveBasicData;
import io.terminus.doctor.move.handler.DoctorSourceDataHandler;
import io.terminus.doctor.move.model.View_EventListBoar;
import io.terminus.doctor.move.model.View_EventListGain;
import io.terminus.doctor.move.model.View_EventListSow;
import io.terminus.doctor.move.tools.DoctorImportEventExecutor;
import io.terminus.doctor.move.tools.DoctorImportExcelAnalyzer;
import io.terminus.doctor.move.tools.DoctorImportInputSplitter;
import io.terminus.doctor.move.tools.DoctorMoveEventExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by xjn on 17/8/4.
 * 迁移与导入
 */
@Slf4j
@Component
public class DoctorMoveAndImportManager {

    @Autowired
    private DoctorSourceDataHandler doctorSourceDataHandler;
    @Autowired
    private DoctorPigDao doctorPigDao;
    @Autowired
    private DoctorPigTrackDao doctorPigTrackDao;
    @Autowired
    private DoctorPigEventDao doctorPigEventDao;
    @Autowired
    private DoctorGroupDao doctorGroupDao;
    @Autowired
    private DoctorGroupTrackDao doctorGroupTrackDao;
    @Autowired
    private DoctorGroupEventDao doctorGroupEventDao;
    @Autowired
    private DoctorMoveEventExecutor moveEventExecutor;
    @Autowired
    private DoctorImportEventExecutor importEventExecutor;
    @Autowired
    private DoctorImportExcelAnalyzer importExcelAnalyzer;
    @Autowired
    private DoctorImportInputSplitter importInputSplitter;

    public void movePig(Long moveId, DoctorMoveBasicData moveBasicData) {

        //获取所有猪事件的原始数据
        List<View_EventListSow> sowRawEventList = doctorSourceDataHandler.getAllRawSowEvent(moveId, moveBasicData.getDoctorFarm());
        List<View_EventListBoar> boarRawEventList = doctorSourceDataHandler.getAllRawBoarEvent(moveId, moveBasicData.getDoctorFarm());

        //按猪维度分组
        Map<String, List<View_EventListSow>> sowOutIdToRawEventMap = sowRawEventList.stream()
                .collect(Collectors.groupingBy(View_EventListSow::getPigOutId));
        Map<String, List<View_EventListBoar>> boarOutIdToRawEventMap = boarRawEventList.stream()
                .collect(Collectors.groupingBy(View_EventListBoar::getPigOutId));

        log.info("move sow total:{}, event total:{}", sowOutIdToRawEventMap.keySet().size(), sowRawEventList.size());
        log.info("move boar total:{}, event total:{}", boarOutIdToRawEventMap.keySet().size(), boarRawEventList.size());

        //循环执行事件
        try {
            rollbackPig(moveBasicData.getDoctorFarm().getId());

            boarOutIdToRawEventMap.entrySet().parallelStream().forEach(entry ->
                    moveEventExecutor.executePigEvent(moveBasicData, entry.getValue()));

            Map<String, DoctorPig> boarMap = Maps.newHashMap();
            doctorPigDao.findPigsByFarmIdAndPigType(moveBasicData.getDoctorFarm().getId(), DoctorPig.PigSex.BOAR.getKey())
                    .forEach(boar -> boarMap.put(boar.getPigCode(), boar));
            moveBasicData.setBoarMap(boarMap);

            sowOutIdToRawEventMap.entrySet().parallelStream().forEach(entry ->
                    moveEventExecutor.executePigEvent(moveBasicData, entry.getValue()));
        } catch (Exception e) {
            // TODO: 17/8/8 测试暂时注释
//            rollbackPig(moveBasicData.getDoctorFarm().getId());
            throw e;
        }
    }

    public void moveGroup(Long moveId, DoctorMoveBasicData moveBasicData) {
        List<View_EventListGain> allRawGroupEvent = doctorSourceDataHandler.getAllRawGroupEvent(moveId);
        List<View_EventListGain> newEventList = doctorSourceDataHandler.getAllRawNewGroupEvent(allRawGroupEvent);
        List<View_EventListGain> excludeNewList = doctorSourceDataHandler.getAllRawGroupEventExcludeNew(allRawGroupEvent);

        Map<String, List<View_EventListGain>> groupOutIdToRawEventMap = excludeNewList.stream()
                .collect(Collectors.groupingBy(View_EventListGain::getGroupOutId));

        log.info("move group total:{}, event total:{}", newEventList.size(), allRawGroupEvent.size());

        try {
            rollbackGroupForMove(moveBasicData.getDoctorFarm().getId());

            moveEventExecutor.executeNewGroupEvent(moveBasicData, newEventList);
            List<DoctorGroup> groupList = doctorGroupDao.findByFarmId(moveBasicData.getDoctorFarm().getId());
            Map<String, DoctorGroup> groupMap = groupList.stream().collect(Collectors.toMap(DoctorGroup::getOutId, v -> v));
            moveBasicData.setGroupMap(groupMap);

            groupOutIdToRawEventMap.entrySet().forEach(entry ->
                    moveEventExecutor.executeGroupEvent(moveBasicData, entry.getValue()));
        } catch (Exception e) {
            // TODO: 17/8/8 测试暂时注释
//            rollbackGroup(moveBasicData.getDoctorFarm().getId());
            throw e;
        }
    }

    public void importPig(Sheet boarSheet, Sheet sowSheet, DoctorImportBasicData importBasicData) {

        List<DoctorImportPigEvent> importEventList = importInputSplitter
                .splitForBoar(importExcelAnalyzer.getImportBoar(boarSheet));
        importEventList.addAll(importInputSplitter.splitForSow(importExcelAnalyzer.getImportSow(sowSheet), importBasicData));
        log.info("pig event total:{}", importEventList.size());

        try {
            rollbackPigForImport(importBasicData.getDoctorFarm().getId());
            importEventList.forEach(importPigEvent ->
                    importEventExecutor.executePigEvent(importBasicData, importPigEvent));
        } catch (Exception e) {
            // TODO: 17/8/25 测试暂时注释
//            rollbackPig(moveBasicData.getDoctorFarm().getId());
            throw e;
        }
    }

    public void importGroup(Sheet groupSheet, DoctorImportBasicData importBasicData) {
        List<DoctorImportGroupEvent> importEventList = importInputSplitter
                .splitForGroup(importExcelAnalyzer.getImportGroup(groupSheet));

        log.info("group event total:{}", importEventList.size());

        try {
            rollbackGroupForImport(importBasicData.getDoctorFarm().getId());
            importEventList.forEach(importGroupEvent ->
                    importEventExecutor.executeGroupEvent(importBasicData, importGroupEvent));
        } catch (Exception e) {
            // TODO: 17/8/28 测试暂时注释
//            rollbackGroup(moveBasicData.getDoctorFarm().getId());
            throw e;
        }

    }

    private void rollbackPigForImport(Long farmId) {
        rollbackPig(farmId);
        rollbackGroup(farmId, Lists.newArrayList(PigType.DELIVER_SOW.getValue()));
    }
    private void rollbackPig(Long farmId) {
        //1、删除pig
        doctorPigDao.deleteByFarmId(farmId);

        //2、删除pigTrack
        doctorPigTrackDao.deleteByFarmId(farmId);

        //3、删除pigEvent
        doctorPigEventDao.deleteByFarmId(farmId);
    }

    private void rollbackGroupForMove(Long farmId) {
        //1、删除group
        doctorGroupDao.deleteByFarmId(farmId);

        //2、删除groupTrack // TODO: 17/8/23 暂不删除因为groupTrack表里没有farmId

        //3、删除groupEvent
        doctorGroupEventDao.deleteByFarmId(farmId);
    }

    private void rollbackGroupForImport(Long farmId) {
        List<Integer> includePigTypes  = Lists.newArrayList(PigType.NURSERY_PIGLET.getValue(),
                PigType.FATTEN_PIG.getValue(), PigType.RESERVE.getValue());
        rollbackGroup(farmId, includePigTypes);
    }

    private void rollbackGroup(Long farmId, List<Integer> includePigTypes ) {
        //1、删除group
        doctorGroupDao.deleteByFarmId(farmId, includePigTypes);

        //2、删除groupTrack // TODO: 17/8/23 暂不删除因为groupTrack表里没有farmId

        //3、删除groupEvent
        doctorGroupEventDao.deleteByFarmId(farmId, includePigTypes);
    }
}
