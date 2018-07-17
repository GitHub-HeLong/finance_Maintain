package com.server;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import com.es.EsDao;
import com.mysqlDao.FinanceMysql;
import com.mysqlDao.OperationMysql;
import com.tool.PropertyConfigUtil;

/**
 * 用于每月自动获取金融行业用户的事件信息更新到信息表。 基本上查询ex
 * 
 * @author ywhl
 *
 */
@Service
public class EventService {

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

	@Resource
	EsDao esDao;

	/**
	 * 检查更新用户试机信息
	 */
	public JSONObject checkoutTryZoneAndAlarm() {
		JSONObject json = new JSONObject();

		List<Future<?>> futures = new ArrayList<Future<?>>();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
		Date date = new Date();
		final String month = simpleDateFormat.format(date);

		List<Map<String, Object>> list = financeMysql // 获取所有用户和防区编号
				.queryTyrZoneOrderByMonth();
		for (final Map<String, Object> map : list) {
			Future<?> alertProcessingsFuture = INCIDENT_THREADPOOL
					.submit(new Runnable() {
						public void run() {

							esDao.updateTryStatus(map.get("userId").toString(),
									month + "-01T00:00:00", "verify");

							esDao.updateTryStatus(map.get("userId").toString(),
									month + "-01T00:00:00", "processing");

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

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

	/**
	 * 加载真警和误报信息。 index:索引名称 ；fieldType：es查询的字段名称； actualSituations：报警原因编号或者系统码；
	 * eventType:统计的类型
	 */
	public JSONObject checkIsAlarm(final String index, final String fieldType,
			final String[] actualSituations, final String eventType) {
		JSONObject json = new JSONObject();

		List<Future<?>> futures = new ArrayList<Future<?>>();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
		Date date = new Date();
		final String month = simpleDateFormat.format(date);

		List<Map<String, Object>> list = financeMysql // 获取当月的所有用户（有主设备的用户）
				.queryUserOrderByMonth(month);

		LOGGER.info("list size {}", list.size());

		for (final Map<String, Object> map : list) {

			Future<?> alertProcessingsFuture = INCIDENT_THREADPOOL
					.submit(new Runnable() {
						public void run() {

							esDao.queryEventToUpdateEventinof(map.get("userId")
									.toString(), month + "-01T00:00:00",
									fieldType, actualSituations, index,
									eventType);

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

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

}
