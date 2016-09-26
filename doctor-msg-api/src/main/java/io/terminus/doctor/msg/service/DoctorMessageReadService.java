package io.terminus.doctor.msg.service;

import io.terminus.common.model.Paging;
import io.terminus.common.model.Response;
import io.terminus.doctor.msg.dto.DoctorMessageSearchDto;
import io.terminus.doctor.msg.model.DoctorMessage;

import java.util.List;
import java.util.Map;

/**
 * Code generated by terminus code gen
 * Desc: 猪场软件消息表读服务
 * Date: 2016-05-30
 * author: chk@terminus.io
 */

public interface DoctorMessageReadService {

    /**
     * 根据id查询猪场软件消息表
     * @param messageId 主键id
     * @return 猪场软件消息表
     */
    Response<DoctorMessage> findMessageById(Long messageId);

    /**
     * 分页预警消息列表
     * @param doctorMessageSearchDto
     * @param pageNo
     * @param pageSize
     * @return
     */
    Response<Paging<DoctorMessage>> pagingWarnMessages(DoctorMessageSearchDto doctorMessageSearchDto, Integer pageNo, Integer pageSize);

    /**
     * 分页系统消息列表
     * @param criteria
     * @param pageNo
     * @param pageSize
     * @return
     */
    Response<Paging<DoctorMessage>> pagingSysMessages(Map<String, Object> criteria, Integer pageNo, Integer pageSize);

    /**
     * 根据查询条件查询
     * @param criteria
     * @return
     */
    Response<List<DoctorMessage>> findMessageByCriteria(Map<String, Object> criteria);

    /**
     * 获取系统消息(最新)
     * @param templateId    模板id
     * @return
     */
    Response<DoctorMessage> findLatestSysMessage(Long templateId);

    /**
     * 获取预警消息(最新)
     * @param templateId    模板id
     * @param farmId        猪场id
     * @param roleId        角色id
     * @return
     */
    Response<DoctorMessage> findLatestWarnMessage(Long templateId, Long farmId, Long roleId);

    /**
     * 获取预警消息(最新)
     * @param templateId    模板id
     * @param farmId        猪场id
     * @return
     */
    Response<DoctorMessage> findLatestWarnMessage(Long templateId, Long farmId);

    /**
     * 获取未读站内信的数量
     * @param userId    用户id
     * @return
     */
    Response<Long> findNoReadCount(Long userId);

    /**
     * 获取未发送的短信消息
     */
    Response<Paging<DoctorMessage>> findMsgMessage(Integer pageNo, Integer pageSize);

    /**
     * 获取未发送的email消息
     */
    Response<Paging<DoctorMessage>> findEmailMessage(Integer pageNo, Integer pageSize);

    /**
     * 获取未发送的app推送消息
     */
    Response<Paging<DoctorMessage>> findAppPushMessage(Integer pageNo, Integer pageSize);

    /**
     * 根据条件获取businessId列表
     * @param doctorMessageSearchDto
     * @return
     */
    Response<List<Long>> findBusinessListByCriteria(DoctorMessageSearchDto doctorMessageSearchDto);

}
