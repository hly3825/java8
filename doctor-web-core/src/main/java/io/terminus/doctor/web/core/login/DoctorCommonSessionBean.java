package io.terminus.doctor.web.core.login;

import com.github.cage.token.RandomCharacterGeneratorFactory;
import com.github.cage.token.RandomTokenGenerator;
import com.google.common.base.Charsets;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import io.terminus.boot.session.properties.SessionProperties;
import io.terminus.common.exception.JsonResponseException;
import io.terminus.common.model.Response;
import io.terminus.common.utils.Splitters;
import io.terminus.doctor.common.enums.IsOrNot;
import io.terminus.doctor.common.enums.UserStatus;
import io.terminus.doctor.common.enums.UserType;
import io.terminus.doctor.common.event.CoreEventDispatcher;
import io.terminus.doctor.common.utils.Params;
import io.terminus.doctor.common.utils.RespHelper;
import io.terminus.doctor.event.enums.SmsCodeType;
import io.terminus.doctor.user.service.DoctorUserReadService;
import io.terminus.doctor.web.core.component.CaptchaGenerator;
import io.terminus.doctor.web.core.component.MobilePattern;
import io.terminus.doctor.web.core.enums.MobileDeviceType;
import io.terminus.doctor.web.core.events.user.RegisterEvent;
import io.terminus.doctor.web.core.util.DoctorUserMaker;
import io.terminus.pampas.common.UserUtil;
import io.terminus.parana.common.utils.EncryptUtil;
import io.terminus.parana.user.model.LoginType;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.model.UserDevice;
import io.terminus.parana.user.service.DeviceReadService;
import io.terminus.parana.user.service.DeviceWriteService;
import io.terminus.parana.user.service.UserWriteService;
import io.terminus.parana.web.msg.MsgWebService;
import io.terminus.session.AFSessionManager;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.github.cage.token.RandomCharacterGeneratorFactory.ARABIC_NUMERALS;
import static io.terminus.common.utils.Arguments.isEmpty;
import static io.terminus.common.utils.Arguments.notNull;

/**
 * Desc: session??????????????????open?????? web???????????????????????????
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 2016/11/17
 */
@Slf4j
@Component
public class DoctorCommonSessionBean {

    public static final Character SEP='Z';
    private final String hostIpMd5;
    private final RandomTokenGenerator tokenGenerator = new RandomTokenGenerator(null, new RandomCharacterGeneratorFactory(ARABIC_NUMERALS, null, new Random()), 4, 0);

    @Autowired
    private CaptchaGenerator captchaGenerator;
    @Autowired
    private AFSessionManager sessionManager;
    @Autowired
    private SessionProperties sessionProperties;
    @Autowired
    private DoctorUserReadService doctorUserReadService;
    @Autowired
    private UserWriteService<User> userWriteService;
    @Autowired(required = false)
    private DeviceWriteService deviceWriteService;
    @Autowired
    private MsgWebService smsWebService;
    @Autowired
    private MobilePattern mobilePattern;
    @Autowired
    private CoreEventDispatcher coreEventDispatcher;
    @Autowired
    private DeviceReadService deviceReadService;
    @Autowired
    private DoctorUserMaker doctorUserMaker;

    public DoctorCommonSessionBean() {
        String hostIp;
        try {
            hostIp = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            hostIp = UUID.randomUUID().toString();
        }
        hostIpMd5 = Hashing.md5().hashString(hostIp, Charsets.UTF_8).toString().substring(0,8);
    }

    /**
     * ??????sessionId
     * @param key
     * @return
     */
    public String getSessionId(String key) {
        if (isEmpty(key)) {
            throw new JsonResponseException("key.miss");
        }
        return generateId(key);
    }

    /**
     * sessionId ????????????
     */
    private String generateId(String key) {
        StringBuilder builder = new StringBuilder(30);
        String clientKey =  Hashing.md5().hashString(key, Charsets.UTF_8).toString().substring(0, 8);
        builder.append(clientKey).append(SEP).append(hostIpMd5)
                .append(SEP).append(Long.toHexString(System.currentTimeMillis()))
                .append(SEP).append(UUID.randomUUID().toString().substring(0,4));
        return builder.toString();
    }

    /**
     * ?????????????????????
     * @param mobile
     * @param sessionId
     * @param type ?????? io.terminus.doctor.event.enums.SmsCodeType.
     *             ??????type == null, ???????????????????????????
     * @return
     */
    public Boolean getMobileCode(String mobile, String sessionId, Integer type) {
        if (!mobilePattern.getPattern().matcher(mobile).matches()) {
            throw new JsonResponseException("mobile.format.error");
        }
        SmsCodeType smsCodeType;
        if(type == null){
            smsCodeType = SmsCodeType.REGISTER;
        }else{
            smsCodeType = SmsCodeType.from(type);
        }
        if(smsCodeType == null){
            throw new JsonResponseException("sms.code.type.error");
        }

        Response<User> result = doctorUserReadService.findBy(mobile, LoginType.MOBILE);
        switch (smsCodeType){
            case REGISTER:
                // ???????????????????????????
                if(result.isSuccess() && result.getResult() != null){
                    throw new JsonResponseException("user.register.mobile.has.been.used");
                }
                break;
            default:
                //????????????????????????,???????????????
                if(!result.isSuccess() || result.getResult() == null){
                    throw new JsonResponseException(result.getError());
                }
                break;
        }

        return sendSmsCode(mobile, sessionId, smsCodeType.template());
    }

    /**
     * ?????????????????????
     * @param mobile
     * @return
     */
    public Boolean sendSmsCode(String mobile, String sessionId, String templateName) {
        if (mobilePattern.getPattern().matcher(mobile).matches()) {
            Map<String, Object> snapshot = sessionManager.findSessionById(Sessions.MSG_PREFIX, sessionId);
            String activateCode = null;
            if(notNull(snapshot)){
                activateCode = Params.get(snapshot, "code");
            }
            Response<Boolean> result;

            if (!Strings.isNullOrEmpty(activateCode)) { //???????????????????????????????????????
                List<String> parts = Splitters.AT.splitToList(activateCode);
                long sendTime = Long.parseLong(parts.get(1));
                if (System.currentTimeMillis() - sendTime < TimeUnit.MINUTES.toMillis(1)) { //
                    log.error("could not send sms, sms only can be sent once in one minute");
                    throw new JsonResponseException(500, "sms.code.once.a.minute");
                } else {
                    String code = generateMsgCode();

                    // ???code?????????session??????
                    sessionManager.save(Sessions.MSG_PREFIX, sessionId,
                            ImmutableMap.of("code", code + "@" + System.currentTimeMillis()+"@"+mobile),
                            Sessions.SHORT_INACTIVE_INTERVAL);
                    // ???????????????
                    result = doSendSms(code, mobile, templateName);
                }
            } else { //??????????????????
                String code = generateMsgCode();
                // ???code?????????session??????
                sessionManager.save(Sessions.MSG_PREFIX, sessionId,
                        ImmutableMap.of("code", code + "@" + System.currentTimeMillis()+"@"+mobile),
                        Sessions.SHORT_INACTIVE_INTERVAL);
                // ???????????????
                result = doSendSms(code, mobile, templateName);
            }
            if(!result.isSuccess()) {
                log.warn("send sms single fail, cause:{}", result.getError());
                throw new JsonResponseException(500, result.getError());
            }
            return result.getResult();
        } else {
            throw new JsonResponseException(500, "mobile.format.error");
        }
    }

    /**
     * ????????????
     *
     * @param password   ??????
     * @param mobile     ?????????
     * @param code       ???????????????
     * @return ???????????????????????????ID
     */
    public Long register(@NotEmpty(message = "user.password.miss") String password,
                         @NotEmpty(message = "user.mobile.miss") String mobile,
                         @NotEmpty(message = "user.code.miss") String code,
                         @NotEmpty(message = "session.id.miss")String sessionId) {
        checkPasswordFormat(password);

        // ?????????????????????
        validateSmsCode(code, mobile, sessionId);
        User user = registerByMobile(mobile, password, null);
        coreEventDispatcher.publish(new RegisterEvent(null, null, doctorUserMaker.from(user)));
        return user.getId();
    }

    /**
     * ????????????
     */
    public Token login(String name, String password, String code, String sessionId, String deviceId) {
        if (isEmpty(name)) {
            throw new JsonResponseException(500, "user.mobile.miss");
        }
        if (isEmpty(password)) {
            throw new JsonResponseException(500, "user.password.miss");
        }
        if (isEmpty(sessionId)) {
            throw new JsonResponseException(500, "session.id.miss");
        }

        // ???????????????????????????????????????,????????????code
        if (arriveErrorLimit(sessionId)) {
            if (isEmpty(code)) {
                throw new JsonResponseException(500, "user.code.miss");
            }
        }
        // ??????CODE????????????
        Map<String, Object> snapshot = sessionManager.findSessionById(Sessions.CODE_PREFIX, sessionId);
        if (code != null && snapshot.get("code") != null && !snapshot.get("code").equals(code)) {
            throw new JsonResponseException(500, "user.code.mismatch");
        }

        // ?????? ????????????
        User user = doLogin(name, password, sessionId);

        // ?????????????????? sessionId ??? deviceId, ????????????????????????sessionId, ????????????
        sessionManager.save(
                Sessions.TOKEN_PREFIX,
                sessionId,
                ImmutableMap.of(Sessions.USER_ID, (Object) user.getId(), Sessions.DEVICE_ID, (Object) deviceId),
                Sessions.LONG_INACTIVE_INTERVAL);

        // ?????? limit & code
        sessionManager.deletePhysically(Sessions.LIMIT_PREFIX, sessionId);
        sessionManager.deletePhysically(Sessions.CODE_PREFIX, sessionId);

        // ?????????????????????
        Token token = new Token();
        token.setUserId(user.getId());
        token.setName(user.getName());
        token.setDomain(sessionProperties.getCookieDomain());
        token.setExpiredAt(DateTime.now().plusSeconds(Sessions.LONG_INACTIVE_INTERVAL)
                .toString(DateTimeFormat.forPattern("yyyyMMddHHmmss")));
        token.setSessionId(sessionId);
        token.setDeviceId(deviceId);
        token.setCookieName(sessionProperties.getCookieName());
        token.setRoles(MoreObjects.firstNonNull(user.getRoles(), Lists.<String>newArrayList()));
        return token;
    }

    /**
     * ??????????????????, ???????????? sessionId ??? deviceId ??????sessionId??????
     * @param sessionId
     * @param deviceId
     * @return
     */
    public DoctorCommonSessionBean.Token autologin(String sessionId, String deviceId) {
        if (isEmpty(sessionId)) {
            throw new JsonResponseException("session.id.miss");
        }
        if (isEmpty(deviceId)) {
            throw new JsonResponseException("device.id.miss");
        }

        Map<String, Object> snapshot = sessionManager.findSessionById(Sessions.TOKEN_PREFIX, sessionId);
        if (snapshot == null || snapshot.size() == 0 || snapshot.get(Sessions.USER_ID) == null) {
            throw new JsonResponseException(400, "session.id.expired");
        }

        //??????????????????????????????
        checkDeviceId(snapshot, deviceId);

        // refresh
        sessionManager.refreshExpireTime(Sessions.TOKEN_PREFIX, sessionId, Sessions.LONG_INACTIVE_INTERVAL);
        Long uid = Long.parseLong(snapshot.get(Sessions.USER_ID).toString());
        Response<User> res = doctorUserReadService.findById(uid);
        if (!res.isSuccess()) {
            throw new JsonResponseException(400, res.getError());
        }

        // ?????????????????????
        Token token = new Token();
        token.setName(res.getResult().getName());
        token.setDomain(sessionProperties.getCookieDomain());
        token.setExpiredAt(DateTime.now().plusSeconds(Sessions.LONG_INACTIVE_INTERVAL)
                .toString(DateTimeFormat.forPattern("yyyyMMddHHmmss")));
        token.setSessionId(sessionId);
        token.setDeviceId(deviceId);
        token.setCookieName(sessionProperties.getCookieName());
        return token;
    }

    //???????????????????????????
    private void checkDeviceId(Map<String, Object> snapshot, String deviceId) {
        if (snapshot == null || snapshot.size() == 0 || snapshot.get(Sessions.DEVICE_ID) == null) {
            throw new JsonResponseException(400, "device.id.expired");
        }

        if (!Objects.equals(deviceId, String.valueOf(snapshot.get(Sessions.DEVICE_ID)))) {
            throw new JsonResponseException(400, "device.id.not.match");
        }
    }

    public Map<String, String> captcher(String sessionId) {
        if (isEmpty(sessionId)) {
            throw new JsonResponseException("session.id.miss");
        }
        String code = refreshCaptcher(sessionId);
        byte[] data = captchaGenerator.captcha(code);
        return ImmutableMap.of("captcher", Base64Utils.encodeToString(data));
    }

    public void bindDevice(String deviceToken, Integer deviceType) {
        if (isEmpty(deviceToken)) {
            throw new JsonResponseException("device.token.miss");
        }
        MobileDeviceType type = MobileDeviceType.from(deviceType);
        if(type == null){
            throw new JsonResponseException("device.type.error");
        }
        Long userId = UserUtil.getUserId();

        //?????????????????????????????????
        for(UserDevice userDevice : RespHelper.or500(deviceReadService.findByUserId(userId))){
            if(Objects.equals(userDevice.getDeviceToken(), deviceToken)){
                return;
            }
        }

        //??????????????????,??????????????????
        UserDevice userDevice = new UserDevice();
        userDevice.setUserId(userId);
        userDevice.setUserName(UserUtil.getCurrentUser().getName());
        userDevice.setDeviceToken(deviceToken);
        userDevice.setDeviceType(type.name().toLowerCase());
        RespHelper.or500(deviceWriteService.create(userDevice));
    }

    public void unbindDevice(String deviceToken){
        if (isEmpty(deviceToken)) {
            throw new JsonResponseException("device.token.miss");
        }
        Long userId = UserUtil.getUserId();
        for(UserDevice userDevice : RespHelper.or500(deviceReadService.findByUserId(userId))){
            if(Objects.equals(userDevice.getDeviceToken(), deviceToken)){
                RespHelper.or500(deviceWriteService.delete(userDevice.getId()));
            }
        }
    }

    public void logout(String sessionId) {
        log.info("logout session :{}", sessionId);
        sessionManager.deletePhysically(Sessions.TOKEN_PREFIX, sessionId);
    }

    /**
     * ??????????????????
     * @param oldPassword ?????????
     * @param newPassword ?????????
     * @return ????????????
     */
    public Boolean changePassword(@NotEmpty(message = "oldPassword.not.empty") String oldPassword,
                                  @NotEmpty(message = "newPassword.not.empty") String newPassword,
                                  @NotEmpty(message = "session.id.miss")String sessionId) {

        //??????????????????
        checkPasswordFormat(newPassword);

        //1.???session?????????????????????
        Map<String, Object> snapshot = sessionManager.findSessionById(Sessions.TOKEN_PREFIX, sessionId);
        if (snapshot == null || snapshot.size() == 0 || snapshot.get(Sessions.USER_ID) == null) {
            throw new JsonResponseException(400, "session.id.expired");
        }
        //2.????????????
        Response<User> userResp = doctorUserReadService.findById(Long.valueOf(snapshot.get(Sessions.USER_ID).toString()));
        if(!userResp.isSuccess()){
            throw new JsonResponseException(userResp.getError());
        }
        User user = userResp.getResult();
        //3.????????????
        checkPassword(oldPassword, user.getPassword());

        //4.????????????
        user.setPassword(EncryptUtil.encrypt(newPassword));
        Response<Boolean> res = userWriteService.update(user);
        if (!res.isSuccess()) {
            throw new JsonResponseException(res.getError());
        }
        return Boolean.TRUE;
    }

    /**
     * ????????????
     * @param inputPassword
     * @param dbpassword
     */
    private void checkPassword(String inputPassword, String dbpassword){
        if(!EncryptUtil.match(inputPassword, dbpassword)){
            throw new JsonResponseException("user.password.error");
        }
    }

    /**
     * ????????????
     * @param mobile  ?????????
     * @param code    ?????????
     * @param newPassword  ?????????
     * @return ????????????
     */
    public Boolean forgetPassword(@NotEmpty(message = "mobile.not.empty") String mobile,
                                  @NotEmpty(message = "code.not.empty") String code,
                                  @NotEmpty(message = "newPassword.not.empty") String newPassword,
                                  @NotEmpty(message = "session.id.miss")String sessionId) {
        //??????????????????
        checkPasswordFormat(newPassword);
        User user = null;
        // ?????????????????????
        validateSmsCode(code, mobile, sessionId);
        Response<User> userResp = doctorUserReadService.findBy(mobile, LoginType.MOBILE);
        if(!userResp.isSuccess()){
            throw new JsonResponseException(userResp.getError());
        }
        user = userResp.getResult();
        user.setPassword(EncryptUtil.encrypt(newPassword));
        Response<Boolean> res = userWriteService.update(user);
        if (!res.isSuccess()) {
            throw new JsonResponseException(res.getError());
        }
        return Boolean.TRUE;
    }


    /**
     * ?????????????????????
     *
     * @param code   ?????????
     * @param mobile ?????????
     * @return ????????????
     */
    private Response<Boolean> doSendSms(String code, String mobile, String templateName){
        try {
            Map<String, Serializable> context = new HashMap<>();
            context.put("code", code);
            smsWebService.send(mobile, templateName, context, null);
            return Response.ok(Boolean.TRUE);
        }catch (Exception e) {
            log.error("send sms failed, error : {}", Throwables.getStackTraceAsString(e));
            throw new JsonResponseException(500, "sms.send.fail");
        }
    }

    /**
     * ?????????????????????
     */
    private static String generateMsgCode(){
        return String.valueOf((int) ((Math.random() * 9 + 1) * 100000));
    }

    /**
     * ??????????????????
     * @param password
     */
    private void checkPasswordFormat(String password){
        if (!password.matches("[\\s\\S]{6,25}")){
            throw new JsonResponseException(500, "user.password.6to25");
        }
    }

    /**
     * ?????????????????????
     *
     * @param code    ??????????????????
     * @param mobile  ?????????
     * @param sessionId
     */
    private void validateSmsCode(String code, String mobile, String sessionId) {
        Map<String, Object> msgSession = sessionManager.findSessionById(Sessions.MSG_PREFIX, sessionId);
        // session verify, value = code@time@mobile
        String codeInSession = Params.get(msgSession, "code");
        if(isEmpty(codeInSession)){
            throw new JsonResponseException(500, "sms.token.error");
        }
        String expectedCode = Splitters.AT.splitToList(codeInSession).get(0);
        if(!Objects.equals(code, expectedCode)){
            throw new JsonResponseException(500, "sms.token.error");
        }
        String expectedMobile = Splitters.AT.splitToList(codeInSession).get(2);
        if(!Objects.equals(mobile, expectedMobile)){
            throw new JsonResponseException(500, "sms.token.error");
        }
        // ????????????????????????????????????code
        sessionManager.deletePhysically(Sessions.MSG_PREFIX, sessionId);
    }

    /**
     * ????????????
     *
     * @param mobile ?????????
     * @param password ??????
     * @param userName ?????????
     * @return ???????????????????????????
     */
    private User registerByMobile(String mobile, String password, String userName) {
        Response<User> result = doctorUserReadService.findBy(mobile, LoginType.MOBILE);
        // ??????????????????????????????
        if(result.isSuccess() && result.getResult() != null){
            throw new JsonResponseException(500, "user.register.mobile.has.been.used");
        }
        // ??????????????????
        User user = new User();
        user.setMobile(mobile);
        user.setPassword(password);
        user.setName(userName);

        // ???????????? 0: ?????????, 1: ??????, -1: ??????, -2: ??????, -3: ??????
        user.setStatus(UserStatus.NORMAL.value());

        user.setType(UserType.FARM_ADMIN_PRIMARY.value());

        // ???????????????????????????????????????
        user.setRoles(Lists.newArrayList("PRIMARY", "PRIMARY(OWNER)"));

        Response<Long> resp = userWriteService.create(user);
        if (!resp.isSuccess()) {
            throw new JsonResponseException(500, resp.getError());
        }
        user.setId(resp.getResult());
        return user;
    }


    private User doLogin(String name, String password, String sessionId) {
        LoginType loginType;
        if(mobilePattern.getPattern().matcher(name).find()){
            loginType = LoginType.MOBILE;
        } else if(name.contains("@")){
            loginType = LoginType.OTHER;
        } else {
            loginType = LoginType.NAME;
        }
        Response<User> loginUser = doctorUserReadService.findBy(name, loginType);
        if (loginUser.isSuccess() && notNull(loginUser.getResult())
                && notNull(loginUser.getResult().getExtra())
                && loginUser.getResult().getExtra().containsKey("frozen")
                && loginUser.getResult().getExtra().get("frozen").equals(IsOrNot.YES.getKey().toString())) {
            throw new JsonResponseException(500, "user.is.frozen");
        }

        Response<User> result = doctorUserReadService.login(name, password, loginType);
        if (!result.isSuccess()) {
            plusErrorCount(sessionId);
            refreshCaptcher(sessionId);
            // ????????????, ????????????????????????
            throw new JsonResponseException(500, result.getError());
        }
        return result.getResult();
    }

    private void plusErrorCount(String sessionId) {
        Map<String, Object> snapshot = sessionManager.findSessionById(Sessions.LIMIT_PREFIX, sessionId);
        boolean arriveLimit = false;

        if (snapshot.size() == 0) {
            snapshot = Maps.newHashMap();
            snapshot.put("count", 1);
        } else {
            Integer count = (Integer) snapshot.get("count");
            count = count + 1;
            snapshot.put("count", count);
            if (count > 3) {
                arriveLimit = true;
            }
        }
        sessionManager.save(Sessions.LIMIT_PREFIX, sessionId, snapshot, Sessions.MIDDLE_INACTIVE_INTERVAL);
    }

    private boolean arriveErrorLimit(String sessionId) {
        Map<String, Object> snapshot = sessionManager.findSessionById(Sessions.LIMIT_PREFIX, sessionId);
        boolean arriveLimit = false;

        if (snapshot.size() >= 0 && snapshot.get("count") != null) {
            Integer count = (Integer) snapshot.get("count");
            if (count > 2) {
                arriveLimit = true;
            }
        }
        return arriveLimit;
    }

    private String refreshCaptcher(String sessionId) {
        // ????????????????????????session
        String code = tokenGenerator.next();
        Map<String, Object> snapshot = Maps.newHashMap();
        snapshot.put("code", code);
        sessionManager.save(Sessions.CODE_PREFIX, sessionId, snapshot, Sessions.SHORT_INACTIVE_INTERVAL);
        return code;
    }


    @Data
    public static class Token implements Serializable {
        private static final long serialVersionUID = 5867053861663885693L;
        Long userId;
        String name;
        String expiredAt;
        String sessionId;
        String deviceId;
        String cookieName;
        String domain;
        List<String> roles;
    }
}
