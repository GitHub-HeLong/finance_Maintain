package com.es;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tool.PropertyConfigUtil;

public class ESUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(ESUtils.class);

	private static PropertyConfigUtil propertyConfigUtil = PropertyConfigUtil
			.getInstance("properties/es.properties");
	public static Client client;
	public static TransportClient transportClient;

	synchronized static public void loads() {
		if (client == null || transportClient == null) {
			try {
				String eip = propertyConfigUtil.getValue("elasticsearch.ip");
				String clustername = propertyConfigUtil
						.getValue("elasticsearch.name");
				int port = propertyConfigUtil.getIntValue("elasticsearch.port");

				Settings settings = Settings.settingsBuilder()
						.put("cluster.name", clustername)
						.put("client.transport.ignore_cluster_name", false)
						.put("node.client", true)
						.put("client.transport.sniff", false).build();
				try {
					client = TransportClient
							.builder()
							.settings(settings)
							.build()
							.addTransportAddress(
									new InetSocketTransportAddress(InetAddress
											.getByName(eip), port));
				} catch (UnknownHostException e) {
					LOGGER.error(e.getMessage(), e);
				}
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	static {
		loads();
	}
}
