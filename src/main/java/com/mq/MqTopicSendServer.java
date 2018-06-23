package com.mq;

import javax.annotation.Resource;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Service;

@Service
public class MqTopicSendServer {
	@Autowired
	@Qualifier("sendtopic")
	private Destination destination;

	private final static Logger logger = LoggerFactory
			.getLogger(MqTopicSendServer.class);

	@Resource
	private JmsTemplate topicJMSTemplate;

	public void sendMessage(final String message) {
		logger.info("发送广播 message ：{}", message);
		try {
			topicJMSTemplate.send(destination, new MessageCreator() {
				public Message createMessage(Session session)
						throws JMSException {
					return session.createTextMessage(message);
				}
			});
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
}
