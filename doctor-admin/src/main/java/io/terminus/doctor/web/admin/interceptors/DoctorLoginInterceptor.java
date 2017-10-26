/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package io.terminus.doctor.web.admin.interceptors;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.terminus.common.model.Response;
import io.terminus.common.redis.dao.RedisBaseDao;
import io.terminus.common.redis.utils.JedisTemplate;
import io.terminus.doctor.common.enums.UserType;
import io.terminus.doctor.web.core.util.DoctorUserMaker;
import io.terminus.pampas.common.UserUtil;
import io.terminus.parana.common.model.ParanaUser;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.service.UserReadService;
import io.terminus.doctor.web.core.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.concurrent.TimeUnit;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-01-28
 */
@Slf4j
@Component("loginInterceptor")
public class DoctorLoginInterceptor extends HandlerInterceptorAdapter {

    private final LoadingCache<Long, Response<User>> userCache;
    private final DoctorUserMaker doctorUserMaker;

    private JedisTemplate template;

    @Autowired
    public DoctorLoginInterceptor(final UserReadService<User> userReadService, DoctorUserMaker doctorUserMaker) {
        userCache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).maximumSize(10000).build(new CacheLoader<Long, Response<User>>() {
            @Override
            public Response<User> load(Long userId) throws Exception {
                return userReadService.findById(userId);
            }
        });

        this.doctorUserMaker = doctorUserMaker;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession(false);

        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                System.out.println("get user info from redis 0");
                System.out.println(jedis.get("afsession:dZDYXn3MmThSnxyZ5QV9vBWn6IweKhOW"));
            }
        }, 0);
        template.execute(new JedisTemplate.JedisActionNoResult() {
            @Override
            public void action(Jedis jedis) {
                System.out.println("get user info from redis 1");
                System.out.println(jedis.get("afsession:dZDYXn3MmThSnxyZ5QV9vBWn6IweKhOW"));
            }
        }, 1);

        if (session != null) {
            Object userIdInSession = session.getAttribute(Constants.SESSION_USER_ID);
            if (userIdInSession != null) {

                final Long userId = Long.valueOf(userIdInSession.toString());
                Response<? extends User> result = userCache.getUnchecked(userId);
                if (!result.isSuccess()) {
                    // TODO: 开发阶段先不缓存错误数据
                    userCache.invalidate(userId);
                    log.warn("failed to find user where id={},error code:{}", userId, result.getError());
                    return false;
                }
                User user = result.getResult();
                if (user != null) {
                    ParanaUser paranaUser = doctorUserMaker.from(user);
                    if (user.getType() != UserType.ADMIN.value()) {
                        log.error("user(id={})'s is not admin, its type is {}", userId, user.getType());
                    }
                    UserUtil.putCurrentUser(paranaUser);
                }
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserUtil.clearCurrentUser();
    }
}
