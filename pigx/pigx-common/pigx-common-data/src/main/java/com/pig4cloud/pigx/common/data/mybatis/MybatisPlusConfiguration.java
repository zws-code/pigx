/*
 *    Copyright (c) 2018-2025, lengleng All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * Neither the name of the pig4cloud.com developer nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * Author: lengleng (wangiegie@gmail.com)
 */

package com.pig4cloud.pigx.common.data.mybatis;

import java.util.List;

import javax.sql.DataSource;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.pig4cloud.pigx.common.data.datascope.DataScopeHandle;
import com.pig4cloud.pigx.common.data.datascope.DataScopeInnerInterceptor;
import com.pig4cloud.pigx.common.data.datascope.DataScopeSqlInjector;
import com.pig4cloud.pigx.common.data.datascope.PigxDefaultDatascopeHandle;
import com.pig4cloud.pigx.common.data.resolver.SqlFilterArgumentResolver;
import com.pig4cloud.pigx.common.data.tenant.PigxTenantHandler;
import com.pig4cloud.pigx.common.security.service.PigxUser;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author lengleng
 * @date 2020-02-08
 */
@Configuration
@ConditionalOnBean(DataSource.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class MybatisPlusConfiguration implements WebMvcConfigurer {

	/**
	 * ?????????????????????????????????????????????????????????SQL ??????
	 * @param resolverList
	 */
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolverList) {
		resolverList.add(new SqlFilterArgumentResolver());
	}

	/**
	 * pigx ???????????????????????????
	 * @return PigxDefaultDatascopeHandle
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnClass(PigxUser.class)
	public DataScopeHandle dataScopeHandle() {
		return new PigxDefaultDatascopeHandle();
	}

	/**
	 * mybatis plus ???????????????
	 * @return PigxDefaultDatascopeHandle
	 */
	@Bean
	public MybatisPlusInterceptor mybatisPlusInterceptor() {
		MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
		// ???????????????
		TenantLineInnerInterceptor tenantLineInnerInterceptor = new TenantLineInnerInterceptor();
		tenantLineInnerInterceptor.setTenantLineHandler(pigxTenantHandler());
		interceptor.addInnerInterceptor(tenantLineInnerInterceptor);
		// ????????????
		DataScopeInnerInterceptor dataScopeInnerInterceptor = new DataScopeInnerInterceptor();
		dataScopeInnerInterceptor.setDataScopeHandle(dataScopeHandle());
		interceptor.addInnerInterceptor(dataScopeInnerInterceptor);
		// ????????????
		interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
		return interceptor;
	}

	/**
	 * ?????????????????????????????????
	 * @return ?????????????????????????????????
	 */
	@Bean
	@ConditionalOnMissingBean
	public PigxTenantHandler pigxTenantHandler() {
		return new PigxTenantHandler();
	}

	/**
	 * ?????? mybatis-plus baseMapper ??????????????????
	 * @return
	 */
	@Bean
	@ConditionalOnBean(DataScopeHandle.class)
	public DataScopeSqlInjector dataScopeSqlInjector() {
		return new DataScopeSqlInjector();
	}

}
