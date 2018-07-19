package com.mysqlDao.mysqlImpl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.mysqlDao.FinanceMysql;
import com.tool.AgainTool;

//操作可视化数据库
@Repository
public class FinanceMysqlImp implements FinanceMysql {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(FinanceMysqlImp.class);

	@Resource(name = "financeJdbcTemplateTow")
	private JdbcTemplate financeJdbcTemplate;

	/**
	 * 批量插入每月用户信息到事件表
	 */
	public void insertFinace(final Map<String, Object> map,
			final List<String> type, final String month) {

		String sql = "insert into event_info (companyId,bankType,bankSubType,userId,month,eventType) values(?,?,?,?,?,?)";
		financeJdbcTemplate.batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {
						ps.setString(1, (String) map.get("platformId"));
						ps.setString(2, map.get("bankType") + "");
						ps.setString(3, map.get("bankSubType") + "");
						ps.setString(4, (String) map.get("userId"));
						ps.setString(5, month);
						ps.setString(6, type.get(i));
					}

					public int getBatchSize() {
						return type.size();
					}
				});

	}

	/**
	 * 获取事件表中，某个月份的所有用户编号
	 */
	public List<Map<String, Object>> queryUserOrderByMonth(String month) {
		String sql = "SELECT DISTINCT userId FROM event_info WHERE MONTH = ?";
		List<Map<String, Object>> list = financeJdbcTemplate.queryForList(sql,
				month);
		return list;
	}

	/**
	 * 获取所有用户的主设备编号
	 */
	public List<Map<String, Object>> queryDevIdOrderByMonth() {
		String sql = "SELECT devId,userId,companyId FROM user_info WHERE devId !=''";
		List<Map<String, Object>> list = financeJdbcTemplate.queryForList(sql);
		return list;
	}

	/**
	 * 获取设备防区表中，所有用户和设备防区编号
	 */
	public List<Map<String, Object>> queryTyrZoneOrderByMonth() {
		String sql = "SELECT DISTINCT userId FROM devZone_info";
		List<Map<String, Object>> list = financeJdbcTemplate.queryForList(sql);
		return list;
	}

	/**
	 * 插入设备信息
	 */
	public void insertDeviceZone(final List<Map<String, Object>> list) {
		String sql = "INSERT INTO device_info (companyId,bankType,bankSubType,userId,devId,MONTH,zoneNum,oldDate) VALUES(?,?,?,?,?,?,?,?)";
		financeJdbcTemplate.batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {

						Map<String, Object> map = list.get(i);

						ps.setString(1, (String) map.get("companyId"));
						ps.setString(2, map.get("bankType") + "");
						ps.setString(3, map.get("bankSubType") + "");
						ps.setString(4, (String) map.get("userId"));
						ps.setString(5, (String) map.get("devId"));
						ps.setString(6, (String) map.get("MONTH"));
						ps.setInt(7, (Integer) map.get("zoneNum"));
						ps.setInt(8, (Integer) map.get("oldDate"));
					}

					public int getBatchSize() {
						return list.size();
					}
				});
	}

	/**
	 * 每月更新试机防区表时候清空表中的数据重新加载信息
	 */
	public void cleanTryZone() {
		String sql = "TRUNCATE TABLE try_zone";
		financeJdbcTemplate.execute(sql);
	}

	/**
	 * 插入试机防区信息
	 */
	public void insertDeviceTyrZone(final List<Map<String, Object>> list,
			final List<Map<String, Object>> results) {
		String sql = "INSERT INTO devzone_info (companyId,userId,devId,zone,devModelId,protect,bankType) VALUES(?,?,?,?,?,?,?)";
		financeJdbcTemplate.batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {

						Map<String, Object> result = results.get(i);

						Map<String, Object> map = null;
						for (Map<String, Object> mapChild : list) {
							if (result.get("devId").equals(
									mapChild.get("devId"))) {
								map = mapChild;
							} else {
								continue;
							}
						}

						int devModelId = Integer.parseInt(result.get("snType")
								.toString());

						ps.setString(1, (String) map.get("companyId"));
						ps.setString(2, (String) map.get("userId"));
						ps.setString(3, (String) result.get("devId"));
						ps.setString(4, (String) result.get("devZoneId"));
						ps.setInt(5, devModelId);
						ps.setInt(6, AgainTool.protectType(devModelId));
						ps.setString(7, map.get("bankType") + "");

					}

					public int getBatchSize() {
						return results.size();
					}
				});
	}

	/**
	 * 更新用户设备防区试机信息, 试机状态不为1，如果设备试机更新成功，在设备表加1
	 */
	public void updateDeviceTyrZone(String userId, String zoneId, String month) {

		String sql = "UPDATE devZone_info SET tryStatus=1 WHERE userId =? AND zone=? AND  (tryStatus <> 1 OR tryStatus IS NULL)";
		int i = financeJdbcTemplate.update(sql, userId, zoneId);

		LOGGER.info("试机用户  accountNum :" + userId + "  试机用户防区zoneNum :"
				+ zoneId + "  是否更新试机信息i:" + i);

		// 如果设备试机更新成功，在试机表加1
		if (i == 1) {
			String sqlTryAlarm = "UPDATE device_info SET tryNum=tryNum+1 WHERE month=? AND userId = ? ";
			financeJdbcTemplate.update(sqlTryAlarm, month, userId);
		}
	}

	/**
	 * 更新用户事件信息,更新真警信息：3,6,9；更新误报信息为9，10,11,12,13
	 */
	public void updateEvent(String userId, String month, String D,
			String eventType) {
		String sqlStr = "UPDATE event_info SET D%s=D%s+1,enentTotal=enentTotal+1 WHERE month=? AND userId=? AND eventType = ?";

		String sql = String.format(sqlStr, D, D);

		int i = financeJdbcTemplate.update(sql, month, userId, eventType);

		LOGGER.info("获取事件信息更新到事件表  userId:" + userId + "  eventType:"
				+ eventType);
	}

	/**
	 * 更新用户布撤防状态
	 */
	public void updateBCF(String month, String D, String userId,
			String initIsBFTime) {

		String sqlStr = "UPDATE isbf_info SET D%s=? WHERE MONTH=? AND userId=? AND (D%s!=1||D%s IS NULL)";
		if ("2200".equals(initIsBFTime)) {
			sqlStr = "UPDATE isbf_info SET D%s=?,isBFTotal=isBFTotal+1 WHERE MONTH=? AND userId=? AND (D%s!=1||D%s IS NULL)";
		}

		String sql = String.format(sqlStr, D, D, D);

		int i = financeJdbcTemplate.update(sql, 1, month, userId);

	}

	/**
	 * 查询银行小类信息
	 */
	public List<Map<String, Object>> queryBankSubTypeName() {
		String sql = "SELECT * FROM bank_type WHERE parentId  != '0'";
		List<Map<String, Object>> list = financeJdbcTemplate.queryForList(sql);
		return list;
	}

	public void insertUserInfo(Map<String, Object> userInfo) {
		String sql = "INSERT INTO user_info (companyId,userId,userName,bankType,bankSubType,devId,devLng,devLat) VALUES(?,?,?,?,?,?,?,?)";
		financeJdbcTemplate.update(sql, userInfo.get("platformId").toString(),
				userInfo.get("userId").toString(), userInfo.get("userName")
						.toString(), userInfo.get("bankType").toString(),
				userInfo.get("bankSubType").toString(), userInfo.get("devId")
						.toString(), userInfo.get("devLng").toString(),
				userInfo.get("devlat").toString());
	}

	public void insertUserInfoIsBFTable(Map<String, Object> userInfo,
			String month) {
		String sql = "INSERT INTO isbf_info (companyId,bankType,bankSubType,userId,month) VALUES(?,?,?,?,?)";
		financeJdbcTemplate.update(sql, userInfo.get("platformId").toString(),
				userInfo.get("bankType").toString(), userInfo
						.get("bankSubType").toString(), userInfo.get("userId")
						.toString(), month);
	}

	public void rankingInfoOldDateTotal(String companyId) {
		String sql = "UPDATE ranking_info SET oldDateTotal=oldDateTotal+1 WHERE companyId = ?";
		financeJdbcTemplate.update(sql, companyId);
	}

	public List<Map<String, Object>> queryCtil() {
		String sql = "SELECT * FROM city";
		List<Map<String, Object>> result = financeJdbcTemplate
				.queryForList(sql);
		return result;
	}

	public int queryNoBFbyCtilId(String ctilId, String month, int Ds) {
		String isBFNumSql = "SELECT SUM(isBFTotal) FROM isbf_info WHERE companyId=? AND MONTH=?";
		int isBFNum = financeJdbcTemplate
				.queryForInt(isBFNumSql, ctilId, month);

		String allNumSql = "SELECT COUNT(*) FROM isbf_info WHERE companyId=? AND MONTH=?";
		int allNum = financeJdbcTemplate.queryForInt(allNumSql, ctilId, month);

		String sql = "UPDATE ranking_info SET noBFTotal=? WHERE companyId = ? ";
		financeJdbcTemplate.update(sql, allNum * Ds - isBFNum, ctilId);

		return allNum * Ds - isBFNum;
	}

	public void updateIsAlarm(String ctilId, String month) {
		String isAlarmNumSql = "SELECT SUM(enentTotal) FROM event_info WHERE companyId = ? AND MONTH=? AND eventType = 'isAlarm'";
		int isAlarmNum = financeJdbcTemplate.queryForInt(isAlarmNumSql, ctilId,
				month);

		String sql = "UPDATE ranking_info SET isAlarmTotal=? WHERE companyId = ? ";
		financeJdbcTemplate.update(sql, isAlarmNum, ctilId);

	}

	public void updateOldDate(String devId) {
		String sql = "UPDATE device_info SET oldDate=1 WHERE devId = ? AND oldDate=0";
		financeJdbcTemplate.update(sql, devId);
	}

	public void isAlarmAdd(String companyId) {
		String sql = "UPDATE ranking_info SET isAlarmTotal=isAlarmTotal+1 WHERE companyId = ? ";
		financeJdbcTemplate.update(sql, companyId);
	}

	public String getCompanyId(String devId) {
		String sql = "SELECT companyId FROM user_info WHERE devId = '" + devId
				+ "'";
		String caopanyId = financeJdbcTemplate
				.queryForObject(sql, String.class);
		return caopanyId;
	}

	public int queryDevInstallType(String devId) {
		String sqlYG = "SELECT COUNT(*) FROM devzone_info WHERE devId='"
				+ devId + "' AND devModelId='18' ";
		int ygNum = financeJdbcTemplate.queryForInt(sqlYG);

		String sqlZD = "SELECT COUNT(*) FROM devzone_info WHERE devId='"
				+ devId + "' AND devModelId='19' ";
		int zdNum = financeJdbcTemplate.queryForInt(sqlZD);

		int result = 0;

		if (ygNum > 0 && zdNum > 0) {
			result = 2;
		} else if (ygNum > 0 || zdNum > 0) {
			result = 1;
		}

		return result;
	}

	public void updateDevInstallType(String type, String devId) {
		String sql = "UPDATE user_info SET devInstallType=? WHERE devId = ? ";
		financeJdbcTemplate.update(sql, type, devId);
	}

	@Override
	public int dellUser(String userId) {
		String sql = "DELETE FROM user_info WHERE userId=?";
		int i = financeJdbcTemplate.update(sql, userId);
		return i;
	}

	@Override
	public int dellEvent(String userId) {
		String sql = "DELETE FROM event_info WHERE userId=?";
		int i = financeJdbcTemplate.update(sql, userId);
		return i;
	}

	@Override
	public int dellIsBF(String userId) {
		String sql = "DELETE FROM isbf_info WHERE userId=?";
		int i = financeJdbcTemplate.update(sql, userId);
		return i;
	}

	@Override
	public int dellDevInfo(String userId) {
		String sql = "DELETE FROM device_info WHERE userId=?";
		int i = financeJdbcTemplate.update(sql, userId);
		return i;
	}

	@Override
	public int dellZoneInfo(String userId) {
		String sql = "DELETE FROM devzone_info WHERE userId=?";
		int i = financeJdbcTemplate.update(sql, userId);
		return i;
	}

	@Override
	public List<Map<String, Object>> gitUser(String name, String id) {
		String sqlStr = "SELECT companyId platformId,userId,userName,bankType,bankSubType,devId,devLng,devLat,devInstallType FROM user_info WHERE %s=?";
		String sql = String.format(sqlStr, name);
		List<Map<String, Object>> list = financeJdbcTemplate.queryForList(sql,
				id);
		return list;
	}

	@Override
	public void updateUser(Map<String, Object> map) {
		String sql = "UPDATE user_info SET userName =?,devId=?,devLng=?,devLat=? WHERE userId=? ";
		financeJdbcTemplate.update(sql, map.get("userName"), map.get("devId"),
				map.get("devLng"), map.get("devLat"), map.get("userId"));
	}

	@Override
	public void updateBankType(String userId, String bankType,
			String bankSubType) {

		String sqlUser = "UPDATE user_info SET bankType=?,bankSubType=? WHERE userId=? ";
		String sqlDevice = "UPDATE device_info SET bankType=?,bankSubType=? WHERE userId=? ";
		String sqlZone = "UPDATE devzone_info SET bankType=? WHERE userId=? ";
		String sqlEvent = "UPDATE event_info SET bankType=?,bankSubType=? WHERE userId=? ";
		String sqlIsbf = "UPDATE isbf_info SET bankType=?,bankSubType=? WHERE userId=? ";

		financeJdbcTemplate.update(sqlUser, bankType, bankSubType, userId);
		financeJdbcTemplate.update(sqlDevice, bankType, bankSubType, userId);
		financeJdbcTemplate.update(sqlZone, bankType, userId);
		financeJdbcTemplate.update(sqlEvent, bankType, bankSubType, userId);
		financeJdbcTemplate.update(sqlIsbf, bankType, bankSubType, userId);

	}

	@Override
	public List<Map<String, Object>> gitDevice(String devId) {
		String sql = "SELECT * FROM device_info WHERE devId=?";
		List<Map<String, Object>> list = financeJdbcTemplate.queryForList(sql,
				devId);
		return list;
	}

	@Override
	public void insertZone(String companyId, String userId, String devId,
			String zone, String snType, String protect, String bankType) {
		String sqlZone = "INSERT INTO devzone_info (companyId,userId,devId,zone,devModelId,protect,bankType) VALUES(?,?,?,?,?,?,?)";
		int i = financeJdbcTemplate.update(sqlZone, companyId, userId, devId,
				zone, snType, protect, bankType);
		if (i > 0) {
			String sqlDevice = "UPDATE device_info SET zoneNum=zoneNum+1 WHERE devId=?";
			financeJdbcTemplate.update(sqlDevice, devId);
		}

	}

	@Override
	public void dellZone(String devId, String zone) {
		String sql = "DELETE FROM devzone_info WHERE devId=? AND zone=?";
		int i = financeJdbcTemplate.update(sql, devId, zone);
		if (i > 0) {
			String sqlDevice = "UPDATE device_info SET zoneNum=zoneNum-1 WHERE devId=?";
			financeJdbcTemplate.update(sqlDevice, devId);
		}
	}

	@Override
	public void updateZone(String devId, String zone, String protect,
			String snType) {
		String sqlUser = "UPDATE devzone_info SET protect=?,devModelId=? WHERE devId=? AND zone=? ";
		financeJdbcTemplate.update(sqlUser, protect, snType, devId, zone);
	}

	@Override
	public int queryIsBfNum(String userId) {
		String sql = "SELECT COUNT(*) FROM isbf_info WHERE userId='" + userId
				+ "'";
		int i = financeJdbcTemplate.queryForInt(sql);
		return i;
	}
}
