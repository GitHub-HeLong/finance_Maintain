package com.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.es.ESUtils;
import com.mysqlDao.FinanceMysql;
import com.tool.KeyValue;

/**
 * 用于实时监测报警事件和单据事件更新数据信息
 * 
 * @author ywhl
 *
 */
@Service
public class MqService {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(MqService.class);

	@Resource
	FinanceMysql financeMysql;

	static List<String> noAlarmCode = new ArrayList<String>();// 误报

	static List<String> sysCodys = new ArrayList<String>(); // 报警类型

	// 处警单、核警单 误报类型
	// static List<String> errorTypes = new ArrayList<String>();
	// static Map<String, String> mapError = new HashMap<String, String>();

	// 处警单 真警类型
	// static List<String> isAlarmTypes = new ArrayList<String>();

	// 处警单、核警单 报警类型需要统计的非真警、误报类型
	// static List<String> noIsAlarmTypesAndErrorTypes = new
	// ArrayList<String>();

	/**
	 * 更新核警单信息
	 */
	public JSONObject verifyInfo1(JSONObject alertPojo) {
		JSONObject json = new JSONObject();

		String accountNum = alertPojo.getString("accountNum");
		String actualSituation = alertPojo.getString("actualSituation");
		String eventTime = alertPojo.getString("eventTime");

		String D = Integer.parseInt(eventTime.substring(8, 10)) + "";

		String month = eventTime.substring(0, 7);

		try {
			if (KeyValue.errorType.contains(actualSituation)) { // 核警单报警属于需要统计的误报类型

				financeMysql.updateEvent(accountNum, month, D, "noAlarm");

				financeMysql.updateEvent(accountNum, month, D,
						KeyValue.mapError.get(actualSituation));

			} else if (KeyValue.noIsAlarmTypesAndErrorTypes
					.contains(actualSituation)) { // 核警单 报警属于不需要统计的误报类型

				financeMysql.updateEvent(accountNum, month, D,
						KeyValue.notIsAlarmAndErrorValue(actualSituation));

				if ("1".equals(actualSituation)) {

					String devZoneId = checkDevZoneId(alertPojo
							.getString("eventNum"));

					financeMysql.updateDeviceTyrZone(accountNum, devZoneId,
							month); // 更新试机信息
				}

			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

	/**
	 * 更新处警单信息
	 */
	public JSONObject processingInfo1(JSONObject alertPojo) {
		JSONObject json = new JSONObject();

		String accountNum = alertPojo.getString("accountNum");
		String actualSituation = alertPojo.getString("actualSituation");
		String eventTime = alertPojo.getString("eventTime");

		String D = eventTime.substring(8, 9).equals("0") ? eventTime.substring(
				9, 10) : eventTime.substring(8, 10);

		String month = eventTime.substring(0, 7);

		try {
			if (KeyValue.errorType.contains(actualSituation)) { // 处警单报警属于需要统计的误报类型

				financeMysql.updateEvent(accountNum, eventTime.substring(0, 7),
						D, "noAlarm");

				financeMysql.updateEvent(accountNum, eventTime.substring(0, 7),
						D, KeyValue.mapError.get(actualSituation));

			} else if (KeyValue.noIsAlarmTypesAndErrorTypes
					.contains(actualSituation)) { // 处警单 报警属于不需要统计的误报类型

				financeMysql.updateEvent(accountNum, eventTime.substring(0, 7),
						D, KeyValue.notIsAlarmAndErrorValue(actualSituation));

				if ("1".equals(actualSituation)) {

					String devZoneId = checkDevZoneId(alertPojo
							.getString("eventNum"));

					financeMysql.updateDeviceTyrZone(accountNum, devZoneId,
							month); // 更新试机信息
				}

			} else if (KeyValue.isAlarmTypes.contains(actualSituation)) { // 处警单报警属于需要统计的真警类型

				financeMysql.updateEvent(accountNum, eventTime.substring(0, 7),
						D, "isAlarm");

				financeMysql.updateEvent(accountNum, eventTime.substring(0, 7),
						D, KeyValue.mapIsAlarm.get(actualSituation));

				// 风险排行，实时更新真警数
				String companyId = financeMysql.getCompanyId(alertPojo
						.getString("devId"));
				if (!"".equals(companyId) && companyId != null) {
					LOGGER.info("平台{}真警", companyId);
					financeMysql.isAlarmAdd(companyId);
				}

			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		json.put("code", 200);
		json.put("msg", "success");
		return json;

	}

	/**
	 * 级别报警
	 * 
	 * @param alertPojo
	 * @return
	 */
	public JSONObject levelInfo(JSONObject alertPojo) {
		JSONObject json = new JSONObject();

		String accountNum = alertPojo.getString("accountNum");
		String codeTypeId = alertPojo.getString("codeTypeId");
		String eventTime = alertPojo.getString("eventTime");

		String D = eventTime.substring(8, 9).equals("0") ? eventTime.substring(
				9, 10) : eventTime.substring(8, 10);

		if (KeyValue.oneLevelType.contains(codeTypeId)) {
			financeMysql.updateEvent(accountNum, eventTime.substring(0, 7), D,
					"oneLevel");
		} else if (KeyValue.towLevelType.contains(codeTypeId)) {
			financeMysql.updateEvent(accountNum, eventTime.substring(0, 7), D,
					"towLevel");
		} else if (KeyValue.threeLevelType.contains(codeTypeId)) {
			financeMysql.updateEvent(accountNum, eventTime.substring(0, 7), D,
					"threeLevel");
		}

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

	/**
	 * 根据事件编号查询事件索引中的设备防区
	 */
	public String checkDevZoneId(String eventNum) {

		BoolQueryBuilder boolQuery = new BoolQueryBuilder();
		boolQuery.must(QueryBuilders.termQuery("eventNum", eventNum));

		SearchResponse searchResponse = ESUtils.client
				.prepareSearch("alert_processing").setQuery(boolQuery)
				.execute().actionGet();

		SearchHits hits = searchResponse.getHits();
		SearchHit[] searchHits = hits.getHits();

		String devZoneId = null;
		for (SearchHit hit : searchHits) {
			Map<String, Object> map = hit.sourceAsMap();
			devZoneId = map.get("devZoneId").toString();

			if (devZoneId != null) {
				break;
			}
		}
		return devZoneId;
	}

}
