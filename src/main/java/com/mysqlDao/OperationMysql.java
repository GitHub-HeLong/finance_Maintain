package com.mysqlDao;

import java.util.List;
import java.util.Map;

/**
 * 操作河北安防mysql
 * 
 * @author ywhl
 *
 */
public interface OperationMysql {

	public List<Map<String, Object>> queryDate();

	/**
	 * 查询所有金融行业的用户以及主设备等信息
	 * 
	 * @return
	 */
	public List<Map<String, Object>> queryFinance();

	/**
	 * 查询所有用户主设备的防区
	 */
	public List<Map<String, Object>> queryDeviceZones(
			List<Map<String, Object>> list);

	/**
	 * 查询用户布撤防状态
	 * 
	 * @param list
	 * @return
	 */
	public List<Map<String, Object>> queryDeviceBCF(
			List<Map<String, Object>> list);
}
