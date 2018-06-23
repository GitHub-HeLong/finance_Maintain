package com.server;

import java.text.SimpleDateFormat;
import java.util.Date;
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
 * 检查设备布撤防数据
 * 
 * @author ywhl
 *
 */
@Service
public class DeviceBCFService {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(DeviceBCFService.class);

	@Resource
	OperationMysql operationMysql;

	@Resource
	FinanceMysql financeMysql;

	/**
	 * 设备布撤防转态更新，每天晚上22点定时查询一遍
	 */
	public JSONObject updateBCFService() {
		JSONObject json = new JSONObject();

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Date date = new Date();
		String stringDate = simpleDateFormat.format(date);
		String month = stringDate.substring(0, 7);

		String D = stringDate.substring(8, 9).equals("0") ? stringDate
				.substring(9, 10) : stringDate.substring(8, 10);

		List<Map<String, Object>> list = financeMysql
				.queryUserOrderByMonth(month);

		List<Map<String, Object>> result = operationMysql.queryDeviceBCF(list);

		for (Map<String, Object> map : result) {
			financeMysql.updateBCF(month, D, map.get("userId").toString(), map
					.get("devId").toString());
		}

		LOGGER.info("布防用户  size：" + result.size());

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

}
