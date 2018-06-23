package com.server;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.es.EsDao;
import com.mysqlDao.FinanceMysql;
import com.mysqlDao.OperationMysql;

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

	@Resource
	OperationMysql operationMysql;

	@Resource
	FinanceMysql financeMysql;

	@Resource
	EsDao esDao;

	String[] sysCody = { "E123", "E122", "E134", "E131", "E130" };
	String[] errorActualSituation = { "9", "10", "12" };
	static Map<String, String> mapError = new HashMap<String, String>();

	static {
		mapError.put("9", "rg_error");
		mapError.put("10", "hj_error");
		mapError.put("12", "sb_error");
	}

	/**
	 * 检查更新用户试机信息
	 */
	public JSONObject checkoutTryZoneAndAlarm() {
		JSONObject json = new JSONObject();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
		Date date = new Date();
		String month = simpleDateFormat.format(date);

		List<Map<String, Object>> list = financeMysql // 获取所有用户和防区编号
				.queryTyrZoneOrderByMonth();
		for (Map<String, Object> map : list) {

			long zoneSize = esDao.queryTryZone(map.get("userId").toString(),
					month + "-01T00:00:00", map.get("zone").toString(),
					"verify");

			if (zoneSize > 0) {
				LOGGER.info("试机 userId:" + map.get("userId").toString()
						+ "  zoneId:" + map.get("zone").toString()
						+ " zoneSize:" + zoneSize);

				financeMysql.updateDeviceTyrZone(map.get("userId").toString(),
						map.get("zone").toString(), month); // 更新试机信息
			}
		}

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

	/**
	 * 加载真警和误报信息
	 */
	public JSONObject checkIsAlarm(String index, String[] actualSituations,
			String eventType) {
		JSONObject json = new JSONObject();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
		Date date = new Date();
		String month = simpleDateFormat.format(date);

		List<Map<String, Object>> list = financeMysql // 获取当月的所有用户
				.queryUserOrderByMonth(month);

		// 查询处警单processing中用户本月真警事件
		LOGGER.info("获取表单信息  index：{}  ,报警原因 actualSituations：{}", index,
				Arrays.toString(actualSituations));

		for (Map<String, Object> map : list) {
			esDao.queryEventToUpdateEventinof(map.get("userId").toString(),
					month + "-01T00:00:00", actualSituations, index, eventType);
		}

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

}
