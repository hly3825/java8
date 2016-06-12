package io.terminus.doctor.warehouse.service;

import io.terminus.common.model.Response;
import io.terminus.doctor.warehouse.dao.DoctorWareHouseDao;
import io.terminus.doctor.warehouse.dao.DoctorWareHouseTrackDao;
import io.terminus.doctor.warehouse.model.DoctorFarmWareHouseType;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;

/**
 * Created by yaoqijun.
 * Date:2016-05-25
 * Email:yaoqj@terminus.io
 * Descirbe: 测试
 */
public class DoctorWareHouseReadServiceTest extends BasicServiceTest{

    @Autowired
    private DoctorWareHouseReadService doctorWareHouseReadService;

    @Autowired
    private DoctorWareHouseDao doctorWareHouseDao;

    @Autowired
    private DoctorWareHouseTrackDao doctorWareHouseTrackDao;

    @Test
    public void testFarmWareHouseTypeQuery(){
        Response<List<DoctorFarmWareHouseType>> response = doctorWareHouseReadService.queryDoctorFarmWareHouseType(12345l);
        Assert.assertTrue(response.isSuccess());

        List<DoctorFarmWareHouseType> types = response.getResult();
        Assert.assertThat(types.size(), is(5));

        // validate each item
        types.stream().forEach(t->{
            Assert.assertEquals(t.getFarmId(), new Long(12345l));
            Assert.assertEquals(t.getLotNumber(), new Long(1000));
        });
        Assert.assertThat(123, is(123));
    }
}
