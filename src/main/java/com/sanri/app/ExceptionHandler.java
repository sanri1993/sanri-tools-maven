package com.sanri.app;

import com.sanri.frame.HandlerExceptionResolver;
import com.sanri.frame.ModelAndView;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 
 * 作者:sanri <br/>
 * 时间:2017-4-20上午10:13:42<br/>
 * 功能:异常处理类 <br/>
 */
public class ExceptionHandler implements HandlerExceptionResolver{
	private Log logger = LogFactory.getLog(getClass());
	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Throwable ex) {
		ModelAndView modelAndView = new ModelAndView();
		logger.error(ex.getMessage(),ex);
//		ex.printStackTrace();
		response.setStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR);
		response.setCharacterEncoding("utf-8");
		PrintWriter writer = null;
		try {
			writer = response.getWriter();
			if(writer !=null) {
				if(StringUtils.isNotBlank(ex.getMessage())) {
					writer.write(ex.getMessage());
					writer.flush();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			IOUtils.closeQuietly(writer);
		}
		return modelAndView;
	}

}
