package com.mq;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.server.MqService;

@Component(value = "msgListener")
public class MQListenerContainer implements MessageListener {
	private Logger logger = Logger.getLogger(MQListenerContainer.class);

	// 需要统计的级别报警
	static List<String> codeTypeIds = new ArrayList<String>();

	// 核警单报警类型（包含了需要统计的和不需要统计的）
	static List<String> verifyActualSituation = new ArrayList<String>();

	// 处警单报警类型（包含了需要统计的和不需要统计的）
	static List<String> processingActualSituation = new ArrayList<String>();

	static {
		codeTypeIds.add("0");
		codeTypeIds.add("1");
		codeTypeIds.add("2");
		codeTypeIds.add("3");
		codeTypeIds.add("4");
		codeTypeIds.add("6");
		codeTypeIds.add("10");
		codeTypeIds.add("14");

		verifyActualSituation.add("1");
		verifyActualSituation.add("2");
		verifyActualSituation.add("4");
		verifyActualSituation.add("5");
		verifyActualSituation.add("8");
		verifyActualSituation.add("9");
		verifyActualSituation.add("10");
		verifyActualSituation.add("11");
		verifyActualSituation.add("12");
		verifyActualSituation.add("13");
		verifyActualSituation.add("14");
		verifyActualSituation.add("15");
		verifyActualSituation.add("17");
		verifyActualSituation.add("18");

		processingActualSituation.add("1");
		processingActualSituation.add("2");
		processingActualSituation.add("3");
		processingActualSituation.add("4");
		processingActualSituation.add("5");
		processingActualSituation.add("6");
		processingActualSituation.add("7");
		processingActualSituation.add("8");
		processingActualSituation.add("14");
		processingActualSituation.add("15");
		processingActualSituation.add("17");
		processingActualSituation.add("18");

	}

	@Resource
	MqService mqService;

	public void onMessage(Message message) {
		if (message instanceof TextMessage) {
			TextMessage textMessage = (TextMessage) message;
			try {
				String text = textMessage.getText();
				logger.info("Listener received text : " + text);

				JSONObject json = JSONObject.parseObject(text);
				JSONObject alertPojo = json.getJSONObject("alertPojo");

				String mode = (String) json.get("mode");

				if ("complete".equals(mode)
						&& "2".equals(alertPojo.get("disposeType"))
						&& verifyActualSituation.contains(alertPojo// 新核警单
								.get("actualSituation"))) {

					mqService.verifyInfo1(alertPojo);

				} else if ("complete".equals(mode)
						&& "1".equals(alertPojo.get("disposeType"))
						&& processingActualSituation.contains( // 新处警单
								alertPojo.get("actualSituation"))) {

					mqService.processingInfo1(alertPojo);

				} else if ("add".equals(mode)// 新级别报警
						&& codeTypeIds.contains(alertPojo.get("codeTypeId"))) {
					mqService.levelInfo(alertPojo);
				}

			} catch (JMSException e) {
				e.printStackTrace();
			}

		}
	}
}
