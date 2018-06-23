package com.mysqlDao.mysqlImpl;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.mysqlDao.OperationMysql;

/**
 * 操作河北安防mysql
 * 
 * @author ywhl
 *
 */
@Repository
public class OperationMysqlImp implements OperationMysql {

	@Resource(name = "jdbcTemplate")
	private JdbcTemplate jdbctemplate;

	public List<Map<String, Object>> queryDate() {
		String sql = "SELECT * FROM imm_camera LIMIT 0,10";
		List<Map<String, Object>> list = jdbctemplate.queryForList(sql);
		return list;
	}

	/**
	 * 查询所有金融行业的用户以及主设备和用户本平台id信息 设备只统计报警主机
	 * 
	 * @return
	 */
	public List<Map<String, Object>> queryFinance() {
		String sql = "SELECT a.userId,b.platformId,d.devId,c.devInstDate FROM imm_customerattr a,imm_userinfo b,imm_devinfo c,imm_alarmhostattr d "
				+ "WHERE a.businessId IN (19,42,65) AND a.userId=b.userId AND a.userId=c.ownerId AND c.controlType IN ('master','both') and c.devId=d.devId";
		List<Map<String, Object>> list = jdbctemplate.queryForList(sql);
		return list;
	}

	/**
	 * 查询所有用户主设备的防区
	 */
	public List<Map<String, Object>> queryDeviceZones(
			List<Map<String, Object>> list) {

		StringBuffer devIds = new StringBuffer();
		devIds.append("(");
		for (Map<String, Object> map : list) {
			devIds.append("'" + (String) map.get("devId") + "',");
		}
		if (list.size() == 0) {
			devIds.append("''");
		} else {
			devIds.deleteCharAt(devIds.length() - 1);
		}
		devIds.append(")");

		String sql = "SELECT devId,devZoneId FROM imm_devzone WHERE devId IN "
				+ devIds;
		List<Map<String, Object>> result = jdbctemplate.queryForList(sql);
		return result;
	}

	/**
	 * 查询设备布撤防转态
	 */
	public List<Map<String, Object>> queryDeviceBCF(
			List<Map<String, Object>> list) {

		StringBuffer userIds = new StringBuffer();
		userIds.append("(");
		for (Map<String, Object> map : list) {
			userIds.append("'" + (String) map.get("devId") + "',");
		}
		if (list.size() == 0) {
			userIds.append("''");
		} else {
			userIds.deleteCharAt(userIds.length() - 1);
		}
		userIds.append(")");

		String sql = "SELECT userId,devId,isBF FROM mcs_customer_status WHERE isBF=1 AND userId IN "
				+ userIds;
		List<Map<String, Object>> result = jdbctemplate.queryForList(sql);
		return result;
	}

}
