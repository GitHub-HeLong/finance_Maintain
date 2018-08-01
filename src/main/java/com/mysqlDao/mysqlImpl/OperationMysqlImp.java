package com.mysqlDao.mysqlImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger LOGGER = LoggerFactory
			.getLogger(OperationMysqlImp.class);

	@Resource(name = "jdbcTemplate")
	private JdbcTemplate jdbctemplate;

	/**
	 * 根据银行小类名称查询符合范围内的用户信息,并且把用户所属的大类和小类写入数据返回
	 */
	public List<Map<String, Object>> queryFinance(Map<String, Object> bankSub) {
		String sql = " SELECT a.userId,a.userName,IFNULL(a.platformId,'') platformId,IFNULL(b.devId,'') devId,IFNULL(b.devLng,0.00) devLng,IFNULL(b.devlat,0.00) devlat,IFNULL(b.devInstDate,'') devInstDate "
				+ " FROM imm_userinfo a LEFT JOIN imm_devinfo b ON a.userId=b.ownerId AND b.controlType IN ('master','both') AND b.devType='1' "
				+ " WHERE a.userName LIKE '%"
				+ bankSub.get("bankNameRule").toString() + "%'";
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
	 * 根据用户编号查询用户信息（更新信息功能）
	 */
	public Map<String, Object> queryFinanceByUserId(String userId,
			Map<String, Object> bankSub) {
		String sql = " SELECT a.userId,a.userName,IFNULL(a.platformId,'') platformId,IFNULL(b.devId,'') devId,IFNULL(b.devLng,0.00) devLng,IFNULL(b.devlat,0.00) devlat,IFNULL(b.devInstDate,'') devInstDate "
				+ " FROM imm_userinfo a LEFT JOIN imm_devinfo b ON a.userId=b.ownerId AND b.controlType IN ('master','both') AND b.devType='1' "
				+ " WHERE a.userId ='" + userId + "'";
		List<Map<String, Object>> list = jdbctemplate.queryForList(sql);

		List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();

		for (Map<String, Object> map : list) {
			map.put("bankType", bankSub.get("parentId"));
			map.put("bankSubType", bankSub.get("bankId"));
			resultList.add(map);
		}

		return resultList.get(0);
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

		String sql = "SELECT devId,devZoneId,snType FROM imm_devzone WHERE devId IN "
				+ devIds;
		List<Map<String, Object>> result = jdbctemplate.queryForList(sql);
		return result;
	}

	/**
	 * 查询设备布撤防状态
	 */
	public int queryDeviceBCF(Map<String, Object> map) {

		String sql = "SELECT devStatus FROM mcs_devstatus_view WHERE devId = ? AND devStatus > 0";
		List list = jdbctemplate.queryForList(sql, map.get("devId"));

		return list.size();
	}

	// (sql, String.class)
	public String queryDevInsDate(String devId) {
		String sql = "SELECT IFNULL(devInstDate,'') devInstDate FROM imm_devinfo WHERE devId = '"
				+ devId + "'";
		String devInstDate = "";
		try {
			devInstDate = jdbctemplate.queryForObject(sql, String.class);
		} catch (Exception e) {
			LOGGER.error("设备{}没有安装时间", devId);
		}
		return devInstDate;
	}

	public List<Map<String, Object>> queryInfo() {
		String sql = "SELECT id,operationType,operationId,IFNULL(devId,'') devId,IFNULL(remark,'') remark FROM fnc_log";
		List<Map<String, Object>> result = jdbctemplate.queryForList(sql);
		return result;
	}

	@Override
	public void dellFncLogInfo(String id) {
		String sql = "DELETE FROM fnc_log WHERE id=?";
		jdbctemplate.update(sql, id);
	}
}
