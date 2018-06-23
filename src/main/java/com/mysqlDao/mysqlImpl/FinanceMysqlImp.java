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

@Repository
public class FinanceMysqlImp implements FinanceMysql {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(FinanceMysqlImp.class);

	@Resource(name = "financeJdbcTemplateTow")
	private JdbcTemplate financeJdbcTemplate;

	/**
	 * 批量插入每月用户
	 */
	public void insertFinace(final Map<String, Object> map,
			final List<String> type, final String month) {

		String sql = "insert into event_info (companyId,userId,devId,month,eventType) values(?,?,?,?,?)";
		financeJdbcTemplate.batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {
						ps.setString(1, (String) map.get("platformId"));
						ps.setString(2, (String) map.get("userId"));
						ps.setString(3, (String) map.get("devId"));
						ps.setString(4, month);
						ps.setString(5, type.get(i));
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
		String sql = "SELECT DISTINCT userId FROM event_info WHERE MONTH = ? ";
		List<Map<String, Object>> list = financeJdbcTemplate.queryForList(sql,
				month);
		return list;
	}

	/**
	 * 获取试机表中，所有用户和设备防区编号
	 */
	public List<Map<String, Object>> queryTyrZoneOrderByMonth() {
		String sql = "SELECT userId,zone FROM try_zone ";
		List<Map<String, Object>> list = financeJdbcTemplate.queryForList(sql);
		return list;
	}

	/**
	 * 插入设备试机信息
	 */
	public void insertDeviceZone(final List<Map<String, Object>> list) {
		String sql = "INSERT INTO try_alarm (companyId,userId,devId,MONTH,zoneNum,oldDate) VALUES(?,?,?,?,?,?)";
		financeJdbcTemplate.batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {

						Map<String, Object> map = list.get(i);

						ps.setString(1, (String) map.get("companyId"));
						ps.setString(2, (String) map.get("userId"));
						ps.setString(3, (String) map.get("devId"));
						ps.setString(4, (String) map.get("MONTH"));
						ps.setInt(5, (Integer) map.get("zoneNum"));
						ps.setInt(6, (Integer) map.get("oldDate"));
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
		String sql = "INSERT INTO try_zone (companyId,userId,devId,zone) VALUES(?,?,?,?)";
		financeJdbcTemplate.batchUpdate(sql,
				new BatchPreparedStatementSetter() {
					public void setValues(PreparedStatement ps, int i)
							throws SQLException {

						Map<String, Object> result = results.get(i);

						Map<String, Object> map = null;
						for (Map<String, Object> mapChild : list) {
							if (result.get("devId").equals(
									mapChild.get("devId")))
								map = mapChild;
						}

						ps.setString(1, (String) map.get("companyId"));
						ps.setString(2, (String) map.get("userId"));
						ps.setString(3, (String) result.get("devId"));
						ps.setString(4, (String) result.get("devZoneId"));

					}

					public int getBatchSize() {
						return results.size();
					}
				});
	}

	/**
	 * 更新用户设备防区试机信息, 试机状态不为1，如果设备试机更新成功，在试机表加1
	 */
	public void updateDeviceTyrZone(String userId, String zoneId, String month) {

		String sql = "UPDATE try_zone SET tryStatus=1 WHERE userId =? AND zone=? AND  (tryStatus <> 1 OR tryStatus IS NULL)";
		int i = financeJdbcTemplate.update(sql, userId, zoneId);

		LOGGER.info("试机用户  accountNum :" + userId + "  试机用户防区zoneNum :"
				+ zoneId + "  是否更新试机信息i:" + i);

		// 如果设备试机更新成功，在试机表加1
		if (i == 1) {
			String sqlTryAlarm = "UPDATE try_alarm SET tryNum=tryNum+1 WHERE month=? AND userId = ? ";
			financeJdbcTemplate.update(sqlTryAlarm, month, userId);
		}
	}

	/**
	 * 更新用户事件信息,更新真警信息：3,6,9
	 */
	public void updateEvent(String userId, String month, String D,
			String eventType) {

		String sql = "UPDATE event_info SET D"
				+ D
				+ "=D"
				+ D
				+ "+1,total=total+1 WHERE month=? AND userId=? AND eventType = ? ";
		int i = financeJdbcTemplate.update(sql, month, userId, eventType);

		LOGGER.info("获取事件信息更新到事件表  userId:" + userId + "  eventType:"
				+ eventType);
	}

	/**
	 * 更新用户布撤防状态
	 */
	public void updateBCF(String month, String D, String userId, String devId) {
		String sql = "update event_info set D"
				+ D
				+ "='1' WHERE eventType='isBF' AND userId = ? and devId = ? and month=?";

		int i = financeJdbcTemplate.update(sql, userId, devId, month);
	}

}
