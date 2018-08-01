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

import com.mysqlDao.FinanceMysql;
import com.server.DeviceBCFService;
import com.server.EventService;
import com.server.SpringMVCService;
import com.tool.AgainTool;
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

	@Resource
	FinanceMysql financeMysql;

	private @Value("${checkTime}") String queryEventUrl;

	@Scheduled(cron = "${checkTime}")
	public void taskCycle() throws Exception {
		LOGGER.info(" --- 每月初始化相关信息 ---");

		financeMysql.cleanTryZone(); // 每个月清空防区表数据
		financeMysql.cleanRankingInfo();// 每月清零排行表数据

		springMVCService.updateMonth();
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

			if (initIsBFTime != null) {
				deviceBCFService.updateBCFService(KeyValue.getHMS(hms)); // 更新布撤防信息
			}

			if ("2200".equals(initIsBFTime)) {
				deviceBCFService.initRanking(false);// 更新风险排行布撤防
			}
		}
	}

	// 更新陈损设备信息,每天检查一次即可
	@Scheduled(cron = "${checkOldDateTime}")
	public void checkOldDate() throws Exception {
		LOGGER.info(" --- checkOldDateTime ---");
		deviceBCFService.checkOldDateService();// 更新陈损设备，更新风险排行陈损设备
	}

	// 定时检查信息是否修改
	@Scheduled(cron = "${checkUpdateInfoTime}")
	public void checkUpdateInfo() {
		// 定时更新信息
		deviceBCFService.updateInfo();
	}

	// 每天校正报警、单据信息
	@Scheduled(cron = "${checkAlarmInfo}")
	public void checkAlarmInfo() {

		String yesterday = AgainTool.yesterdayForm();

		String month = yesterday.substring(0, 7);
		String D = Integer.parseInt(yesterday.substring(8, 10)) + "";

		// 先清空昨天的事件信息
		financeMysql.cleanEventDate(month, D);

		// 获取真警信息更新到数据事件表
		String[] actualSituations = { "3", "6", "7" };
		eventService.checkIsAlarmEveryDay("processing", "actualSituation",
				actualSituations, "isAlarm");

		// 处警单中：获取误报信息更新到数据事件表
		String[] noAlarmActualSituations = { "9", "10", "11", "12", "13" };
		eventService.checkIsAlarmEveryDay("processing", "actualSituation",
				noAlarmActualSituations, "noAlarm");
		// 核单中：获取误报信息更新到数据事件表
		eventService.checkIsAlarmEveryDay("verify", "actualSituation",
				noAlarmActualSituations, "noAlarm");

		// 获取非真警、非误报信息更新到数据事件表
		String[] noisAlarmAndError = { "1", "2", "4", "5", "8", "14", "15",
				"17", "18" };
		eventService.checkIsAlarmEveryDay("processing", "actualSituation",
				noisAlarmAndError, "noIsAlarmAndError");
		eventService.checkIsAlarmEveryDay("verify", "actualSituation",
				noisAlarmAndError, "noIsAlarmAndError");

		// 获取级别报警写入到事件表
		String[] levels = { "0", "1", "2", "3", "4", "6", "10", "14" };
		eventService.checkIsAlarmEveryDay("alert_processing", "codeTypeId",
				levels, "level");

		LOGGER.info("深夜更新{}完成", yesterday);
	}
}
