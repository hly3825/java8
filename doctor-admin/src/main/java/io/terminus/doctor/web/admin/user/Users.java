/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package io.terminus.doctor.web.admin.user;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.eventbus.EventBus;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.BaseUser;
import io.terminus.common.model.Response;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.common.utils.JsonMapper;
import io.terminus.doctor.common.enums.DataEventType;
import io.terminus.doctor.common.enums.UserStatus;
import io.terminus.doctor.common.enums.UserType;
import io.terminus.doctor.common.event.DataEvent;
import io.terminus.doctor.user.model.DoctorUser;
import io.terminus.doctor.user.service.DoctorFarmReadService;
import io.terminus.doctor.user.service.DoctorUserReadService;
import io.terminus.doctor.user.util.DoctorUserMaker;
import io.terminus.doctor.web.core.Constants;
import io.terminus.doctor.web.core.component.MobilePattern;
import io.terminus.doctor.web.core.events.user.LoginEvent;
import io.terminus.doctor.web.core.events.user.LogoutEvent;
import io.terminus.pampas.common.UserUtil;
import io.terminus.parana.auth.core.AclLoader;
import io.terminus.parana.auth.core.PermissionHelper;
import io.terminus.parana.auth.model.Acl;
import io.terminus.parana.auth.model.ParanaThreadVars;
import io.terminus.parana.auth.model.PermissionData;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.user.model.LoginType;
import io.terminus.parana.user.model.User;
import io.terminus.zookeeper.pubsub.Publisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

import static io.terminus.common.utils.Arguments.isNull;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-01-30
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class Users {

    private static final String ImportExcelRedisKey = "import-excel-result:";

    private final DoctorUserReadService doctorUserReadService;

    private final DoctorFarmReadService doctorFarmReadService;

    private final MobilePattern mobilePattern;

    private final EventBus eventBus;

    private final AclLoader aclLoader;

    private final PermissionHelper permissionHelper;

    private final Publisher publisher;

    private final JedisTemplate jedisTemplate;

    @Autowired
    public Users(DoctorUserReadService doctorUserReadService,
                 DoctorFarmReadService doctorFarmReadService, EventBus eventBus,
                 AclLoader aclLoader,
                 PermissionHelper permissionHelper,
                 MobilePattern mobilePattern,
                 Publisher publisher,
                 JedisTemplate jedisTemplate) {
        this.doctorUserReadService = doctorUserReadService;
        this.doctorFarmReadService = doctorFarmReadService;
        this.eventBus = eventBus;
        this.aclLoader = aclLoader;
        this.permissionHelper = permissionHelper;
        this.mobilePattern = mobilePattern;
        this.publisher = publisher;
        this.jedisTemplate = jedisTemplate;
    }

    @RequestMapping("")
    public BaseUser getLoginUser() {
        DoctorUser doctorUser = UserUtil.getCurrentUser();
        try {
            Acl acl = aclLoader.getAcl(ParanaThreadVars.getApp());
            BaseUser user = UserUtil.getCurrentUser();
            PermissionData perm = permissionHelper.getPermissions(acl, user, true);
            perm.setAllRequests(null); // empty it
            doctorUser.setAuth(JsonMapper.nonEmptyMapper().toJson(perm));
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, JsonResponseException.class);
            log.error("get permissions of user failed, cause:{}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException("auth.permission.find.fail");
        }
        return doctorUser;
    }

    /**
     * 登录
     */
    @RequestMapping(value = "/login", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> login(@RequestParam("loginBy") String loginBy, @RequestParam("password") String password,
                                     @RequestParam(value = "target", required = false) String target,
                                     @RequestParam(value = "type", required = false) Integer type,
                                     HttpServletRequest request, HttpServletResponse response) {
        loginBy = loginBy.toLowerCase();
        LoginType loginType;
        if(isNull(type)){
            if(mobilePattern.getPattern().matcher(loginBy).find()){
                loginType = LoginType.MOBILE;
            }
            else if(loginBy.indexOf("@") != -1){
                loginType = LoginType.OTHER;
            }
            else {
                loginType = LoginType.NAME;
            }
        } else {
            loginType = LoginType.from(type);
        }

        Map<String, Object> map = new HashMap<>();

        Response<User> result = doctorUserReadService.login(loginBy, password, loginType);

        if (!result.isSuccess()) {
            log.warn("failed to login with(loginBy={}), error: {}", loginBy, result.getError());
            throw new JsonResponseException(500, result.getError());
        }

        User user = result.getResult();
        //判断下user type, 只允许admin和运维能登录
        if(!Objects.equal(user.getType(), UserType.ADMIN.value()) && !Objects.equal(user.getType(), UserType.OPERATOR.value())){
            throw new JsonResponseException("authorize.fail");
        }

        //判断当前用户是否激活
        if (Objects.equal(user.getStatus(), UserStatus.NOT_ACTIVATE.value())) {
            log.warn("user({}) isn't active", user);
        }
        request.getSession().setAttribute(Constants.SESSION_USER_ID, user.getId());

        LoginEvent loginEvent = new LoginEvent(request, response, DoctorUserMaker.from(user));
        eventBus.post(loginEvent);
        target = !StringUtils.hasText(target)?"/":target;
        map.put("redirect",target);
        return map;
    }

    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            ParanaUser loginUser = UserUtil.getCurrentUser();
            if (loginUser != null) {
                //delete login token cookie
                LogoutEvent logoutEvent = new LogoutEvent(request, response, loginUser);
                eventBus.post(logoutEvent);
            }
            return "/";
        } catch (Exception e) {
            log.error("failed to logout user,cause:", e);
            throw new JsonResponseException(500, "user.logout.fail");
        }
    }

    @RequestMapping(value = "/importExcel", method = RequestMethod.GET)
    public String importExcel(@RequestParam String fileUrl){
        final int maxWaitTime = 90; // 最长等待时间，秒
        final int sleepTime = 1; //每次沉睡多少秒
        final String redisKey = ImportExcelRedisKey + fileUrl;
        try {
            jedisTemplate.execute(jedis -> {
                jedis.set(redisKey, "null");
            });

            publisher.publish(DataEvent.toBytes(DataEventType.ImportExcel.getKey(), fileUrl));

            int plusTime = 0; //已累计等待时间，秒
            while (true) {
                String result = jedisTemplate.execute(jedis -> {
                    return jedis.get(redisKey);
                });
                if ("null".equals(result)) {
                    if (plusTime >= maxWaitTime) {
                        throw new JsonResponseException("time out " + maxWaitTime + " seconds");
                    }
                    Thread.sleep(1000L * sleepTime);
                    plusTime += sleepTime;
                } else if ("true".equals(result)) {
                    return "true";
                } else {
                    return "导入猪场失败，您可以将此错误信息发送给工程师以帮您分析错误原因\n" + result;
                }
            }
        } catch (JsonResponseException e){
            throw e;
        } catch (Exception e) {
            log.error(Throwables.getStackTraceAsString(e));
            return "导入猪场失败，您可以将此错误信息发送给工程师以帮您分析错误原因\n" + Throwables.getStackTraceAsString(e);
        } finally {
            jedisTemplate.execute(jedis -> {
                jedis.del(redisKey);
            });
        }
    }

}
