package sanri.utils;

import org.apache.commons.lang3.math.NumberUtils;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * 创建时间:2017-9-1下午10:11:51<br/>
 * 创建者:sanri<br/>
 * 功能:扩展 org.apache.commons.lang3.math.NumberUtils 增加四舍五入,算百分比等功能<br/>
 */
public class NumberUtil extends NumberUtils{
	
	// 地球半径(单位:千米)
	private static final double EARTH_RADIUS = 6378.137;
	/**
	 * 功能:将值四舍五入<br/>
	 * 创建时间:2017-9-1下午9:59:21<br/>
	 * 作者：sanri<br/>
	 * @param value 	原来的值
	 * @param precision 保留位数
	 * @return<br/>
	 * 
	 * 四舍五入保留位数还有其它的方法,目前先采用这个,四舍五入,保留两位小数可用的方法有 <br/>
	 * String.format("%.2f", d)
	 * Math.rint(num * 100)/100  Math.rint 返回 double Math.round 返回 long
	 * new BigDecimal(num).setScale(2,RoundingMode.HALF_EVEN).doubleValue()
	 * new DecimalFormat("#.00").format(num)
	 */
	public static String round(double value, int precision) {
		NumberFormat numberFormat = NumberFormat.getNumberInstance();
		numberFormat.setMaximumFractionDigits(precision);
		return numberFormat.format(value);
	}

	/**
	 * 
	 * 功能:百分比计算<br/>
	 * 创建时间:2017-9-1下午10:31:00<br/>
	 * 作者：sanri<br/>
	 * @param numerator 		分子
	 * @param denominator		分母
	 * @param precision 		保留的位数
	 * @return<br/>
	 */
	public static double percent(double numerator,double denominator,int precision){
		String round = round((numerator / denominator) * 100, precision);
		return toDouble(round);
	}
	
	/**
	 * 
	 * 功能:计算弧度值<br/>
	 * 创建时间:2017-9-1下午10:59:26<br/>
	 * 作者：sanri<br/>
	 * @param d
	 * @return<br/>
	 */
    private static double rad(double d) {  
        return d * Math.PI / 180.0;  
    }
    
    /**
     * 根据两个位置的经纬度，来计算两地的距离（单位为KM）
     * 参数为String类型
     * @param lat1 用户经度
     * @param lng1 用户纬度
     * @param lat2 商家经度
     * @param lng2 商家纬度
     * @return
     */
    public static String distance(double lat1, double lng1, double lat2, double lng2) {
    	double patm = 2;
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double difference = radLat1 - radLat2;
        double mdifference = rad(lng1) - rad(lng2);
        double distance = patm * Math.asin(Math.sqrt(Math.pow(Math.sin(difference / patm), patm)
                + Math.cos(radLat1) * Math.cos(radLat2)
                * Math.pow(Math.sin(mdifference / patm), patm)));
        distance = distance * EARTH_RADIUS;
        String distanceStr = String.valueOf(distance);
        return distanceStr;
    }
    
    /**
	 * 获取当前用户一定距离以内的经纬度值
	 * 单位米 return minLat 
	 * 最小经度 minLng 
	 * 最小纬度 maxLat 
	 * 最大经度 maxLng 
	 * 最大纬度 minLat
	 */
	public static Map<String,String> around(String latStr, String lngStr, String raidus) {
		Map<String,String> map = new HashMap<String,String>();
		
		Double latitude = Double.parseDouble(latStr);// 传值给经度
		Double longitude = Double.parseDouble(lngStr);// 传值给纬度

		Double degree = (24901 * 1609) / 360.0; // 获取每度
		double raidusMile = Double.parseDouble(raidus);
		
		Double mpdLng = Double.parseDouble((degree * Math.cos(latitude * (Math.PI / 180))+"").replace("-", ""));
		Double dpmLng = 1 / mpdLng;
		Double radiusLng = dpmLng * raidusMile;
		//获取最小经度
		Double minLat = longitude - radiusLng;
		// 获取最大经度
		Double maxLat = longitude + radiusLng;
		
		Double dpmLat = 1 / degree;
		Double radiusLat = dpmLat * raidusMile;
		// 获取最小纬度
		Double minLng = latitude - radiusLat;
		// 获取最大纬度
		Double maxLng = latitude + radiusLat;
		
		map.put("minLat", minLat+"");
		map.put("maxLat", maxLat+"");
		map.put("minLng", minLng+"");
		map.put("maxLng", maxLng+"");
		
		return map;
	}
	
}
