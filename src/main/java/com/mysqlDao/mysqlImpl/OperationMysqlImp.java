package com.mysqlDao.mysqlImpl;

import java.util.ArrayList;
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

	/**
	 * 根据银行小类名称查询符合范围内的用户信息,并且把用户所属的大类和小类写入数据返回
	 * 
	 * @return
	 */
	public List<Map<String, Object>> queryFinance(Map<String, Object> bankSub) {
		String sql = " SELECT a.userId,a.userName,a.platformId,b.devId,b.devLng,b.devlat "
				+ " FROM imm_userinfo a,imm_devinfo b "
				+ " WHERE a.userId=b.ownerId AND b.controlType IN ('master','both') AND a.userName LIKE '%"
				+ bankSub.get("bankName").toString() + "%'";
		List<Map<String, Object>> list = jdbctemplate.queryForList(sql);

		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

		for (Map<String, Object> map : list) {
			map.put("bankType", bankSub.get("parentId"));
			map.put("bankSubType", bankSub.get("bankId"));
			resultList.add(map);
		}

		return resultList;
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
