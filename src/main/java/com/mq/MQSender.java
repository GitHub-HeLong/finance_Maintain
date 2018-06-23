package com.mq;

import javax.annotation.Resource;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;
import org.springframework.stereotype.Component;

/**
 * 本类提供发送往mq的方法
 * 
 * @author Administrator
 *
 */
@Component
public class MQSender {
	private static final Logger logger = LoggerFactory
			.getLogger(MQSender.class);

	@Resource(name = "jmsTemplate")
	public JmsTemplate jmsTemplate;

	@Resource(name = "sendQueue")
	public Destination destination;

	/**
	 * 使用默认{@link Destination}发送mq消息
	 * 
	 * @param MqMessage
	 */
	public void send(String message) {
		send(message, destination);
	}

	/**
	 * 使用自定义{@link Destination}发送mq消息
	 * 
	 * @param MqMessage
	 * @param destination
	 */
	public void send(final String message, Destination destination) {
		logger.info("向mq:[{}]发送消息:{}", destination, message);
		jmsTemplate.send(destination, new MessageCreator() {
			public Message createMessage(Session session) throws JMSException {
				return session.createTextMessage(message);
			}
		});
	}
}
