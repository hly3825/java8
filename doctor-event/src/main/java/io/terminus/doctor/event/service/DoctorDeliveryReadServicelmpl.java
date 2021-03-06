package io.terminus.doctor.event.service;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.JsonMapperUtil;
import io.terminus.doctor.event.dao.DoctorBarnDao;
import io.terminus.doctor.event.dao.DoctorChgFarmInfoDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorPigEventDao;
import io.terminus.doctor.event.dao.reportBi.DoctorReportDeliverDao;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.enums.PigSource;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.doctor.event.model.DoctorChgFarmInfo;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.model.DoctorPigTrack;
import io.terminus.doctor.event.util.EventUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@Slf4j
@Service
@RpcProvider
public class DoctorDeliveryReadServicelmpl implements DoctorDeliveryReadService{

    private final DoctorReportDeliverDao doctorReportDeliverDao;
    private final DoctorPigEventDao doctorPigEventDao;
    private final DoctorGroupEventDao doctorGroupEventDao;
    private final DoctorChgFarmInfoDao doctorChgFarmInfoDao;
    private final DoctorBarnDao doctorBarnDao;
    private static final JsonMapperUtil JSON_MAPPER = JsonMapperUtil.JSON_NON_DEFAULT_MAPPER;

    @Autowired
    public DoctorDeliveryReadServicelmpl(DoctorReportDeliverDao doctorReportDeliverDao, DoctorPigEventDao doctorPigEventDao, DoctorGroupEventDao doctorGroupEventDao, DoctorBarnDao doctorBarnDao,
                                         DoctorChgFarmInfoDao doctorChgFarmInfoDao) {
        this.doctorReportDeliverDao = doctorReportDeliverDao;
        this.doctorPigEventDao = doctorPigEventDao;
        this.doctorGroupEventDao = doctorGroupEventDao;
        this.doctorBarnDao = doctorBarnDao;
        this.doctorChgFarmInfoDao = doctorChgFarmInfoDao;
    }

    @Override
    public Map<String,Object> getMating(Long farmId, Date beginDate, Date endDate,String pigCode,String operatorName,int isdelivery){
        List<Map<String,Object>> matingList = doctorReportDeliverDao.getMating(farmId, beginDate, endDate,pigCode,operatorName);
        List<Map<String,Object>> delivery = new ArrayList<>(); //?????????
        List<Map<String,Object>> nodeliver = new ArrayList<>();//?????????
        int deliverycount = 0;//?????????
        int fqcount = 0;//?????????
        int lccount = 0;//?????????
        int yxcount = 0;//?????????
        int swcount = 0;//?????????
        int ttcount = 0;//?????????
        for(int i = 0; i<matingList.size(); i++){
            Map map = matingList.get(i);
            String a = String.valueOf(map.get("pig_status"));
            if(a.equals(String.valueOf(PigStatus.Entry.getKey()))){
                map.put("pig_status",PigStatus.Entry.getName());
            }
            if(a.equals(String.valueOf(PigStatus.Removal.getKey()))){
                map.put("pig_status",PigStatus.Removal.getName());
            }
            if(a.equals(String.valueOf(PigStatus.Mate.getKey()))){
                map.put("pig_status",PigStatus.Mate.getName());
            }
            if(a.equals(String.valueOf(PigStatus.Pregnancy.getKey()))){
                map.put("pig_status",PigStatus.Pregnancy.getName());
            }
            if(a.equals(String.valueOf(PigStatus.KongHuai.getKey()))){
                map.put("pig_status",PigStatus.KongHuai.getName());
            }
            if(a.equals(String.valueOf(PigStatus.Farrow.getKey()))){
                map.put("pig_status",PigStatus.Farrow.getName());
            }
            if(a.equals(String.valueOf(PigStatus.FEED.getKey()))){
                map.put("pig_status",PigStatus.FEED.getName());
            }
            if(a.equals(String.valueOf(PigStatus.Wean.getKey()))){
                map.put("pig_status",PigStatus.Wean.getName());
            }
            if(a.equals(String.valueOf(PigStatus.CHG_FARM.getKey()))){
                map.put("pig_status",PigStatus.CHG_FARM.getName());
            }
            BigInteger id = (BigInteger)map.get("id");
            BigInteger pig_id = (BigInteger)map.get("pig_id");
            Map<String,Object> farmId1 = doctorReportDeliverDao.getFarmId(pig_id);
            Long farmId2 = Long.valueOf(String.valueOf(farmId1.get("farm_id"))).longValue();
            if(!farmId2.equals(farmId)){
                map.put("pig_status","?????????");
            }
            int parity = (int)map.get("parity");

            Map<String,Object> matingCount =  doctorReportDeliverDao.getMatingCount(pig_id,(Date)map.get("event_at"));
            if(matingCount != null){
                map.put("current_mating_count",matingCount.get("current_mating_count"));
            }
            List<Map<String,Object>> deliveryBarn = doctorReportDeliverDao.deliveryBarn(id,pig_id);//??????????????????????????????????????????
            if(deliveryBarn != null) {
                if (deliveryBarn.size() != 0) {
                    map.put("deliveryFarm", (String) deliveryBarn.get(0).get("farm_name"));
                    map.put("deliveryBarn", (String) deliveryBarn.get(0).get("barn_name"));
                    map.put("deliveryDate", (Date) deliveryBarn.get(0).get("event_at"));
                    map.put("notdelivery", "??????");
                    map.put("deadorescape", "");
                    map.put("check_event_at", "");
                    map.put("leave_event_at", "");
                    delivery.add(map);
                    deliverycount = deliverycount + 1;
                } else {
                    map.put("deliveryBarn", "?????????");
                    map.put("deliveryDate", "");
                    map.put("deliveryFarm", "?????????");

                    Map<String,Object> idsameparity = doctorReportDeliverDao.idsameparity(id,pig_id, parity);//??????????????????????????????????????????
                    //????????????????????????????????????????????????????????????
                    BigInteger id1 = null;
                    if(idsameparity != null){
                        id1 = (BigInteger)idsameparity.get("id");
                    }
                    //????????????????????????
                    Map<String,Object> notdelivery = doctorReportDeliverDao.notdelivery(id,pig_id, parity,id1);
                    if(notdelivery != null) {
                        int b = (int) notdelivery.get("preg_check_result");
                        if (b == 1) {
                            map.put("notdelivery", "??????");
                        }
                        if (b == 2) {
                            map.put("notdelivery", "??????");
                            map.put("pig_status","??????");
                            yxcount = yxcount + 1;
                        }
                        if (b == 3) {
                            map.put("notdelivery", "??????");
                            map.put("pig_status","??????");
                            lccount = lccount + 1;
                        }
                        if (b == 4) {
                            map.put("notdelivery", "??????");
                            map.put("pig_status","??????");
                            fqcount = fqcount + 1;
                        }
                        map.put("check_event_at",notdelivery.get("event_at"));
                    }else{
                        map.put("notdelivery", "");
                        map.put("check_event_at", "");
                    }
                    //?????????
                    Map<String,Object> leave = doctorReportDeliverDao.leave(id,pig_id, parity,id1);
                    if(leave != null) {
                        long b = (long) leave.get("change_type_id");
                        if (b == 110) {
                            map.put("deadorescape", "??????");
                            map.put("pig_status","?????????");
                            swcount = swcount + 1;
                        }else if (b == 111) {
                            map.put("deadorescape", "??????");
                            map.put("pig_status","?????????");
                            ttcount = ttcount + 1;
                        }else{
                            map.put("deadorescape", "");
                        }
                        map.put("leave_event_at",leave.get("event_at"));
                    }else{
                        map.put("deadorescape", "");
                        map.put("leave_event_at", "");
                    }
                    nodeliver.add(map);
                }
            }
        }
        int matingcount = 0;
        String deliveryrate = "0";
        String fqrate = "0";
        String lcrate = "0";
        String yxrate = "0";
        String swrate = "0";
        String ttrate = "0";
        if(matingList.size()!=0) {
            matingcount = matingList.size();
            deliveryrate = divide(deliverycount, matingcount);//?????????
            fqrate = divide(fqcount, matingcount);//?????????
            lcrate = divide(lccount, matingcount);//?????????
            yxrate = divide(yxcount, matingcount);//?????????
            swrate = divide(swcount, matingcount);//?????????
            ttrate = divide(ttcount, matingcount);//?????????
        }

        Map<String,Object> list = new HashMap<>();
        list.put("matingcount",matingcount);
        list.put("deliverycount",deliverycount);
        list.put("fqcount",fqcount);
        list.put("lccount",lccount);
        list.put("yxcount",yxcount);
        list.put("swcount",swcount);
        list.put("ttcount",ttcount);
        list.put("deliveryrate",deliveryrate);
        list.put("fqrate",fqrate);
        list.put("lcrate",lcrate);
        list.put("yxrate",yxrate);
        list.put("swrate",swrate);
        list.put("ttrate",ttrate);
        if(isdelivery == 1){
            list.put("data",delivery);
        }else if(isdelivery == 2){
            list.put("data",nodeliver);
        }else{
            list.put("data",matingList);
        }
            return list;
    }
    private String divide(int i,int j){
        double k = (double)i/j*100;
        BigDecimal big   =   new  BigDecimal(k);
        String  l = big.setScale(2,BigDecimal.ROUND_HALF_UP).doubleValue() +"%";
        return l;
    }

    /*@Override
    public List<Map<String,Object>> sowsReport(Long farmId,Date time,String pigCode,String operatorName,Long barnId,Integer breed,Integer parity,Integer pigStatus,Date beginInFarmTime, Date endInFarmTime, Integer sowsStatus){
        List<Map<String,Object>> inFarmPigId = null;
//        if(pigStatus == 0){//??????
        if(sowsStatus == 0) {
            inFarmPigId = doctorPigEventDao.getInFarmPigId(farmId, time, pigCode, breed, beginInFarmTime, endInFarmTime);//????????????????????????????????????????????????????????????
        } else{
            inFarmPigId = doctorPigEventDao.getInFarmPigId3(farmId, time, pigCode, breed, beginInFarmTime, endInFarmTime);//????????????????????????????????????????????????????????????
        }
// }else if(pigStatus == 2){//??????
//            inFarmPigId = doctorPigEventDao.getInFarmPigId2(farmId,time,pigCode,breed, beginInFarmTime,endInFarmTime);//????????????????????????????????????????????????????????????
//        } else if(pigStatus == 10){//??????
//            inFarmPigId = doctorPigEventDao.getInFarmPigId3(farmId,time,pigCode,breed, beginInFarmTime,endInFarmTime);//????????????????????????????????????????????????????????????
//        }else {
//            inFarmPigId = doctorPigEventDao.getInFarmPigId(farmId, time, pigCode, breed, beginInFarmTime, endInFarmTime);//????????????????????????????????????????????????????????????
//        }
        *//*boolean f = true;
        boolean g = true;
        boolean h = true;
        List<Map<String,Object>> j = new ArrayList<>();*//*
        for(Iterator<Map<String,Object>> it = inFarmPigId.iterator();it.hasNext();){
            Map map = it.next();
            int source = (int)map.get("source");
            if (source == 1) {
                map.put("source","??????");
            }else if (source == 2) {
                map.put("source","??????");
            }else{
                map.put("source","");
            }
            BigInteger id = (BigInteger)map.get("id");
            BigInteger pigId = (BigInteger)map.get("pig_id");
            Date eventAt = (Date)map.get("event_at");
            //BigInteger isBarn = doctorPigEventDao.isBarn(id,pigId,eventAt,time);//???????????????????????????
            Map<String,Object> afterEvent = doctorPigEventDao.afterEvent(pigId,time);//??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            BigInteger afterEventFarmId = null;
            if(afterEvent != null) {
                afterEventFarmId = (BigInteger) afterEvent.get("farm_id");
           // }
            //if(isBarn != null) {
                Map<String,Object> currentBarn = doctorPigEventDao.findBarn((BigInteger)afterEvent.get("id"),id,pigId,eventAt,time,operatorName,barnId);//???????????????????????????,????????????????????????
                if(currentBarn != null) {
                    map.put("current_barn_name", currentBarn.get("barn_name"));
                        map.put("staff_name", currentBarn.get("staff_name"));//?????????
                } else{
                    it.remove();
                    continue;
                    //f = false;
                }
            }else{
                Map<String,Object> currentBarns = doctorPigEventDao.findBarns(pigId,operatorName,barnId);//?????????????????????
                afterEventFarmId = (BigInteger)doctorPigEventDao.findBarns(pigId,null,null).get("farm_id");
                if(currentBarns != null) {
                    map.put("current_barn_name", currentBarns.get("current_barn_name"));
                        map.put("staff_name", currentBarns.get("staff_name"));//?????????
                } else{
                    it.remove();
                    continue;
                    //g = false;
                }
            }

                Map<String,Object> frontEvent = doctorPigEventDao.frontEvent(parity,pigId,time,pigStatus);
                if(frontEvent != null) {
                    map.put("parity", frontEvent.get("parity"));//????????????
                    String Status = null;

                    if(afterEventFarmId == null || afterEventFarmId.equals(new BigInteger(farmId.toString()))) {
                        int status = (int) frontEvent.get("pig_status_after");

                        if (status == PigStatus.Entry.getKey()) {
                            Status = PigStatus.Entry.getName();
                        }
                        if (status == PigStatus.Removal.getKey()) {
                            Status = PigStatus.Removal.getName();
                        }
                        if (status == PigStatus.Mate.getKey()) {
                            Status = PigStatus.Mate.getName();
                        }
                        if (status == PigStatus.Pregnancy.getKey()) {
                            Status = PigStatus.Pregnancy.getName();
                        }
                        if (status == PigStatus.KongHuai.getKey()) {
                            int pregCheckResult = doctorPigEventDao.getPregCheckResult(parity,pigId,time,pigStatus);

                                //doctorPigEventDao.getPregCheckResult(parity,pigId,time,pigStatus)

                                if (pregCheckResult == 2) {
                                    Status = "??????";
                                } else if (pregCheckResult == 3) {
                                    Status = "??????";
                                } else if (pregCheckResult == 4) {
                                    Status = "??????";
                                } else {
                                    Status = PigStatus.KongHuai.getName();
                                }
                        }
                        if (status == PigStatus.Farrow.getKey()) {
                            Status = PigStatus.Farrow.getName();
                        }
                        if (status == PigStatus.FEED.getKey()) {
                            Status = PigStatus.FEED.getName();
                        }
                        if (status == PigStatus.Wean.getKey()) {
                            Status = PigStatus.Wean.getName();
                        }
                        if (status == PigStatus.CHG_FARM.getKey()) {
                            Status = PigStatus.CHG_FARM.getName();
                        }
                    } else {
                        Status = "??????";
                    }
                    map.put("status", Status);//????????????
                    if(frontEvent.get("type") != null){
                        int a = (int)frontEvent.get("type");
                        if(a == 15 || a == 17 || a == 18 ||a == 19 ){
                            if(a == 15){
                                if(frontEvent.get("live_count") != null) {
                                    map.put("daizaishu", frontEvent.get("live_count"));//??????????????????????????????????????????????????????????????????????????????
                                }else{
                                    map.put("daizaishu",0);
                                }
                            }else{
                                Map<String,Object> nearDeliver = doctorPigEventDao.nearDeliver(pigId,time);
                                BigDecimal daizaishu = new BigDecimal((int)nearDeliver.get("live_count"));
                                List<Map<String,Object>> b = doctorPigEventDao.getdaizaishu(pigId,time,(Date)nearDeliver.get("event_at"));//?????????????????????????????????????????????
                                for(int i = 0;i < b.size(); i++){
                                    if((int)b.get(i).get("type") == 17){
                                        daizaishu = daizaishu.subtract((BigDecimal)b.get(i).get("quantity"));
                                    }
                                    if((int)b.get(i).get("type") == 18){
                                        daizaishu = daizaishu.subtract((BigDecimal)b.get(i).get("quantity"));
                                    }
                                    if((int)b.get(i).get("type") == 19){
                                        daizaishu = daizaishu.add((BigDecimal)b.get(i).get("quantity"));
                                    }
                                }
                                map.put("daizaishu",daizaishu);
                            }
                        }else{
                            map.put("daizaishu",0);
                        }
                    }else{
                        map.put("daizaishu",0);
                    }
                }else{
                    it.remove();
                }
                *//*if(f && g && h){
                    j.add(map);
                }
                if(j.size() == 20){
                    break;
                }*//*
            }
       // }
        //}
        return inFarmPigId;
    }*/
    @Override
    public List<Map<String,Object>> sowsReport(Long farmId,Date time,String pigCode,String operatorName,Long barnId,Integer breed,Integer parity,Integer pigStatus,Date beginInFarmTime, Date endInFarmTime, Integer sowsStatus){
        List<Map<String,Object>> inFarmPigId = null;
        if(sowsStatus == 0) {
            inFarmPigId = doctorPigEventDao.getInFarmPigId(farmId, time, pigCode, breed, beginInFarmTime, endInFarmTime,parity,pigStatus,operatorName,barnId);//??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        } else{
            inFarmPigId = doctorPigEventDao.getInFarmPigId1(farmId, time, pigCode, breed, beginInFarmTime, endInFarmTime,parity,pigStatus,operatorName,barnId);//????????????????????????????????????????????????
        }
        List<Map<String,Object>> inFarmPigId1 = Collections.synchronizedList(new ArrayList<>());
        inFarmPigId.parallelStream().forEach(map ->{
            boolean istrue = true;
            int source = (int)map.get("source");
            if (source == 1) {
                map.put("source","??????");
            }else if (source == 2) {
                map.put("source","??????");
            }else{
                map.put("source","");
            }
            int a = (int)map.get("type");
            BigInteger id = (BigInteger)map.get("id");
            BigInteger pigId = (BigInteger)map.get("pig_id");
            Date eventAt = (Date)map.get("event_at");
            if(a ==1 || a==10  || a==12 || a==14) {//???????????????????????????????????????????????????????????????????????????????????????????????????
                Map<String, Object> afterEvent = doctorPigEventDao.afterEvent(pigId, time);//??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                if (afterEvent != null) {
                    Map<String, Object> currentBarn = doctorPigEventDao.findBarn((BigInteger) afterEvent.get("id"), id, pigId, eventAt, time, operatorName, barnId);//???????????????????????????,????????????????????????
                    if (currentBarn != null) {
                        map.put("current_barn_name", currentBarn.get("barn_name"));
                        map.put("staff_name", currentBarn.get("staff_name"));//?????????
                    } else {
                        istrue = false;
                    }
                } else {
                    Map<String, Object> currentBarns = doctorPigEventDao.findBarns(pigId, operatorName, barnId);//?????????????????????
                    if (currentBarns != null) {
                        map.put("current_barn_name", currentBarns.get("current_barn_name"));
                        map.put("staff_name", currentBarns.get("staff_name"));//?????????
                    } else {
                        istrue = false;
                    }
                }
            }

                String Status = null;
                    int status = (int) map.get("pig_status_after");

                    if (status == PigStatus.Entry.getKey()) {
                        Status = PigStatus.Entry.getName();
                    }
                    if (status == PigStatus.Removal.getKey()) {
                        Status = PigStatus.Removal.getName();
                    }
                    if (status == PigStatus.Mate.getKey()) {
                        Status = PigStatus.Mate.getName();
                    }
                    if (status == PigStatus.Pregnancy.getKey()) {
                        Status = PigStatus.Pregnancy.getName();
                    }
                    if (status == PigStatus.KongHuai.getKey()) {
                        int pregCheckResult = doctorPigEventDao.getPregCheckResult(parity,pigId,time,pigStatus);

                        if (pregCheckResult == 2) {
                            Status = "??????";
                        } else if (pregCheckResult == 3) {
                            Status = "??????";
                        } else if (pregCheckResult == 4) {
                            Status = "??????";
                        } else {
                            Status = PigStatus.KongHuai.getName();
                        }
                    }
                    if (status == PigStatus.Farrow.getKey()) {
                        Status = PigStatus.Farrow.getName();
                    }
                    if (status == PigStatus.FEED.getKey()) {
                        Status = PigStatus.FEED.getName();
                    }
                    if (status == PigStatus.Wean.getKey()) {
                        Status = PigStatus.Wean.getName();
                    }
                    if (status == PigStatus.CHG_FARM.getKey()) {
                        Status = PigStatus.CHG_FARM.getName();
                    }
                    if(sowsStatus != 0 && status != PigStatus.Removal.getKey()){
                        Status = PigStatus.CHG_FARM.getName();
                    }
                map.put("status", Status);//????????????

                if(a == 15 || a == 17 || a == 18 ||a == 19 ){
                    if(a == 15){
                        if(map.get("live_count") != null) {
                            map.put("daizaishu", map.get("live_count"));//??????????????????????????????????????????????????????????????????????????????
                        }else{
                            map.put("daizaishu",0);
                        }
                    }else{
                        Map<String,Object> nearDeliver = doctorPigEventDao.nearDeliver(pigId,time);
                        BigDecimal daizaishu = new BigDecimal((int)nearDeliver.get("live_count"));
                        List<Map<String,Object>> b = doctorPigEventDao.getdaizaishu(pigId,time,(Date)nearDeliver.get("event_at"));//?????????????????????????????????????????????
                        for(int i = 0;i < b.size(); i++){
                            if((int)b.get(i).get("type") == 17){
                                daizaishu = daizaishu.subtract((BigDecimal)b.get(i).get("quantity"));
                            }
                            if((int)b.get(i).get("type") == 18){
                                daizaishu = daizaishu.subtract((BigDecimal)b.get(i).get("quantity"));
                            }
                            if((int)b.get(i).get("type") == 19){
                                daizaishu = daizaishu.add((BigDecimal)b.get(i).get("quantity"));
                            }
                        }
                        map.put("daizaishu",daizaishu);
                    }
                }else{
                    map.put("daizaishu",0);
                }
            if(istrue == true) {
                inFarmPigId1.add(map);
            }
        });
        return inFarmPigId1;
    }
    @Override
    public List<Map<String, Object>> boarReport(Long farmId,Integer pigType, Integer boarsStatus, Date queryDate, String pigCode, String staffName, Integer barnId, Integer breedId, Date beginDate, Date endDate) {
        List<Map<String, Object>> inFarmBoarId = null;
        if(boarsStatus == 0){
            inFarmBoarId = doctorPigEventDao.getInFarmBoarId1(farmId,pigType,queryDate,barnId,pigCode,breedId,staffName,beginDate,endDate);
        }else {
            inFarmBoarId = doctorPigEventDao.getInFarmBoarId2(farmId,pigType,queryDate,barnId,pigCode,breedId,staffName,beginDate,endDate);
        }
        for (Iterator<Map<String,Object>> it = inFarmBoarId.iterator();it.hasNext();) {
            Map map = it.next();
            if (map.get("source") != null){
                int source = (int)map.get("source");
                if (source == PigSource.LOCAL.getKey()) {
                    map.put("source",PigSource.LOCAL.getDesc());
                }
                if (source == PigSource.OUTER.getKey()) {
                    map.put("source",PigSource.OUTER.getDesc());
                }
            }
            if (map.get("type") != null){
                int type = (int)map.get("type");
                if (type == 7 || type == 20){
                    map.put("status","??????");
                }
                if (type == 2){
                    map.put("status","??????");
                }
                if (type == 6){
                    map.put("status","??????");
                }
            }
            if (map.get("boar_type") != null){
                int boarType = (int)map.get("boar_type");
                if (boarType == 1){
                    map.put("boar_type","?????????");
                }
                if (boarType == 2){
                    map.put("boar_type","????????????");
                }
                if (boarType == 3){
                    map.put("boar_type","????????????");
                }
            }
            BigInteger id = (BigInteger)map.get("id");
            BigInteger pigId = (BigInteger)map.get("pig_id");
            Date eventAt = (Date)map.get("event_at");
            BigInteger isBoarBarn = doctorPigEventDao.isBoarBarn(id,pigId,eventAt,queryDate,farmId); //??????????????????????????????????????????
            if(isBoarBarn != null) {
                Map<String,Object> currentBoarBarn = doctorPigEventDao.findBoarBarn(isBoarBarn,id,pigId,eventAt,queryDate,staffName,barnId);//???????????????????????????,????????????????????????
                if(currentBoarBarn != null) {
                    map.put("current_barn_name", currentBoarBarn.get("barn_name"));
                    map.put("staff_name", currentBoarBarn.get("staff_name"));//?????????
                } else{
                    it.remove();
                }
            }else{
                Map<String,Object> currentBoarBarn = doctorPigEventDao.findBoarBarns(pigId,staffName,barnId);//?????????????????????
                if(currentBoarBarn != null) {
                    map.put("current_barn_name", currentBoarBarn.get("current_barn_name"));
                    map.put("staff_name", currentBoarBarn.get("staff_name"));//?????????
                } else{
                    it.remove();
                }
            }
                BigInteger isBoarChgFarm = doctorPigEventDao.isBoarChgFarm(id, pigId, eventAt, queryDate,farmId); //??????????????????????????????????????????
                DoctorChgFarmInfo doctorChgFarmInfo;
                DoctorPigTrack doctorPigTrack;
                DoctorPig doctorPig;
                if (isBoarChgFarm != null) {
                    doctorChgFarmInfo = doctorChgFarmInfoDao.findBoarChgFarm(farmId, pigId, isBoarChgFarm);
                    if (doctorChgFarmInfo != null){
                        doctorPigTrack = JSON_MAPPER.fromJson(doctorChgFarmInfo.getTrack(), DoctorPigTrack.class);
                        doctorPig = JSON_MAPPER.fromJson(doctorChgFarmInfo.getPig(), DoctorPig.class);
                        map.put("current_barn_name", doctorPigTrack.getCurrentBarnName());
                        map.put("in_farm_date", doctorPig.getInFarmDate());
                        Long currentBarnId = doctorPigTrack.getCurrentBarnId();
                        String currentStaffName = doctorPigEventDao.findStaffName(currentBarnId,staffName,barnId);
                        if (currentStaffName != null){
                            map.put("staff_name",currentStaffName);//?????????
                        }else {
                            it.remove();
                        }
                    } else {
                        it.remove();
                    }
                }
        }
        return inFarmBoarId;
    }

    @Override
    public Map<String,Object> groupReport(Long farmId,Date time,String groupCode,String operatorName,Long barn,Integer groupType,Integer groupStatus,Date buildBeginGroupTime,Date buildEndGroupTime,Date closeBeginGroupTime,Date closeEndGroupTime){
        if(groupCode == ""){
            groupCode = null;
        }
        if (operatorName == "") {
            operatorName = null;
        }
        List<Map<String,Object>> groupList = null;
        if(groupStatus == 0) {
            groupList = doctorGroupEventDao.groupList(farmId, time, barn, groupCode, operatorName, groupType, buildBeginGroupTime, buildEndGroupTime,closeBeginGroupTime,closeEndGroupTime);
        } else{
            groupList = doctorGroupEventDao.groupList1(farmId, time, barn, groupCode, operatorName, groupType, buildBeginGroupTime, buildEndGroupTime,closeBeginGroupTime,closeEndGroupTime);
        }
        int zongcunlan = 0;
        for(Iterator<Map<String,Object>> it = groupList.iterator();it.hasNext();){
            Map map = it.next();
            int status = (int) map.get("pig_type");
            if(status == 7){
                map.put("pig_type","????????????");
            }
            if(status == 2){
                map.put("pig_type","?????????");
            }
            if(status == 3){
                map.put("pig_type","?????????");
            }
            if(status == 4){
                map.put("pig_type","?????????");
            }
            Long groupId = (Long)map.get("group_id");
            if(map.get("build_event_at") == null){
                Date buildEventAt =  doctorGroupEventDao.getBuildEventAt(groupId);
                map.put("build_event_at",buildEventAt);
            }
            Integer getCunlan = doctorGroupEventDao.getCunlan(groupId,time);
            if(getCunlan != null && groupStatus == 0) {
                if(getCunlan == 0){
                    it.remove();
                    continue;
                }
                map.put("cunlanshu", getCunlan);
                zongcunlan = zongcunlan + getCunlan;
            }else if(getCunlan == null && groupStatus == 0){
                it.remove();
                continue;
            }else{
                map.put("cunlanshu", 0);
            }
            Double getInAvgweight = doctorGroupEventDao.getInAvgweight(groupId,time);
            if(getInAvgweight != null) {
                map.put("inAvgweight", (double)Math.round(getInAvgweight*1000)/1000);
            }else{
                map.put("inAvgweight", 0);
            }
            Double getOutAvgweight = doctorGroupEventDao.getOutAvgweight(groupId,time);
            if(getOutAvgweight != null) {
                map.put("outAvgweight", (double)Math.round(getOutAvgweight*1000)/1000);
            }else{
                map.put("outAvgweight", 0);
            }
            Double getAvgDayAge = doctorGroupEventDao.getAvgDayAge(groupId,time);
            if(getAvgDayAge != null && getCunlan != null && getCunlan != 0 && groupStatus == 0) {
                map.put("getAvgDayAge", (double)Math.round((DateUtil.getDeltaDays(getAvgDay(groupId,time), time))*1000)/1000);
            }else{
                map.put("getAvgDayAge", 0);
            }
        }
        Map<String,Object> data = new HashMap<>();
        data.put("data",groupList);
        data.put("zongcunlan",zongcunlan);
        return data;
    }
    public List<Map<String,Object>> barnsReport(Long farmId,String operatorName,String barnName,Date beginTime,Date endTime,Integer pigType){
        if(operatorName == ""){
            operatorName = null;
        }
        if (barnName == "") {
            barnName = null;
        }
        List<Map<String,Object>> barnList =  doctorBarnDao.findBarnIdsByfarmId(farmId, operatorName,barnName,pigType);
        if(barnList != null) {
            List<Map<String,Object>> list =  Collections.synchronizedList(new ArrayList<>());
            barnList.parallelStream().forEach(map -> {
                int barnType = (int)(map.get("pig_type"));
                Long barnId = (Long)(map.get("id"));
                if(barnType == 5){
                    map.put("pig_type","????????????");
                }
                if(barnType == 6){
                    map.put("pig_type","????????????");
                }
                if(barnType == 9){
                    map.put("pig_type","?????????");
                }
                if(barnType == 2){
                    map.put("pig_type","?????????");
                }
                if(barnType == 3){
                    map.put("pig_type","?????????");
                }
                if(barnType == 4){
                    map.put("pig_type","?????????");
                }
                if (barnType == 5 || barnType == 6 || barnType == 9) {
                    Integer qichucunlan = doctorBarnDao.qichucunlan(farmId, barnId, beginTime);
                    Integer qimucunlan = doctorBarnDao.qimucunlan(farmId, barnId, endTime);
                    if (qichucunlan != null) {
                        map.put("qichucunlan", qichucunlan);
                    } else {
                        map.put("qichucunlan", 0);
                    }
                    if (qimucunlan != null) {
                        map.put("qimucunlan", qimucunlan);
                    } else {
                        map.put("qimucunlan", 0);
                    }
                    List<Map<Integer, Long>> jianshao = doctorBarnDao.jianshao(barnId, beginTime, endTime);
                    Long qitajianshao = 0L;
                    if (jianshao != null) {
                        for (int i = 0; i < jianshao.size(); i++) {
                            Long a = jianshao.get(i).get("change_type_id");
                            if (a == 109) {
                                map.put("xiaoshou", jianshao.get(i).get("count") == null ? 0 : jianshao.get(i).get("count"));
                            }
                            if (a == 110) {
                                map.put("siwang", jianshao.get(i).get("count")==null ? 0 : jianshao.get(i).get("count"));
                            }
                            if (a == 111) {
                                map.put("taotai", jianshao.get(i).get("count")==null ? 0 : jianshao.get(i).get("count"));
                            }
                            if (a == 112 || a == 113 || a == 114 || a == 115) {
                                qitajianshao = qitajianshao + jianshao.get(i).get("count");
                            }
                        }
                        map.put("qitajianshao", qitajianshao);
                    } else {
                        map.put("xiaoshou", 0);
                        map.put("siwang", 0);
                        map.put("taotai", 0);
                        map.put("qitajianshao", 0);
                    }
                    Integer zhuanchu = doctorBarnDao.zhuanchu(barnId, beginTime, endTime);
                    if (zhuanchu != null) {
                        map.put("zhuanchu", zhuanchu);
                    } else {
                        map.put("zhuanchu", 0);
                    }
                    Long zhuanru = qimucunlan - qichucunlan + (zhuanchu == null ? 0 : zhuanchu) + (map.get("xiaoshou") == null ? 0 : (Long) map.get("xiaoshou")) + (map.get("siwang") == null ? 0 : (Long) map.get("siwang")) + (map.get("taotai") == null ? 0 : (Long) map.get("taotai")) + (map.get("qitajianshao") == null ? 0 : (Long) map.get("qitajianshao"));
                    map.put("zhuanru", zhuanru);
                }
                if(barnType == 7){
                    Map map1 = new HashMap();
                    Integer pigqichucunlan = doctorBarnDao.qichucunlan(farmId, barnId, beginTime);
                    Integer groupqichucunlan = doctorBarnDao.groupqichucunlan(farmId, barnId, beginTime);
                    Integer pigqimucunlan = doctorBarnDao.qimucunlan(farmId, barnId, endTime);
                    Integer groupqimucunlan = doctorBarnDao.groupqimucunlan(farmId, barnId, endTime);
                    //Integer qichucunlan = (pigqichucunlan == null? 0 : pigqichucunlan) + (groupqichucunlan == null ? 0:groupqichucunlan);
                    //Integer qimucunlan = (pigqimucunlan == null? 0 : pigqimucunlan) + (groupqimucunlan == null ? 0:groupqimucunlan);
                    map.put("qichucunlan",pigqichucunlan);
                    map1.put("qichucunlan",groupqichucunlan);
                    map.put("qimucunlan",pigqimucunlan);
                    map1.put("qimucunlan",groupqimucunlan);
                    List<Map<Integer, Long>> jianshao = doctorBarnDao.jianshao(barnId, beginTime, endTime);
                    Long pigqitajianshao = 0L;
                    Long pigxiaoshou = 0L;
                    Long pigsiwang = 0L;
                    Long pigtaotai = 0L;
                    if (jianshao != null) {
                        for (int i = 0; i < jianshao.size(); i++) {
                            Long a = jianshao.get(i).get("change_type_id");
                            if (a == 109) {
                                pigxiaoshou = jianshao.get(i).get("count");
                            }
                            if (a == 110) {
                                pigsiwang = jianshao.get(i).get("count");
                            }
                            if (a == 111) {
                                pigtaotai = jianshao.get(i).get("count");
                            }
                            if (a == 112 || a == 113 || a == 114 || a == 115) {
                                pigqitajianshao = pigqitajianshao + jianshao.get(i).get("count");
                            }
                        }
                    }
                    List<Map<Integer, Long>> jianshao1 = doctorBarnDao.groupjianshao(barnId, beginTime, endTime);
                    int groupqitajianshao = 0;
                    Long groupxiaoshou = 0L;
                    Long groupsiwang = 0L;
                    Long grouptaotai = 0L;
                    if (jianshao1 != null) {
                        for (int i = 0; i < jianshao1.size(); i++) {
                            Long a = jianshao1.get(i).get("change_type_id");
                            if (a == 109) {
                                Object h = jianshao1.get(i).get("count");
                                groupxiaoshou = Long.parseLong(h.toString());
                            }
                            if (a == 110) {
                                Object h = jianshao1.get(i).get("count");
                                groupsiwang = Long.parseLong(h.toString());
                            }
                            if (a == 111) {
                                Object h = jianshao1.get(i).get("count");
                                grouptaotai = Long.parseLong(h.toString());
                            }
                            if (a == 112 || a == 113 || a == 114 || a == 115) {
                                Object  ob = jianshao1.get(i).get("count");
                                int b =Integer.parseInt(ob.toString());
                                groupqitajianshao = groupqitajianshao+b;
                            }
                        }
                    }
                    map.put("xiaoshou",pigxiaoshou);
                    map.put("siwang",pigsiwang);
                    map.put("taotai",pigtaotai);
                    map.put("qitajianshao",pigqitajianshao);
                    map1.put("xiaoshou",groupxiaoshou);
                    map1.put("siwang",groupsiwang);
                    map1.put("taotai",grouptaotai);
                    map1.put("qitajianshao",groupqitajianshao);
                    Integer pigzhuanchu = doctorBarnDao.zhuanchu(barnId, beginTime, endTime);
                    Integer groupzhuanchu = doctorBarnDao.groupzhuanchu(barnId,beginTime,endTime);
                    Long zhuanru = pigqimucunlan - pigqichucunlan + (pigzhuanchu == null ? 0 : pigzhuanchu) + (map.get("xiaoshou") == null ? 0 : (Long) map.get("xiaoshou")) + (map.get("siwang") == null ? 0 : (Long) map.get("siwang")) + (map.get("taotai") == null ? 0 : (Long) map.get("taotai")) + (map.get("qitajianshao") == null ? 0 : (Long) map.get("qitajianshao"));
                    Integer groupzhuanru = doctorBarnDao.groupzhuanru(barnId,beginTime,endTime);
                    map.put("zhuanru", zhuanru);
                    map.put("zhuanchu", pigzhuanchu);
                    map1.put("zhuanru", groupzhuanru);
                    map1.put("zhuanchu", groupzhuanchu);
                    map.put("pig_type", "????????????");
                    map1.put("pig_type","??????");
                    map1.put("name",map.get("name"));
                    map1.put("staff_name",map.get("staff_name"));
                    list.add(map1);
                    if(pigType !=null && pigType == 17){
                        list.remove(map1);
                    }
                }
                if (barnType == 2 || barnType == 3 || barnType == 4) {
                    Integer qichucunlan = doctorBarnDao.groupqichucunlan(farmId, barnId, beginTime);
                    Integer qimucunlan = doctorBarnDao.groupqimucunlan(farmId, barnId, endTime);
                    if (qichucunlan != null) {
                        map.put("qichucunlan", qichucunlan);
                    } else {
                        map.put("qichucunlan", 0);
                    }
                    if (qimucunlan != null) {
                        map.put("qimucunlan", qimucunlan);
                    } else {
                        map.put("qimucunlan", 0);
                    }
                    Integer zhuanru = doctorBarnDao.groupzhuanru(barnId,beginTime,endTime);
                    map.put("zhuanru", zhuanru);
                    Integer zhuanchu = doctorBarnDao.groupzhuanchu(barnId,beginTime,endTime);
                    if (zhuanchu != null) {
                        map.put("zhuanchu", zhuanchu);
                    } else {
                        map.put("zhuanchu", 0);
                    }
                    List<Map<Integer, Long>> jianshao = doctorBarnDao.groupjianshao(barnId, beginTime, endTime);
                    int qitajianshao = 0;
                    if (jianshao != null) {
                        for (int i = 0; i < jianshao.size(); i++) {
                            Long a = jianshao.get(i).get("change_type_id");
                            if (a == 109) {
                                map.put("xiaoshou", jianshao.get(i).get("count")==null ? 0 : jianshao.get(i).get("count"));
                            }
                            if (a == 110) {
                                map.put("siwang", jianshao.get(i).get("count")==null ? 0 : jianshao.get(i).get("count"));
                            }
                            if (a == 111) {
                                map.put("taotai", jianshao.get(i).get("count")==null ? 0 : jianshao.get(i).get("count"));
                            }
                            if (a == 112 || a == 113 || a == 114 || a == 115) {
                                Object  ob = jianshao.get(i).get("count");
                                int b =Integer.parseInt(ob.toString());
                               qitajianshao = qitajianshao+b;
                            }
                        }
                        map.put("qitajianshao", qitajianshao);
                    } else {
                        map.put("xiaoshou", 0);
                        map.put("siwang", 0);
                        map.put("taotai", 0);
                        map.put("qitajianshao", 0);
                    }
                }
                list.add(map);
                if(pigType !=null &&  pigType == 27){
                    list.remove(map);
                }
            });
            return list;
        } else{
            return null;
        }
    }
    /**
     * ?????????????????????????????????????????????
     *
     * @param groupId ??????id
     * @return ??????
     */
    public Date getAvgDay(Long groupId,Date time) {
        List<Integer> includeTypes = Lists.newArrayList(GroupEventType.CHANGE.getValue(), GroupEventType.MOVE_IN.getValue(),
                GroupEventType.TRANS_FARM.getValue(), GroupEventType.TRANS_GROUP.getValue());
        List<DoctorGroupEvent> groupEventList = doctorGroupEventDao.findEventIncludeTypes1(groupId, includeTypes,time);
        int currentQuantity = 0;
        int avgDay = 0;
        Date lastEvent = new Date();
        for (DoctorGroupEvent groupEvent : groupEventList) {
            if (Objects.equals(MoreObjects.firstNonNull(groupEvent.getQuantity(), 0), 0)) {
                continue;
            }
            if (Objects.equals(groupEvent.getType(), GroupEventType.MOVE_IN.getValue())) {
                avgDay = avgDay + DateUtil.getDeltaDays(lastEvent, groupEvent.getEventAt());
                avgDay = EventUtil.getAvgDayAge(avgDay, currentQuantity, groupEvent.getAvgDayAge(), groupEvent.getQuantity());
                currentQuantity += groupEvent.getQuantity();
                lastEvent = groupEvent.getEventAt();
            } else {
                currentQuantity -= groupEvent.getQuantity();
            }
        }
        return new DateTime(lastEvent).minusDays(avgDay).toDate();
    }
}
