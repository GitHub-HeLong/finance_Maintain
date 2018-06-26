package com.server;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.mysqlDao.FinanceMysql;
import com.mysqlDao.OperationMysql;

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

		List<Map<String, Object>> bankSubTypeList = null;
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		try {

			bankSubTypeList = financeMysql.queryBankSubTypeName();// 查询银行小类信息

			for (Map<String, Object> bankSub : bankSubTypeList) {
				list.addAll(operationMysql.queryFinance(bankSub));
			}

		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		// for (Map<String, Object> userInfo : list) {
		// financeMysql.insertUserInfo(userInfo);
		// }

		// List<String> type = new ArrayList<String>();
		// type.add("oneLevel");
		// type.add("towLevel");
		// type.add("threeLevel");
		// type.add("yhError");
		// type.add("hjError");
		// type.add("azError");
		// type.add("sbError");
		// type.add("wzError");
		// type.add("isAlarm");
		// type.add("noAlarm");
		// type.add("userTest");
		// type.add("skillTest");
		// type.add("companyArarm");
		// type.add("noCompanyArarm");
		// type.add("noCommandArarm");
		// type.add("commandArarm");
		// type.add("noCCAram");
		// type.add("routeBad");
		// type.add("arrears");
		// type.add("unknown");
		// type.add("noBF");
		// type.add("CCAram");
		//
		// SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
		// Date date = new Date();
		// String dateFormat = simpleDateFormat.format(date);
		//
		// for (Map<String, Object> map : list) {
		// try {
		// financeMysql.insertFinace(map, type, dateFormat);
		// } catch (Exception e) {
		// LOGGER.error(e.getMessage(), e);
		// }
		// }

		insertDeviceZoneService(list);

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

	/**
	 * 此方法只在每个月初调用一次 ，获取金融行业的设备防区
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
			mapTry.put("companyId", mapDevinfo.get("platformId"));
			mapTry.put("bankType", mapDevinfo.get("bankType"));
			mapTry.put("bankSubType", mapDevinfo.get("bankSubType"));
			mapTry.put("userId", mapDevinfo.get("userId"));
			mapTry.put("devId", mapDevinfo.get("devId"));
			mapTry.put("MONTH", dateFormat);
			mapTry.put("zoneNum", map.get(mapDevinfo.get("devId")) == null ? 0
					: map.get(mapDevinfo.get("devId")));
			mapTry.put("oldDate",
					isOldDate((String) mapDevinfo.get("devInstDate")) ? 0 : 1);
			tryAlarm.add(mapTry);
		}

		try {
			// financeMysql.insertDeviceZone(tryAlarm); // 插入数据到试机表

			// financeMysql.cleanTryZone(); // 每个月清空试机防区表数据，或者每次加载数据的时候清空表数据
			financeMysql.insertDeviceTyrZone(tryAlarm, results); // 插入数据到试机防区表
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

	/**
	 * 判断设备安装时间是否大于5年 过期返回false,未过期返回true
	 * 
	 * @param args
	 * @throws ParseException
	 */
	public boolean isOldDate(String devInstDate) {

		if (devInstDate == null || "".equals(devInstDate)) {
			return false;
		}

		Date date = new Date();// 取时间
		Calendar calendar = new GregorianCalendar();
		calendar.setTime(date);
		calendar.add(calendar.DATE, -(365 * 5));// 把日期往后增加一天.整数往后推,负数往前移动
		date = calendar.getTime(); // 这个时间就是日期往后推一天的结果

		long fiveYear = date.getTime();

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		long devDate = 0;
		try {
			devDate = sdf.parse(devInstDate.substring(0, 10)).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return fiveYear < devDate ? true : false;
	}

}
