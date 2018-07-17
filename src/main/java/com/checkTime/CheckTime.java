package com.checkTime;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.server.DeviceBCFService;
import com.server.EventService;
import com.server.SpringMVCService;
import com.tool.KeyValue;

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

	// 更新布撤防信息
	@Scheduled(cron = "${checkUpdateBCF}")
	public void checkUpdateBCF() throws Exception {
		LOGGER.info(" --- checkUpdateBCF ---");

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HHmmss");
		Date newDate = new Date();
		int hms = Integer.parseInt(simpleDateFormat.format(newDate));

		if (hms > 172959 && hms < 221000) {

			String initIsBFTime = KeyValue.getHMS(hms);

			System.out.println("hms:" + hms + "   hms-value:" + initIsBFTime);

			if (initIsBFTime != null) {
				deviceBCFService.updateBCFService(KeyValue.getHMS(hms)); // 更新布撤防信息
			}
		}
	}

	// 更新陈损设备信息,因为布撤防在22点更新，所以这个检查时间必须大于22点
	@Scheduled(cron = "${checkOldDateTime}")
	public void checkOldDate() throws Exception {
		LOGGER.info(" --- checkOldDateTime ---");
		deviceBCFService.checkOldDateService();// 更新陈损设备，更新风险排行陈损设备
		deviceBCFService.initRanking(false);// 更新风险排行布撤防
	}

	// 定时检查信息是否修改
	@Scheduled(cron = "${checkUpdateInfoTime}")
	public void checkUpdateInfo() {
		// 定时更新信息
		deviceBCFService.updateInfo();
	}

}
