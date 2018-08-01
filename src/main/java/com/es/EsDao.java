package com.es;

import org.elasticsearch.action.search.SearchResponse;

public interface EsDao {

	public SearchResponse queryTryAlarm(String index, String eventTimeStart,
			String[] actualSituations) throws Exception;

	// 根据用户编号和月份、设备防区编号，查询设备防区是否试机
	public long queryTryZone(String userId, String eventTimeStart,
			String zoneId, String index);

	/**
	 * 处警单、核警单，查询用户的是否存在试机信息，存在则更新防区表和设备表
	 */
	public void updateTryStatus(String userId, String eventTimeStart,
			String index);

	/**
	 * 查询用户的单据中事件是真警和误报的单据事件,更新到数据库中
	 */
	public void queryEventToUpdateEventinof(String userId,
			String eventTimeStart, String fieldType, String[] actualSituations,
			String index, String eventType);

	/**
	 * 查询用户的单据中事件是真警和误报的单据事件,更新到数据库中,此方法查询昨天的数据
	 */
	public void queryEventToUpdateEventinof(String userId,
			String eventTimeStart, String eventTimeEnd, String fieldType,
			String[] actualSituations, String index, String eventType);

	public SearchResponse queryAlarmType(String eventTimeStart, String sysCode)
			throws Exception;

}
