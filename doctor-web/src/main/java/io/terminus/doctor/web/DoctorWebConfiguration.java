/*
 * Copyright (c) 2016. 杭州端点网络科技有限公司.  All rights reserved.
 */

package io.terminus.doctor.web;

import io.terminus.doctor.user.service.SubRoleReadService;
import io.terminus.doctor.web.core.DoctorCoreWebConfiguration;
import io.terminus.doctor.web.core.advices.JsonExceptionResolver;
import io.terminus.doctor.web.core.msg.email.CommonEmailServiceConfig;
import io.terminus.doctor.web.core.msg.sms.LuoSiMaoSmsServiceConfig;
import io.terminus.doctor.web.core.service.OtherSystemServiceConfig;
import io.terminus.doctor.web.front.auth.DoctorCustomRoleLoaderConfigurer;
import io.terminus.parana.auth.role.CustomRoleLoaderConfigurer;
import io.terminus.parana.auth.role.CustomRoleLoaderRegistry;
import io.terminus.parana.auth.web.WebAuthenticationConfiguration;
import io.terminus.parana.web.msg.config.MsgWebConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Author:  <a href="mailto:i@terminus.io">jlchen</a>
 * Date: 2016-02-01
 */
@EnableSwagger2
@Configuration
@ComponentScan(basePackages = {
        "io.terminus.doctor.web.core.component",
        "io.terminus.doctor.web.core.events",
        "io.terminus.doctor.web.core.service",
        "io.terminus.doctor.web.front.component",
        "io.terminus.doctor.web.front.design"
}, excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                JsonExceptionResolver.class
        })
})
@EnableWebMvc
@EnableAutoConfiguration
@Import({DoctorCoreWebConfiguration.class,
        OtherSystemServiceConfig.class,
        WebAuthenticationConfiguration.class,
        MsgWebConfig.class,
        LuoSiMaoSmsServiceConfig.class,
        CommonEmailServiceConfig.class,
})
public class DoctorWebConfiguration extends WebMvcConfigurerAdapter {

    @Bean
    public CustomRoleLoaderConfigurer customRoleLoaderConfigurer(CustomRoleLoaderRegistry customRoleLoaderRegistry, SubRoleReadService subRoleReadService) {
        CustomRoleLoaderConfigurer configurer = new DoctorCustomRoleLoaderConfigurer(subRoleReadService);
        configurer.configureCustomRoleLoader(customRoleLoaderRegistry);
        return configurer;
    }

    @Bean
    public Docket config() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .useDefaultResponseMessages(false)
                .select()
                .apis(RequestHandlerSelectors.basePackage("io.terminus.doctor.web.front.role"))
                .build();
    }

    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("pigDoctor系统API文档")
                .build();
    }
}
