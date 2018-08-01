package com.mq;

import javax.annotation.Resource;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.server.MqService;
import com.tool.KeyValue;

@Component(value = "msgListener")
public class MQListenerContainer implements MessageListener {
	private Logger logger = Logger.getLogger(MQListenerContainer.class);

	@Resource
	MqService mqService;

	public void onMessage(Message message) {
		if (message instanceof TextMessage) {
			TextMessage textMessage = (TextMessage) message;
			try {
				String text = textMessage.getText();
				logger.info("MQ text : " + text);

				JSONObject json = JSONObject.parseObject(text);
				JSONObject alertPojo = json.getJSONObject("alertPojo");

				String mode = (String) json.get("mode");

				if ("complete".equals(mode)
						&& "2".equals(alertPojo.get("disposeType"))
						&& KeyValue.verifyActualSituation.contains(alertPojo// 新核警单
								.get("actualSituation"))) {

					mqService.verifyInfo(alertPojo);

				} else if ("complete".equals(mode)
						&& "1".equals(alertPojo.get("disposeType"))
						&& KeyValue.processingActualSituation.contains( // 新处警单
								alertPojo.get("actualSituation"))) {

					mqService.processingInfo(alertPojo);

				} else if ("add".equals(mode)// 新级别报警
						&& KeyValue.levelType.contains(alertPojo
								.get("codeTypeId"))) {
					mqService.levelInfo(alertPojo);
				}
			} catch (JMSException e) {
				e.printStackTrace();
			}
		}
	}
}
