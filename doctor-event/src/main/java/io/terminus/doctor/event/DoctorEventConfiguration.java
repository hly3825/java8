package io.terminus.doctor.event;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import io.terminus.boot.mybatis.autoconfigure.MybatisAutoConfiguration;
import io.terminus.doctor.event.dao.DoctorPigDao;
import io.terminus.doctor.event.search.barn.BarnSearchProperties;
import io.terminus.doctor.event.search.barn.BaseBarnQueryBuilder;
import io.terminus.doctor.event.search.barn.DefaultBarnQueryBuilder;
import io.terminus.doctor.event.search.barn.DefaultIndexedBarnFactory;
import io.terminus.doctor.event.search.barn.IndexedBarn;
import io.terminus.doctor.event.search.barn.IndexedBarnFactory;
import io.terminus.doctor.event.search.group.BaseGroupQueryBuilder;
import io.terminus.doctor.event.search.group.DefaultGroupQueryBuilder;
import io.terminus.doctor.event.search.group.DefaultIndexedGroupFactory;
import io.terminus.doctor.event.search.group.GroupSearchProperties;
import io.terminus.doctor.event.search.group.IndexedGroup;
import io.terminus.doctor.event.search.group.IndexedGroupFactory;
import io.terminus.doctor.event.search.pig.BasePigQueryBuilder;
import io.terminus.doctor.event.search.pig.DefaultIndexedPigFactory;
import io.terminus.doctor.event.search.pig.DefaultPigQueryBuilder;
import io.terminus.doctor.event.search.pig.IndexedPig;
import io.terminus.doctor.event.search.pig.IndexedPigFactory;
import io.terminus.doctor.event.search.pig.PigSearchProperties;
import io.terminus.doctor.workflow.DoctorWorkflowConfiguration;
import io.terminus.search.core.ESClient;
import io.terminus.zookeeper.ZKClientFactory;
import io.terminus.zookeeper.pubsub.Publisher;
import io.terminus.zookeeper.pubsub.Subscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

import java.util.concurrent.Executors;

/**
 * Created by yaoqijun.
 * Date:2016-04-22
 * Email:yaoqj@terminus.io
 * Descirbe:
 */
@Configuration
@ComponentScan(basePackages = {
        "io.terminus.doctor.event",
})
@Import(DoctorWorkflowConfiguration.class)
@AutoConfigureAfter(MybatisAutoConfiguration.class)
public class  DoctorEventConfiguration {

    @Bean
    public EventBus eventBus(){
        return new AsyncEventBus(
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }

    @Configuration
    @ConditionalOnBean(ZKClientFactory.class)
    @Profile("zookeeper")
    public static class ZookeeperConfiguration{

        @Bean
        public Subscriber cacheListenerBean(ZKClientFactory zkClientFactory,
                                            @Value("${zookeeper.cacheTopic}") String cacheTopic) throws Exception{
            return new Subscriber(zkClientFactory,cacheTopic);
        }

        @Bean
        public Publisher cachePublisherBean(ZKClientFactory zkClientFactory,
                                            @Value("${zookeeper.cacheTopic}}") String cacheTopic) throws Exception{
            return new Publisher(zkClientFactory, cacheTopic);
        }
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
        @EnableConfigurationProperties(PigSearchProperties.class)
        protected static class PigSearchConfiguration {
            @Bean
            @ConditionalOnMissingBean(IndexedPigFactory.class)
            public IndexedPigFactory<? extends IndexedPig> indexedPigFactory(DoctorPigDao doctorPigDao) {
                return new DefaultIndexedPigFactory(doctorPigDao);
            }

            @Bean
            @ConditionalOnMissingBean(BasePigQueryBuilder.class)
            public BasePigQueryBuilder pigQueryBuilder() {
                return new DefaultPigQueryBuilder();
            }
        }

        @Configuration
        @EnableConfigurationProperties(GroupSearchProperties.class)
        protected static class GroupSearchConfiguration {
            @Bean
            @ConditionalOnMissingBean(IndexedGroupFactory.class)
            public IndexedGroupFactory<? extends IndexedGroup> indexedGroupFactory() {
                return new DefaultIndexedGroupFactory();
            }

            @Bean
            @ConditionalOnMissingBean(BaseGroupQueryBuilder.class)
            public BaseGroupQueryBuilder groupQueryBuilder() {
                return new DefaultGroupQueryBuilder();
            }
        }

        @Configuration
        @EnableConfigurationProperties(BarnSearchProperties.class)
        protected static class BarnSearchConfiguration {
            @Bean
            @ConditionalOnMissingBean(IndexedBarnFactory.class)
            public IndexedBarnFactory<? extends IndexedBarn> indexedBarnFactory() {
                return new DefaultIndexedBarnFactory();
            }

            @Bean
            @ConditionalOnMissingBean(BaseBarnQueryBuilder.class)
            public BaseBarnQueryBuilder BarnQueryBuilder() {
                return new DefaultBarnQueryBuilder();
            }
        }
    }
}
