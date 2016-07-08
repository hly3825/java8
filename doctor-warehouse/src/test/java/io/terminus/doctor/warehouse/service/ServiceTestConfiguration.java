package io.terminus.doctor.warehouse.service;

import com.google.common.collect.Lists;
import io.terminus.boot.mybatis.autoconfigure.MybatisAutoConfiguration;
import io.terminus.boot.rpc.dubbo.config.DubboBaseAutoConfiguration;
import io.terminus.boot.search.autoconfigure.ESSearchAutoConfiguration;
import io.terminus.doctor.warehouse.handler.DoctorWareHouseHandlerChain;
import io.terminus.doctor.warehouse.handler.IHandler;
import io.terminus.doctor.warehouse.handler.consume.DoctorConsumerEventHandler;
import io.terminus.doctor.warehouse.handler.consume.DoctorInWareHouseConsumeHandler;
import io.terminus.doctor.warehouse.handler.consume.DoctorMaterialAvgConsumerHandler;
import io.terminus.doctor.warehouse.handler.consume.DoctorWareHouseTrackConsumeHandler;
import io.terminus.doctor.warehouse.handler.consume.DoctorWareHouseTypeConsumerHandler;
import io.terminus.doctor.warehouse.handler.provider.DoctorInWareHouseProviderHandler;
import io.terminus.doctor.warehouse.handler.provider.DoctorProviderEventHandler;
import io.terminus.doctor.warehouse.handler.provider.DoctorTrackProviderHandler;
import io.terminus.doctor.warehouse.handler.provider.DoctorTypeProviderHandler;
import io.terminus.doctor.warehouse.search.material.BaseMaterialQueryBuilder;
import io.terminus.doctor.warehouse.search.material.DefaultIndexedMaterialFactory;
import io.terminus.doctor.warehouse.search.material.DefaultMaterialQueryBuilder;
import io.terminus.doctor.warehouse.search.material.IndexedMaterial;
import io.terminus.doctor.warehouse.search.material.IndexedMaterialFactory;
import io.terminus.doctor.warehouse.search.material.MaterialSearchProperties;
import io.terminus.search.core.ESClient;
import io.terminus.zookeeper.ZKClientFactory;
import io.terminus.zookeeper.pubsub.Publisher;
import io.terminus.zookeeper.pubsub.Subscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Created by yaoqijun.
 * Date:2016-05-25
 * Email:yaoqj@terminus.io
 * Descirbe: Service 信息配置工具类
 */
@Configuration
@EnableAutoConfiguration(exclude = {DubboBaseAutoConfiguration.class, ESSearchAutoConfiguration.class})
@ComponentScan("io.terminus.doctor.warehouse.*")
@AutoConfigureAfter(MybatisAutoConfiguration.class)
public class ServiceTestConfiguration {


    @Configuration
    @Profile("zookeeper")
    public static class ZookeeperConfiguration{

        @Bean
        public Subscriber cacheListenerBean(ZKClientFactory zkClientFactory,
                                            @Value("${zookeeper.zkTopic}") String zkTopic) throws Exception{
            return new Subscriber(zkClientFactory,zkTopic);
        }

        @Bean
        public Publisher cachePublisherBean(ZKClientFactory zkClientFactory,
                                            @Value("${zookeeper.zkTopic}}") String zkTopic) throws Exception{
            return new Publisher(zkClientFactory, zkTopic);
        }
    }

    @Bean
    public DoctorWareHouseHandlerChain doctorWareHouseHandlerChain(
            DoctorConsumerEventHandler doctorConsumerEventHandler, DoctorInWareHouseConsumeHandler doctorInWareHouseConsumeHandler,
            DoctorMaterialAvgConsumerHandler doctorMaterialAvgConsumerHandler, DoctorWareHouseTrackConsumeHandler doctorWareHouseTrackConsumeHandler,
            DoctorWareHouseTypeConsumerHandler doctorWareHouseTypeConsumerHandler,
            DoctorInWareHouseProviderHandler doctorInWareHouseProviderHandler, DoctorProviderEventHandler doctorProviderEventHandler,
            DoctorTrackProviderHandler doctorTrackProviderHandler, DoctorTypeProviderHandler doctorTypeProviderHandler){
        List<IHandler> iHandlers = Lists.newArrayList();

        // consumer
        iHandlers.add(doctorConsumerEventHandler);
        iHandlers.add(doctorInWareHouseConsumeHandler);
        iHandlers.add(doctorMaterialAvgConsumerHandler);
        iHandlers.add(doctorWareHouseTrackConsumeHandler);
        iHandlers.add(doctorWareHouseTypeConsumerHandler);

        // provider
        iHandlers.add(doctorProviderEventHandler);
        iHandlers.add(doctorInWareHouseProviderHandler);
        iHandlers.add(doctorTrackProviderHandler);
        iHandlers.add(doctorTypeProviderHandler);

        return new DoctorWareHouseHandlerChain(iHandlers);
    }

    @Configuration
    @ConditionalOnClass(ESClient.class)
    @ComponentScan({"io.terminus.search.api"})
    public static class SearchConfiguration {
        @Bean
        public ESClient esClient(@Value("${search.host:localhost}") String host,
                                 @Value("${search.port:9200}") Integer port) {
            return new ESClient(host, port);
        }

        @Configuration
        @EnableConfigurationProperties(MaterialSearchProperties.class)
        protected static class MaterialSearchConfiguration {

            @Bean
            @ConditionalOnMissingBean(IndexedMaterialFactory.class)
            public IndexedMaterialFactory<? extends IndexedMaterial> indexedMaterialFactory() {
                return new DefaultIndexedMaterialFactory();
            }

            @Bean
            @ConditionalOnMissingBean(BaseMaterialQueryBuilder.class)
            public BaseMaterialQueryBuilder baseMaterialQueryBuilder() {
                return new DefaultMaterialQueryBuilder();
            }
        }
    }

}
