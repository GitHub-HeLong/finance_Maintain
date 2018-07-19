package com.tool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeyValue {

	// 真警类型
	public static List<String> isAlarmTypes = new ArrayList<String>();
	public static Map<String, String> mapIsAlarm = new HashMap<String, String>();

	// 误报类型
	public static List<String> errorType = new ArrayList<String>();
	public static Map<String, String> mapError = new HashMap<String, String>();

	// 级别报警
	public static List<String> levelType = new ArrayList<String>();
	public static Map<String, String> mapLevel = new HashMap<String, String>();

	// 一级报警
	public static List<String> oneLevelType = new ArrayList<String>();
	// 二级报警
	public static List<String> towLevelType = new ArrayList<String>();
	// 三级报警
	public static List<String> threeLevelType = new ArrayList<String>();

	// 非真警、非误报的报警类型
	public static List<String> noIsAlarmTypesAndErrorTypes = new ArrayList<String>();

	static {
		isAlarmTypes.add("3");
		isAlarmTypes.add("6");
		isAlarmTypes.add("7");

		mapIsAlarm.put("3", "companyArarm");
		mapIsAlarm.put("6", "commandArarm");
		mapIsAlarm.put("7", "CCAram");

		errorType.add("9");
		errorType.add("10");
		errorType.add("11");
		errorType.add("12");
		errorType.add("13");

		mapError.put("9", "yhError");
		mapError.put("10", "hjError");
		mapError.put("11", "azError");
		mapError.put("12", "sbError");
		mapError.put("13", "wzError");

		levelType.add("0");
		levelType.add("1");
		levelType.add("2");
		levelType.add("3");
		levelType.add("4");
		levelType.add("6");
		levelType.add("10");
		levelType.add("14");

		mapLevel.put("1", "oneLevel");
		mapLevel.put("3", "oneLevel");
		mapLevel.put("4", "oneLevel");

		mapLevel.put("2", "towLevel");

		mapLevel.put("0", "threeLevel");
		mapLevel.put("6", "threeLevel");
		mapLevel.put("10", "threeLevel");
		mapLevel.put("14", "threeLevel");

		oneLevelType.add("1");
		oneLevelType.add("3");
		oneLevelType.add("4");

		towLevelType.add("2");

		threeLevelType.add("0");
		threeLevelType.add("6");
		threeLevelType.add("10");
		threeLevelType.add("14");

		noIsAlarmTypesAndErrorTypes.add("1");
		noIsAlarmTypesAndErrorTypes.add("2");
		noIsAlarmTypesAndErrorTypes.add("4");
		noIsAlarmTypesAndErrorTypes.add("5");
		noIsAlarmTypesAndErrorTypes.add("8");
		noIsAlarmTypesAndErrorTypes.add("14");
		noIsAlarmTypesAndErrorTypes.add("15");
		noIsAlarmTypesAndErrorTypes.add("17");
		noIsAlarmTypesAndErrorTypes.add("18");

	}

	// 非真警、非误报的却要统计的报警信息，返回对应的类型值
	public static String notIsAlarmAndErrorValue(String actualSituation) {
		String value = null;
		switch (Integer.parseInt(actualSituation)) {
		case 1:
			value = "userTest";
			break;
		case 2:
			value = "skillTest";
			break;
		case 4:
			value = "noCompanyArarm";
			break;
		case 5:
			value = "noCommandArarm";
			break;
		case 8:
			value = "noCCAram";
			break;
		case 14:
			value = "routeBad";
			break;
		case 15:
			value = "arrears";
			break;
		case 17:
			value = "unknown";
			break;
		case 18:
			value = "noBF";
			break;
		default:
			break;
		}

		return value;
	}

	// 判断当前时间点，返回数据那个统计的时间
	public static String getHMS(int hms) {
		String value = null;
		if (hms >= 173000 && hms < 174000) {
			value = "1730";
		} else if (hms >= 180000 && hms < 181000) {
			value = "1800";
		} else if (hms >= 183000 && hms < 184000) {
			value = "1830";
		} else if (hms >= 190000 && hms < 191000) {
			value = "1900";
		} else if (hms >= 193000 && hms < 194000) {
			value = "1930";
		} else if (hms >= 200000 && hms < 201000) {
			value = "2000";
		} else if (hms >= 203000 && hms < 204000) {
			value = "2030";
		} else if (hms >= 210000 && hms < 211000) {
			value = "2100";
		} else if (hms >= 213000 && hms < 214000) {
			value = "2130";
		} else if (hms >= 220000 && hms < 221000) {
			value = "2200";
		}
		return value;
	}
}
