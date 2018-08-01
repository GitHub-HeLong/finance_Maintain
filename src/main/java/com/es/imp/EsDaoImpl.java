package com.es.imp;

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
import com.tool.KeyValue;

@Repository
public class EsDaoImpl implements EsDao {
	private final static Logger LOGGER = LoggerFactory
			.getLogger(EsDaoImpl.class);

	static int LIMIT = 100;

	// // 真警类型
	// static List<String> isAlarmTypes = new ArrayList<String>();
	// static Map<String, String> mapIsAlarm = new HashMap<String, String>();
	//
	// // 误报类型
	// static List<String> errorType = new ArrayList<String>();
	// static Map<String, String> mapError = new HashMap<String, String>();
	//
	// // 级别报警
	// static List<String> levelType = new ArrayList<String>();
	// static Map<String, String> mapLevel = new HashMap<String, String>();

	@Resource
	FinanceMysql financeMysql;

	// 根据用户编号和月份、设备防区编号，查询设备防区是否试机
	// ，由于防区编号已经不存在单据中，所以换了一个方法（updateTryStatus），此方法目前不用
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
	 * 处警单、核警单，查询用户的是否存在试机信息，存在则更新防区表和设备表
	 */
	public void updateTryStatus(String userId, String eventTimeStart,
			String index) {

		BoolQueryBuilder boolQuery = new BoolQueryBuilder();
		boolQuery.must(QueryBuilders.termQuery("accountNum", userId));
		boolQuery.must(QueryBuilders.rangeQuery("eventTime")
				.gte(eventTimeStart));
		boolQuery.must(QueryBuilders.termQuery("actualSituation", "1"));

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
				LOGGER.info("查询试机信息：{}", map.toString());

				String eventTime = null;
				String month = null;
				String eventNum = null;
				try {
					eventTime = map.get("eventTime").toString()
							.substring(0, 10);
					month = eventTime.substring(0, 7);
					// 单据中的事件编号
					eventNum = map.get("eventNum").toString();
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
					continue;
				}

				// 获取设备防区编号
				String zoneId = checkDevZoneId(eventNum);
				try {
					// 更新试机信息
					financeMysql.updateDeviceTyrZone(userId, zoneId, month);
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}
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
	// --》应该是原来的逻辑查询单据中所有的用户，但是每次返回默认只有10条，给了逻辑用下面的函数分页，所以这个方法没有使用
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
	 * 查询用户的单据中事件是真警和误报的单据事件,更新到数据库中,此方法查询本月的数据
	 */
	public void queryEventToUpdateEventinof(String userId,
			String eventTimeStart, String fieldType, String[] actualSituations,
			String index, String eventType) {
		BoolQueryBuilder boolQuery = new BoolQueryBuilder();

		boolQuery.must(QueryBuilders.termQuery("accountNum", userId));
		boolQuery.must(QueryBuilders.rangeQuery("eventTime")
				.gte(eventTimeStart));
		boolQuery.must(QueryBuilders.termsQuery(fieldType, actualSituations));

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
				LOGGER.info("事件信息：{}", map.toString());

				String eventTime = map.get("eventTime").toString()
						.substring(0, 10);
				String month = eventTime.substring(0, 7);
				String D = Integer.parseInt(eventTime.substring(8, 10)) + "";

				String actualSituation = null;
				try {
					actualSituation = map.get("actualSituation").toString();// 处警单、核警单用到
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
					continue;
				}

				String codeTypeId = null;// 级别报警中用到

				if ("noIsAlarmAndError".equals(eventType)) {// 判断为非真警、非误报需要统计的报警类型
					eventType = KeyValue
							.notIsAlarmAndErrorValue(actualSituation);
				} else if ("level".equals(eventType)) {// 判断为级别信息
					try {
						codeTypeId = map.get("codeTypeId").toString();
					} catch (Exception e) {
						LOGGER.error(e.getMessage(), e);
						continue;
					}
					eventType = KeyValue.mapLevel.get(codeTypeId);
				}

				try {
					financeMysql.updateEvent(userId, month, D, eventType);

					if ("isAlarm".equals(eventType)) {
						if (KeyValue.isAlarmTypes.contains(actualSituation)) {
							financeMysql.updateEvent(userId, month, D,
									KeyValue.mapIsAlarm.get(actualSituation));
						}
					} else if ("noAlarm".equals(eventType)) {
						if (KeyValue.errorType.contains(actualSituation)) {
							financeMysql.updateEvent(userId, month, D,
									KeyValue.mapError.get(actualSituation));
						}
					}
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * 查询用户的单据中事件是真警和误报的单据事件,更新到数据库中,此方法查询昨天的数据
	 */
	public void queryEventToUpdateEventinof(String userId,
			String eventTimeStart, String eventTimeEnd, String fieldType,
			String[] actualSituations, String index, String eventType) {
		BoolQueryBuilder boolQuery = new BoolQueryBuilder();

		boolQuery.must(QueryBuilders.termQuery("accountNum", userId));

		boolQuery.must(QueryBuilders.rangeQuery("eventTime")
				.gte(eventTimeStart));

		boolQuery.must(QueryBuilders.rangeQuery("eventTime").lt(eventTimeEnd));

		boolQuery.must(QueryBuilders.termsQuery(fieldType, actualSituations));

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
				LOGGER.info("事件信息：{}", map.toString());

				String eventTime = map.get("eventTime").toString()
						.substring(0, 10);
				String month = eventTime.substring(0, 7);
				String D = Integer.parseInt(eventTime.substring(8, 10)) + "";

				String actualSituation = null;
				try {
					actualSituation = map.get("actualSituation").toString();// 处警单、核警单用到
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
					continue;
				}

				String codeTypeId = null;// 级别报警中用到

				if ("noIsAlarmAndError".equals(eventType)) {// 判断为非真警、非误报需要统计的报警类型
					eventType = KeyValue
							.notIsAlarmAndErrorValue(actualSituation);
				} else if ("level".equals(eventType)) {// 判断为级别信息
					try {
						codeTypeId = map.get("codeTypeId").toString();
					} catch (Exception e) {
						LOGGER.error(e.getMessage(), e);
						continue;
					}
					eventType = KeyValue.mapLevel.get(codeTypeId);
				}

				try {
					financeMysql.updateEvent(userId, month, D, eventType);

					if ("isAlarm".equals(eventType)) {
						if (KeyValue.isAlarmTypes.contains(actualSituation)) {
							financeMysql.updateEvent(userId, month, D,
									KeyValue.mapIsAlarm.get(actualSituation));
						}
					} else if ("noAlarm".equals(eventType)) {
						if (KeyValue.errorType.contains(actualSituation)) {
							financeMysql.updateEvent(userId, month, D,
									KeyValue.mapError.get(actualSituation));
						}
					}
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}
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
			try {
				devZoneId = map.get("devZoneId").toString();
			} catch (Exception e) {
				LOGGER.error("事件表获取设备防区标号异常  esInfo:{}", map.toString());
				continue;
			}

			if (devZoneId != null) {
				break;
			}
		}
		return devZoneId;
	}

	// 根据月份，系统码，去EX的真警中查询有声劫盗，无声劫盗，出入防区，周边防区，盗取，上版本逻辑，这个方法没有使用
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
