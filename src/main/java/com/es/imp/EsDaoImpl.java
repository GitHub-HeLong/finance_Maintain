package com.es.imp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.elasticsearch.action.count.CountRequestBuilder;
import org.elasticsearch.action.count.CountResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import com.es.ESUtils;
import com.es.EsDao;
import com.mysqlDao.FinanceMysql;

@Repository
public class EsDaoImpl implements EsDao {
	private final static Logger LOGGER = LoggerFactory
			.getLogger(EsDaoImpl.class);

	static int LIMIT = 100;

	static List<String> sysCodys = new ArrayList<String>();
	static List<String> errorType = new ArrayList<String>();
	static Map<String, String> mapError = new HashMap<String, String>();

	static {
		sysCodys.add("E123");
		sysCodys.add("E122");
		sysCodys.add("E134");
		sysCodys.add("E131");
		sysCodys.add("E130");

		errorType.add("9");
		errorType.add("10");
		errorType.add("12");

		mapError.put("9", "rg_error");
		mapError.put("10", "hj_error");
		mapError.put("12", "sb_error");
	}

	@Resource
	FinanceMysql financeMysql;

	// 根据用户编号和月份、设备防区编号，查询设备防区是否试机
	public long queryTryZone(String userId, String eventTimeStart,
			String zoneId, String index) {

		BoolQueryBuilder boolQuery = new BoolQueryBuilder();

		boolQuery.must(QueryBuilders.termQuery("accountNum", userId));

		boolQuery.must(QueryBuilders.termQuery("zoneNum", zoneId));

		boolQuery.must(QueryBuilders.rangeQuery("eventTime")
				.gte(eventTimeStart));

		boolQuery.must(QueryBuilders.termQuery("actualSituation", "1"));

		long size = countIndex(index, boolQuery);

		return size;

	}

	/**
	 * 返回信息的总条数
	 */
	@SuppressWarnings("deprecation")
	public long countIndex(String index, BoolQueryBuilder boolQuery) {

		CountRequestBuilder countRequestBuilder = ESUtils.client.prepareCount(
				index).setQuery(boolQuery);

		CountResponse countResponse = countRequestBuilder.execute().actionGet();

		return countResponse.getCount();

	}

	/**
	 * 查询核警单中，用户试机
	 */

	// 根据月份,报警原因类型,去EX表中查询处警单、核警单的试机，真警，误报事件
	public SearchResponse queryTryAlarm(String index, String eventTimeStart,
			String[] actualSituations) throws Exception {

		try {
			BoolQueryBuilder boolQuery = new BoolQueryBuilder();

			boolQuery.must(QueryBuilders.rangeQuery("eventTime").gte(
					eventTimeStart));

			boolQuery.must(QueryBuilders.termsQuery("actualSituation",
					actualSituations));

			SearchResponse searchResponse = ESUtils.client.prepareSearch(index)
					.setQuery(boolQuery).execute().actionGet();

			return searchResponse;
		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * 查询用户的单据中事件是真警和误报的单据事件,更新到数据库中
	 */
	public void queryEventToUpdateEventinof(String userId,
			String eventTimeStart, String[] actualSituations, String index,
			String eventType) {
		BoolQueryBuilder boolQuery = new BoolQueryBuilder();

		boolQuery.must(QueryBuilders.termQuery("accountNum", userId));
		boolQuery.must(QueryBuilders.rangeQuery("eventTime")
				.gte(eventTimeStart));
		boolQuery.must(QueryBuilders.termsQuery("actualSituation",
				actualSituations));

		long total = countIndex(index, boolQuery);

		int pages = (int) Math.ceil(total * 1.0 / LIMIT);

		for (int i = 1; i <= pages; i++) {
			SearchResponse searchResponse = ESUtils.client.prepareSearch(index)
					.setQuery(boolQuery).setFrom((i - 1) * LIMIT)
					.setSize(LIMIT).execute().actionGet();

			SearchHits hits = searchResponse.getHits();
			SearchHit[] searchHits = hits.getHits();

			for (SearchHit hit : searchHits) {
				Map<String, Object> map = hit.sourceAsMap();
				String eventTime = map.get("eventTime").toString()
						.substring(0, 10);
				String month = eventTime.substring(0, 7);
				String D = Integer.parseInt(eventTime.substring(8, 10)) + "";
				String actualSituation = map.get("actualSituation").toString();
				String eventNum = map.get("eventNum").toString();
				String sysCode = null;

				LOGGER.info("更新了真警、误报事件  userId: " + userId + "  eventType:"
						+ eventType);

				financeMysql.updateEvent(userId, month, D, eventType);

				if ("isAlarm".equals(eventType)) {
					sysCode = checkSysCode(eventNum);

					if (sysCodys.contains(sysCode)) {
						LOGGER.info("更新了真警子类型  userId: " + userId
								+ "  sysCode:" + sysCode);

						financeMysql.updateEvent(userId, month, D, sysCode);
					}

				} else if ("noAlarm".equals(eventType)) {

					if (errorType.contains(actualSituation)) {
						LOGGER.info("更新了误报子类型  userId: " + userId
								+ "  sysCode:" + mapError.get(actualSituation));

						financeMysql.updateEvent(userId, month, D,
								mapError.get(actualSituation));
					}

				}

			}
		}
	}

	/**
	 * 根据事件编号查询事件索引中的系统码
	 */
	public String checkSysCode(String eventNum) {

		BoolQueryBuilder boolQuery = new BoolQueryBuilder();
		boolQuery.must(QueryBuilders.termQuery("eventNum", eventNum));

		SearchResponse searchResponse = ESUtils.client
				.prepareSearch("alert_processing").setQuery(boolQuery)
				.execute().actionGet();

		SearchHits hits = searchResponse.getHits();
		SearchHit[] searchHits = hits.getHits();

		String sysCode = null;
		for (SearchHit hit : searchHits) {
			Map<String, Object> map = hit.sourceAsMap();
			sysCode = map.get("sysCode").toString();

			if (sysCode != null) {
				break;
			}
		}
		return sysCode;
	}

	// 根据月份，系统码，去EX的真警中查询有声劫盗，无声劫盗，出入防区，周边防区，盗取
	public SearchResponse queryAlarmType(String eventTimeStart, String sysCode)
			throws Exception {
		try {
			BoolQueryBuilder boolQuery = new BoolQueryBuilder();

			boolQuery.must(QueryBuilders.rangeQuery("eventTime").gte(
					eventTimeStart));
			boolQuery.must(QueryBuilders.termsQuery("sysCode", sysCode));

			SearchResponse searchResponse = ESUtils.client
					.prepareSearch("alert_processing").setQuery(boolQuery)
					.execute().actionGet();

			return searchResponse;
		} catch (Exception e) {
			throw e;
		}
	}

}
