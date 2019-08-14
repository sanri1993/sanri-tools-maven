package com.sanri.frame;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InvokeResult {
    private Method method;
    private Map<String,Object> paramsValues = new HashMap<>();
    private boolean success = true;
    private String reson;

    public InvokeResult() {
    }

    public InvokeResult(Method method) {
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public Map<String, Object> getParamsValues() {
        return paramsValues;
    }

    public boolean isSuccess() {
        return success;
    }

    /**
     * 调用出错
     * @param reson
     * @return
     */
    public static InvokeResult error(String reson){
        InvokeResult invokeResult = new InvokeResult();
        invokeResult.reson = reson;
        invokeResult.success = false;
        return invokeResult;
    }

    public void setParamsValues(Map<String, Object> paramsValues) {
        this.paramsValues = paramsValues;
    }

    public String getReson() {
        return reson;
    }
}
