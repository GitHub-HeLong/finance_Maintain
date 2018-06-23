package com.es;

import org.elasticsearch.action.search.SearchResponse;

public interface EsDao {

	public SearchResponse queryTryAlarm(String index, String eventTimeStart,
			String[] actualSituations) throws Exception;

	// 根据用户编号和月份、设备防区编号，查询设备防区是否试机
	public long queryTryZone(String userId, String eventTimeStart,
			String zoneId, String index);

	/**
	 * 查询用户的单据中事件是真警和误报的单据事件,更新到数据库中
	 */
	public void queryEventToUpdateEventinof(String userId,
			String eventTimeStart, String[] actualSituations, String index,
			String eventType);

	public SearchResponse queryAlarmType(String eventTimeStart, String sysCode)
			throws Exception;

}
