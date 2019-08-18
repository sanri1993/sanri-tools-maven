package sanri.utils.excel;

import org.apache.commons.lang3.ArrayUtils;
import sanri.utils.excel.annotation.ExcelColumn;
import sanri.utils.excel.exception.ConfigException;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ColumnConfigUtil {
    /**
     * 功能:获取类的列配置<br/>
     * 创建时间:2017-8-12下午10:08:21<br/>
     * 作者：sanri<br/>
     * @param clazz
     * @param readWrite 解析读时为真,解析写时为假
     * @return
     * @throws IntrospectionException
     * @throws ConfigException
     * @throws NoSuchFieldException
     * @throws SecurityException<br/>
     */
    public static List<ColumnConfig> parseColumnConfig(Class<? extends Object> clazz, boolean readWrite) throws IntrospectionException,
            ConfigException, NoSuchFieldException, SecurityException {
        //获取列配置,所有需要导出的类,最后应该都是从 Object 继承
        BeanInfo beanInfo = Introspector.getBeanInfo(clazz, Object.class);
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
        if(propertyDescriptors == null || propertyDescriptors.length == 0 ){
            //必须要有属性配置
            throw new ConfigException("bean 和其父类, 必须至少包含一个属性");
        }
        //获取 bean 上所有的列配置
        List<ColumnConfig> columnConfigs = new ArrayList<ColumnConfig>();
        for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
            Method readMethod = propertyDescriptor.getReadMethod();
            Method writeMethod = propertyDescriptor.getWriteMethod();
            String propertyName = propertyDescriptor.getName();
            Class<?> propertyType = propertyDescriptor.getPropertyType();
            if(!typeSupport(propertyType)){
                throw new ConfigException("不支持的类型:"+propertyType);
            }
            //只导出属性可读的属性,没有 get 方法的属性不进行导出
            if((readMethod != null && readWrite) || (writeMethod != null && !readWrite)){
                //先从属性列上获取配置,如果属性列上没有,就从读方法上获取,并覆盖属性列上的配置
                ColumnConfig columnConfig = new ColumnConfig(propertyName, readMethod, writeMethod);
                columnConfig.setDataType(propertyType);
                Field propertyField = null;
                Class<?> currentClass = clazz;
                while(currentClass != Object.class && propertyField == null){
                    try{
                        propertyField = currentClass.getDeclaredField(propertyName);
                    }catch(NoSuchFieldException e){
                        currentClass = currentClass.getSuperclass();
                    }
                }
                if(propertyField == null){
                    throw new NoSuchFieldException("没有此属性:"+propertyName);
                }
                ExcelColumn excelColumn = propertyField.getAnnotation(ExcelColumn.class);
                if(excelColumn != null){
                    columnConfig.config(excelColumn.value(), excelColumn.width(),excelColumn.charWidth(),excelColumn.pxWidth(), excelColumn.index(), excelColumn.hidden(), excelColumn.pattern(),excelColumn.chineseWidth());
                }
                //使用方法上的配置,覆盖属性上的配置
                ExcelColumn methodExcelColumn = null;
                if(readWrite){
                    //从读方法上覆盖配置
                    methodExcelColumn = readMethod.getAnnotation(ExcelColumn.class);
                }else{
                    //从写方法上覆盖配置
                    methodExcelColumn = writeMethod.getAnnotation(ExcelColumn.class);
                }
                if(methodExcelColumn != null){
                    columnConfig.config(methodExcelColumn.value(), methodExcelColumn.width(),excelColumn.charWidth(),excelColumn.pxWidth(), methodExcelColumn.index(), methodExcelColumn.hidden(), methodExcelColumn.pattern(),excelColumn.chineseWidth());
                }
                //add by sanri at 2017/08/30 只有配置了 ExcelColumn 的才可进行导入导出
                if(excelColumn != null || methodExcelColumn != null){
                    columnConfigs.add(columnConfig);
                }
            }
        }
        //对导出的属性配置进行排序
        Collections.sort(columnConfigs);
        return columnConfigs;
    }

    /**
     *
     * 作者:sanri <br/>
     * 时间:2017-8-12下午2:29:59<br/>
     * 功能: 是否支持给定的类型<br/>
     * @param clazz
     * @return
     */
    private static boolean typeSupport(Class<?> clazz){
        return clazz.isPrimitive() || clazz == String.class
                || clazz == Integer.class || clazz == Short.class
                || clazz == Long.class || clazz == Float.class
                || clazz == Double.class || clazz == Character.class
                || clazz == Boolean.class || clazz == Byte.class
                || clazz == Date.class;
    }

    /**
     * 获取属性描述器映射
     * @param clazz
     * @return
     */
    public static Map<String,PropertyDescriptor> getPropertyMap(Class<? extends Object> clazz){
        Map<String,PropertyDescriptor> propertyDescriptorMap = new HashMap<String,PropertyDescriptor>();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(clazz, Object.class);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            if(ArrayUtils.isNotEmpty(propertyDescriptors)){
                for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                    String name = propertyDescriptor.getName();
                    propertyDescriptorMap.put(name,propertyDescriptor);
                }
            }
        } catch (IntrospectionException e) {
            e.printStackTrace();
        }

        return propertyDescriptorMap;
    }

    /**
     * 获取属性值
     * @param propertyDescriptor
     * @param object
     * @return
     */
    public static Object getPropertyValue(PropertyDescriptor propertyDescriptor,Object object){
        Method readMethod = propertyDescriptor.getReadMethod();
        try {
            Object invoke = readMethod.invoke(object);
            return invoke;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 设置属性值
     * @param propertyDescriptor
     * @param object
     * @param value
     */
    public static void setPropertyValue(PropertyDescriptor propertyDescriptor,Object object,Object value){
        Method writeMethod = propertyDescriptor.getWriteMethod();
        try {
            writeMethod.invoke(object,value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
