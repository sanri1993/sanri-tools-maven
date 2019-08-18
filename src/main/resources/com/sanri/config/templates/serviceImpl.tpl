package ${serviceImplPackage};

import java.util.List;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.hhx.gps.model.Page;
import com.hhx.gps.web.exception.BussinessException;
import com.hhx.gps.web.exception.SystemErrorException;
import com.hhx.cloudframework.core.context.config.PropertyConfigurer;
import ${entityPackage}.${entity};
import ${servicePackage}.${entity}Service;
import ${daoPackage}.${entity}Dao;
import ${basePackage}.util.*;
import ${basePackage}.common.*;


/**
 * 
 * 作者:sanri <br/>
 * 时间:${datetime}<br/>
 * 功能: ${entity}Service 实现类<br/>
 */
@Service("${lowEntity}Service")
@Transactional(rollbackFor=Exception.class)
public class ${entity}ServiceImpl implements ${entity}Service {
	private Log logger = LogFactory.getLog(getClass());

	@Autowired
	private ${entity}Dao ${lowEntity}Dao;

	public List<${entity}> find${entity}List(Page page) throws BussinessException,SystemErrorException{
		return ${lowEntity}Dao.find${entity}List(page);
	}

	public ${entity} find${entity}One(long idLong) throws BussinessException,SystemErrorException{
		return ${lowEntity}Dao.find${entity}One(idLong);
	}

	public void insert${entity}(${entity} ${lowEntity}) throws BussinessException,SystemErrorException{
		return ${lowEntity}Dao.insert${entity}(${lowEntity});
	}

	public void delete${entity}(long idLong)  throws BussinessException,SystemErrorException{
		return ${lowEntity}Dao.delete${entity}(idLong);
	}

	public void update${entity}(${entity} ${lowEntity}) throws BussinessException,SystemErrorException{
		return ${lowEntity}Dao.update(${lowEntity});
	}
}