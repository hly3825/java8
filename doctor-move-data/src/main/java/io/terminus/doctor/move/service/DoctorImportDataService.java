package io.terminus.doctor.move.service;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Joiners;
import io.terminus.common.utils.MapBuilder;
import io.terminus.doctor.basic.dao.DoctorBasicDao;
import io.terminus.doctor.basic.dao.DoctorBasicMaterialDao;
import io.terminus.doctor.basic.dao.DoctorChangeReasonDao;
import io.terminus.doctor.basic.dao.DoctorFarmBasicDao;
import io.terminus.doctor.basic.dto.DoctorMaterialConsumeProviderDto;
import io.terminus.doctor.basic.model.DoctorBasic;
import io.terminus.doctor.basic.model.DoctorBasicMaterial;
import io.terminus.doctor.basic.model.DoctorChangeReason;
import io.terminus.doctor.basic.model.DoctorFarmBasic;
import io.terminus.doctor.basic.model.DoctorMaterialConsumeProvider;
import io.terminus.doctor.basic.model.DoctorWareHouse;
import io.terminus.doctor.basic.service.DoctorMaterialInWareHouseWriteService;
import io.terminus.doctor.basic.service.DoctorWareHouseTypeWriteService;
import io.terminus.doctor.basic.service.DoctorWareHouseWriteService;
import io.terminus.doctor.common.enums.PigType;
import io.terminus.doctor.common.enums.SourceType;
import io.terminus.doctor.common.enums.UserStatus;
import io.terminus.doctor.common.enums.UserType;
import io.terminus.doctor.common.enums.WareHouseType;
import io.terminus.doctor.common.utils.DateUtil;
import io.terminus.doctor.common.utils.JsonMapperUtil;
import io.terminus.doctor.common.utils.Params;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.common.utils.ToJsonMapper;
import io.terminus.doctor.event.constants.DoctorFarmEntryConstants;
import io.terminus.doctor.event.dao.DoctorBarnDao;
import io.terminus.doctor.event.dao.DoctorGroupDao;
import io.terminus.doctor.event.dao.DoctorGroupEventDao;
import io.terminus.doctor.event.dao.DoctorGroupTrackDao;
import io.terminus.doctor.event.dao.DoctorPigDao;
import io.terminus.doctor.event.dao.DoctorPigEventDao;
import io.terminus.doctor.event.dao.DoctorPigTrackDao;
import io.terminus.doctor.event.dto.event.sow.DoctorFarrowingDto;
import io.terminus.doctor.event.dto.event.sow.DoctorMatingDto;
import io.terminus.doctor.event.dto.event.sow.DoctorPregChkResultDto;
import io.terminus.doctor.event.dto.event.sow.DoctorWeanDto;
import io.terminus.doctor.event.dto.event.usual.DoctorChgLocationDto;
import io.terminus.doctor.event.dto.event.usual.DoctorFarmEntryDto;
import io.terminus.doctor.event.enums.BoarEntryType;
import io.terminus.doctor.event.enums.DoctorMatingType;
import io.terminus.doctor.event.enums.EventStatus;
import io.terminus.doctor.event.enums.FarrowingType;
import io.terminus.doctor.event.enums.GroupEventType;
import io.terminus.doctor.event.enums.InType;
import io.terminus.doctor.event.enums.IsOrNot;
import io.terminus.doctor.event.enums.PigEvent;
import io.terminus.doctor.event.enums.PigSource;
import io.terminus.doctor.event.enums.PigStatus;
import io.terminus.doctor.event.enums.PregCheckResult;
import io.terminus.doctor.event.model.DoctorBarn;
import io.terminus.doctor.event.model.DoctorGroup;
import io.terminus.doctor.event.model.DoctorGroupEvent;
import io.terminus.doctor.event.model.DoctorGroupTrack;
import io.terminus.doctor.event.model.DoctorPig;
import io.terminus.doctor.event.model.DoctorPigEvent;
import io.terminus.doctor.event.model.DoctorPigTrack;
import io.terminus.doctor.event.service.DoctorMessageRuleWriteService;
import io.terminus.doctor.event.service.DoctorPigTypeStatisticWriteService;
import io.terminus.doctor.move.dto.DoctorImportSheet;
import io.terminus.doctor.move.dto.DoctorImportSow;
import io.terminus.doctor.move.util.ImportExcelUtils;
import io.terminus.doctor.user.dao.DoctorAddressDao;
import io.terminus.doctor.user.dao.DoctorFarmDao;
import io.terminus.doctor.user.dao.DoctorFarmExportDao;
import io.terminus.doctor.user.dao.DoctorOrgDao;
import io.terminus.doctor.user.dao.DoctorServiceReviewDao;
import io.terminus.doctor.user.dao.DoctorServiceStatusDao;
import io.terminus.doctor.user.dao.DoctorStaffDao;
import io.terminus.doctor.user.dao.DoctorUserDataPermissionDao;
import io.terminus.doctor.user.dao.PrimaryUserDao;
import io.terminus.doctor.user.dao.SubDao;
import io.terminus.doctor.user.dao.SubRoleDao;
import io.terminus.doctor.user.dao.UserDaoExt;
import io.terminus.doctor.user.interfaces.event.DoctorSystemCode;
import io.terminus.doctor.user.interfaces.event.EventType;
import io.terminus.doctor.user.interfaces.model.UserDto;
import io.terminus.doctor.user.manager.DoctorUserManager;
import io.terminus.doctor.user.manager.UserInterfaceManager;
import io.terminus.doctor.user.model.DoctorFarm;
import io.terminus.doctor.user.model.DoctorFarmExport;
import io.terminus.doctor.user.model.DoctorOrg;
import io.terminus.doctor.user.model.DoctorServiceReview;
import io.terminus.doctor.user.model.DoctorServiceStatus;
import io.terminus.doctor.user.model.DoctorStaff;
import io.terminus.doctor.user.model.DoctorUserDataPermission;
import io.terminus.doctor.user.model.PrimaryUser;
import io.terminus.doctor.user.model.Sub;
import io.terminus.doctor.user.model.SubRole;
import io.terminus.doctor.user.service.DoctorUserReadService;
import io.terminus.doctor.user.service.SubRoleWriteService;
import io.terminus.parana.common.utils.EncryptUtil;
import io.terminus.parana.user.address.model.Address;
import io.terminus.parana.user.impl.dao.UserProfileDao;
import io.terminus.parana.user.model.LoginType;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.model.UserProfile;
import io.terminus.parana.user.service.UserWriteService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.terminus.common.utils.Arguments.*;
import static io.terminus.doctor.event.dto.DoctorBasicInputInfoDto.generateEventDescFromExtra;

/**
 * Desc:
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 2016/10/19
 */
@Slf4j
@Service
public class DoctorImportDataService {
    private static final JsonMapperUtil MAPPER = JsonMapperUtil.nonEmptyMapper();

    //???????????????????????????id
    @Value("${xrnm.auth.user.id: 0}")
    private Long xrnmId;

    @Autowired
    private DoctorBarnDao doctorBarnDao;
    @Autowired
    private DoctorBasicDao doctorBasicDao;
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
    private DoctorMoveBasicService doctorMoveBasicService;
    @Autowired
    private DoctorOrgDao doctorOrgDao;
    @Autowired
    private DoctorFarmDao doctorFarmDao;
    @Autowired
    private DoctorUserReadService doctorUserReadService;
    @Autowired
    private UserWriteService<User> userWriteService;
    @Autowired
    private UserProfileDao userProfileDao;
    @Autowired
    private UserInitService userInitService;
    @Autowired
    private DoctorStaffDao doctorStaffDao;
    @Autowired
    private DoctorUserDataPermissionDao doctorUserDataPermissionDao;
    @Autowired
    private DoctorMessageRuleWriteService doctorMessageRuleWriteService;
    @Autowired
    private SubRoleWriteService subRoleWriteService;
    @Autowired
    private SubRoleDao subRoleDao;
    @Autowired
    private SubDao subDao;
    @Autowired
    private PrimaryUserDao primaryUserDao;
    @Autowired
    private DoctorPigTypeStatisticWriteService doctorPigTypeStatisticWriteService;
    @Autowired
    private DoctorWareHouseTypeWriteService doctorWareHouseTypeWriteService;
    @Autowired
    private DoctorFarmBasicDao doctorFarmBasicDao;
    @Autowired
    private DoctorChangeReasonDao doctorChangeReasonDao;
    @Autowired
    private DoctorBasicMaterialDao basicMaterialDao;
    @Autowired
    private DoctorMaterialInWareHouseWriteService doctorMaterialInWareHouseWriteService;
    @Autowired
    private DoctorWareHouseWriteService doctorWareHouseWriteService;
    @Autowired
    private UserInterfaceManager userInterfaceManager;
    @Autowired
    private DoctorServiceStatusDao doctorServiceStatusDao;
    @Autowired
    private DoctorServiceReviewDao doctorServiceReviewDao;
    @Autowired
    private DoctorGroupEventDao doctorGroupEventDao;
    @Autowired
    private DoctorAddressDao addressDao;
    @Autowired
    private UserDaoExt userDaoExt;
    @Autowired
    private DoctorBasicMaterialDao doctorBasicMaterialDao;
    @Autowired
    private DoctorMoveDataService doctorMoveDataService;
    @Autowired
    private DoctorFarmExportDao doctorFarmExportDao;
    @Autowired
    private DoctorUserManager doctorUserManager;
    /**
     * ??????shit???????????????????????????
     */
    @Transactional
    public DoctorFarm importAll(DoctorImportSheet shit) {
        DoctorFarm farm = null;
        try {
            // ???????????????
            Object[] result = this.importOrgFarmUser(shit.getFarm(), shit.getStaff());
            User primaryUser = (User) result[0];
            farm = (DoctorFarm) result[1];
            Map<String, Long> userMap = doctorMoveBasicService.getSubMap(farm.getOrgId());

            importBarn(farm, userMap, shit.getBarn());
            //???????????????????????????????????????????????????
            userInitService.updatePermissionBarn(farm.getId());

            importBreed(shit.getBreed());

            Map<String, DoctorBarn> barnMap = doctorMoveBasicService.getBarnMap2(farm.getId());
            Map<String, Long> breedMap = doctorMoveBasicService.getBreedMap();

            importBoar(farm, barnMap, breedMap, shit.getBoar());
            importGroup(farm, barnMap, shit.getGroup());
            importSow(farm, barnMap, breedMap, shit.getSow());

            //??????npd
            doctorMoveDataService.flushNpd(farm.getId());

            //??????????????????
            movePigTypeStatistic(farm);

            //??????????????????
            importWarehouse(farm, shit, primaryUser, userMap);

            //??????????????????
            importFarmBasics(farm.getId());
            return farm;
        } catch (Exception e) {
            // ?????????????????????????????????????????????
            deleteUser(farm);
            throw e;
        }
    }

    //???????????????????????? ????????????
    public void importFarmBasics(Long farmId) {
        List<Long> basicIds = doctorBasicDao.list(Maps.newHashMap()).stream().map(DoctorBasic::getId).collect(Collectors.toList());
        List<Long> reasonIds = doctorChangeReasonDao.list(Maps.newHashMap()).stream().map(DoctorChangeReason::getId).collect(Collectors.toList());
        List<Long> materialIds = doctorBasicMaterialDao.list(MapBuilder.<String, Integer>of().put("isValid", 1).map()).stream().map(DoctorBasicMaterial::getId).collect(Collectors.toList());
        DoctorFarmBasic farmBasic = new DoctorFarmBasic();
        farmBasic.setFarmId(farmId);
        farmBasic.setBasicIds(Joiners.COMMA.join(basicIds));
        farmBasic.setReasonIds(Joiners.COMMA.join(reasonIds));
        farmBasic.setMaterialIds(Joiners.COMMA.join(materialIds));
        doctorFarmBasicDao.create(farmBasic);
    }

    //?????????????????????
    private void movePigTypeStatistic(DoctorFarm farm) {
        doctorPigTypeStatisticWriteService.statisticGroup(farm.getOrgId(), farm.getId());
        doctorPigTypeStatisticWriteService.statisticPig(farm.getOrgId(), farm.getId(), DoctorPig.PigSex.BOAR.getKey());
        doctorPigTypeStatisticWriteService.statisticPig(farm.getOrgId(), farm.getId(), DoctorPig.PigSex.SOW.getKey());
    }

    public void deleteUser(DoctorFarm farm) {
        if (farm != null && farm.getOrgId() != null) {
            List<DoctorUserDataPermission> permissions = doctorUserDataPermissionDao.findByOrgId(farm.getOrgId());
            permissions.forEach(permission -> {
                // ????????????????????? zk ???????????????????????????
                try {
                    UserDto dto = new UserDto(permission.getUserId());
                    userInterfaceManager.pulishZkEvent(dto, EventType.DELETE, DoctorSystemCode.PIG_DOCTOR);
                } catch (Exception e) {
                    log.error("??????????????????????????????????????????????????? zk ??????????????????????????????????????????????????????farm={}, userId={}", farm, permission.getUserId());
                }
            });
        }
    }

    @Transactional
    public Object[] importOrgFarmUser(Sheet farmShit, Sheet staffShit) {
        Object[] result = this.importOrgFarm(farmShit);
        User primaryUser = (User) result[0];
        DoctorFarm farm = (DoctorFarm) result[1];
        this.importStaff(staffShit, primaryUser, farm);
        return result;
    }

    private void importStaff(Sheet staffShit, User primaryUser, DoctorFarm farm) {
        final String appKey = "MOBILE";
        List<SubRole> existRoles = subRoleDao.findByFarmIdAndStatus(appKey, farm.getId(), 1);
        if (existRoles.isEmpty()) {
            RespHelper.or500(subRoleWriteService.initDefaultRoles(appKey, primaryUser.getId(), farm.getId()));
            existRoles = subRoleDao.findByFarmIdAndStatus(appKey, farm.getId(), 1);
        }
        // key = roleName, value = roleId
        Map<String, Long> existRole = existRoles.stream().collect(Collectors.toMap(SubRole::getName, SubRole::getId));

        for (Row row : staffShit) {
            if (canImport(row)) {
                String realName = ImportExcelUtils.getStringOrThrow(row, 0);
                String loginName = ImportExcelUtils.getStringOrThrow(row, 1);
                String contact = ImportExcelUtils.getStringOrThrow(row, 2);
                String roleName = ImportExcelUtils.getStringOrThrow(row, 3);

                doctorUserManager.checkExist(contact, loginName);

                User subUser = userDaoExt.findByMobile(contact);
                Long subUserId;
                if (notNull(subUser)) {

                    subUser.setName(loginName + "@" + farm.getFarmCode());
                    subUser.setMobile(contact);
                    subUser.setPassword(EncryptUtil.encrypt("123456"));
                    subUser.setType(UserType.FARM_SUB.value());
                    subUser.setStatus(UserStatus.NORMAL.value());

                    Long roleId = existRole.get(roleName);

                    SubRole subRole;
                    if (notNull(roleId)) {
                        subRole = subRoleDao.findById(roleId);
                    } else {
                        subRole = new SubRole();
                        subRole.setName(roleName);
                        subRole.setUserId(primaryUser.getId());
                        subRole.setFarmId(farm.getId());
                        subRole.setAppKey(appKey);
                        subRole.setStatus(1);
                        subRole.setAllowJson("[]");
                        subRole.setExtraJson("{}");
                        subRoleDao.create(subRole);
                        existRole.put(roleName, subRole.getId());
                    }
                    List<String> roles = Lists.newArrayList("SUB", "SUB(SUB(" + existRole.get(roleName) + "))");
                    subUser.setRoles(roles);

                    subUser.setExtra(MapBuilder.<String, String>of()
                            .put("pid", primaryUser.getId().toString())
                            .put("contact", contact)
                            .put("realName", realName)
                            .map());

//                    Sub sub = new Sub();
//                    sub.setUserId(subUser.getId());
//                    sub.setUserName(subUser.getName());
//                    sub.setRealName(Params.get(subUser.getExtra(), "realName"));
//                    sub.setRoleId(subRole.getId());
//                    sub.setRoleName(subRole.getName());
//                    sub.setParentUserId(Long.valueOf(Params.get(subUser.getExtra(), "pid")));
//                    sub.setContact(Params.get(subUser.getExtra(), "contact"));
//                    sub.setStatus(UserStatus.NORMAL.value());
//                    subDao.create(sub);

                    userWriteService.update(subUser);
//                    userDaoExt.updateAll(subUser);
                    subUserId = subUser.getId();
                } else {
                    subUser = new User();
                    subUser.setName(loginName + "@" + farm.getFarmCode());
                    subUser.setMobile(contact);
                    subUser.setPassword("123456");
                    subUser.setType(UserType.FARM_SUB.value());
                    subUser.setStatus(UserStatus.NORMAL.value());

                    if (existRole.get(roleName) == null) {
                        SubRole subRole = new SubRole();
                        subRole.setName(roleName);
                        subRole.setUserId(primaryUser.getId());
                        subRole.setFarmId(farm.getId());
                        subRole.setAppKey(appKey);
                        subRole.setStatus(1);
                        subRole.setAllowJson("[]");
                        subRole.setExtraJson("{}");
                        subRoleDao.create(subRole);
                        existRole.put(roleName, subRole.getId());
                    }
                    List<String> roles = Lists.newArrayList("SUB", "SUB(SUB(" + existRole.get(roleName) + "))");
                    subUser.setRoles(roles);

                    subUser.setExtra(MapBuilder.<String, String>of()
                            .put("pid", primaryUser.getId().toString())
                            .put("contact", contact)
                            .put("realName", realName)
                            .map());
                    subUserId = RespHelper.or500(userWriteService.create(subUser));
                }
                //???????????????????????????
                io.terminus.doctor.user.model.Sub sub = subDao.findByUserId(subUserId);
                io.terminus.doctor.user.model.Sub updateSub = new io.terminus.doctor.user.model.Sub();
                updateSub.setId(sub.getId());
                updateSub.setFarmId(farm.getId());
                subDao.update(updateSub);

                // ?????????????????????
                //this.createStaff(subUser, farm);

                //?????????????????????
                DoctorUserDataPermission permission = new DoctorUserDataPermission();
                permission.setUserId(subUserId);
                permission.setFarmIds(farm.getId().toString());
                permission.setOrgIds(farm.getOrgId().toString());
                doctorUserDataPermissionDao.create(permission);
            }
        }
    }

    private Integer getAddressId(String name, Integer pid) {
        Address address = addressDao.findByNameAndPid(name, pid);
        if (address == null) {
            throw new JsonResponseException("?????????????????????" + name + "???");
        }
        return address.getId();
    }

    /**
     * ???????????????????????????
     *
     * @return ???????????? user
     */
    @Transactional
    private Object[] importOrgFarm(Sheet farmShit) {
        Row row1 = farmShit.getRow(1);
        String orgName = ImportExcelUtils.getStringOrThrow(row1, 0);
        String farmName = ImportExcelUtils.getStringOrThrow(row1, 1).replaceAll(" ", "");
        String loginName = ImportExcelUtils.getStringOrThrow(row1, 2);
        String mobile = ImportExcelUtils.getStringOrThrow(row1, 3);
        String realName = ImportExcelUtils.getStringOrThrow(row1, 4);
        String province = ImportExcelUtils.getStringOrThrow(row1, 5);
        String city = ImportExcelUtils.getStringOrThrow(row1, 6);
        String district = ImportExcelUtils.getStringOrThrow(row1, 7);
        String detail = ImportExcelUtils.getStringOrThrow(row1, 8);
        String companyMobile = ImportExcelUtils.getString(row1, 9); //?????????????????????

        // ??????
        DoctorOrg org = doctorOrgDao.findByName(orgName);
        if (org == null) {
            org = new DoctorOrg();
            org.setName(orgName);
            org.setMobile(mobile);
            org.setParentId(0L);
            org.setType(DoctorOrg.Type.ORG.getValue());
            doctorOrgDao.create(org);
        } else {
            log.warn("org {} has existed, id = {}", orgName, org.getId());
        }

        // ??????
        DoctorFarm farm = doctorFarmDao.findByOrgId(org.getId()).stream().filter(f -> farmName.equals(f.getName())).findFirst().orElse(null);
        if (farm == null) {
            farm = new DoctorFarm();
            farm.setOrgId(org.getId());
            farm.setOrgName(org.getName());
            farm.setName(farmName);
            farm.setFarmCode(loginName);
            farm.setProvinceId(getAddressId(province, 1));
            farm.setProvinceName(province);
            farm.setCityId(getAddressId(city, farm.getProvinceId()));
            farm.setCityName(city);
            farm.setDistrictId(getAddressId(district, farm.getCityId()));
            farm.setDistrictName(district);
            farm.setDetailAddress(detail);
            farm.setSource(SourceType.IMPORT.getValue());
            doctorFarmDao.create(farm);
            RespHelper.or500(doctorMessageRuleWriteService.initTemplate(farm.getId()));
        } else {
            log.warn("farm {} has existed, id = {}", farmName, farm.getId());
            throw new JsonResponseException("farm.has.been.existed");
        }

        // ?????????
        User user = getUser(mobile, loginName, realName);
        Long userId = user.getId();

        // ????????????????????? user profile
        UserProfile userProfile = userProfileDao.findByUserId(userId);
        userProfile.setRealName(realName);
        userProfileDao.update(userProfile);

        //?????????????????????id
        PrimaryUser primaryUser = primaryUserDao.findByUserId(userId);
        PrimaryUser updatePrimary = new PrimaryUser();
        updatePrimary.setId(primaryUser.getId());
        updatePrimary.setRelFarmId(farm.getId());
        primaryUserDao.update(updatePrimary);

        DoctorUserDataPermission permission = doctorUserDataPermissionDao.findByUserId(userId);
        if (permission == null) {
            //??????????????????
            permission = new DoctorUserDataPermission();
            permission.setUserId(userId);
            permission.setFarmIds(farm.getId().toString());
            permission.setOrgIdsList(Lists.newArrayList(org.getId()));
            doctorUserDataPermissionDao.create(permission);
        } else if (permission.getFarmIdsList() == null || !permission.getFarmIdsList().contains(farm.getId())) {
            permission.setFarmIds(permission.getFarmIds() + "," + farm.getId());
            doctorUserDataPermissionDao.update(permission);
        }

        //admin???????????????
        createOrUpdateAdminPermission();

        //???????????????????????????
        createOrUpdateMultiPermission(companyMobile, org.getId(), farm.getId());

        DoctorServiceStatus serviceStatus = doctorServiceStatusDao.findByUserId(userId);
        if (serviceStatus == null) {
            //?????????????????????
            userInitService.initDefaultServiceStatus(userId);
        } else {
            serviceStatus.setPigdoctorStatus(DoctorServiceStatus.Status.OPENED.value());
            serviceStatus.setPigdoctorReviewStatus(DoctorServiceReview.Status.OK.getValue());
            doctorServiceStatusDao.update(serviceStatus);
        }

        DoctorServiceReview review = doctorServiceReviewDao.findByUserIdAndType(userId, DoctorServiceReview.Type.PIG_DOCTOR);
        if (review == null) {
            //????????????????????????????????????
            userInitService.initServiceReview(userId, mobile, user.getName());
        } else {
            review.setStatus(DoctorServiceReview.Status.OK.getValue());
            doctorServiceReviewDao.update(review);
        }

        return new Object[]{user, farm};
    }


    private User getUser(String mobile, String loginName, String realName) {
        User user;
        Long userId;
        doctorUserManager.checkExist(mobile, loginName);
        Response<User> result = doctorUserReadService.findBy(mobile, LoginType.MOBILE);
        if(result.isSuccess() && result.getResult() != null){
            log.warn("primary user has existed, mobile={}", mobile);
            user = result.getResult();
            //??????????????????
            user.setPassword(EncryptUtil.encrypt("123456"));
            user.setName(loginName);
            user.setStatus(UserStatus.NORMAL.value());
            user.setType(UserType.FARM_ADMIN_PRIMARY.value());
            user.setRoles(Lists.newArrayList("PRIMARY", "PRIMARY(OWNER)"));
            Map<String, String> userExtraMap = Maps.newHashMap();
            userExtraMap.put("realName", realName);
            user.setExtra(userExtraMap);
            userWriteService.update(user);
            userId = user.getId();

            //??????primaryUser???????????????
            PrimaryUser primaryUser = primaryUserDao.findByUserId(userId);
            if (isNull(primaryUser)) {
                //???????????????
                primaryUser = new PrimaryUser();
                primaryUser.setUserId(userId);
                //?????????????????????
                primaryUser.setUserName(user.getMobile());
                primaryUser.setRealName(realName);
                primaryUser.setStatus(UserStatus.NORMAL.value());
                primaryUserDao.create(primaryUser);
            }
        }else{
            user = new User();
            user.setMobile(mobile);
            user.setPassword("123456");
            user.setName(loginName);
            user.setStatus(UserStatus.NORMAL.value());
            user.setType(UserType.FARM_ADMIN_PRIMARY.value());
            user.setRoles(Lists.newArrayList("PRIMARY", "PRIMARY(OWNER)"));
            Map<String, String> userExtraMap = Maps.newHashMap();
            userExtraMap.put("realName", realName);
            user.setExtra(userExtraMap);
            userWriteService.create(user);
        }
        return user;
    }

    //admin???????????????
    public void createOrUpdateAdminPermission() {
        User user = userDaoExt.findById(xrnmId);
        if (user == null) {
            return;
        }

        String orgIds = Joiners.COMMA.join(doctorOrgDao.findAll().stream().map(DoctorOrg::getId).collect(Collectors.toList()));
        String farmIds = Joiners.COMMA.join(doctorFarmDao.findAll().stream().map(DoctorFarm::getId).collect(Collectors.toList()));
        DoctorUserDataPermission permission = doctorUserDataPermissionDao.findByUserId(user.getId());
        if (permission == null) {
            permission = new DoctorUserDataPermission();
            permission.setUserId(user.getId());
            permission.setOrgIds(orgIds);
            permission.setFarmIds(farmIds);
            doctorUserDataPermissionDao.create(permission);
        } else {
            permission.setOrgIds(orgIds);
            permission.setFarmIds(farmIds);
            doctorUserDataPermissionDao.update(permission);
        }
    }

    //????????????????????????
    private void createOrUpdateMultiPermission(String mobile, Long orgId, Long farmId) {
        if (isEmpty(mobile)) {
            return;
        }
        User user = userDaoExt.findByMobile(mobile);
        if (user == null) {
            log.error("createOrUpdateMultiPermission error, mobile({}) not found", mobile);
            throw new JsonResponseException("?????????????????????(" + mobile + ")?????????????????????");
        }
        DoctorUserDataPermission permission = doctorUserDataPermissionDao.findByUserId(user.getId());
        if (permission == null) {
            log.error("createOrUpdateMultiPermission error, data permission not found, user:{}", user);
            throw new JsonResponseException("?????????????????????(" + mobile + ")??????????????????????????????");
        }
        permission.setOrgIds(permission.getOrgIds() + "," + orgId);
        permission.setFarmIds(permission.getFarmIds() + "," + farmId);
        doctorUserDataPermissionDao.update(permission);
    }


    private void createStaff(User user, DoctorFarm farm) {
        DoctorStaff staff = new DoctorStaff();
        staff.setFarmId(farm.getId());
        staff.setUserId(user.getId());
        staff.setStatus(DoctorStaff.Status.PRESENT.value());
        doctorStaffDao.create(staff);
    }

    /**
     * ????????????
     */
    @Transactional
    public void importBarn(DoctorFarm farm, Map<String, Long> userMap, Sheet shit) {
        Map<String/*barnName*/, DoctorBarn> barns = new HashMap<>();//????????????
        shit.forEach(row -> {
            //???????????????????????????
            if (canImport(row)) {

                String barnName = ImportExcelUtils.getString(row, 0);
                if (barns.containsKey(barnName))
                    throw new JsonResponseException("??????: " + row.getSheet().getSheetName() +
                            ",???????????????" + barnName + "?????????");

                DoctorBarn barn = new DoctorBarn();
                barn.setName(barnName);
                barn.setOrgId(farm.getOrgId());
                barn.setOrgName(farm.getOrgName());
                barn.setFarmId(farm.getId());
                barn.setFarmName(farm.getName());

                String barnTypeXls = ImportExcelUtils.getString(row, 1);
                PigType pigType = PigType.from(barnTypeXls);
                if (pigType != null) {
                    barn.setPigType(pigType.getValue());
                } else if ("????????????".equals(barnTypeXls) || "????????????".equals(barnTypeXls)) {
                    barn.setPigType(PigType.RESERVE.getValue());
                } else {
                    throw new JsonResponseException("?????????????????????" + barnTypeXls + "???row " + (row.getRowNum() + 1) + "column " + 2);
                }

                barn.setCanOpenGroup(DoctorBarn.CanOpenGroup.YES.getValue());
                barn.setStatus(DoctorBarn.Status.USING.getValue());
                barn.setCapacity(1000);
                barn.setStaffName(ImportExcelUtils.getString(row, 3));
                barn.setStaffId(userMap.get(barn.getStaffName()));
                barn.setExtra(ImportExcelUtils.getString(row, 4));
                barns.put(barnName, barn);
            }
        });
        if (!barns.isEmpty())
            doctorBarnDao.creates(barns.values().stream().collect(Collectors.toList()));
    }

    @Transactional
    void importBreed(Sheet shit) {
        List<String> breeds = doctorBasicDao.findByType(DoctorBasic.Type.BREED.getValue()).stream()
                .map(DoctorBasic::getName).collect(Collectors.toList());
        shit.forEach(row -> {
            if (canImport(row)) {
                String breedName = ImportExcelUtils.getString(row, 0);
                if (!breeds.contains(breedName)) {
                    DoctorBasic basic = new DoctorBasic();
                    basic.setName(breedName);
                    basic.setType(DoctorBasic.Type.BREED.getValue());
                    basic.setTypeName(DoctorBasic.Type.BREED.getDesc());
                    basic.setIsValid(1);
                    basic.setSrm(ImportExcelUtils.getString(row, 1));
                    doctorBasicDao.create(basic);
                }
            }
        });
    }

    /**
     * ????????????
     */
    @Transactional
    public void importBoar(DoctorFarm farm, Map<String, DoctorBarn> barnMap, Map<String, Long> breedMap, Sheet shit) {
        for (Row row : shit) {
            if (!canImport(row)) {
                continue;
            }

            //??????
            DoctorPig boar = new DoctorPig();
            boar.setOrgId(farm.getOrgId());
            boar.setOrgName(farm.getOrgName());
            boar.setFarmId(farm.getId());
            boar.setFarmName(farm.getName());
            boar.setPigCode(ImportExcelUtils.getString(row, 1));
            boar.setPigType(DoctorPig.PigSex.BOAR.getKey());
            boar.setIsRemoval(IsOrNot.NO.getValue());
            boar.setPigFatherCode(ImportExcelUtils.getString(row, 4));
            boar.setPigMotherCode(ImportExcelUtils.getString(row, 5));
            PigSource source = PigSource.from(ImportExcelUtils.getString(row, 7));
            if (source != null) {
                boar.setSource(source.getKey());
            }
            boar.setBirthDate(ImportExcelUtils.getDate(row, 3));
            boar.setInFarmDate(ImportExcelUtils.getDate(row, 2));

            if (boar.getBirthDate() == null || boar.getInFarmDate() == null) {
                throw new JsonResponseException("????????????" + boar.getPigCode() + " ?????????????????????????????????" + (row.getRowNum() + 1));
            }

            boar.setInitBarnName(ImportExcelUtils.getString(row, 0));
            DoctorBarn barn = barnMap.get(boar.getInitBarnName());
            if (barn != null) {
                boar.setInitBarnId(barn.getId());
            }
            boar.setBreedName(ImportExcelUtils.getString(row, 6));
            if (!StringUtils.isBlank(boar.getBreedName())) {
                boar.setBreedId(breedMap.get(boar.getBreedName()));
                if (boar.getBreedId() == null) {
                    throw new JsonResponseException("?????????????????????" + boar.getBreedName() + "???row " + (row.getRowNum() + 1) + "column" + 7);
                }
            }

            //?????????????????????
            BoarEntryType entryType = BoarEntryType.from(ImportExcelUtils.getString(row, 8));
            boar.setBoarType(entryType == null ? BoarEntryType.HGZ.getKey() : entryType.getKey());
            doctorPigDao.create(boar);

            //??????????????????
            DoctorBarn doctorBarn = doctorBarnDao.findById(boar.getInitBarnId());
            DoctorPigEvent boarEntryEvent = DoctorPigEvent.builder()
                    .orgId(boar.getOrgId())
                    .orgName(boar.getOrgName())
                    .farmId(boar.getFarmId())
                    .farmName(boar.getFarmName())
                    .pigId(boar.getId())
                    .pigCode(boar.getPigCode())
                    .type(PigEvent.ENTRY.getKey())
                    .name(PigEvent.ENTRY.getName())
                    .kind(DoctorPig.PigSex.BOAR.getKey())
                    .isAuto(IsOrNot.YES.getValue())
                    .eventAt(boar.getInFarmDate())
                    .barnId(boar.getInitBarnId())
                    .barnName(boar.getInitBarnName())
                    .barnType(doctorBarn.getPigType())
                    .creatorId(boar.getCreatorId())
                    .creatorName(boar.getCreatorName())
                    .operatorId(boar.getCreatorId())
                    .operatorName(boar.getCreatorName())
                    .status(EventStatus.VALID.getValue())
                    .eventSource(SourceType.IMPORT.getValue())
                    .source(boar.getSource())
                    .breedId(boar.getBreedId())
                    .breedName(boar.getBreedName())
                    .breedTypeId(boar.getGeneticId())
                    .breedTypeName(boar.getGeneticName())
                    .boarType(boar.getBoarType())
                    .npd(0)
                    .dpnpd(0)
                    .pfnpd(0)
                    .plnpd(0)
                    .psnpd(0)
                    .pynpd(0)
                    .ptnpd(0)
                    .jpnpd(0)
                    .build();
            Map<String, String> fieldMap = Maps.newHashMap();
            if (!Strings.isNullOrEmpty(boar.getBreedName())) {
                fieldMap.put("??????", boar.getBreedName());
            }
            String desc = Joiner.on("#").withKeyValueSeparator("???").join(fieldMap);
            boarEntryEvent.setDesc(desc);
            doctorPigEventDao.create(boarEntryEvent);

            //????????????
            DoctorPigTrack boarTrack = new DoctorPigTrack();
            boarTrack.setFarmId(boar.getFarmId());
            boarTrack.setPigId(boar.getId());
            boarTrack.setPigType(boar.getPigType());
            boarTrack.setStatus(PigStatus.BOAR_ENTRY.getKey());
            boarTrack.setIsRemoval(boar.getIsRemoval());
            boarTrack.setCurrentBarnId(boar.getInitBarnId());
            boarTrack.setCurrentBarnName(boar.getInitBarnName());
            boarTrack.setCurrentBarnType(barn.getPigType());
            boarTrack.setCurrentParity(1);      //??????????????????1
            DoctorPigEvent lastEvent = doctorPigEventDao.queryLastPigEventById(boar.getId());
            boarTrack.setCurrentEventId(notNull(lastEvent) ? lastEvent.getId() : 0L);
            doctorPigTrackDao.create(boarTrack);
        }
    }

    //???????????????????????????
    private void checkGroupCodeExist(Long farmId, String groupCode) {
        List<DoctorGroup> groups = doctorGroupDao.findByFarmId(farmId);
        if (groups.stream().map(DoctorGroup::getGroupCode).collect(Collectors.toList()).contains(groupCode)) {
            throw new ServiceException("??????????????????" + groupCode + "???");
        }
    }

    /**
     * ????????????
     */
    @Transactional
    private void importGroup(DoctorFarm farm, Map<String, DoctorBarn> barnMap, Sheet shit) {
        List<String> existGroupCode = new ArrayList<>();
        for (Row row : shit) {
            if (!canImport(row)) {
                continue;
            }

            //??????
            DoctorGroup group = new DoctorGroup();
            group.setOrgId(farm.getOrgId());
            group.setOrgName(farm.getOrgName());
            group.setFarmId(farm.getId());
            group.setFarmName(farm.getName());
            String code = ImportExcelUtils.getString(row, 0);
            group.setGroupCode(code);
            if (existGroupCode.contains(code)) {
                throw new JsonResponseException("????????????" + code + "?????????");
            } else {
                existGroupCode.add(code);
            }

            Date openAt = ImportExcelUtils.getDate(row, 6);
            if (openAt == null) {
                throw new JsonResponseException("????????????" + code + "???????????????????????????");
            }

            group.setOpenAt(openAt);  //????????????
            group.setStatus(DoctorGroup.Status.CREATED.getValue());
            group.setInitBarnName(ImportExcelUtils.getString(row, 1));

            DoctorBarn barn = barnMap.get(group.getInitBarnName());
            if (barn != null) {
                group.setInitBarnId(barn.getId());
                group.setPigType(barn.getPigType());
                group.setStaffId(barn.getStaffId());
                group.setStaffName(barn.getStaffName());
            } else {
                throw new JsonResponseException("??????????????????" + group.getGroupCode() + "?????????????????????" + group.getInitBarnName() + "???");
            }
            group.setCurrentBarnId(group.getInitBarnId());
            group.setCurrentBarnName(group.getInitBarnName());
            doctorGroupDao.create(group);

            //????????????
            DoctorGroupTrack groupTrack = new DoctorGroupTrack();
            groupTrack.setGroupId(group.getId());

            DoctorGroupTrack.Sex sex = DoctorGroupTrack.Sex.from(ImportExcelUtils.getString(row, 2));
            if (sex != null) {
                groupTrack.setSex(sex.getValue());
            }
            groupTrack.setQuantity(ImportExcelUtils.getInt(row, 3));
            groupTrack.setBoarQty(0);
            groupTrack.setSowQty(groupTrack.getQuantity() - groupTrack.getBoarQty());

            //excel??????????????????????????????????????????????????????
            Integer dayAge = MoreObjects.firstNonNull(ImportExcelUtils.getInt(row, 4), 1);
            groupTrack.setAvgDayAge(dayAge + DateUtil.getDeltaDaysAbs(openAt, new Date()));
            groupTrack.setBirthDate(DateTime.now().minusDays(groupTrack.getAvgDayAge()).toDate());

            Double avgWeight = ImportExcelUtils.getDoubleOrDefault(row, 5, 0);

            groupTrack.setBirthWeight(avgWeight * groupTrack.getQuantity());

            //?????????????????????????????????
            if (PigType.FARROW_TYPES.contains(group.getPigType())) {
                groupTrack.setWeanWeight(groupTrack.getBirthWeight());
                groupTrack.setNest(0);
                groupTrack.setLiveQty(groupTrack.getQuantity());
                groupTrack.setHealthyQty(groupTrack.getQuantity());
                groupTrack.setWeakQty(0);
                groupTrack.setUnweanQty(0);
                groupTrack.setUnqQty(0);
                groupTrack.setWeanQty(groupTrack.getQuantity());    //??????????????????
                groupTrack.setQuaQty(groupTrack.getQuantity());
            }
            doctorGroupTrackDao.create(groupTrack);
            DoctorGroupEvent moveInEvent = createMoveInGroupEvent(group, groupTrack, dayAge, avgWeight, false);

            //groupTrack??????????????????
            groupTrack.setRelEventId(moveInEvent.getId());
            doctorGroupTrackDao.update(groupTrack);
        }
    }

    /**
     * ???????????????????????????
     */
    private DoctorGroupEvent createMoveInGroupEvent(DoctorGroup group, DoctorGroupTrack groupTrack, Integer dayAge, Double avgWeight, boolean isSowEvent) {
        DoctorGroupEvent event = new DoctorGroupEvent();
        event.setOrgId(group.getOrgId());
        event.setOrgName(group.getOrgName());
        event.setFarmId(group.getFarmId());
        event.setFarmName(group.getFarmName());
        event.setGroupId(group.getId());
        event.setGroupCode(group.getGroupCode());
        event.setEventAt(group.getOpenAt());
        event.setType(GroupEventType.MOVE_IN.getValue());
        event.setName(GroupEventType.MOVE_IN.getDesc());
        event.setBarnId(group.getInitBarnId());
        event.setBarnName(group.getInitBarnName());
        event.setPigType(group.getPigType());
        event.setQuantity(groupTrack.getQuantity());
        event.setAvgWeight(avgWeight);
        event.setWeight(event.getQuantity() * avgWeight);
        event.setDesc("???????????????????????????#????????????" + groupTrack.getQuantity() + "#???????????????" + groupTrack.getAvgDayAge() + "#?????????" + avgWeight);
        event.setAvgDayAge(dayAge);
        event.setIsAuto(IsOrNot.YES.getValue());
        event.setInType(InType.PIGLET.getValue());
        event.setStatus(EventStatus.VALID.getValue());
        event.setEventSource(SourceType.IMPORT.getValue());
        if (isSowEvent) {
            event.setRelPigEventId(-1L);
            event.setSowId(-1L);
            event.setSowCode("-1");
        } else {
            event.setRelGroupEventId(-1L);
        }
        doctorGroupEventDao.create(event);
        return event;
    }

    /**
     * ?????????????????????
     *
     * @param feedSowLast ?????????????????????????????????????????????
     */
    private void importFarrowPiglet(List<DoctorPigTrack> feedSowTrack, List<DoctorImportSow> feedSowLast, Map<String, DoctorBarn> barnMap, DoctorFarm farm) {
        Map<Long, List<DoctorPigTrack>> feedMap = feedSowTrack.stream().collect(Collectors.groupingBy(DoctorPigTrack::getCurrentBarnId));

        // ????????????????????????
        Map<String, List<DoctorImportSow>> sowMap = feedSowLast.stream().collect(Collectors.groupingBy(DoctorImportSow::getBarnName));
        sowMap.entrySet().forEach(entry -> {
            DoctorBarn barn = barnMap.get(entry.getKey());
            List<DoctorImportSow> sows = entry.getValue();
            Date openAt = sows.stream().map(sow -> sow.getPregDate() == null ? new Date() : sow.getPregDate()).min(Date::compareTo).orElse(new Date()); // ?????????????????????????????????????????????
            Integer pigletCount = sows.stream().map(DoctorImportSow::getHealthyCount).reduce((a, b) -> a + b).orElse(0); // ?????????
            Integer weak = sows.stream().map(DoctorImportSow::getWeakCount).reduce((a, b) -> a + b).orElse(0); // ?????????

            DoctorGroup group = new DoctorGroup();
            group.setOrgId(farm.getOrgId());
            group.setOrgName(farm.getOrgName());
            group.setFarmId(farm.getId());
            group.setFarmName(farm.getName());
            group.setGroupCode(barn.getName() + "(" + DateUtil.toDateString(openAt) + ")");
            // ???????????????????????????
            checkGroupCodeExist(farm.getId(), group.getGroupCode());

            group.setOpenAt(openAt);  //????????????
            group.setStatus(DoctorGroup.Status.CREATED.getValue());
            group.setInitBarnName(barn.getName());
            group.setInitBarnId(barn.getId());
            group.setPigType(barn.getPigType());
            group.setStaffId(barn.getStaffId());
            group.setStaffName(barn.getStaffName());
            group.setCurrentBarnId(group.getInitBarnId());
            group.setCurrentBarnName(group.getInitBarnName());
            doctorGroupDao.create(group);

            //final double baseWeight = 1.5D;
            DoctorGroupTrack groupTrack = new DoctorGroupTrack();
            groupTrack.setGroupId(group.getId());
            groupTrack.setSex(DoctorGroupTrack.Sex.MIX.getValue());
            groupTrack.setQuantity(pigletCount);
            groupTrack.setBoarQty(0);
            groupTrack.setSowQty(groupTrack.getQuantity() - groupTrack.getBoarQty());
            groupTrack.setAvgDayAge(DateUtil.getDeltaDaysAbs(new DateTime(openAt).withTimeAtStartOfDay().toDate(), DateTime.now().withTimeAtStartOfDay().toDate()));
            groupTrack.setBirthDate(openAt);
            groupTrack.setWeakQty(weak);
            groupTrack.setUnweanQty(pigletCount);

            //??????????????????
            groupTrack.setNest(sows.size());
            groupTrack.setWeanWeight(groupTrack.getBirthWeight());
            groupTrack.setLiveQty(groupTrack.getQuantity());
            groupTrack.setHealthyQty(groupTrack.getQuantity());
            groupTrack.setUnqQty(0);
            groupTrack.setWeanQty(0);    //?????????????????????
            groupTrack.setQuaQty(0);

            if (groupTrack.getQuantity() == null) {
                throw new JsonResponseException("???????????????????????????????????????????????????" + group.getGroupCode());
            }

            doctorGroupTrackDao.create(groupTrack);
            DoctorGroupEvent moveInEvent = createMoveInGroupEvent(group, groupTrack, 1, MoreObjects.firstNonNull(groupTrack.getBirthWeight(), 7D) / groupTrack.getQuantity(), true);

            //groupTrack??????????????????
            groupTrack.setRelEventId(moveInEvent.getId());
            doctorGroupTrackDao.update(groupTrack);

            // ??? ??????????????? ???groupId ?????????????????????????????????
            List<DoctorPigTrack> feedTracks = feedMap.get(group.getInitBarnId());
            if (notEmpty(feedTracks)) {
                feedTracks.forEach(feedTrack -> {
                    DoctorPigTrack newTrack = new DoctorPigTrack();
                    newTrack.setId(feedTrack.getId());
                    newTrack.setGroupId(group.getId());
                    doctorPigTrackDao.update(newTrack);

                    //??????????????????groupId
                    setFarrowGroupId(feedTrack.getPigId(), group.getId());
                });
            }
        });
    }

    //???????????????????????????????????????????????????????????????groupId
    private void setFarrowGroupId(Long pigId, Long groupId) {
        List<DoctorPigEvent> pigEvents = doctorPigEventDao.limitPigEventOrderByEventAt(pigId, 2);
        pigEvents.stream()
                .filter(event -> Objects.equals(event.getType(), PigEvent.FARROWING.getKey())
                        || Objects.equals(event.getType(), PigEvent.WEAN.getKey()))
                .forEach(event -> {
                    DoctorPigEvent updateEvent = new DoctorPigEvent();
                    updateEvent.setId(event.getId());
                    updateEvent.setGroupId(groupId);
                    doctorPigEventDao.update(updateEvent);
                });
    }

    /**
     * ????????????
     */
    @Transactional
    private void importSow(DoctorFarm farm, Map<String, DoctorBarn> barnMap, Map<String, Long> breedMap, Sheet shit) {
        List<DoctorImportSow> feedSowLast = new ArrayList<>(); // ?????????????????????????????????????????????
        List<DoctorPigTrack> feedSowTrack = new ArrayList<>();
        Map<String, List<DoctorImportSow>> sowMap = getImportSows(shit).stream().collect(Collectors.groupingBy(DoctorImportSow::getSowCode));
        sowMap.entrySet().forEach(map -> {
            List<DoctorImportSow> importSows = map.getValue().stream().sorted(Comparator.comparing(DoctorImportSow::getParity)).collect(Collectors.toList());
            int size = importSows.size();

            DoctorImportSow first = importSows.get(0);
            DoctorImportSow last = importSows.get(size - 1);
            checkStatus(last);
            DoctorPig sow = getSow(farm, barnMap, breedMap, first, last);

            //?????????????????????
            DoctorPigEvent entryEvent = createEntryEvent(first, sow);
            Map<Integer, List<Long>> parityMap = MapBuilder.<Integer, List<Long>>of()
                    .put(first.getParity(), Lists.newArrayList(entryEvent.getId())).map();

//            int delta = 0;
//            boolean isPreg = true;
            for (int i = 0; i < size; i++) {
                DoctorImportSow is = importSows.get(i);

                //??????????????????????????????????????????
//                if (i == 0) {
//                    delta = is.getParity();
//                } else {
//                    if ((is.getParity() - i) != delta) {
//                        if (!isPreg) {
//                            throw new JsonResponseException("?????????(" + sow.getPigCode() + ")?????????( " + is.getParity() + " )?????????, ?????????");
//                        }
//                    }
//                    isPreg = true;
//                    if (notEmpty(is.getRemark()) && is.getRemark().contains("?????????")) {
//                        isPreg = false;
//                        delta -= 1;
//                    }
//                }

                //???????????????????????????????????????????????????????????????
                if (i == size - 1) {
                    if (Objects.equals(is.getStatus(), PigStatus.Entry.getKey())) {
                        continue;
                    }
                    if (Objects.equals(is.getStatus(), PigStatus.Mate.getKey())) {
                        DoctorPigEvent mateEvent = createMateEvent(is, sow, entryEvent, DoctorMatingType.DP, null, null);
                        putParityMap(parityMap, is.getParity(), Lists.newArrayList(mateEvent.getId()));
                        continue;
                    }
                    if (Objects.equals(is.getStatus(), PigStatus.Pregnancy.getKey())) {
                        DoctorPigEvent mateEvent = createMateEvent(is, sow, entryEvent, DoctorMatingType.DP, 1, null);
                        DoctorPigEvent pregYang = createPregCheckEvent(is, sow, mateEvent, PregCheckResult.YANG, null);
                        putParityMap(parityMap, is.getParity(), Lists.newArrayList(mateEvent.getId(), pregYang.getId()));
                        continue;
                    }
                    if (Objects.equals(is.getStatus(), PigStatus.KongHuai.getKey())) {
                        DoctorPigEvent mateEvent = createMateEvent(is, sow, entryEvent, DoctorMatingType.DP, null, null);
                        DoctorPigEvent pregYang = createPregCheckEvent(is, sow, mateEvent, getCheckResultByRemark(is.getRemark()), getCheckDateByRemark(is.getRemark()));
                        putParityMap(parityMap, is.getParity(), Lists.newArrayList(mateEvent.getId(), pregYang.getId()));
                        continue;
                    }
                    if (Objects.equals(is.getStatus(), PigStatus.FEED.getKey())) {
                        DoctorPigEvent mateEvent = createMateEvent(is, sow, entryEvent, DoctorMatingType.DP, 1, 1);
                        DoctorPigEvent pregYang = createPregCheckEvent(is, sow, mateEvent, PregCheckResult.YANG, null);
                        DoctorPigEvent farrowEvent = createFarrowEvent(is, sow, pregYang);
                        putParityMap(parityMap, is.getParity(), Lists.newArrayList(mateEvent.getId(), pregYang.getId(), farrowEvent.getId()));
                        feedSowLast.add(last);
                        continue;
                    }
                    //?????????????????????????????????
                }

                //??????????????????????????? -> ?????? -> ?????? -> ??????
                DoctorPigEvent mateEvent = createMateEvent(is, sow, entryEvent, DoctorMatingType.DP, 1, 1);
                putParityMap(parityMap, is.getParity(), Lists.newArrayList(mateEvent.getId()));

                //??????????????????????????????????????????????????????
                if (notEmpty(is.getRemark()) && is.getRemark().contains("?????????")) {
                    DoctorPigEvent pregNotYang = createPregCheckEvent(is, sow, mateEvent, getCheckResultByRemark(is.getRemark()), getCheckDateByRemark(is.getRemark()));
                    putParityMap(parityMap, is.getParity(), Lists.newArrayList(pregNotYang.getId()));
                } else {
                    DoctorPigEvent pregYang = createPregCheckEvent(is, sow, mateEvent, PregCheckResult.YANG, null);

                    // ??????????????????????????????????????????????????? ??????????????????
                    DoctorPigEvent toFarrowEvent = new DoctorPigEvent();
                    if (last.getStatus().equals(PigStatus.Pregnancy.getKey()) && barnMap.get(last.getBarnName()).getPigType().equals(PigType.DELIVER_SOW.getValue())) {
                        toFarrowEvent = createToFarrowEvent(is, sow, pregYang);
                    }

                    DoctorPigEvent farrowEvent = createFarrowEvent(is, sow, pregYang);
                    DoctorPigEvent weanEvent = createWeanEvent(is, sow, farrowEvent);
                    putParityMap(parityMap, is.getParity(), Lists.newArrayList(pregYang.getId(), toFarrowEvent.getId(), farrowEvent.getId(), weanEvent.getId()));
                }
            }

            DoctorBarn barn = barnMap.get(last.getBarnName());
            if (isNull(barn)) {
                throw new JsonResponseException("?????????????????????????????????,?????????:" + last.getBarnName());
            }
            DoctorPigTrack track = getSowTrack(sow, last, parityMap, barn);

            if (Objects.equals(track.getStatus(), PigStatus.FEED.getKey())) {
                feedSowTrack.add(track);
            }
        });

        // ??????????????????????????????holy shit
        this.importFarrowPiglet(feedSowTrack, feedSowLast, barnMap, farm);
    }

    private static PregCheckResult getCheckResultByRemark(String remark) {
        if (notEmpty(remark) && remark.contains("??????")) {
            return PregCheckResult.FANQING;
        }
        if (notEmpty(remark) && remark.contains("??????")) {
            return PregCheckResult.LIUCHAN;
        }
        return PregCheckResult.YING;
    }

    private static Date getCheckDateByRemark(String remark) {
        try {
            if (isEmpty(remark)) {
                return null;
            }
            return DateUtil.toDate(remark.substring(remark.length() - 10, remark.length()));
        } catch (Exception e) {
            log.error("get check date by remark failed, remark:{}, cause:{}", remark, Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("?????????????????????????????????????????????" + remark);
        }
    }

    //?????????????????????id??????????????????list???
    private void putParityMap(Map<Integer, List<Long>> parityMap, Integer parity, List<Long> eventIds) {
        List<Long> ids = MoreObjects.firstNonNull(parityMap.get(parity), Lists.<Long>newArrayList());
        ids.addAll(eventIds);
        parityMap.put(parity, ids);
    }

    private void checkStatus(DoctorImportSow is) {
        if (is == null || is.getStatus() == null) {
            throw new ServiceException("sow.status.error");
        }
    }

    //????????????
    private DoctorPig getSow(DoctorFarm farm, Map<String, DoctorBarn> barnMap, Map<String, Long> breedMap, DoctorImportSow first, DoctorImportSow last) {
        DoctorPig sow = new DoctorPig();
        sow.setOrgId(farm.getOrgId());
        sow.setOrgName(farm.getOrgName());
        sow.setFarmId(farm.getId());
        sow.setFarmName(farm.getName());
        sow.setPigCode(last.getSowCode());
        sow.setIsRemoval(IsOrNot.NO.getValue());
        sow.setPigFatherCode(last.getFatherCode());
        sow.setPigMotherCode(last.getMotherCode());
        sow.setSource(PigSource.LOCAL.getKey());
        sow.setBirthWeight(0D);

        //??????????????????
        if (notNull(first.getInFarmDate())) {
            sow.setInFarmDate(first.getInFarmDate());
        } else if (first.getMateDate() != null) {
            sow.setInFarmDate(new DateTime(first.getMateDate()).plusDays(-1).toDate()); //?????????????????????????????????????????????
        } else {
            throw new JsonResponseException("??????:" + sow.getPigCode() + "??????????????????");
        }

        sow.setInitBarnName(last.getBarnName());
        sow.setPigType(DoctorPig.PigSex.SOW.getKey());   //??????
        if (last.getBirthDate() != null) {
            sow.setBirthDate(last.getBirthDate());
            sow.setInFarmDayAge(DateUtil.getDeltaDaysAbs(sow.getInFarmDate(), sow.getBirthDate()));
        }

        DoctorBarn barn = barnMap.get(last.getBarnName());
        if (barn != null) {
            sow.setInitBarnId(barn.getId());
        }
        if (StringUtils.isNotBlank(last.getBreed())) {
            sow.setBreedName(last.getBreed());
            sow.setBreedId(breedMap.get(last.getBreed()));
            if (sow.getBreedId() == null) {
                throw new JsonResponseException("?????????:" + sow.getPigCode() + " ??????????????????:" + sow.getBreedName());
            }
        }
        doctorPigDao.create(sow);
        return sow;
    }

    //??????????????????
    private DoctorPigTrack getSowTrack(DoctorPig sow, DoctorImportSow last, Map<Integer, List<Long>> parityMap, DoctorBarn barn) {
        DoctorPigTrack sowTrack = new DoctorPigTrack();
        sowTrack.setFarmId(sow.getFarmId());
        sowTrack.setPigId(sow.getId());
        sowTrack.setPigType(sow.getPigType());

        // ??????????????????????????????????????????????????? ??????????????? ?????????
        if (last.getStatus().equals(PigStatus.Pregnancy.getKey()) && barn.getPigType().equals(PigType.DELIVER_SOW.getValue())) {
            sowTrack.setStatus(PigStatus.Farrow.getKey());
        } else {
            sowTrack.setStatus(last.getStatus());
        }
        sowTrack.setIsRemoval(sow.getIsRemoval());
        sowTrack.setCurrentBarnId(sow.getInitBarnId());
        sowTrack.setCurrentBarnName(sow.getInitBarnName());
        sowTrack.setCurrentBarnType(barn.getPigType());
        sowTrack.setWeight(sow.getBirthWeight());
        sowTrack.setCurrentParity(last.getParity());

        Map<Integer, String> relMap = Maps.newHashMap();
        parityMap.entrySet().forEach(map -> relMap.put(map.getKey(), Joiners.COMMA.join(map.getValue())));

        sowTrack.setRelEventIds(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(relMap));
        sowTrack.setCurrentMatingCount(0);       //??????????????????0 ????????????????????????1
        sowTrack.setFarrowQty(0);
        sowTrack.setUnweanQty(0);
        sowTrack.setWeanQty(0);
        sowTrack.setFarrowAvgWeight(0D);
        sowTrack.setWeanAvgWeight(0D);

        if (Objects.equals(sowTrack.getStatus(), PigStatus.FEED.getKey())) {
            sowTrack.setGroupId(getGroup(sowTrack.getCurrentBarnId()).getId());
            sowTrack.setFarrowQty(last.getHealthyCount());
            sowTrack.setUnweanQty(last.getHealthyCount());
            sowTrack.setFarrowAvgWeight(MoreObjects.firstNonNull(last.getNestWeight(), 0D)
                    / (last.getHealthyCount() == 0 ? 1 : last.getHealthyCount()));
        }

        Map<String, Object> extra = new HashMap<>();
        if (last.getPrePregDate() != null) {
            extra.put("judgePregDate", last.getPrePregDate());
        }
        if (!extra.isEmpty()) {
            sowTrack.setExtraMap(extra);
        }
        DoctorPigEvent lastEvent = doctorPigEventDao.queryLastPigEventById(sow.getId());
        sowTrack.setCurrentEventId(notNull(lastEvent) ? lastEvent.getId() : 0L);

        doctorPigTrackDao.create(sowTrack);
        return sowTrack;
    }

    private DoctorGroup getGroup(Long barnId) {
        return doctorGroupDao.findByCurrentBarnId(barnId).stream()
                .filter(group -> group.getStatus() == DoctorGroup.Status.CREATED.getValue())
                .findFirst()
                .orElse(new DoctorGroup());
    }

    private DoctorPigEvent createSowEvent(DoctorImportSow info, DoctorPig sow) {
        DoctorPigEvent event = new DoctorPigEvent();
        event.setOrgId(sow.getOrgId());
        event.setOrgName(sow.getOrgName());
        event.setFarmId(sow.getFarmId());
        event.setFarmName(sow.getFarmName());
        event.setPigId(sow.getId());
        event.setPigCode(sow.getPigCode());
        event.setIsAuto(IsOrNot.NO.getValue());
        event.setKind(DoctorPig.PigSex.SOW.getKey());
        event.setBarnId(sow.getInitBarnId());
        event.setBarnName(sow.getInitBarnName());
        DoctorBarn doctorBarn = doctorBarnDao.findById(sow.getInitBarnId());
        if (isNull(doctorBarn)) {
            throw new JsonResponseException("??????:" + event.getBarnName() + ",?????????");
        }
        event.setBarnType(doctorBarn.getPigType());
        event.setRemark(info.getRemark());
        event.setStatus(EventStatus.VALID.getValue());
        return event;
    }

    //??????????????????
    private DoctorPigEvent createEntryEvent(DoctorImportSow info, DoctorPig sow) {
        DoctorPigEvent event = createSowEvent(info, sow);
        event.setEventAt(sow.getInFarmDate());
        event.setType(PigEvent.ENTRY.getKey());
        event.setName(PigEvent.ENTRY.getName());
        event.setPigStatusAfter(PigStatus.Entry.getKey());
        event.setParity(info.getParity());

        //??????extra
        DoctorFarmEntryDto entry = new DoctorFarmEntryDto();
        entry.setPigType(sow.getPigType());
        entry.setPigCode(sow.getPigCode());
        entry.setBirthday(sow.getBirthDate());
        entry.setInFarmDate(sow.getInFarmDate());
        entry.setBarnId(event.getBarnId());
        entry.setBarnName(event.getBarnName());
        entry.setSource(PigSource.LOCAL.getKey());
        entry.setBreed(sow.getBreedId());
        entry.setBreedName(sow.getBreedName());
        entry.setFatherCode(sow.getPigFatherCode());
        entry.setMotherCode(sow.getPigMotherCode());
        entry.setEntryMark(event.getRemark());
        entry.setParity(event.getParity());

        //??????
        event.setSource(entry.getSource());
        event.setBreedId(entry.getBreed());
        event.setBreedName(entry.getBreedName());
        event.setBreedTypeId(entry.getBreedType());
        event.setBreedTypeName(entry.getBreedTypeName());
        event.setDesc(getEventDesc(entry.descMap()));
        event.setEventSource(SourceType.IMPORT.getValue());
        event.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(entry));
        if (event.getEventAt() == null) {
            throw new JsonResponseException("?????????" + event.getPigCode() + "???????????????????????????????????????????????????");
        }
        if (event.getEventAt().after(new Date())) {
            event.setEventAt(DateTime.now().minusDays(1).toDate());
        }
        doctorPigEventDao.create(event);
        return event;
    }

    // ?????????????????????
    private DoctorPigEvent createToFarrowEvent(DoctorImportSow info, DoctorPig sow, DoctorPigEvent beforeEvent) {
        DoctorPigEvent event = createSowEvent(info, sow);
        event.setEventAt(new DateTime(info.getPrePregDate()).minusDays(7).toDate());
        event.setType(PigEvent.TO_FARROWING.getKey());
        event.setName(PigEvent.TO_FARROWING.getName());
        event.setRelEventId(beforeEvent.getId());
        event.setPigStatusBefore(PigStatus.Pregnancy.getKey());
        event.setPigStatusAfter(PigStatus.Farrow.getKey());
        event.setParity(info.getParity());
        event.setBoarCode(info.getBoarCode());

        DoctorChgLocationDto extra = new DoctorChgLocationDto();
        extra.setChgLocationToBarnName(info.getBarnName());
        event.setEventSource(SourceType.IMPORT.getValue());
        event.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(extra));
        event.setDesc(getEventDesc(extra.descMap()));
        if (event.getEventAt() == null) {
            throw new JsonResponseException("?????????" + event.getPigCode() + "?????????????????????????????????????????????????????????");
        }
        if (event.getEventAt().after(new Date())) {
            event.setEventAt(DateTime.now().minusDays(1).toDate());
        }
        doctorPigEventDao.create(event);
        return event;
    }

    //??????????????????
    private DoctorPigEvent createMateEvent(DoctorImportSow info, DoctorPig sow, DoctorPigEvent beforeEvent, DoctorMatingType mateType, Integer isPreg, Integer isDevelivery) {
        DoctorPigEvent event = createSowEvent(info, sow);
        event.setEventAt(info.getMateDate());
        event.setType(PigEvent.MATING.getKey());
        event.setName(PigEvent.MATING.getName());
        event.setRelEventId(beforeEvent.getId());
        event.setPigStatusBefore(PigStatus.Entry.getKey());
        event.setPigStatusAfter(PigStatus.Mate.getKey());
        event.setParity(info.getParity());
        event.setCurrentMatingCount(1);     //????????????
        event.setMattingDate(event.getEventAt());
        event.setDoctorMateType(mateType.getKey());
        event.setBoarCode(info.getBoarCode());
        event.setIsImpregnation(isPreg);        //????????????
        event.setIsDelivery(isDevelivery);      //????????????
        event.setEventSource(SourceType.IMPORT.getValue());

        DoctorMatingDto mate = new DoctorMatingDto();
        mate.setMatingBoarPigCode(info.getBoarCode());
        mate.setJudgePregDate(info.getPrePregDate());
        event.setJudgePregDate(mate.getJudgePregDate());
        event.setDesc(getEventDesc(mate.descMap()));
        event.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(mate));
        if (event.getEventAt() == null) {
            throw new JsonResponseException("?????????" + event.getPigCode() + "???????????????????????????????????????????????????");
        }
        if (event.getEventAt().after(new Date())) {
            event.setEventAt(DateTime.now().minusDays(1).toDate());
        }
        doctorPigEventDao.create(event);
        return event;
    }

    //??????????????????
    private DoctorPigEvent createPregCheckEvent(DoctorImportSow info, DoctorPig sow, DoctorPigEvent beforeEvent, PregCheckResult checkResult, Date pregDate) {
        DoctorPigEvent event = createSowEvent(info, sow);
        event.setEventAt(MoreObjects.firstNonNull(pregDate, new DateTime(beforeEvent.getEventAt()).plusWeeks(3).toDate()));  //?????????????????? = ???????????? + 3???
        event.setType(PigEvent.PREG_CHECK.getKey());
        event.setName(PigEvent.PREG_CHECK.getName());
        event.setRelEventId(beforeEvent.getId());
        event.setPigStatusBefore(PigStatus.Mate.getKey());
        event.setEventSource(SourceType.IMPORT.getValue());

        //????????????
        if (checkResult == PregCheckResult.YANG) {
            event.setPigStatusAfter(PigStatus.Pregnancy.getKey());
        } else {
            event.setPigStatusAfter(PigStatus.KongHuai.getKey());
        }
        event.setParity(info.getParity());
        event.setPregCheckResult(checkResult.getKey());
        event.setCheckDate(event.getEventAt());
        event.setRemark(info.getRemark());

        //??????extra
        DoctorPregChkResultDto result = new DoctorPregChkResultDto();
        result.setCheckDate(event.getCheckDate());
        result.setCheckResult(checkResult.getKey());
        result.setCheckMark(info.getRemark());

        event.setDesc(getEventDesc(result.descMap()));
        event.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(result));
        if (event.getEventAt() == null) {
            throw new JsonResponseException("?????????" + event.getPigCode() + "?????????????????????????????????????????????????????????");
        }
        if (event.getEventAt().after(new Date())) {
            event.setEventAt(DateTime.now().minusDays(1).toDate());
        }
        doctorPigEventDao.create(event);
        return event;
    }

    //??????????????????
    private DoctorPigEvent createFarrowEvent(DoctorImportSow info, DoctorPig sow, DoctorPigEvent beforeEvent) {
        DoctorPigEvent event = createSowEvent(info, sow);
        event.setEventAt(info.getPregDate());
        event.setType(PigEvent.FARROWING.getKey());
        event.setName(PigEvent.FARROWING.getName());
        event.setRelEventId(beforeEvent.getId());
        event.setPigStatusBefore(PigStatus.Farrow.getKey());
        event.setPigStatusAfter(PigStatus.FEED.getKey());
        event.setParity(info.getParity());
        event.setPregDays(DateUtil.getDeltaDaysAbs(info.getPrePregDate(), info.getPregDate())); //?????????
        event.setFarrowWeight(info.getNestWeight());
        event.setLiveCount(info.getHealthyCount());
        event.setHealthCount(info.getHealthyCount() - info.getWeakCount());
        event.setWeakCount(info.getWeakCount());
        event.setMnyCount(info.getMummyCount());
        event.setJxCount(info.getJixingCount());
        event.setDeadCount(info.getDeadCount());
        event.setBlackCount(info.getBlackCount());
        event.setFarrowingDate(event.getEventAt());
        event.setEventSource(SourceType.IMPORT.getValue());

        //??????extra
        DoctorFarrowingDto farrow = new DoctorFarrowingDto();
        farrow.setFarrowingDate(event.getEventAt());
        farrow.setBarnId(sow.getInitBarnId());
        farrow.setBarnName(sow.getInitBarnName());
        farrow.setBedCode(info.getBed());
        farrow.setFarrowingType(FarrowingType.USUAL.getKey());
        farrow.setGroupCode(getGroup(event.getBarnId()).getGroupCode());
        farrow.setBirthNestAvg(info.getNestWeight());
        farrow.setFarrowingLiveCount(event.getLiveCount());
        farrow.setHealthCount(event.getHealthCount());
        farrow.setWeakCount(event.getWeakCount());
        farrow.setMnyCount(event.getMnyCount());
        farrow.setJxCount(event.getJxCount());
        farrow.setDeadCount(event.getDeadCount());
        farrow.setBlackCount(event.getDeadCount());
        farrow.setToBarnId(event.getBarnId());
        farrow.setToBarnName(event.getBarnName());
        farrow.setFarrowIsSingleManager(IsOrNot.NO.getValue());
        farrow.setFarrowStaff1(info.getStaff1());
        farrow.setFarrowStaff2(info.getStaff2());
        farrow.setFarrowRemark(info.getRemark());
        farrow.setEventType(PigEvent.FARROWING.getKey());

        event.setDesc(generateEventDescFromExtra(farrow));
        event.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(farrow));
        if (event.getEventAt() == null) {
            throw new JsonResponseException("?????????" + event.getPigCode() + "???????????????????????????????????????????????????");
        }
        if (event.getEventAt().after(new Date())) {
            event.setEventAt(DateTime.now().minusDays(1).toDate());
        }
        doctorPigEventDao.create(event);
        return event;
    }

    //??????????????????
    private DoctorPigEvent createWeanEvent(DoctorImportSow info, DoctorPig sow, DoctorPigEvent beforeEvent) {
        DoctorPigEvent event = createSowEvent(info, sow);
        event.setEventAt(info.getWeanDate());
        event.setType(PigEvent.WEAN.getKey());
        event.setName(PigEvent.WEAN.getName());
        event.setRelEventId(beforeEvent.getId());
        event.setPigStatusBefore(PigStatus.FEED.getKey());
        event.setPigStatusAfter(PigStatus.Wean.getKey());
        event.setParity(info.getParity());
        event.setFeedDays(DateUtil.getDeltaDaysAbs(beforeEvent.getEventAt(), event.getEventAt()));
        event.setWeanCount(info.getHealthyCount());
        event.setWeanAvgWeight(MoreObjects.firstNonNull(info.getWeanWeight(), 0D) / (event.getWeanCount() == 0 ? 1 : event.getWeanCount()));
        event.setPartweanDate(event.getEventAt());
        event.setBoarCode(info.getBoarCode());
        event.setEventSource(SourceType.IMPORT.getValue());

        //??????extra
        DoctorWeanDto wean = new DoctorWeanDto();
        wean.setPartWeanDate(event.getEventAt());
        wean.setPartWeanPigletsCount(event.getWeanCount());
        wean.setPartWeanAvgWeight(event.getWeanAvgWeight());
        wean.setPartWeanRemark(info.getRemark());
        wean.setQualifiedCount(event.getWeanCount());
        wean.setNotQualifiedCount(0);
        wean.setFarrowingLiveCount(event.getWeakCount());
        wean.setWeanPigletsCount(0);

        event.setDesc(getEventDesc(wean.descMap()));
        event.setExtra(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(wean));
        if (event.getEventAt() == null) {
            throw new JsonResponseException("?????????" + event.getPigCode() + "???????????????????????????????????????????????????");
        }
        if (event.getEventAt().after(new Date())) {
            event.setEventAt(DateTime.now().minusDays(1).toDate());
        }
        doctorPigEventDao.create(event);
        return event;
    }

    private static String getEventDesc(Map<String, String> map) {
        return Joiner.on("#").withKeyValueSeparator("???").join(map);
    }

    //???excel????????????cell?????????bean
    private List<DoctorImportSow> getImportSows(Sheet shit) {
        List<DoctorImportSow> sows = Lists.newArrayList();
        for (Row row : shit) {
            if (canImport(row)) {
                DoctorImportSow sow = new DoctorImportSow();
                sow.setBarnName(ImportExcelUtils.getString(row, 0));       //??????
                sow.setSowCode(ImportExcelUtils.getString(row, 1));        //????????????

                //??????????????????
                PigStatus status = getPigStatus(ImportExcelUtils.getString(row, 2));
                if (status == null) {
                    throw new JsonResponseException("??????????????????????????????" + sow.getSowCode() + "???row " + (row.getRowNum() + 1));
                } else {
                    sow.setStatus(status.getKey());         //????????????
                }
                sow.setParity(MoreObjects.firstNonNull(ImportExcelUtils.getInt(row, 3), 1));            //??????
                sow.setMateDate(ImportExcelUtils.getDate(row, 4));        //????????????
                sow.setBoarCode(ImportExcelUtils.getString(row, 5));                                    //????????????
                sow.setMateStaffName(ImportExcelUtils.getString(row, 6));                               //?????????
                sow.setPrePregDate(ImportExcelUtils.getDate(row, 7));     //????????????
                if (sow.getPrePregDate() == null) {
                    sow.setPrePregDate(new DateTime(sow.getMateDate()).plusDays(114).toDate());
                }
                sow.setPregDate(ImportExcelUtils.getDate(row, 8));        //????????????
                sow.setFarrowBarnName(ImportExcelUtils.getString(row, 9));                              //????????????
                sow.setBed(ImportExcelUtils.getString(row, 10));                                        //??????
                sow.setWeanDate(ImportExcelUtils.getDate(row, 11));       //????????????
                sow.setHealthyCount(ImportExcelUtils.getIntOrDefault(row, 12, 0) + ImportExcelUtils.getIntOrDefault(row, 14, 0)); //?????????
                sow.setJixingCount(ImportExcelUtils.getIntOrDefault(row, 13, 0));                       //??????
                sow.setWeakCount(ImportExcelUtils.getIntOrDefault(row, 14, 0));                         //?????????
                sow.setDeadCount(ImportExcelUtils.getIntOrDefault(row, 15, 0));                         //??????
                sow.setMummyCount(ImportExcelUtils.getIntOrDefault(row, 16, 0));                        //?????????
                sow.setBlackCount(ImportExcelUtils.getIntOrDefault(row, 17, 0));                        //??????
                sow.setNestWeight(ImportExcelUtils.getDoubleOrDefault(row, 18, 0D));                    //??????
                sow.setStaff1(ImportExcelUtils.getString(row, 19));                                     //?????????1
                sow.setStaff2(ImportExcelUtils.getString(row, 20));                                     //?????????2
                sow.setSowEarCode(ImportExcelUtils.getString(row, 21));                                 //????????????
                sow.setBirthDate(ImportExcelUtils.getDate(row, 22));      //????????????
                sow.setRemark(ImportExcelUtils.getString(row, 23));                                     //??????
                sow.setBreed(ImportExcelUtils.getString(row, 24));                                      //??????

                if (isEmpty(sow.getBreed())) {
                    throw new JsonResponseException("????????????????????????????????????" + sow.getSowCode() + "????????????" + (row.getRowNum() + 1));
                }

                sow.setWeanWeight(ImportExcelUtils.getDoubleOrDefault(row, 25, 0D));                    //?????????
                sow.setWeanCount(ImportExcelUtils.getIntOrDefault(row, 26, 0));                         //?????????
                sow.setFatherCode(ImportExcelUtils.getString(row, 27));                                 //??????
                sow.setMotherCode(ImportExcelUtils.getString(row, 28));                                 //??????
                sow.setInFarmDate(ImportExcelUtils.getDate(row, 29));
                sows.add(sow);
            }
        }
        return sows;
    }

    private static PigStatus getPigStatus(String status) {
        if (Strings.isNullOrEmpty(status)) {
            log.error("what the fuck!!! the status is fucking null!");
            return PigStatus.Wean;
        }
        if (status.contains("??????")) {
            return PigStatus.Entry;
        }
        if (status.contains("?????????")) {
            return PigStatus.Mate;
        }
        if (status.contains("??????")) {
            return PigStatus.Pregnancy;
        }
        if (status.contains("??????")) {
            return PigStatus.FEED;
        }
        if (status.contains("??????")) {
            return PigStatus.Wean;
        }
        if (status.contains("??????") || status.contains("??????") || status.contains("??????")) {
            return PigStatus.KongHuai;
        }
        return null;
    }

    private void importWarehouse(DoctorFarm farm, DoctorImportSheet shit, User user, Map<String, Long> userMap) {
        // ???????????? profile
        UserProfile userProfile = userProfileDao.findByUserId(user.getId());

        // ????????????????????????????????????0
        RespHelper.or500(doctorWareHouseTypeWriteService.initDoctorWareHouseType(farm.getId(), farm.getName(), user.getId(), userProfile.getRealName()));


        // ????????????
        Map<String, Long> warehouseMap = new HashMap<>(); // key = ????????????, value = ??????id
        for (Row row : shit.getWarehouse()) {
            if (canImport(row)) {
                int line = row.getRowNum() + 1;
                String warehouseName = ImportExcelUtils.getStringOrThrow(row, 0);
                WareHouseType wareHouseType = WareHouseType.from(ImportExcelUtils.getStringOrThrow(row, 1));
                if (wareHouseType == null) {
                    throw new JsonResponseException("????????????????????????????????????" + warehouseName + "???row " + line + "column" + 2);
                }
                DoctorWareHouse wareHouse = DoctorWareHouse.builder()
                        .wareHouseName(warehouseName).type(wareHouseType.getKey())
                        .farmId(farm.getId()).farmName(farm.getName())
                        .creatorId(user.getId()).creatorName(userProfile.getRealName())
                        .build();
                String manager = ImportExcelUtils.getString(row, 2);
                if (StringUtils.isNotBlank(manager) && userMap.get(manager) != null) {
                    wareHouse.setManagerId(userMap.get(manager));
                    wareHouse.setManagerName(manager);
                } else {
                    wareHouse.setManagerId(user.getId());
                    wareHouse.setManagerName(userProfile.getRealName());
                }
                if (warehouseMap.containsKey(warehouseName)) {
                    throw new JsonResponseException("?????????????????????" + warehouseName + "???row " + line + "column" + 1);
                } else {
                    warehouseMap.put(warehouseName, RespHelper.or500(doctorWareHouseWriteService.createWareHouse(wareHouse)));
                }
            }
        }

        // ????????????
        this.importStock(shit.getFeed(), WareHouseType.FEED, warehouseMap, farm, user.getId(), userProfile.getRealName());
        this.importStock(shit.getMaterial(), WareHouseType.MATERIAL, warehouseMap, farm, user.getId(), userProfile.getRealName());
        this.importStock(shit.getConsume(), WareHouseType.CONSUME, warehouseMap, farm, user.getId(), userProfile.getRealName());
        this.importStock(shit.getMedicine(), WareHouseType.MEDICINE, warehouseMap, farm, user.getId(), userProfile.getRealName());
        this.importStock(shit.getVacc(), WareHouseType.VACCINATION, warehouseMap, farm, user.getId(), userProfile.getRealName());
    }

    private void importStock(Sheet shit, WareHouseType materialType, Map<String, Long> warehouseMap, DoctorFarm farm, Long userId, String userName) {
        String shitName = materialType.getDesc();
        for (Row row : shit) {
            if (canImport(row)) {
                int line = row.getRowNum() + 1;
                String warehouseName = ImportExcelUtils.getStringOrThrow(row, 0);
                String materialName = ImportExcelUtils.getStringOrThrow(row, 1);
                Double stock = ImportExcelUtils.getDouble(row, 2);
                if (stock == null || stock < 0D) {
                    throw new JsonResponseException("?????????????????????sheet:" + shitName + "???row " + line + "column " + 3);
                }
                String unitName;
                Double unitPrice;
                if (materialType == WareHouseType.FEED || materialType == WareHouseType.MATERIAL) {
                    unitPrice = ImportExcelUtils.getDouble(row, 3);
                    unitName = "??????";
                } else {
                    unitName = ImportExcelUtils.getStringOrThrow(row, 3);
                    if (StringUtils.isBlank(unitName)) {
                        throw new JsonResponseException("???????????????sheet:" + shitName + "???row " + line);
                    }
                    unitPrice = ImportExcelUtils.getDouble(row, 4);
                }
                if (unitPrice == null || unitPrice <= 0) {
                    throw new JsonResponseException("???????????????sheet:" + shitName + "???row " + line);
                } else {
                    unitPrice = unitPrice * 100D;
                }
                Long wareHouseId = warehouseMap.get(warehouseName);
                if (wareHouseId == null) {
                    throw new JsonResponseException("?????????????????????sheet:" + shitName + "???row " + line);
                }
                DoctorBasicMaterial basicMaterial = basicMaterialDao.findByTypeAndName(materialType, materialName);
                if (basicMaterial == null) {
                    throw new JsonResponseException("????????????????????????" + materialName + "???sheet:" + shitName + "???row " + line);
                }

                // ??????????????????????????????????????????????????????????????????
                RespHelper.or500(doctorMaterialInWareHouseWriteService.providerMaterialInfo(
                        DoctorMaterialConsumeProviderDto.builder()
                                .actionType(DoctorMaterialConsumeProvider.EVENT_TYPE.PROVIDER.getValue())
                                .type(materialType.getKey()).farmId(farm.getId()).farmName(farm.getName())
                                .materialTypeId(basicMaterial.getId()).materialName(materialName)
                                .wareHouseId(wareHouseId).wareHouseName(warehouseName)
                                .staffId(userId).staffName(userName)
                                .count(stock).unitPrice(unitPrice.longValue())
                                .unitName(unitName).eventTime(new Date())
                                .build()
                ));
            }
        }

    }

    //???????????????????????????  ?????????????????????
    private static boolean canImport(Row row) {
        return row.getRowNum() > 0 && StringUtils.isNotBlank(ImportExcelUtils.getString(row, 0));
    }

    /**
     * ??????????????????????????????
     */
    @Transactional
    public void updateMateRate(Long farmId) {
        //?????????????????????????????????????????????
        List<DoctorPigEvent> events = doctorPigEventDao.findByFarmIdAndKindAndEventTypes(farmId, DoctorPig.PigSex.SOW.getKey(),
                Lists.newArrayList(PigEvent.MATING.getKey(), PigEvent.PREG_CHECK.getKey(), PigEvent.FARROWING.getKey()));
        events.stream()
                .filter(event -> {
                    if (Objects.equals(event.getType(), PigEvent.FARROWING.getKey())) {
                        return true;
                    }
                    if (Objects.equals(event.getType(), PigEvent.PREG_CHECK.getKey())) {
                        return Objects.equals(event.getPregCheckResult(), PregCheckResult.YANG.getKey());
                    }
                    return Objects.equals(event.getType(), PigEvent.MATING.getKey()) && event.getCurrentMatingCount() == 1;
                })
                .collect(Collectors.groupingBy(DoctorPigEvent::getPigId))
                .entrySet()
                .forEach(map -> map.getValue().stream()
                        .collect(Collectors.groupingBy(DoctorPigEvent::getParity))
                        .values()
                        .forEach(this::updateMate));
    }

    //????????????????????? ????????????????????????
    private void updateMate(List<DoctorPigEvent> events) {
        Map<Integer, DoctorPigEvent> eventMap = Maps.newHashMap();
        events.forEach(event -> eventMap.put(event.getType(), event));

        if (!eventMap.containsKey(PigEvent.MATING.getKey())) {
            return;
        }
        DoctorPigEvent mateEvent = eventMap.get(PigEvent.MATING.getKey());
        boolean needUpdate = false;
        if (eventMap.containsKey(PigEvent.PREG_CHECK.getKey())) {
            if (mateEvent.getIsImpregnation() == null || mateEvent.getIsImpregnation() != 1) {
                needUpdate = true;
                mateEvent.setIsImpregnation(1);
            }
        }
        if (eventMap.containsKey(PigEvent.FARROWING.getKey())) {
            if (mateEvent.getIsDelivery() == null || mateEvent.getIsDelivery() != 1) {
                needUpdate = true;
                mateEvent.setIsDelivery(1);
            }
        }
        if (needUpdate) {
            DoctorPigEvent updateEvent = new DoctorPigEvent();
            updateEvent.setId(mateEvent.getId());
            updateEvent.setIsImpregnation(mateEvent.getIsImpregnation());
            updateEvent.setIsDelivery(mateEvent.getIsDelivery());
            doctorPigEventDao.update(updateEvent);
        }
    }


    /**
     * ???????????????????????????????????????group_id ??????????????????
     */
    @Transactional
    public void flushFarrowGroupId(Long farmId) {
        //?????????????????????, ????????????(??????????????????group_id???)
        List<DoctorPigEvent> events = doctorPigEventDao.findByFarmIdAndKindAndEventTypes(farmId, DoctorPig.PigSex.SOW.getKey(),
                Lists.newArrayList(PigEvent.WEAN.getKey(), PigEvent.FARROWING.getKey()))
                .stream()
                .filter(e -> e.getGroupId() == null)
                .collect(Collectors.toList());

        // Map<Long, Map<Integer, List<DoctorPigEvent>>> ?????????id???????????????
        events.stream()
                .collect(Collectors.groupingBy(DoctorPigEvent::getPigId))
                .entrySet()
                .forEach(map -> map.getValue().stream()
                        .filter(e -> e.getParity() != null)
                        .collect(Collectors.groupingBy(DoctorPigEvent::getParity))
                        .values()
                        .forEach(this::updateFarrowGroupId)
                );

    }

    //???????????????group_id
    private void updateFarrowGroupId(List<DoctorPigEvent> events) {
        //??????????????????????????????
        events = events.stream().sorted(Comparator.comparing(DoctorPigEvent::getType)).collect(Collectors.toList());
        DoctorGroup group = null;

        for (DoctorPigEvent event : events) {
            if (Objects.equals(event.getType(), PigEvent.FARROWING.getKey())) {
                group = getHistoryFarrowGroup(event);
            } else if (Objects.equals(event.getType(), PigEvent.WEAN.getKey())) {
                group = group == null ? getHistoryWeanGroup(event) : group;
            }
            updateEventGroupId(event.getId(), group);
        }
    }

    /**
     * ?????????????????????
     */
    private DoctorGroup getHistoryFarrowGroup(DoctorPigEvent farrowEvent) {
        DoctorFarrowingDto farrow = MAPPER.fromJson(farrowEvent.getExtra(), DoctorFarrowingDto.class);
        if (notEmpty(farrow.getGroupCode())) {
            DoctorGroup group = doctorGroupDao.findByFarmIdAndGroupCode(farrowEvent.getFarmId(), farrow.getGroupCode());
            if (group != null) {
                return group;
            }
            log.warn("dirty data, farrow event group not found, sowId:{}, parity:{}, groupCode:{}", farrowEvent.getPigId(), farrowEvent.getParity(), farrow.getGroupCode());
        }
        log.warn("dirty data, farrow event groupCode empty, sowId:{}, parity:{}, farrow:{}", farrowEvent.getPigId(), farrowEvent.getParity(), farrow);
        return doctorGroupDao.findByFarmIdAndBarnIdAndDate(farrowEvent.getFarmId(), farrowEvent.getBarnId(), farrowEvent.getEventAt());
    }

    /**
     * ?????????????????????
     */
    private DoctorGroup getHistoryWeanGroup(DoctorPigEvent weanEvent) {
        DoctorGroup group = doctorGroupDao.findByFarmIdAndBarnIdAndDate(weanEvent.getFarmId(), weanEvent.getBarnId(), weanEvent.getEventAt());
        return MoreObjects.firstNonNull(group, new DoctorGroup());
    }

    private void updateEventGroupId(Long eventId, DoctorGroup group) {
        if (group != null && group.getId() != null) {
            DoctorPigEvent updateEvent = new DoctorPigEvent();
            updateEvent.setId(eventId);
            updateEvent.setGroupId(group.getId());
            doctorPigEventDao.update(updateEvent);
            log.warn("update event group id success! eventId:{}, groupId:{}", eventId, group.getId());
        } else {
            log.warn("update event group id error, group not found! eventId:{}", eventId);
        }
    }

    /**
     * ??????????????????
     *
     * @see BoarEntryType
     */
    @Transactional
    public void flushBoarType(Long farmId) {
        List<DoctorPig> boars = doctorPigDao.findPigsByFarmIdAndPigType(farmId, DoctorPig.PigSex.BOAR.getKey());
        if (!notEmpty(boars)) {
            log.info("this farm do not have any boars, farmId:{}", farmId);
            return;
        }
        boars.forEach(boar -> {
            Map<String, Object> map = boar.getExtraMap();
            if (map == null || map.get(DoctorFarmEntryConstants.BOAR_TYPE_ID) == null) {
                updateBoarType(boar.getId(), BoarEntryType.HGZ.getKey());
            } else {
                Integer boarType = Integer.valueOf(String.valueOf(map.get(DoctorFarmEntryConstants.BOAR_TYPE_ID)));
                updateBoarType(boar.getId(), boarType);
            }
        });
    }

    private void updateBoarType(Long boarId, Integer boarType) {
        DoctorPig pig = new DoctorPig();
        pig.setId(boarId);
        pig.setBoarType(MoreObjects.firstNonNull(boarType, BoarEntryType.HGZ.getKey()));
        doctorPigDao.update(pig);
    }

    /**
     * ????????????
     *
     * @param groupId
     */
    private void createNotGroupSnapshot(Long groupId) {

        DoctorGroupEvent lastEvent = doctorGroupEventDao.findLastEventByGroupId(groupId);
        if (isNull(lastEvent)) {
            log.info("??????????????????,??????id:" + groupId);
            return;
        }
        DoctorGroup group = doctorGroupDao.findById(groupId);
        DoctorGroupTrack groupTrack = doctorGroupTrackDao.findByGroupId(groupId);
    }

    /**
     * ??????????????????
     *
     * @param farmExport ????????????
     */
    public void createFarmExport(DoctorFarmExport farmExport) {
        doctorFarmExportDao.create(farmExport);
    }

    /**
     * ??????????????????
     *
     * @param farmExport ????????????
     */
    public void updateFarmExport(DoctorFarmExport farmExport) {
        doctorFarmExportDao.update(farmExport);
    }

    /**
     * ??????????????????
     *
     * @param farmId  ??????id
     * @param newName ????????????
     */
    @Transactional
    public void updateFarmName(Long farmId, String newName) {
        //????????????
        DoctorFarm updateFarm = new DoctorFarm();
        updateFarm.setId(farmId);
        updateFarm.setName(newName);
        doctorFarmDao.update(updateFarm);

        //????????????
        doctorBarnDao.updateFarmName(farmId, newName);

        //?????????
        doctorPigDao.updateFarmName(farmId, newName);

        //???????????????
        doctorPigEventDao.updateFarmName(farmId, newName);

        //????????????
        doctorGroupDao.updateFarmName(farmId, newName);

        //??????????????????
        doctorGroupEventDao.updateFarmName(farmId, newName);
    }
}
