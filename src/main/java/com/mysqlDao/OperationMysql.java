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

	/**
	 * 根据银行小类名称查询符合范围内的用户信息,并且把用户所属的大类和小类写入数据返回
	 * 
	 * @return
	 */
	public List<Map<String, Object>> queryFinance(Map<String, Object> bankSub);

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
