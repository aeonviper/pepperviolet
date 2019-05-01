package core.interceptor;

import java.util.*;

import com.opensymphony.xwork2.Action;
import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.*;
import com.opensymphony.xwork2.util.TextParseUtil;
import com.opensymphony.xwork2.ValidationAware;


public class ParameterizedValidationInterceptor extends AbstractInterceptor {
        
    private Set excludedActionNames = Collections.EMPTY_SET;
    
    public void setExcludedActionNames(String excludedActionNames) {
        this.excludedActionNames = TextParseUtil.commaDelimitedStringToSet(excludedActionNames);
    }
    
    public String intercept(ActionInvocation invocation) throws Exception {       
        String actionName = invocation.getProxy().getActionName();
        String result = null;
        
        if (excludedActionNames.contains(actionName)) {
            return invocation.invoke();
        }
        
        Object action = invocation.getAction();
        if (action instanceof ValidationAware) {
            ValidationAware validationAwareAction = (ValidationAware) action;
            if (validationAwareAction.hasErrors()) {
            	result = Action.INPUT + "-" + invocation.getProxy().getMethod();
            	if (action instanceof ParameterizedValidationAware) {
            		result = ((ParameterizedValidationAware) action).onFailedValidation(invocation.getProxy().getMethod());
            		if (result == null) {
            			result = Action.INPUT + "-" + invocation.getProxy().getMethod();
            		}
            	}
                return result;
            }
        }
        return invocation.invoke();
    }
}

