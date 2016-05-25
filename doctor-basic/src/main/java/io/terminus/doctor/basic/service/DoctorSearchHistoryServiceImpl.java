package io.terminus.doctor.basic.service;

import com.google.common.base.Throwables;
import io.terminus.common.model.Response;
import io.terminus.doctor.basic.dao.redis.DoctorSearchHistoryDao;
import io.terminus.doctor.basic.enums.SearchType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Desc: 搜索历史接口
 * Mail: yangzl@terminus.io
 * author: DreamYoung
 * Date: 16/5/24
 */
@Slf4j
@Service
public class DoctorSearchHistoryServiceImpl implements DoctorSearchHistoryService {

    private final DoctorSearchHistoryDao doctorSearchHistoryDao;

    @Autowired
    public DoctorSearchHistoryServiceImpl(DoctorSearchHistoryDao doctorSearchHistoryDao) {
        this.doctorSearchHistoryDao = doctorSearchHistoryDao;
    }

    @Override
    public Response<Boolean> createSearchHistory(Long userId, int searchType, String word) {
        try {
            doctorSearchHistoryDao.setWord(userId, SearchType.from(searchType), word);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("create search history failed, userId:{}, type:{} word:{}, cause:{}",
                    userId, searchType, word, Throwables.getStackTraceAsString(e));
            return Response.fail("search.history.create.fail");
        }
    }

    @Override
    public Response<Set<String>> findSearchHistory(Long userId, int searchType) {
        try {
            return Response.ok(doctorSearchHistoryDao.getWords(userId, SearchType.from(searchType)));
        } catch (Exception e) {
            log.error("find search history failed, userId:{}, type:{} cause:{}",
                    userId, searchType, Throwables.getStackTraceAsString(e));
            return Response.fail("search.history.find.fail");
        }
    }

    @Override
    public Response<Set<String>> findSearchHistory(Long userId, int searchType, Long size) {
        try {
            if (size == null) {
                size = 10L; // 默认查10条
            }
            return Response.ok(doctorSearchHistoryDao.getWords(userId, SearchType.from(searchType), size));
        } catch (Exception e) {
            log.error("find search history failed, userId:{}, type:{} cause:{}",
                    userId, searchType, Throwables.getStackTraceAsString(e));
            return Response.fail("search.history.find.fail");
        }
    }

    @Override
    public Response<Boolean> deleteSearchHistory(Long userId, int searchType, String word) {
        try {
            doctorSearchHistoryDao.deleteWord(userId, SearchType.from(searchType), word);
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("delete search history failed, userId:{}, type:{}, word:{}, cause:{}",
                    userId, searchType, word, Throwables.getStackTraceAsString(e));
            return Response.fail("search.history.delete.fail");
        }
    }

    @Override
    public Response<Boolean> deleteAllSearchHistories(Long userId, int searchType) {
        try {
            doctorSearchHistoryDao.deleteAllWords(userId, SearchType.from(searchType));
            return Response.ok(Boolean.TRUE);
        } catch (Exception e) {
            log.error("delete search history failed, userId:{}, cause:{}",
                    userId, Throwables.getStackTraceAsString(e));
            return Response.fail("search.history.delete.fail");
        }
    }
}
