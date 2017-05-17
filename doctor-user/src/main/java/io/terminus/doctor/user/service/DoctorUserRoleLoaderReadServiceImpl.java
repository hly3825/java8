package io.terminus.doctor.user.service;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.doctor.user.dao.*;
import io.terminus.doctor.user.model.*;
import io.terminus.parana.user.impl.dao.UserDao;
import io.terminus.parana.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static io.terminus.doctor.common.utils.UserRoleUtil.*;
import static io.terminus.doctor.common.utils.UserRoleUtil.isNormal;

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
    @Autowired
    public DoctorUserRoleLoaderReadServiceImpl(UserDao userDao, SellerDao sellerDao,  OperatorDao operatorDao, SubDao subDao, SubRoleDao subRoleDao, OperatorRoleDao operatorRoleDao, PigScoreApplyDao pigScoreApplyDa) {
        this.userDao = userDao;
        this.sellerDao = sellerDao;
        this.operatorDao = operatorDao;
        this.subDao = subDao;
        this.subRoleDao = subRoleDao;
        this.operatorRoleDao = operatorRoleDao;
        this.pigScoreApplyDao = pigScoreApplyDa;
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
            return Response.ok(roleContent);
        } catch (Exception e) {
            log.error("hard load roles failed, userId={}, cause:{}", userId, Throwables.getStackTraceAsString(e));
            return Response.fail("user.role.load.fail");
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
        if (user == null) {
            return;
        }

        User u = userDao.findById(user.getId());

        Map<String, String> extra = u.getExtra();
        Long farmId, orgId;
        if(extra != null &&  extra.containsKey("farmId") && extra.containsKey("orgId")){
            farmId = Long.parseLong(extra.get("farmId"));
            orgId = Long.parseLong(extra.get("orgId"));
        }else{
            return;
        }
        if(farmId == null|| orgId == null){
            return;
        }
        PigScoreApply apply = pigScoreApplyDao.findByFarmIdAndUserId(orgId, farmId, user.getId());
        if (apply != null && apply.getStatus().equals(1)) {
            List<DoctorRole> roles= roleContent.getRoles();
            if(roles == null){
                roles = Lists.newArrayList();
            }
            roles.add(DoctorRole.createStatic("PIGSCORE"));
            roleContent.setRoles(roles);
        }
    }
}
