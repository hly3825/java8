package io.terminus.doctor.user.service;

import com.google.common.base.Throwables;
import io.terminus.boot.rpc.common.annotation.RpcProvider;
import io.terminus.common.model.Response;
import io.terminus.doctor.user.dao.PrimaryUserDao;
import io.terminus.doctor.user.dao.SubDao;
import io.terminus.doctor.user.model.PrimaryUser;
import io.terminus.doctor.user.model.Sub;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created by houluyao on 16/5/24.
 */
@Slf4j
@Service
@RpcProvider
public class PrimaryUserWriteServiceImpl implements PrimaryUserWriteService {

    private final PrimaryUserDao primaryUserDao;

    private final SubDao subDao;

    @Autowired
    public PrimaryUserWriteServiceImpl(PrimaryUserDao primaryUserDao, SubDao subDao) {
        this.primaryUserDao = primaryUserDao;
        this.subDao = subDao;
    }

    @Override
    public Response<Long> createPrimaryUser(PrimaryUser primaryUser) {
        try {
            log.info("=====================================PrimaryUserWriteServiceImpl.primaryUserDao.create");
            primaryUserDao.create(primaryUser);
            return Response.ok(primaryUser.getId());
        } catch (Exception e) {
            log.error("create primaryUser failed, seller={}, cause:{}",
                    primaryUser, Throwables.getStackTraceAsString(e));
            return Response.fail("primaryUser.create.fail");
        }
    }

    @Override
    public Response<Boolean> updatePrimaryUser(PrimaryUser primaryUser) {
        try {
            return Response.ok(primaryUserDao.update(primaryUser));
        } catch (Exception e) {
            log.error("update primaryUser failed, seller={}, cause:{}",
                    primaryUser, Throwables.getStackTraceAsString(e));
            return Response.fail("primaryUser.update.fail");
        }
    }

    @Override
    public Response<Long> createSub(Sub sub) {
        try {
            subDao.create(sub);
            return Response.ok(sub.getId());
        } catch (Exception e) {
            log.error("create sub ={} failed, cause:{}",
                    sub, Throwables.getStackTraceAsString(e));
            return Response.fail("sub.create.fail");
        }
    }

    @Override
    public Response<Boolean> updateSub(Sub sub) {
        try {
            return Response.ok(subDao.update(sub));
        } catch (Exception e) {
            log.error("update sub={} failed, cause:{}",
                    sub, Throwables.getStackTraceAsString(e));
            return Response.fail("sub.update.fail");
        }
    }

    @Override
    public Response<Boolean> updateRoleName(Long subRoleId, String newRoleName){
        try{
            subDao.updateRoleName(subRoleId, newRoleName);
            return Response.ok(true);
        }catch(Exception e){
            log.error("updateRoleName failed, subRoleId={}, newRoleName={}, cause:{}",
                    subRoleId, newRoleName, Throwables.getStackTraceAsString(e));
            return Response.fail("update.role.name.fail");
        }
    }
}
