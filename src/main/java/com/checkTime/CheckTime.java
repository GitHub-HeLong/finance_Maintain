package com.checkTime;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.server.DeviceBCFService;
import com.server.EventService;
import com.server.SpringMVCService;

@PropertySource(value = { "classpath:properties/config.properties" })
@Component
public class CheckTime {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(CheckTime.class);

	@Resource
	SpringMVCService springMVCService;

	@Resource
	DeviceBCFService deviceBCFService;

	@Resource
	EventService eventService;

	private @Value("${checkTime}") String queryEventUrl;

	// @Scheduled(cron = "${checkTime}")
	public void taskCycle() throws Exception {
		LOGGER.info(" --- CheckTime ---");
		springMVCService.queryFinanceService();// 获取用户信息表
	}

	// @Scheduled(cron = "${checkUpdateBCF}")
	public void checkUpdateBCF() throws Exception {
		LOGGER.info(" --- checkUpdateBCF ---");
		deviceBCFService.updateBCFService(); // 更新布撤防信息
	}
}
