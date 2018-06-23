package com.mq;

import java.util.Arrays;

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

	String[] sysCody = { "E123", "E122", "E134", "E131", "E130" };
	String[] verifyActualSituation = { "1", "4", "5", "8", "9", "10", "12" };
	String[] processingActualSituation = { "3", "6", "7" };

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
						&& Arrays.asList(verifyActualSituation).contains( // 新核警单
								alertPojo.get("actualSituation"))) {

					logger.info(" -- 新核警单 -- ");
					mqService.verifyInfo(alertPojo);

				} else if ("complete".equals(mode)
						&& "1".equals(alertPojo.get("disposeType"))
						&& Arrays.asList(processingActualSituation).contains( // 新处警单
								alertPojo.get("actualSituation"))) {

					logger.info(" -- 新处警单 -- ");
					mqService.processingInfo(alertPojo);

				}

			} catch (JMSException e) {
				e.printStackTrace();
			}

		}
	}
}
