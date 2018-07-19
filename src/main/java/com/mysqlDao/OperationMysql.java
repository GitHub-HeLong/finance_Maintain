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
	 */
	public List<Map<String, Object>> queryFinance(Map<String, Object> bankSub);

	/**
	 * 根据用户编号查询用户信息（更新信息功能）
	 */
	public Map<String, Object> queryFinanceByUserId(String userId,
			Map<String, Object> bankSub);

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
	public int queryDeviceBCF(Map<String, Object> map);

	/**
	 * 查询设备是否过期
	 */
	public String queryDevInsDate(String devId);

	/**
	 * 查询管理平台中，修改用户、设备、防区信息的记录
	 */
	public List<Map<String, Object>> queryInfo();

	/**
	 * 删除已处理的记录表中的信息
	 */
	public void dellFncLogInfo(String id);

}
