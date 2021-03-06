package io.terminus.doctor.user.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.doctor.user.dao.IotRoleDao;
import io.terminus.doctor.user.dao.IotUserDao;
import io.terminus.doctor.user.dao.OperatorDao;
import io.terminus.doctor.user.dao.OperatorRoleDao;
import io.terminus.doctor.user.dao.PigScoreApplyDao;
import io.terminus.doctor.user.dao.SellerDao;
import io.terminus.doctor.user.dao.SubDao;
import io.terminus.doctor.user.dao.SubRoleDao;
import io.terminus.doctor.user.model.DoctorRole;
import io.terminus.doctor.user.model.DoctorRoleContent;
import io.terminus.doctor.user.model.IotRole;
import io.terminus.doctor.user.model.IotUser;
import io.terminus.doctor.user.model.Operator;
import io.terminus.doctor.user.model.OperatorRole;
import io.terminus.doctor.user.model.PigScoreApply;
import io.terminus.doctor.user.model.Seller;
import io.terminus.doctor.user.model.Sub;
import io.terminus.doctor.user.model.SubRole;
import io.terminus.parana.user.impl.dao.UserDao;
import io.terminus.parana.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.terminus.doctor.common.utils.UserRoleUtil.*;
import static java.util.Objects.isNull;

/**
 * Created by yudi on 2016/12/7.
 * Mail to yd@terminus.io
 */
@Slf4j
@Service
@RpcProvider
public class DoctorUserRoleLoaderReadServiceImpl implements DoctorUserRoleLoader {
    private final UserDao userDao;

    private final SellerDao sellerDao;


    private final OperatorDao operatorDao;

    private final SubDao subDao;
    private final SubRoleDao subRoleDao;
    private final OperatorRoleDao operatorRoleDao;
    private final PigScoreApplyDao pigScoreApplyDao;
    private final IotUserDao iotUserDao;
    private final IotRoleDao iotRoleDao;
    @Autowired
    public DoctorUserRoleLoaderReadServiceImpl(UserDao userDao, SellerDao sellerDao, OperatorDao operatorDao,
                                               SubDao subDao, SubRoleDao subRoleDao, OperatorRoleDao operatorRoleDao,
                                               PigScoreApplyDao pigScoreApplyDa, IotUserDao iotUserDao, IotRoleDao iotRoleDao) {
        this.userDao = userDao;
        this.sellerDao = sellerDao;
        this.operatorDao = operatorDao;
        this.subDao = subDao;
        this.subRoleDao = subRoleDao;
        this.operatorRoleDao = operatorRoleDao;
        this.pigScoreApplyDao = pigScoreApplyDa;
        this.iotUserDao = iotUserDao;
        this.iotRoleDao = iotRoleDao;
    }

    @Override
    public Response<DoctorRoleContent> hardLoadRoles(Long userId) {
        try {
            if (userId == null) {
                log.warn("hard load roles failed, userId=null");
                return Response.fail("user.id.null");
            }
            User user = userDao.findById(userId);
            if (user == null) {
                log.warn("user(id={}) is not exist, no roles found", userId);
                return Response.fail("user.not.found");
            }
            DoctorRoleContent roleContent=new DoctorRoleContent();
            forAdmin(user, roleContent);
            forOperator(user, roleContent);
            forNormal(user, roleContent);
            forPrimary(user, roleContent);
            forSub(user, roleContent);
            forPigScore(user, roleContent);
            forSubNoRole(roleContent);
            forIot(user, roleContent);
            return Response.ok(roleContent);
        } catch (Exception e) {
            log.error("hard load roles failed, userId={}, cause:{}", userId, Throwables.getStackTraceAsString(e));
            return Response.fail("user.role.load.fail");
        }
    }

    private void forIot(User user,  DoctorRoleContent roleContent) {
            Long userId = user.getId();
            IotUser iotUser = iotUserDao.findByUserId(userId);
            if (isNull(iotUser)) {
                return;
            }
            if (Objects.equals(iotUser.getType(), IotUser.TYPE.IOT_ADMIN.getValue())) {
                List<DoctorRole> roles = roleContent.getRoles();
                DoctorRole iotRole = DoctorRole.createStatic("IOT_ADMIN");
                if (isNull(roles)) {
                    roleContent.setRoles(Lists.newArrayList(iotRole));
                } else {
                    roles.add(iotRole);
                }
            } else if (Objects.equals(iotUser.getType(), IotUser.TYPE.IOT_OPERATOR.getValue())) {
                IotRole iotRole = iotRoleDao.findById(iotUser.getIotRoleId());
                roleContent.setDynamicRoles(Lists.newArrayList(DoctorRole.createDynamic("IOT_OPERATOR",
                        iotRole.getAllow(), Lists.newArrayList(iotRole.getName()))));
            }
    }

    /**
     * ?????????,??????????????????,????????????hasRole??????
     * @param roleContent
     */
    private void forSubNoRole(DoctorRoleContent roleContent) {
        List<DoctorRole> roles= roleContent.getRoles();
        if(roles == null){
            roles = Lists.newArrayList(DoctorRole.createStatic("LOGIN"));
            roleContent.setRoles(roles);
        }
    }

    protected void forAdmin(User user, DoctorRoleContent roleContent) {
        if (user == null || !isAdmin(user.getType())) {
            return;
        }
        roleContent.setRoles(Lists.newArrayList(DoctorRole.createStatic("ADMIN")));
    }

    protected void forOperator(User user, DoctorRoleContent roleContent) {
        if (user == null || !isOperator(user.getType())) {
            return;
        }
        Operator operator = operatorDao.findByUserId(user.getId());
        if (operator != null) {
            if (operator.isActive() && operator.getRoleId() != null) {
                OperatorRole operatorRole=operatorRoleDao.findById(operator.getRoleId());
                if (operatorRole==null){
                    return;
                }
                roleContent.setDynamicRoles(Lists.newArrayList(DoctorRole.createDynamic("ADMIN",operatorRole.getAllow(),Lists.newArrayList(operatorRole.getName()))));
            }
        }
    }

    protected void forPrimary(User user, DoctorRoleContent roleContent) {
        if (user == null || !isPrimary(user.getType())) {
            return;
        }
        roleContent.setRoles(Lists.newArrayList(DoctorRole.createStatic("PRIMARY")));
    }

    protected void forSub(User user, DoctorRoleContent roleContent) {
        if (user == null || !isSub(user.getType())) {
            return;
        }

        Sub sub = subDao.findByUserId(user.getId());
        if (sub != null) {
            if (sub.isActive() && sub.getRoleId() != null) {
                SubRole subRole=subRoleDao.findById(sub.getRoleId());
                if (subRole==null){
                    return;
                }
                roleContent.setDynamicRoles(Lists.newArrayList( DoctorRole.createDynamic("SUB",subRole.getAllow(),Lists.newArrayList(subRole.getName()))));
            }
        }
    }

    protected void forNormal(User user, DoctorRoleContent roleContent) {
        if (user == null || !isNormal(user.getType())) {
            return;
        }
        // for buyer
        if (user.getRoles() != null) {
            boolean isBuyer = false;
            List<DoctorRole> roles=Lists.newArrayList();
            for (String role : user.getRoles()) {
                if (role.startsWith("BUYER")) {
                    roles.add(DoctorRole.createStatic(role));
                    isBuyer = true;
                }
            }
            if (isBuyer) {
                roles.add(DoctorRole.createStatic("BUYER"));
            }
            roleContent.setRoles(roles);
        }
        // for seller
        Seller seller = sellerDao.findByUserId(user.getId());
        if (seller != null) {
            if (seller.isActive() && seller.getShopId() != null) {
                roleContent.setRoles(Lists.newArrayList(DoctorRole.createStatic("SELLER")));
            }
        }
    }

    protected void forPigScore(User user, DoctorRoleContent roleContent){
        List<DoctorRole> roles= roleContent.getRoles();

        if(user == null || roles == null || roles.isEmpty()){
            return;
        }
        List<DoctorRole> newroles = Lists.newArrayList(roles);
        User u = userDao.findById(user.getId());
        Map<String, String> extra = u.getExtra();
        try {
            if (extra != null && extra.containsKey("farmId") && extra.containsKey("orgId")) {
                Long farmId = Long.parseLong(extra.get("farmId"));
                Long orgId = Long.parseLong(extra.get("orgId"));
                // ??????????????????
                PigScoreApply apply = pigScoreApplyDao.findByOrgAndFarmId(orgId, farmId);
                // ???????????????????????????????????????????????????
                if (apply != null && apply.getStatus().equals(1)) {
                    for(DoctorRole role: roles){
                        if("PRIMARY".equals(role.getBase())){
                            newroles.add(DoctorRole.createStatic("PIGSCORE"));
                        }
                    }
                    roleContent.setRoles(newroles);
                }
            }
        }catch (NumberFormatException nfe){
            log.error("failed to parse farmId:{} or orgId:{} to Long", extra.get("farmId"), extra.get("orgId"));
        }
    }
}
