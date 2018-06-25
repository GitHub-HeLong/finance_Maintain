package com.mysqlDao;

import java.util.List;
import java.util.Map;

public interface FinanceMysql {

	/**
	 * 批量插入每月用户
	 */
	public void insertFinace(final Map<String, Object> map,
			final List<String> type, final String month);

	/**
	 * 获取事件表中，某个月份的所有用户编号
	 */
	public List<Map<String, Object>> queryUserOrderByMonth(String month);

	/**
	 * 获取试机表中，所有用户和设备防区编号
	 */
	public List<Map<String, Object>> queryTyrZoneOrderByMonth();

	/**
	 * 每月更新试机防区表时候清空表中的数据重新加载信息
	 */
	public void cleanTryZone();

	/**
	 * 插入设备试机信息
	 */
	public void insertDeviceZone(final List<Map<String, Object>> list);

	public void insertDeviceTyrZone(final List<Map<String, Object>> list,
			final List<Map<String, Object>> results);

	public void updateDeviceTyrZone(String userId, String zoneId, String month);

	public void updateEvent(String userId, String month, String D,
			String eventType);

	public void updateBCF(String month, String D, String userId, String devId);

	/**
	 * 查询银行小类信息
	 */
	public List<Map<String, Object>> queryBankSubTypeName();

	/**
	 * 把用户信息写入用户信息表中
	 */
	public void insertUserInfo(Map<String, Object> userInfo);

}
