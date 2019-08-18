package ${controllerPackage};

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.hhx.gps.model.Page;
import com.hhx.gps.model.ResultVo;
import com.hhx.gps.web.exception.BussinessException;
import com.hhx.gps.web.exception.SystemErrorException;
import ${entityPackage}.${entity};
import ${servicePackage}.${entity}Service;
import ${basePackage}.util.*;
import ${basePackage}.common.*;

/**
 * 
 * 作者:sanri <br/>
 * 时间:${datetime}<br/>
 * 功能: controller generate from table @${tableName}<br/>
 */
@Controller
@RequestMapping("/${lowEntity}")
public class ${entity}Controller extends BaseController{
	
	//注入 ${entity}Service
	@Autowired
	private ${entity}Service ${lowEntity}Service;

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:${datetime}<br/>
	 * 功能: 分页查询 ${entity} 数据列表 <br/>
	 * TODO 需要添加查询条件
	 * @param page
	 * @return
	 */
	@RequestMapping(value={"/list${entity}"},method={RequestMethod.GET})
	@ResponseBody
	@OperateLog(action=ActionConstants.?,level=Constants.LOG_LEVEL_SECOND)
	public ResultVo list${entity}(Page page) throws BussinessException,SystemErrorException {
		ResultVo res = new ResultVo();
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("page", page);
		List<${entity}> ${lowEntity}List =  ${lowEntity}Service.find${entity}List(page);
		map.put("${lowEntity}List",${lowEntity}List);
		res.setObj(map);
		res.setResult(ErrorCodeConstants.EC_SUCCESS);
		return res;
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:${datetime}<br/>
	 * 功能: 根据 id 查询数据 <br/>
	 * @return
	 */
	@RequestMapping(value={"/query${entity}One"},method={RequestMethod.GET})
	@ResponseBody
	@OperateLog(action=ActionConstants.?,level=Constants.LOG_LEVEL_SECOND)
	public ResultVo query${entity}One(String id) throws BussinessException,SystemErrorException {
		ResultVo res = new ResultVo();
		long idLong = Long.parserLong(id);
		${entity} ${lowEntity} = ${lowEntity}Service.find${entity}One(idLong);
		res.setObj(${lowEntity});
		res.setResult(ErrorCodeConstants.EC_SUCCESS);
		return res;
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:${datetime}<br/>
	 * 功能: 增加 ${entity} 数据 <br/>
	 * @return
	 */
	@RequestMapping(value={"/insert${entity}"},method={RequestMethod.POST})
	@ResponseBody
	@OperateLog(action=ActionConstants.?,level=Constants.LOG_LEVEL_SECOND)
	public ResultVo insert${entity}(${entity} ${lowEntity}) throws BussinessException,SystemErrorException {
		ResultVo res = new ResultVo();
		${lowEntity}Service.insert${entity}(${lowEntity});
		res.setResult(ErrorCodeConstants.EC_SUCCESS);
		return res;
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:${datetime}<br/>
	 * 功能: 删除 ${entity} 数据 <br/>
	 * @return
	 */
	@RequestMapping(value={"/delete${entity}"},method={RequestMethod.POST})
	@ResponseBody
	@OperateLog(action=ActionConstants.?,level=Constants.LOG_LEVEL_SECOND)
	public ResultVo delete${entity}(String id) throws BussinessException,SystemErrorException {
		ResultVo res = new ResultVo();
		long idLong = Long.parserLong(id);
		${lowEntity}Service.delete${entity}(idLong);
		res.setResult(ErrorCodeConstants.EC_SUCCESS);
		return res;
	}

	/**
	 * 
	 * 作者:sanri <br/>
	 * 时间:${datetime}<br/>
	 * 功能: 修改 ${entity} 数据 <br/>
	 * @return
	 */
	@RequestMapping(value={"/update${entity}"},method={RequestMethod.POST})
	@ResponseBody
	@OperateLog(action=ActionConstants.?,level=Constants.LOG_LEVEL_SECOND)
	public ResultVo update${entity}(${entity} ${lowEntity}) throws BussinessException,SystemErrorException {
		ResultVo res = new ResultVo();
		${lowEntity}Service.update${entity}(${lowEntity});
		res.setResult(ErrorCodeConstants.EC_SUCCESS);
		return res;
	}

}