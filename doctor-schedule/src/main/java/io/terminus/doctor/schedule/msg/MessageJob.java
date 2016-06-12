package io.terminus.doctor.schedule.msg;

import com.google.common.base.Throwables;
import io.terminus.zookeeper.leader.HostLeader;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Desc: 消息 job
 * Mail: chk@terminus.io
 * Created by icemimosa
 * Date: 16/6/1
 */
@Component
@Configurable
@EnableScheduling
@Slf4j
public class MessageJob {

    @Autowired
    private HostLeader hostLeader;

    @Autowired
    private MsgManager msgManager;

    /**
     * 产生消息
     */
    // @Scheduled(cron = "0 0/10 * * * ?")
    @Scheduled(cron = "0/10 * * * * ?")
    public void messageProduce() {
        try {
            if (!hostLeader.isLeader()) {
                log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
                return;
            }
            log.info("message produce fired");
            msgManager.produce();
            log.info("message produce end");
        } catch (Exception e) {
            log.error("message produce failed, cause by {}", Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 消费短信消息
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void messageConsume() {
        try {
            if (!hostLeader.isLeader()) {
                log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
                return;
            }
            log.info("msg message consume fired");
            msgManager.consumeMsg();
            log.info("msg message consume end");
        } catch (Exception e) {
            log.error("msg message consume failed, cause by {}", Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 消费邮件消息
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void emailConsume() {
        try {
            if (!hostLeader.isLeader()) {
                log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
                return;
            }
            log.info("msg message consume fired");
            msgManager.consumeEmail();
            log.info("msg message consume end");
        } catch (Exception e) {
            log.error("msg message consume failed, cause by {}", Throwables.getStackTraceAsString(e));
        }
    }

    /**
     * 消费 app push 消息
     */
    @Scheduled(cron = "0 0/10 * * * ?")
    public void appPushConsume() {
        try {
            if (!hostLeader.isLeader()) {
                log.info("current leader is:{}, skip", hostLeader.currentLeaderId());
                return;
            }
            log.info("app push message consume fired");
            msgManager.consumeAppPush();
            log.info("app push message consume end");
        } catch (Exception e) {
            log.error("app push message consume failed, cause by {}", Throwables.getStackTraceAsString(e));
        }
    }
}
