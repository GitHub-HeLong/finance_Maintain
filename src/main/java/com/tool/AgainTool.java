package com.tool;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class AgainTool {

	// 加载事件表信息
	public final static List<String> type = new ArrayList<String>();
	static {
		type.add("oneLevel");
		type.add("towLevel");
		type.add("threeLevel");
		type.add("yhError");
		type.add("hjError");
		type.add("azError");
		type.add("sbError");
		type.add("wzError");
		type.add("isAlarm");
		type.add("noAlarm");
		type.add("userTest");
		type.add("skillTest");
		type.add("companyArarm");
		type.add("noCompanyArarm");
		type.add("noCommandArarm");
		type.add("commandArarm");
		type.add("noCCAram");
		type.add("routeBad");
		type.add("arrears");
		type.add("unknown");
		type.add("noBF");
		type.add("CCAram");
	}

	/**
	 * 判断设备安装时间是否大于5年 过期返回true,未过期返回false
	 * 
	 * @param args
	 * @throws ParseException
	 */
	public static boolean isOldDate(String devInstDate) {

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
		return fiveYear < devDate ? false : true;
	}

	// 设备防区防护类型对应值
	public static int protectType(int protect) {
		int i = 3;
		if (protect == 16 || protect == 18 || protect == 26) {
			i = 0;
		} else if (protect == 8) {
			i = 1;
		} else if (protect == 6 || protect == 21) {
			i = 2;
		}
		return i;
	}

}
