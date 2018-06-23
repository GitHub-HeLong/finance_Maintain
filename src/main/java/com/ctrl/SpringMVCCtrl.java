package com.ctrl;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONObject;
import com.es.EsDao;
import com.mq.MQSender;
import com.mq.MqTopicSendServer;
import com.server.DeviceBCFService;
import com.server.EventService;
import com.server.SpringMVCService;
import com.tool.HttpTool;

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
	EsDao esDao;

	@Resource
	DeviceBCFService deviceBCFService;

	@Resource
	MqTopicSendServer mqTopicSendServer;

	@RequestMapping("requestService")
	@ResponseBody
	public JSONObject requestService(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		JSONObject json = new JSONObject();

		springMVCService.queryFinanceService();// 获取用户信息表

		eventService.checkoutTryZoneAndAlarm(); // 更新试机表信息

		// // 获取真警信息更新到数据事件表
		// String[] actualSituations = { "3", "6", "7" };
		// eventService.checkIsAlarm("processing", actualSituations, "isAlarm");
		//
		// // 获取误报信息更新到数据事件表
		// String[] noAlarmActualSituations = { "4", "5", "8", "9", "10", "12"
		// };
		// eventService.checkIsAlarm("verify", noAlarmActualSituations,
		// "noAlarm");
		//
		// // 更新布撤防信息
		// deviceBCFService.updateBCFService();

		return json;
	}

	@RequestMapping("init")
	@ResponseBody
	public JSONObject init(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		JSONObject json = new JSONObject();

		LOGGER.info(" --- 初始化用户信息开始 --- ");

		springMVCService.queryFinanceService();// 获取用户信息表

		eventService.checkoutTryZoneAndAlarm(); // 更新试机表信息

		// 获取真警信息更新到数据事件表
		String[] actualSituations = { "3", "6", "7" };
		eventService.checkIsAlarm("processing", actualSituations, "isAlarm");

		// 获取误报信息更新到数据事件表
		String[] noAlarmActualSituations = { "4", "5", "8", "9", "10", "12" };
		eventService.checkIsAlarm("verify", noAlarmActualSituations, "noAlarm");

		// 更新布撤防信息
		deviceBCFService.updateBCFService();

		LOGGER.info(" --- 初始化用户信息结束--- ");

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
