package com.tool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.alibaba.fastjson.JSONObject;

/**
 * @author http相关工具整合
 */

public class HttpTool {
	private final static Logger log = LoggerFactory.getLogger(HttpTool.class);

	private static boolean bFirst = true;

	public static void init() {
		if (bFirst) {
			bFirst = false;
		}
	}

	/**
	 * 发送信令（String）到设备服务器
	 * 
	 * @param httpUrl
	 * @param json
	 * @return 设备服务器返回结果
	 */
	public static String post2(String _httpUrl, String json) {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		init();

		String httpUrl = _httpUrl;
		log.info("-------------HttpTool post2 {}  -------------", httpUrl);
		String strRes = null;
		try {
			Map<String, ContentBody> bodyMap = null;
			bodyMap = new HashMap<String, ContentBody>();
			bodyMap.put("json", new StringBody(json,
					ContentType.APPLICATION_JSON));
			String requestUri = httpUrl;
			HttpPost httppost = new HttpPost(requestUri);
			MultipartEntityBuilder entityBuilder = MultipartEntityBuilder
					.create();
			for (Map.Entry<String, ContentBody> entry : bodyMap.entrySet()) {
				String name = entry.getKey();
				ContentBody body = entry.getValue();
				entityBuilder.addPart(name, body);
			}
			HttpEntity reqEntity = entityBuilder.build();
			httppost.setEntity(reqEntity);
			CloseableHttpResponse response = httpclient.execute(httppost);

			try {
				int statusCode = response.getStatusLine().getStatusCode();
				if (statusCode != 200) {
					throw new IOException();
				}
				HttpEntity resEntity = response.getEntity();
				if (resEntity != null) {
					// 打印响应内容
					strRes = EntityUtils.toString(resEntity);
					log.info("-------------HttpTool post2 responsed---------------");
				}
				EntityUtils.consume(resEntity);
			} finally {
				response.close();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		} finally {
			try {
				httpclient.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
				return null;
			}
		}
		return strRes;
	}

	public static String post3(String interfaceName, String strQueryJson)
			throws Exception {
		CloseableHttpClient httpclient = HttpClients.createDefault();
		String requestUrl = interfaceName;
		String resp = new String();
		HttpPost httppost = new HttpPost(requestUrl);
		httppost.setHeader("Accept-Encoding", "gzip, deflate");
		httppost.setHeader("Accept-Language", "zh-CN");
		httppost.setHeader("Accept",
				"application/json, application/xml, text/html, text/*, image/*, */*");
		httppost.setHeader("Content-Type", "application/json; charset=UTF-8");

		HttpEntity sendEntity;
		sendEntity = new StringEntity(strQueryJson, "UTF-8");
		httppost.setEntity(sendEntity);
		CloseableHttpResponse response = httpclient.execute(httppost);
		try {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				resp = EntityUtils.toString(entity);
			}
			EntityUtils.consume(entity);
		} finally {
			response.close();
		}
		return resp;
	}

	public static String readJSONString(HttpServletRequest request) {
		String method = request.getMethod();
		if (method == "GET") {
			String cs = "";
			if (request.getQueryString() != null) {
				try {
					cs = URLDecoder.decode(request.getQueryString(), "utf-8");// 将中文转码
				} catch (UnsupportedEncodingException e) {
					log.error(e.getMessage(), e);
				}
			}
			return cs;
		} else {
			StringBuffer json = new StringBuffer();
			String line = null;
			try {
				BufferedReader reader = request.getReader();
				while ((line = reader.readLine()) != null) {
					json.append(line);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return json.toString();
		}
	}

	public static JSONObject readJSONParam(HttpServletRequest request) {
		String method = request.getMethod();
		if (method == "GET") {
			return JSONObject.parseObject(request.getQueryString());
		} else {
			StringBuffer json = new StringBuffer();
			String line = null;
			try {
				BufferedReader reader = request.getReader();
				while ((line = reader.readLine()) != null) {
					json.append(line);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			return JSONObject.parseObject(json.toString());
		}
	}

	public static ResponseEntity<String> GetResponseEntity(String result) {
		HttpHeaders responseHeaders = new HttpHeaders();
		MediaType mediaType = new MediaType("text", "html",
				Charset.forName("UTF-8"));
		responseHeaders.setContentType(mediaType);
		return new ResponseEntity<String>(result, responseHeaders,
				HttpStatus.OK);
	}

}
