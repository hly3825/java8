package io.terminus.doctor.web.admin.controller;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.terminus.boot.rpc.common.annotation.RpcConsumer;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.BaseUser;
import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Arguments;
import io.terminus.doctor.common.enums.IsOrNot;
import io.terminus.doctor.common.enums.UserStatus;
import io.terminus.doctor.common.enums.UserType;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.service.DoctorBarnReadService;
import io.terminus.doctor.user.dto.FarmCriteria;
import io.terminus.doctor.user.model.DoctorFarm;
import io.terminus.doctor.user.model.DoctorServiceStatus;
import io.terminus.doctor.user.model.PrimaryUser;
import io.terminus.doctor.user.model.Sub;
import io.terminus.doctor.user.service.DoctorFarmReadService;
import io.terminus.doctor.user.service.DoctorFarmWriteService;
import io.terminus.doctor.user.service.DoctorServiceStatusReadService;
import io.terminus.doctor.user.service.DoctorUserProfileReadService;
import io.terminus.doctor.user.service.DoctorUserReadService;
import io.terminus.doctor.user.service.PrimaryUserReadService;
import io.terminus.doctor.web.admin.dto.UserApplyServiceDetailDto;
import io.terminus.doctor.web.admin.service.DoctorInitBarnService;
import io.terminus.doctor.web.admin.service.DoctorInitFarmService;
import io.terminus.doctor.web.core.dto.FarmStaff;
import io.terminus.pampas.common.UserUtil;
import io.terminus.parana.common.utils.RespHelper;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.model.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static io.terminus.common.utils.Arguments.isNull;
import static io.terminus.common.utils.Arguments.notNull;

/**
 * Created by chenzenghui on 16/7/15.
 */
//@Api(tags = "??????????????????")
@Slf4j
@RestController
@RequestMapping("/api/admin/farm")
public class FarmController {
    private final DoctorFarmReadService doctorFarmReadService;
    private final DoctorFarmWriteService doctorFarmWriteService;
    private final DoctorUserReadService doctorUserReadService;
    private final DoctorServiceStatusReadService doctorServiceStatusReadService;
    private final DoctorInitBarnService doctorInitBarnService;
    private final DoctorInitFarmService doctorInitFarmService;
    @RpcConsumer
    private PrimaryUserReadService primaryUserReadService;
    @RpcConsumer
    private DoctorBarnReadService doctorBarnReadService;
    @RpcConsumer
    private DoctorUserProfileReadService doctorUserProfileReadService;

    @Autowired
    public FarmController(DoctorFarmReadService doctorFarmReadService,
                          DoctorUserReadService doctorUserReadService,
                          DoctorServiceStatusReadService doctorServiceStatusReadService,
                          DoctorFarmWriteService doctorFarmWriteService,
                          DoctorInitBarnService doctorInitBarnService,
                          DoctorInitFarmService doctorInitFarmService){
        this.doctorFarmReadService = doctorFarmReadService;
        this.doctorUserReadService = doctorUserReadService;
        this.doctorServiceStatusReadService = doctorServiceStatusReadService;
        this.doctorFarmWriteService = doctorFarmWriteService;
        this.doctorInitBarnService = doctorInitBarnService;
        this.doctorInitFarmService = doctorInitFarmService;
    }

    /**
     * ??????????????????????????????????????????, ???????????????????????????????????????????????????
     * @param dto userId:??????????????????id , farms: ??????,???????????????(name??????)??????
     * @return
     */
    @RequestMapping(value = "/add", method = RequestMethod.POST)
    public Boolean addFarm(@RequestBody UserApplyServiceDetailDto dto){

        //??????????????????
        if(dto.getUserId() == null){
            throw new JsonResponseException(500, "user.id.invalid");
        }
        if(dto.getFarms() == null || dto.getFarms().isEmpty()){
            throw new JsonResponseException(500, "need.at.least.one.farm"); //??????????????????????????????
        }

        //??????????????????????????????
        checkUserTypeOperator();

        //??????????????????????????????????????????
        User primaryUser = RespHelper.or500(doctorUserReadService.findById(dto.getUserId()));

        if (notNull(primaryUser)&&notNull(primaryUser.getExtra()) && primaryUser.getExtra().containsKey("frozen")
                && primaryUser.getExtra().get("frozen").equals(IsOrNot.YES.getKey().toString())) {
            throw new JsonResponseException("user.is.frozen");
        }

        if(!Objects.equals(primaryUser.getType(), UserType.FARM_ADMIN_PRIMARY.value())){
            throw new JsonResponseException("user.not.primary"); // ??????????????????????????????
        }

        //???????????????????????????????????????????????????
        DoctorServiceStatus serviceStatus = RespHelper.or500(doctorServiceStatusReadService.findByUserId(primaryUser.getId()));
        if(!Objects.equals(serviceStatus.getPigdoctorStatus(), DoctorServiceStatus.Status.OPENED.value())){
            throw new JsonResponseException("user.service.pigdoctor.not.opened"); //????????????????????????????????????
        }

        //?????????????????????
        List<DoctorFarm> farms = RespHelper.or500(doctorFarmReadService.findFarmsByUserId(primaryUser.getId()));

        //??????????????????????????????
        Set<String> dbFarmName = farms.stream().map(DoctorFarm::getName).collect(Collectors.toSet());
        Set<String> addFarmName = dto.getFarms().stream().map(farm -> {
            if (farm.getName() == null || farm.getName().trim().isEmpty()) {
                throw new JsonResponseException("farm.name.not.null"); //????????????????????????
            }
            if (dbFarmName.contains(farm.getName())) {
                throw new JsonResponseException("farm.name.duplicate"); //??????????????????
            }
            return farm.getName();
        }).collect(Collectors.toSet());
        if(addFarmName.size() < dto.getFarms().size()){
            throw new JsonResponseException("farm.name.duplicate"); //??????????????????
        }

        if (dto.getOrg() == null || dto.getOrg().getId() == null) {
            throw new JsonResponseException("orgId.not.null");
        }

        //???????????????????????????...
        List<DoctorFarm> newFarms = RespHelper.or500(doctorFarmWriteService.addFarms4PrimaryUser(primaryUser.getId(), dto.getOrg().getId(), dto.getFarms()));

        log.info("init barn start, userId:{}, farms:{}", dto.getUserId(), newFarms);
        //???????????????
        newFarms.forEach(farm -> doctorInitFarmService.initFarm(farm, dto.getUserId()));

        log.info("init barn end");
        return Boolean.TRUE;
    }

    /**
     *??????????????????
     * @return ????????????
     */
    @RequestMapping(value = "/findAllFarm", method = RequestMethod.GET)
    public List<DoctorFarm> findAllFarm() {
        return RespHelper.or500(doctorFarmReadService.findAllFarms());
    }

    /**
     * ?????????????????????staff??????
     *
     * @param farmId ??????id
     * @return staff??????
     */
    @RequestMapping(value = "/staff/{farmId}", method = RequestMethod.GET)
    public List<FarmStaff> findStaffByFarmId(@PathVariable Long farmId) {
        return transformStaffs(farmId);
    }

    /**
     * ?????????????????????????????????
     * @param farmId ??????id
     * @return ????????????
     */
    @RequestMapping(value = "/allBarn/{farmId}", method = RequestMethod.GET)
    public List<DoctorBarn> findAllUnUse(@PathVariable Long farmId) {
        return RespHelper.or500(doctorBarnReadService.findBarnsByFarmId(farmId));
    }

    @ApiOperation("??????????????????")
    @RequestMapping(value = "/paging/farm", method = RequestMethod.GET)
    public Paging<DoctorFarm> pagingFarm(@RequestParam(required = false) @ApiParam("??????????????????, ??????") String fuzzyName,
                                         @RequestParam(required = false) @ApiParam("??????") Integer pageNo,
                                         @RequestParam(required = false) @ApiParam("????????????") Integer pageSize) {
        FarmCriteria farmCriteria = new FarmCriteria();
        if (!Strings.isNullOrEmpty(fuzzyName)) {
            farmCriteria.setFuzzyName(fuzzyName);
        }
        return RespHelper.or500(doctorFarmReadService.pagingFarm(farmCriteria, pageNo, pageSize));
    }

    @ApiOperation("??????????????????, isIntelligent: 1-??????????????????0-??????????????????")
    @RequestMapping(value = "/switch/isIntelligent", method = RequestMethod.PUT)
    public Boolean switchIsIntelligent(@RequestParam @ApiParam("??????id") Long farmId) {
        return RespHelper.or500(doctorFarmWriteService.switchIsIntelligent(farmId));
    }

    @ApiOperation("??????????????????, isWeak: 1-??????????????????0-??????????????????")
    @RequestMapping(value = "/switch/isWeak", method = RequestMethod.PUT)
    public Boolean switchIsWeak(@RequestParam @ApiParam("??????id") Long farmId) {
        return RespHelper.or500(doctorFarmWriteService.switchIsWeak(farmId));
    }

    @ApiOperation("????????????")
    @RequestMapping(value = "/freeze/farm")
    public Boolean freezeFarm(@RequestParam @ApiParam("??????id") Long farmId) {
        return RespHelper.or500(doctorFarmWriteService.freezeFarm(farmId));
    }


    @ApiOperation("??????????????????")
    @RequestMapping(value = "/update/{farmId}/options", method = RequestMethod.PUT)
    public Boolean updateFarmOptions(@PathVariable @ApiParam("??????id") Long farmId,
                                  @RequestParam(required = false) @ApiParam("???????????????") String newName,
                                  @RequestParam(required = false) @ApiParam("????????????") String number,
                                  @RequestParam(required = false) @ApiParam("????????????") Integer isWeak,
                                  @RequestParam(required = false) @ApiParam("??????????????????") Integer isIntelligent) {
        DoctorFarm doctorFarm = RespHelper.or500(doctorFarmReadService.findFarmById(farmId));
        if (isNull(doctorFarm)) {
            throw  new JsonResponseException("farm.not.found");
        }

        return RespHelper.or500(doctorFarmWriteService.updateFarmOptions(farmId, newName, number, isWeak, isIntelligent));
    }

    @ApiOperation("?????????????????????????????????")
    @RequestMapping(value = "/check/exist/number", method = RequestMethod.GET)
    public Boolean checkExistNumber(@RequestParam @ApiParam("????????????") String number) {
        if (Strings.isNullOrEmpty(number)) {
            throw new JsonResponseException("farm.number.is.illegal");
        }

        DoctorFarm doctorFarm = RespHelper.or500(doctorFarmReadService.findByNumber(number));
        if (isNull(doctorFarm)) {
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    /**
     * ??????????????????
     * @param farmId ??????id
     * @return ????????????
     */
    private List<FarmStaff> transformStaffs(Long farmId) {
        List<Sub> subList = RespHelper.or500(primaryUserReadService.findSubsByFarmIdAndStatus(farmId, Sub.Status.ACTIVE.value(),null));
        List<FarmStaff> staffList = Lists.newArrayList();
        if (!Arguments.isNullOrEmpty(subList)) {
            staffList.addAll(subList.stream().map(sub -> {
                FarmStaff farmStaff = new FarmStaff();
                farmStaff.setUserId(sub.getUserId());
                farmStaff.setRealName(sub.getRealName());
                farmStaff.setStatus(sub.getStatus());
                farmStaff.setFarmId(sub.getFarmId());
                return farmStaff;
            }).collect(Collectors.toList()));
        }
        PrimaryUser primaryUser = RespHelper.or500(primaryUserReadService.findPrimaryByFarmIdAndStatus(farmId, UserStatus.NORMAL.value()));
        if(primaryUser !=null){
            FarmStaff farmStaff = new FarmStaff();
            farmStaff.setFarmId(primaryUser.getRelFarmId());
            farmStaff.setUserId(primaryUser.getUserId());
            farmStaff.setStatus(primaryUser.getStatus());
            Response<UserProfile> userProfileResponse = doctorUserProfileReadService.findProfileByUserId(primaryUser.getUserId());
            if (userProfileResponse.isSuccess()
                    && notNull(userProfileResponse.getResult())
                    && !Strings.isNullOrEmpty(userProfileResponse.getResult().getRealName())) {
                farmStaff.setRealName(userProfileResponse.getResult().getRealName());
            } else {
                User user = io.terminus.doctor.common.utils.RespHelper.or500(doctorUserReadService.findById(primaryUser.getUserId()));
                farmStaff.setRealName(user.getName());
            }
            staffList.add(farmStaff);
        }
        return staffList;
    }

    /**
     * ???????????????????????????????????????, ?????????????????????????????????
     * @return
     */
    private BaseUser checkUserTypeOperator(){
        BaseUser baseUser = UserUtil.getCurrentUser();
        if(!Objects.equals(UserType.ADMIN.value(), baseUser.getType())){
            throw new JsonResponseException("authorize.fail");
        }
        return baseUser;
    }
}
