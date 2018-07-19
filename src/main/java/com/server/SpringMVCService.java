package com.server;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.mysqlDao.FinanceMysql;
import com.mysqlDao.OperationMysql;
import com.tool.AgainTool;
import com.tool.PropertyConfigUtil;

/**
 * 用于每月自动获取金融行业用户信息更新到信息表， 基本上查询mysql
 * 
 * @author ywhl
 *
 */
@Service
public class SpringMVCService {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(SpringMVCService.class);

	private static final PropertyConfigUtil propertyconfigUtil = PropertyConfigUtil
			.getInstance("properties/config.properties");

	// 线程池大小
	private static final int MATERIAL_THREADPLOOL_SIZE = propertyconfigUtil
			.getIntValue("threadPool.size");

	// 线程池
	private static final ExecutorService INCIDENT_THREADPOOL = Executors
			.newFixedThreadPool(MATERIAL_THREADPLOOL_SIZE);

	@Resource
	OperationMysql operationMysql;

	@Resource
	FinanceMysql financeMysql;

	/**
	 * 此方法只在每个月初调用一次 1.查询所有金融行业用户 2.插入数据到事件表
	 * 
	 * @return
	 */
	public JSONObject queryFinanceService() {
		JSONObject json = new JSONObject();

		List<Future<?>> futures = new ArrayList<Future<?>>();

		final List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		final List<Map<String, Object>> removeUserlist = new ArrayList<Map<String, Object>>(); // 用来保存没有主设备的用户

		try {

			final List<Map<String, Object>> bankSubTypeList = financeMysql
					.queryBankSubTypeName();// 查询银行小类信息

			for (final Map<String, Object> bankSub : bankSubTypeList) {

				Future<?> alertProcessingsFuture = INCIDENT_THREADPOOL
						.submit(new Runnable() {
							public void run() {

								List<Map<String, Object>> bankSunList = operationMysql
										.queryFinance(bankSub);
								for (final Map<String, Object> userInfo : bankSunList) {

									// 加载用户信息到用户信息表中
									financeMysql.insertUserInfo(userInfo);

									if ("".equals(userInfo.get("devId")
											.toString())) {

										LOGGER.info("用户{}没有主设备",
												userInfo.get("userId")
														.toString());
										removeUserlist.add(userInfo);
									}
								}
								list.addAll(bankSunList);
							}
						});
				futures.add(alertProcessingsFuture);
			}

			for (Future<?> future : futures) {
				try {
					future.get();
				} catch (Exception e) {
					LOGGER.debug(e.getMessage(), e);
				}
			}

			LOGGER.info("结束加载用户信息到用户信息表中！");
			list.removeAll(removeUserlist); // 移除没有主设备的用户

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		// SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
		// Date date = new Date();
		// final String dateFormat = simpleDateFormat.format(date);
		//
		// for (final Map<String, Object> map : list) {
		//
		// Future<?> alertProcessingsFuture = INCIDENT_THREADPOOL
		// .submit(new Runnable() {
		// public void run() {
		//
		// try {
		// financeMysql.insertFinace(map, AgainTool.type,
		// dateFormat);// 加载用户基本信息到事件表
		// } catch (Exception e) {
		// LOGGER.error(e.getMessage(), e);
		// }
		//
		// }
		// });
		// futures.add(alertProcessingsFuture);
		// }
		//
		// for (Future<?> future : futures) {
		// try {
		// future.get();
		// } catch (Exception e) {
		// LOGGER.debug(e.getMessage(), e);
		// }
		// }
		//
		// LOGGER.info("结束加载用户基本信息到事件表！");

		insertEventService(list);// 插入基本数据到事件表

		insertDeviceZoneService(list); // 加载用户基本信息到设备表、设备防区表

		updateDevInstallType(list, false);// 初始化设备安装类型

		insertDateisBF(list);// 加载用户基本信息到布撤防状态表

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

	// 插入基本数据到事件表
	public JSONObject insertEventService(final List<Map<String, Object>> list) {
		JSONObject json = new JSONObject();

		List<Future<?>> futures = new ArrayList<Future<?>>();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
		Date date = new Date();
		final String dateFormat = simpleDateFormat.format(date);

		for (final Map<String, Object> map : list) {

			Future<?> alertProcessingsFuture = INCIDENT_THREADPOOL
					.submit(new Runnable() {
						public void run() {

							try {
								financeMysql.insertFinace(map, AgainTool.type,
										dateFormat);// 加载用户基本信息到事件表
							} catch (Exception e) {
								LOGGER.error(e.getMessage(), e);
							}

						}
					});
			futures.add(alertProcessingsFuture);
		}

		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (Exception e) {
				LOGGER.debug(e.getMessage(), e);
			}
		}

		LOGGER.info("结束加载用户基本信息到事件表！");

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

	/**
	 * 每个月初调用一次 ，添加新用户主设备调用，获取金融行业的设备防区
	 */
	public JSONObject insertDeviceZoneService(List<Map<String, Object>> list) {
		JSONObject json = new JSONObject();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
		Date date = new Date();
		String dateFormat = simpleDateFormat.format(date);

		List<Map<String, Object>> results = null;
		try {
			results = operationMysql.queryDeviceZones(list);// 获取到设备编号、设备防区、防区类型
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		Map<String, Integer> map = new HashMap<String, Integer>(); // 计算出每个设备有多少个防区
		for (Map<String, Object> mapChild : results) {
			if (map.containsKey(mapChild.get("devId"))) {
				map.put(mapChild.get("devId") + "",
						1 + map.get(mapChild.get("devId")));
			} else {
				map.put(mapChild.get("devId") + "", 1);
			}
		}

		List<Map<String, Object>> tryAlarm = new ArrayList<Map<String, Object>>();
		for (Map<String, Object> mapDevinfo : list) {
			Map<String, Object> mapTry = new HashMap<String, Object>();

			String companyId = mapDevinfo.get("platformId").toString();

			mapTry.put("companyId", companyId);
			mapTry.put("bankType", mapDevinfo.get("bankType"));
			mapTry.put("bankSubType", mapDevinfo.get("bankSubType"));
			mapTry.put("userId", mapDevinfo.get("userId"));
			mapTry.put("devId", mapDevinfo.get("devId"));
			mapTry.put("MONTH", dateFormat);
			mapTry.put("zoneNum", map.get(mapDevinfo.get("devId")) == null ? 0
					: map.get(mapDevinfo.get("devId")));

			boolean oldDate = AgainTool.isOldDate((String) mapDevinfo
					.get("devInstDate"));

			mapTry.put("oldDate", oldDate ? 1 : 0);

			// 初始化风险排行陈损设备信息
			if (oldDate) {
				financeMysql.rankingInfoOldDateTotal(companyId);
			}

			tryAlarm.add(mapTry);
		}

		try {
			financeMysql.insertDeviceZone(tryAlarm); // 插入数据到设备表
			LOGGER.info("结束加载用户基本信息到设备表！");

			// financeMysql.cleanTryZone(); // 每个月清空试机防区表数据，或者每次加载数据的时候清空表数据

			financeMysql.insertDeviceTyrZone(tryAlarm, results); // 插入数据到设备防区表
			LOGGER.info("结束加载用户基本信息到设备防区表！");

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

	/**
	 * 加载用户基本信息到布撤防状态表
	 */
	public JSONObject insertDateisBF(List<Map<String, Object>> list) {
		JSONObject json = new JSONObject();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
		Date date = new Date();
		String month = simpleDateFormat.format(date);

		for (Map<String, Object> userInfo : list) {
			financeMysql.insertUserInfoIsBFTable(userInfo, month);
		}
		LOGGER.info("结束加载用户基本信息到布撤防状态表！");

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

	/**
	 * 设备安装类型,初始化、用户换主设备的时候更新设备安装类型
	 */
	public JSONObject updateDevInstallType(List<Map<String, Object>> list,
			boolean updateUserDevId) {
		JSONObject json = new JSONObject();

		for (Map<String, Object> map : list) {

			String devId = map.get("devId").toString();

			int result = financeMysql.queryDevInstallType(devId);

			if (result == 2) {
				financeMysql.updateDevInstallType("2", devId);
			} else if (result == 1) {
				financeMysql.updateDevInstallType("1", devId);
			}

			// 这个判断在初始化的时候，安装类型默认是0，不需要更新，但是在更换主设备的时候，原来的设备类型不一定是0，所以需要更新
			if (updateUserDevId && result == 0) {
				financeMysql.updateDevInstallType("0", devId);
			}

		}

		json.put("code", 200);
		json.put("msg", "success");
		return json;

	}

}
