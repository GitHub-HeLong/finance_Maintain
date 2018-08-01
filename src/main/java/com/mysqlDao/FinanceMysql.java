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
	 * 获取所有用户的主设备编号
	 */
	public List<Map<String, Object>> queryDevIdOrderByMonth();

	/**
	 * 获取设备防区表中，所有用户和设备防区编号
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

	// 更新用户设备防区试机信息, 试机状态不为1，如果设备试机更新成功，在设备表加1
	public void updateDeviceTyrZone(String userId, String zoneId, String month);

	/**
	 * 更新用户事件信息,更新真警信息：3,6,9；更新误报信息为9，10,11,12,13
	 */
	public void updateEvent(String userId, String month, String D,
			String eventType);

	/**
	 * 更新用户布撤防状态
	 */
	public void updateBCF(String month, String D, String userId,
			String initIsBFTime);

	/**
	 * 查询银行小类信息
	 */
	public List<Map<String, Object>> queryBankSubTypeName();

	/**
	 * 把用户信息写入用户信息表中
	 */
	public void insertUserInfo(Map<String, Object> userInfo);

	/**
	 * 把用户信息写入布撤防状态表
	 */
	public void insertUserInfoIsBFTable(Map<String, Object> userInfo,
			String month);

	/**
	 * 初始化风险排行陈损设备
	 */
	public void rankingInfoOldDateTotal(String companyId);

	/**
	 * 查询所有地市信息
	 */
	public List<Map<String, Object>> queryCtil();

	/**
	 * 风险排行，查询某地市未布防总数
	 */
	public int queryNoBFbyCtilId(String ctilId, String month, int Ds);

	/**
	 * 风险排行，更新某地市的真警总数
	 */
	public void updateIsAlarm(String ctilId, String month);

	/**
	 * 更新陈损设备为已过期
	 */
	public void updateOldDate(String devId);

	/**
	 * 实时更新风险排行的真警数
	 */
	public void isAlarmAdd(String companyId);

	/**
	 * 根据设备编号查询所属平台编号
	 */
	public String getCompanyId(String devId);

	/**
	 * 查询设备安装类型
	 */
	public int queryDevInstallType(String devId);

	/**
	 * 更新设备安装类型
	 */
	public void updateDevInstallType(String type, String devId);

	/**
	 * 删除用户表信息
	 */
	public int dellUser(String userId);

	/**
	 * 删除事件表信息
	 */
	public int dellEvent(String userId);

	/**
	 * 删除布撤防信息
	 */
	public int dellIsBF(String userId);

	/**
	 * 删除设备信息
	 */
	public int dellDevInfo(String userId);

	/**
	 * 删除防区信息
	 */
	public int dellZoneInfo(String userId);

	/**
	 * 查询用户信息,可以根据用户编号或者设备编号查询
	 */
	public List<Map<String, Object>> gitUser(String name, String id);

	/**
	 * 更新用户基本信息表
	 */
	public void updateUser(Map<String, Object> map);

	/**
	 * 更新用户大类小类
	 */
	public void updateBankType(String userId, String bankType,
			String bankSubType);

	/**
	 * 获取设备信息
	 */
	public List<Map<String, Object>> gitDevice(String devId);

	/**
	 * 插入信息到防区表
	 */
	public void insertZone(String companyId, String userId, String devId,
			String zone, String devModelId, String protect, String bankType);

	/**
	 * 删除防区
	 */
	public void dellZone(String devId, String zone);

	/**
	 * 更新防区类型
	 */
	public void updateZone(String devId, String zone, String protect,
			String snType);

	/**
	 * 查询某个用户在布撤防中是否有记录
	 */
	public int queryIsBfNum(String userId);

	/**
	 * 每个月清空排行表
	 */
	public void cleanRankingInfo();

	/**
	 * 查询含义主设备的用户表信息
	 */
	public List<Map<String, Object>> queryUserHaveDevice();

	/**
	 * 更新设备经纬度
	 */
	public void updateDevLngDevLat(String devId, String devLng, String devLat);

	/**
	 * 更新用户编号
	 */
	public void ukpdateUserId(String oldUserId, String newUserId);

	/**
	 * 清空某天的事件信息,需要把事件总数相对应的减少
	 */
	public void cleanEventDate(String month, String D);

}
