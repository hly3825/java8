package io.terminus.doctor.user.manager;

import com.google.common.collect.Lists;
import io.terminus.common.exception.ServiceException;
import io.terminus.common.utils.BeanMapper;
import io.terminus.doctor.common.enums.UserStatus;
import io.terminus.doctor.common.enums.UserType;
import io.terminus.doctor.common.utils.JsonMapperUtil;
import io.terminus.doctor.common.utils.ToJsonMapper;
import io.terminus.doctor.user.dao.PrimaryUserDao;
import io.terminus.doctor.user.dao.UserDaoExt;
import io.terminus.doctor.user.interfaces.event.EventType;
import io.terminus.doctor.user.interfaces.event.UserEvent;
import io.terminus.doctor.user.interfaces.model.UserDto;
import io.terminus.doctor.user.model.DoctorServiceReview;
import io.terminus.doctor.user.model.DoctorServiceStatus;
import io.terminus.doctor.user.model.PrimaryUser;
import io.terminus.doctor.user.service.DoctorServiceReviewWriteService;
import io.terminus.doctor.user.service.DoctorServiceStatusWriteService;
import io.terminus.parana.user.impl.dao.UserDao;
import io.terminus.parana.user.impl.dao.UserProfileDao;
import io.terminus.parana.user.model.User;
import io.terminus.parana.user.model.UserProfile;
import io.terminus.zookeeper.common.ZKClientFactory;
import io.terminus.zookeeper.pubsub.Publisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by chenzenghui on 16/11/15.
 */
@Slf4j
@Component
public class UserInterfaceManager {

    private final Publisher publisher;

    @Autowired
    private UserDaoExt userDaoExt;
    @Autowired
    private DoctorServiceReviewWriteService doctorServiceReviewWriteService;
    @Autowired
    private DoctorServiceStatusWriteService doctorServiceStatusWriteService;
    @Autowired
    private UserDao userDao;
    @Autowired
    private PrimaryUserDao primaryUserDao;
    @Autowired
    private UserProfileDao userProfileDao;

    @Autowired
    public UserInterfaceManager(ZKClientFactory zkClientFactory,
                                @Value("${user.center.topic}") String userCenterTopic) throws Exception {
        this.publisher = new Publisher(zkClientFactory, userCenterTopic);
    }

    /**
     * ????????????????????? zookeeper topic ????????????
     * @param user ????????????
     * @param eventType ????????????
     * @param systemCode ?????????????????????
     * @throws Exception
     */
    public void pulishZkEvent(UserDto user, EventType eventType, String systemCode) throws Exception {
        try {
            publisher.publish(ToJsonMapper.JSON_NON_EMPTY_MAPPER.toJson(new UserEvent(user, eventType, systemCode)).getBytes());
        } catch(Exception e) {
            log.info("throw a error when publish event, user:{}, eventType:{}, systemCode:{}", user, eventType.name(), systemCode);
            if(!e.getMessage().equals("no subscribers exists")){
                throw e;
            }
        }
    }

    /**
     * ?????????zkTopic?????????????????????????????????????????????????????????????????????
     */
    @Transactional
    public void update(UserDto user, String systemCode) throws Exception{
        User paranaUser = BeanMapper.map(user, User.class);
        userDaoExt.update(paranaUser);
        pulishZkEvent(user, EventType.UPDATE, systemCode);
    }

    /**
     * ?????????zkTopic?????????????????????????????????????????????????????????????????????
     */
    @Transactional
    public UserDto create(UserDto user, String systemCode) throws Exception {
        User newUser = registerByMobile(BeanMapper.map(user, User.class), systemCode);
        user = BeanMapper.map(newUser, UserDto.class);
        pulishZkEvent(user, EventType.CREATE, systemCode);
        return user;
    }

    /**
     * ?????????zkTopic?????????????????????????????????????????????????????????????????????
     */
    @Transactional
    public void deletes(List<Long> ids, String systemCode) throws Exception {
        if(ids != null){
            userDaoExt.deletes(ids);
            for(Long id : ids){
                pulishZkEvent(new UserDto(id), EventType.DELETE, systemCode);
            }
        }
    }

    //?????????????????????
    private User registerByMobile(User user, String systemCode) {
        checkMobileRepeat(user.getMobile());

        //???????????????????????????
        user.setStatus(UserStatus.NORMAL.value());  //????????????
        user.setType(UserType.FARM_ADMIN_PRIMARY.value()); //?????????????????????
        user.setRoles(Lists.newArrayList("PRIMARY", "PRIMARY(OWNER)"));
        userDao.create(user);
        Long userId = user.getId();

        //???????????????
        PrimaryUser primaryUser = new PrimaryUser();
        primaryUser.setUserId(userId);
        //?????????????????????
        primaryUser.setUserName(user.getMobile());
        primaryUser.setStatus(UserStatus.NORMAL.value());
        primaryUserDao.create(primaryUser);

        //??????????????????
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(userId);
        userProfileDao.create(userProfile);

        //?????????????????????
        initReview(user, systemCode);
        return user;
    }

    //???????????????????????? // TODO: 2016/11/15 ???????????? systemCode ???????????????????????????
    private void initReview(User user, String systemCode) {
        DoctorServiceStatus status = new DoctorServiceStatus();
        status.setUserId(user.getId());

        status.setPigdoctorStatus(DoctorServiceStatus.Status.CLOSED.value());
        status.setPigdoctorReviewStatus(DoctorServiceReview.Status.INIT.getValue());

        status.setPigmallStatus(DoctorServiceStatus.Status.BETA.value());
        status.setPigmallReviewStatus(DoctorServiceReview.Status.INIT.getValue());
        status.setPigmallReason("????????????");

        status.setNeverestStatus(DoctorServiceStatus.Status.BETA.value());
        status.setNeverestReviewStatus(DoctorServiceReview.Status.INIT.getValue());
        status.setNeverestReason("????????????");

        status.setPigtradeStatus(DoctorServiceStatus.Status.BETA.value());
        status.setPigtradeReviewStatus(DoctorServiceReview.Status.INIT.getValue());
        status.setPigtradeReason("????????????");

        doctorServiceStatusWriteService.createServiceStatus(status);
        doctorServiceReviewWriteService.initServiceReview(user.getId(), user.getMobile(), user.getName());
    }

    // ??????????????????????????????
    private void checkMobileRepeat(String mobile) {
        if(userDaoExt.findByMobile(mobile) != null){
            throw new ServiceException("user.register.mobile.has.been.used");
        }
    }
}
