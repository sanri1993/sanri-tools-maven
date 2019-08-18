package ${daoImplPackage};

import java.util.List;
import java.util.ArrayList;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;

import org.springframework.stereotype.Repository;

import com.hhx.gps.model.Page;
import com.hhx.gps.web.exception.BussinessException;
import com.hhx.gps.web.exception.SystemErrorException;
import com.hhx.cloudframework.core.context.config.PropertyConfigurer;
import com.hhx.gps.dao.BaseDao;
import ${entityPackage}.${entity};
import ${daoPackage}.${entity}Dao;
import ${basePackage}.util.*;
import ${basePackage}.common.*;


/**
 * 
 * 作者:sanri <br/>
 * 时间:${datetime}<br/>
 * 功能: ${entity}Dao 实现类<br/>
 */
@Repository("${lowEntity}Dao")
public class ${entity}DaoImpl extends BaseDao  implements ${entity}Dao {
	private Log logger = LogFactory.getLog(getClass());

	public List<${entity}> find${entity}List(Page page) throws BussinessException,SystemErrorException{
		//TODO 

		return null;
	}

	public ${entity} find${entity}One(long idLong) throws BussinessException,SystemErrorException{
		//TODO 

		return null;
	}

	public void insert${entity}(${entity} ${lowEntity}) throws BussinessException,SystemErrorException{
		this.saveOrUpdate(${lowEntity});
	}

	public void delete${entity}(long idLong)  throws BussinessException,SystemErrorException{
		${entity} ${lowEntity} = new ${entity};
		${lowEntity}.setId(idLong);
		this.deleteEntity(${lowEntity});
	}

	public void update${entity}(${entity} ${lowEntity}) throws BussinessException,SystemErrorException{
		this.saveOrUpdate(${lowEntity});
	}
}