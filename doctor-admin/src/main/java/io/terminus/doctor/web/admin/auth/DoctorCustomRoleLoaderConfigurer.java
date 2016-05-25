package io.terminus.doctor.web.admin.auth;

import io.terminus.doctor.user.service.OperatorRoleReadService;
import io.terminus.parana.auth.role.CustomRoleLoaderConfigurer;
import io.terminus.parana.auth.role.CustomRoleLoaderRegistry;
import io.terminus.parana.auth.role.CustomRoleReadServiceWrapper;

/**
 * @author houly
 */
public class DoctorCustomRoleLoaderConfigurer implements CustomRoleLoaderConfigurer {

    private final OperatorRoleReadService operatorRoleReadService;

    public DoctorCustomRoleLoaderConfigurer(OperatorRoleReadService operatorRoleReadService) {
        this.operatorRoleReadService = operatorRoleReadService;
    }

    @Override
    public void configureCustomRoleLoader(CustomRoleLoaderRegistry registry) {
        registry.register("PC", "ADMIN", new CustomRoleReadServiceWrapper(operatorRoleReadService));
    }
}
