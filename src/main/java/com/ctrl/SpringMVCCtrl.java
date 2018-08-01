package com.ctrl;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.mq.MQSender;
import com.mq.MqTopicSendServer;
import com.server.DeviceBCFService;
import com.server.EventService;
import com.server.SpringMVCService;
import com.tool.HttpTool;

@PropertySource(value = { "classpath:properties/config.properties" })
@Controller
@RequestMapping("springMVCCtrl")
public class SpringMVCCtrl {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(SpringMVCCtrl.class);

	@Resource
	SpringMVCService springMVCService;

	@Resource
	EventService eventService;

	@Resource
	MQSender mqSender;

	@Resource
	DeviceBCFService deviceBCFService;

	@Resource
	MqTopicSendServer mqTopicSendServer;

	private @Value("${initIsBFTime}") String initIsBFTime;

	@RequestMapping("requestService")
	@ResponseBody
	public JSONObject requestService(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		JSONObject json = new JSONObject();

		LOGGER.info(" --- 初始化用户信息开始 --- ");
		// 加载用户基本信息
		springMVCService.queryFinanceService();

		// 获取真警信息更新到数据事件表
		String[] actualSituations = { "3", "6", "7" };
		eventService.checkIsAlarm("processing", "actualSituation",
				actualSituations, "isAlarm");

		// 处警单中：获取误报信息更新到数据事件表
		String[] noAlarmActualSituations = { "9", "10", "11", "12", "13" };
		eventService.checkIsAlarm("processing", "actualSituation",
				noAlarmActualSituations, "noAlarm");
		// 核单中：获取误报信息更新到数据事件表
		eventService.checkIsAlarm("verify", "actualSituation",
				noAlarmActualSituations, "noAlarm");

		// 获取非真警、非误报信息更新到数据事件表
		String[] noisAlarmAndError = { "1", "2", "4", "5", "8", "14", "15",
				"17", "18" };
		eventService.checkIsAlarm("processing", "actualSituation",
				noisAlarmAndError, "noIsAlarmAndError");
		eventService.checkIsAlarm("verify", "actualSituation",
				noisAlarmAndError, "noIsAlarmAndError");

		// 获取级别报警写入到事件表
		String[] levels = { "0", "1", "2", "3", "4", "6", "10", "14" };
		eventService.checkIsAlarm("alert_processing", "codeTypeId", levels,
				"level");

		// 更新布撤防信息
		deviceBCFService.updateBCFService(initIsBFTime);

		// 更新试机信息
		eventService.checkoutTryZoneAndAlarm();

		// 风险排行,初始化布撤防数、真警数
		deviceBCFService.initRanking(true);

		LOGGER.info("---初始化信息结束---");

		return json;
	}

	@RequestMapping("init")
	@ResponseBody
	public JSONObject init(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		JSONObject json = new JSONObject();

		LOGGER.info(" --- 初始化用户信息开始 --- ");
		// 加载用户基本信息
		springMVCService.queryFinanceService();

		// 获取真警信息更新到数据事件表
		String[] actualSituations = { "3", "6", "7" };
		eventService.checkIsAlarm("processing", "actualSituation",
				actualSituations, "isAlarm");

		// 处警单中：获取误报信息更新到数据事件表
		String[] noAlarmActualSituations = { "9", "10", "11", "12", "13" };
		eventService.checkIsAlarm("processing", "actualSituation",
				noAlarmActualSituations, "noAlarm");
		// 核单中：获取误报信息更新到数据事件表
		eventService.checkIsAlarm("verify", "actualSituation",
				noAlarmActualSituations, "noAlarm");

		// 获取非真警、非误报信息更新到数据事件表
		String[] noisAlarmAndError = { "1", "2", "4", "5", "8", "14", "15",
				"17", "18" };
		eventService.checkIsAlarm("processing", "actualSituation",
				noisAlarmAndError, "noIsAlarmAndError");
		eventService.checkIsAlarm("verify", "actualSituation",
				noisAlarmAndError, "noIsAlarmAndError");

		// 获取级别报警写入到事件表
		String[] levels = { "0", "1", "2", "3", "4", "6", "10", "14" };
		eventService.checkIsAlarm("alert_processing", "codeTypeId", levels,
				"level");

		// 更新布撤防信息
		deviceBCFService.updateBCFService(initIsBFTime);

		// 更新试机信息
		eventService.checkoutTryZoneAndAlarm();

		// 风险排行,初始化布撤防数、真警数
		deviceBCFService.initRanking(true);

		LOGGER.info("---初始化信息结束---");

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

	@RequestMapping("sendMQToQueryService")
	@ResponseBody
	public void sendMQToQueryService(HttpServletRequest request,
			HttpServletResponse response) {
		mqSender.send("集成mq和mysql！");
	}

	@RequestMapping("sendMQToTopicService")
	@ResponseBody
	public void sendMQToTopicService(HttpServletRequest request,
			HttpServletResponse response) {
		mqTopicSendServer.sendMessage("发送广播 ！");
	}

	@RequestMapping("sendHttpTool")
	@ResponseBody
	public void sendHttpTool(HttpServletRequest request,
			HttpServletResponse response) {
		try {
			String str = HttpTool.post3(
					"http://10.0.17.19:8080/data-sync-up/check.do", "");
			LOGGER.info("str : {}", str);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
