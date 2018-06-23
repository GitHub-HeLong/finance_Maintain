package com.server;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.mysqlDao.FinanceMysql;

/**
 * 用于实时监测报警事件和单据事件更新数据信息
 * 
 * @author ywhl
 *
 */
@Service
public class MqService {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(MqService.class);

	@Resource
	FinanceMysql financeMysql;

	static List<String> noAlarmCode = new ArrayList<String>();// 误报

	static List<String> sysCodys = new ArrayList<String>(); // 报警类型

	static {
		noAlarmCode.add("4");
		noAlarmCode.add("5");
		noAlarmCode.add("8");

		sysCodys.add("E123");
		sysCodys.add("E122");
		sysCodys.add("E134");
		sysCodys.add("E131");
		sysCodys.add("E130");
	}

	/**
	 * 更新核警单信息
	 */
	public JSONObject verifyInfo(JSONObject alertPojo) {
		JSONObject json = new JSONObject();

		String accountNum = alertPojo.getString("accountNum");
		String actualSituation = alertPojo.getString("actualSituation");
		String eventTime = alertPojo.getString("eventTime");
		String accountZone = alertPojo.getString("accountZone");

		String D = eventTime.substring(8, 9).equals("0") ? eventTime.substring(
				9, 10) : eventTime.substring(8, 10);

		String zoneCode = "1"; // 用户试机
		String hjErrorCode = "10"; // 环境误报
		String rgErrorCode = "9"; // 人工误报
		String sbErrorCode = "12"; // 设备误报

		try {
			if (zoneCode.equals(actualSituation)) { // 更新试机表

				financeMysql.updateDeviceTyrZone(accountNum, accountZone,
						eventTime.substring(0, 7));

			} else if (hjErrorCode.equals(actualSituation)) { // 更新环境误报
				financeMysql.updateEvent(accountNum, eventTime.substring(0, 7),
						D, "hj_error");

				financeMysql.updateEvent(accountNum, eventTime.substring(0, 7),
						D, "noAlarm");
			} else if (rgErrorCode.equals(actualSituation)) {// 更新人工误报
				financeMysql.updateEvent(accountNum, eventTime.substring(0, 7),
						D, "rg_error");

				financeMysql.updateEvent(accountNum, eventTime.substring(0, 7),
						D, "noAlarm");
			} else if (sbErrorCode.equals(actualSituation)) {// 更新设备误报
				financeMysql.updateEvent(accountNum, eventTime.substring(0, 7),
						D, "sb_error");

				financeMysql.updateEvent(accountNum, eventTime.substring(0, 7),
						D, "noAlarm");
			}

			if (noAlarmCode.contains(actualSituation)) {// 更新误报信息
				financeMysql.updateEvent(accountNum, eventTime.substring(0, 7),
						D, "noAlarm");
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

	/**
	 * 更新处警单信息
	 */
	public JSONObject processingInfo(JSONObject alertPojo) {
		JSONObject json = new JSONObject();

		String accountNum = alertPojo.getString("accountNum");
		String eventTime = alertPojo.getString("eventTime");

		String D = eventTime.substring(8, 9).equals("0") ? eventTime.substring(
				9, 10) : eventTime.substring(8, 10);

		String sysCode = alertPojo.getString("sysCode");

		try {
			financeMysql.updateEvent(accountNum, eventTime.substring(0, 7), D,
					"isAlarm");
			if (sysCodys.contains(sysCode)) {
				financeMysql.updateEvent(accountNum, eventTime.substring(0, 7),
						D, sysCode);
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		json.put("code", 200);
		json.put("msg", "success");
		return json;

	}
}
