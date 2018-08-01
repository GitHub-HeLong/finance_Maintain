package com.server;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import com.tool.AgainTool;

/**
 * 检查设备布撤防数据，加载风险排行数据
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
	@Resource
	SpringMVCService springMVCService;

	/**
	 * 设备布撤防转态更新，每天晚上17.30~22.00定时查询一遍
	 */
	public JSONObject updateBCFService(String initIsBFTime) {
		JSONObject json = new JSONObject();

		String stringDate = AgainTool.dataForm("yyyy-MM-dd");
		String month = stringDate.substring(0, 7);

		String D = Integer.parseInt(stringDate.substring(8, 10)) + "_"
				+ initIsBFTime;

		try {
			// 查询所有用户的主设备
			List<Map<String, Object>> List = financeMysql
					.queryDevIdOrderByMonth();

			for (Map<String, Object> map : List) {

				// 查询布设备撤防状态
				int devStatusMap = operationMysql.queryDeviceBCF(map);
				if (devStatusMap > 0) {
					financeMysql.updateBCF(month, D, map.get("userId")
							.toString(), initIsBFTime);
				}
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

	// 初始化排行的时候布撤防和真警一起初始化，每天定时的时候，只能更新布撤防，真警是MQ实时更新的
	public JSONObject initRanking(boolean startUpUpdateIsAlarm) {
		JSONObject json = new JSONObject();

		String format = AgainTool.dataForm("yyyy-MM-dd");
		String month = format.substring(0, 7);
		int Ds = Integer.parseInt(format.substring(8, 10));

		try {
			List<Map<String, Object>> ctilList = financeMysql.queryCtil();

			for (Map<String, Object> map : ctilList) {
				String ctilId = map.get("id").toString();
				financeMysql.queryNoBFbyCtilId(ctilId, month, Ds);// 更新布撤防

				if (startUpUpdateIsAlarm) {
					financeMysql.updateIsAlarm(ctilId, month); // 更新真警
				}
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

	public JSONObject checkOldDateService() {
		JSONObject json = new JSONObject();

		List<Map<String, Object>> list = financeMysql.queryDevIdOrderByMonth();

		for (Map<String, Object> map : list) {
			String devId = map.get("devId").toString();

			String date = "";
			try {
				date = operationMysql.queryDevInsDate(devId);
			} catch (Exception e) {
				LOGGER.error("查询设备{}安装日期异常", devId);
				continue;
			}

			boolean oldDate = AgainTool.isOldDate(date);

			if (oldDate) {// 如果设备已过期，更新陈损设备为1
				LOGGER.info("设备{}已过期", devId);
				financeMysql.updateOldDate(devId);

				financeMysql.rankingInfoOldDateTotal(map.get("companyId")// 更新风险排行的陈损设备数
						.toString());
			}
		}

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

	public JSONObject updateInfo() {
		JSONObject json = new JSONObject();

		List<Map<String, Object>> list = operationMysql.queryInfo();

		for (Map<String, Object> map : list) {
			String operationType = (String) map.get("operationType");

			switch (operationType) {
			case "addUser":
				// 添加用户
				addUser(map.get("operationId").toString(), map.get("devId")
						.toString(), map.get("remark").toString());
				break;
			case "dellUser":
				// 删除用户
				dellUser(map.get("operationId").toString());
				break;
			case "updateUserDevId":
				// 更新主设备
				updateUserDevId(map.get("operationId").toString(),
						map.get("devId").toString());
				break;
			case "updateUserName":
				// 更新用户名
				updateUserName(map.get("operationId").toString(),
						map.get("remark").toString());
				break;
			case "addZone":
				// 添加主设备防区
				addZone(map.get("operationId").toString(), map.get("devId")
						.toString(), map.get("remark").toString());
				break;
			case "dellZone":
				// 删除主设备防区
				dellZone(map.get("operationId").toString(), map.get("devId")
						.toString());
				break;
			case "updateZone":
				// 更新主设备防区探头类型
				updateZone(map.get("operationId").toString(), map.get("devId")
						.toString(), map.get("remark").toString());
				break;
			case "updateDevice":
				// 更新主设备经纬度
				// 因为数据库前期表设计没想到设备经纬度的修改，后面的方案是operationId保存设备编号，devId保存经度
				// ，remark保存纬度
				updateDevice(map.get("operationId").toString(), map
						.get("devId").toString(), map.get("remark").toString());
				break;
			case "updateUserId":
				// 更新用户编号
				updateUserId(map.get("operationId").toString(), map
						.get("devId").toString());
				break;
			default:
				break;
			}
			operationMysql.dellFncLogInfo(map.get("id").toString());
		}

		json.put("code", 200);
		json.put("msg", "success");
		return json;
	}

	// 添加用户信息
	private void addUser(String userId, String devId, String userName) {

		final List<Map<String, Object>> bankSubTypeList = financeMysql
				.queryBankSubTypeName();// 查询银行小类信息

		Map<String, Object> userInfo = null;

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
		Date date = new Date();
		String dateFormat = simpleDateFormat.format(date);

		for (Map<String, Object> bankSub : bankSubTypeList) {
			String bankSbuName = bankSub.get("bankNameRule").toString();
			if (userName.indexOf(bankSbuName) != -1) {
				LOGGER.info("添加用户");
				userInfo = operationMysql.queryFinanceByUserId(userId, bankSub);
			}
		}

		if (userInfo != null) {
			// 加载用户信息到用户信息表中
			financeMysql.insertUserInfo(userInfo);

			if (!"".equals(userInfo.get("devId").toString())) {
				// 加载用户基本信息到事件表
				financeMysql.insertFinace(userInfo, AgainTool.type, dateFormat);

				List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
				list.add(userInfo);

				springMVCService.insertDeviceZoneService(list); // 加载用户基本信息到设备表、设备防区表
				springMVCService.updateDevInstallType(list, false);// 初始化设备安装类型
				springMVCService.insertDateisBF(list);// 加载用户基本信息到布撤防状态表
			}
		}

	}

	// 删除用户信息
	public void dellUser(String userId) {
		// 删除用户表信息
		int i = financeMysql.dellUser(userId);
		if (i > 0) {
			financeMysql.dellEvent(userId);
			financeMysql.dellIsBF(userId);
			financeMysql.dellDevInfo(userId);
			financeMysql.dellZoneInfo(userId);
			LOGGER.info("删除用户{}", userId);
		}

	}

	// 更新用户主设备编号
	public void updateUserDevId(String userId, String devId) {
		// 查询金融表中用户基本信息
		List<Map<String, Object>> financeUserInfo = financeMysql.gitUser(
				"userId", userId);

		Map<String, Object> userInfo = null;
		Map<String, Object> bankSub = new HashMap<String, Object>();

		if (financeUserInfo.size() > 0) {

			Map<String, Object> bankInfo = financeUserInfo.get(0);

			bankSub.put("parentId", bankInfo.get("bankType"));
			bankSub.put("bankId", bankInfo.get("bankSubType"));

			userInfo = operationMysql.queryFinanceByUserId(userId, bankSub);

			List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
			list.add(userInfo);

			// 更新用户信息（未更新设备安装类型）
			financeMysql.updateUser(userInfo);

			// 先删除设备、防区表中信息，再插入信息
			financeMysql.dellDevInfo(userId);
			financeMysql.dellZoneInfo(userId);

			if (!"".equals(userInfo.get("devId"))) {
				// 更新设备信息表和设备防区表
				springMVCService.insertDeviceZoneService(list);
			}
			// 更新设备安装类型
			springMVCService.updateDevInstallType(list, true);

			// 如果原来的用户没有主设备，那么布撤防表和事件标不会存在记录，现在加了主设备，需要在这两个表中加记录
			int isBfNum = financeMysql.queryIsBfNum(userId);
			if (!"".equals(userInfo.get("devId").toString()) && isBfNum == 0) {

				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
						"yyyy-MM");
				Date date = new Date();
				String dateFormat = simpleDateFormat.format(date);

				// 加载用户基本信息到事件表
				financeMysql.insertFinace(userInfo, AgainTool.type, dateFormat);
				// 加载用户基本信息到布撤防状态表
				springMVCService.insertDateisBF(list);
			}

		}
	}

	// 更新用户名称
	public void updateUserName(String userId, String userName) {
		final List<Map<String, Object>> bankSubTypeList = financeMysql
				.queryBankSubTypeName();// 查询银行小类信息

		for (Map<String, Object> bankSub : bankSubTypeList) {
			String bankSbuName = bankSub.get("bankNameRule").toString();
			if (userName.indexOf(bankSbuName) != -1) {

				// 查询金融表中用户基本信息
				List<Map<String, Object>> financeUserInfo = financeMysql
						.gitUser("userId", userId);

				if (financeUserInfo.size() > 0) {
					LOGGER.info("金融表存在用户,修改用户名称");
					financeMysql.updateBankType(userId, bankSub.get("parentId")
							.toString(), bankSub.get("bankId").toString());
				} else {
					LOGGER.info("金融表不存在用户,修改用户名称符合我们的统计范围，需要添加用户");
					addUser(userId, "", userName);
				}
				return;
			}
		}
		LOGGER.info("用户名称不在范围内，删除用户");
		dellUser(userId);
	}

	// 添加设备防区
	public void addZone(String zoneId, String devId, String snType) {
		List<Map<String, Object>> deviceList = financeMysql.gitDevice(devId);
		// 查询金融表中用户基本信息
		List<Map<String, Object>> financeUserInfo = null;
		if (deviceList.size() > 0) {
			financeUserInfo = financeMysql.gitUser("devId", devId);

			Map<String, Object> map = financeUserInfo.get(0);

			int protect = AgainTool.protectType(Integer.parseInt(snType));

			// 插入信息到防区表，并且设备表设备防区数加1
			financeMysql.insertZone(map.get("platformId").toString(),
					map.get("userId").toString(), map.get("devId").toString(),
					zoneId, snType, protect + "", map.get("bankType")
							.toString());
			// 更新设备安装类型
			springMVCService.updateDevInstallType(financeUserInfo, true);
		}
	}

	// 删除防区信息
	public void dellZone(String zoneIds, String devId) {
		List<Map<String, Object>> deviceList = financeMysql.gitDevice(devId);
		// 查询金融表中用户基本信息
		List<Map<String, Object>> financeUserInfo = null;
		if (deviceList.size() > 0) {
			financeUserInfo = financeMysql.gitUser("devId", devId);

			zoneIds = zoneIds.replace("\"", "");
			zoneIds = zoneIds.substring(1, zoneIds.length() - 1);
			String[] zoneIdsList = zoneIds.split(",");

			for (String zoneId : zoneIdsList) {
				// 删除防区，并且设备表中防区数减1
				financeMysql.dellZone(devId, zoneId);
			}

			// 更新设备安装类型
			springMVCService.updateDevInstallType(financeUserInfo, true);
		}
	}

	// 更新防区探头类型
	public void updateZone(String zoneId, String devId, String snType) {
		List<Map<String, Object>> deviceList = financeMysql.gitDevice(devId);
		// 查询金融表中用户基本信息
		List<Map<String, Object>> financeUserInfo = null;
		if (deviceList.size() > 0) {
			financeUserInfo = financeMysql.gitUser("devId", devId);

			// 更新防护类型、探头类型
			int protect = AgainTool.protectType(Integer.parseInt(snType));
			financeMysql.updateZone(devId, zoneId, protect + "", snType);

			// 更新设备安装类型
			springMVCService.updateDevInstallType(financeUserInfo, true);
		}
	}

	// 更新设备经纬度的修改
	public void updateDevice(String devId, String devLng, String devLat) {
		try {
			financeMysql.updateDevLngDevLat(devId, devLng, devLat);
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	// 更新用户编号
	public void updateUserId(String oldUserId, String newUserId) {
		try {
			List<Map<String, Object>> userInfo = financeMysql.gitUser("userId",
					oldUserId);
			if (userInfo.size() > 0) {
				financeMysql.ukpdateUserId(oldUserId, newUserId);
			}
		} catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
		}
	}

	public static void main(String[] args) {
		String s = "0003,0004";

		String[] strs = s.split(",");
		for (String n : strs) {
			System.out.println(n);
		}

	}
}
