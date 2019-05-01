package core.interceptor;

import java.util.*;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.*;
import com.opensymphony.xwork2.util.TextParseUtil;

public class AuthenticationInterceptor extends AbstractInterceptor {
    
    private Set excludedActionNames = Collections.EMPTY_SET;
    private String resultName = "";
    
    public void setExcludedActionNames(String excludedActionNames) {
        this.excludedActionNames = TextParseUtil.commaDelimitedStringToSet(excludedActionNames);
    }
    
    public void setResultName(String resultName) {
    	this.resultName = resultName;
    }
    
    public String intercept(ActionInvocation invocation) throws Exception {        
        Map session = invocation.getInvocationContext().getSession();      
        String actionName = invocation.getProxy().getActionName();        
        if (!excludedActionNames.contains(actionName) && session.get(this.getClass().getName()) == null) {   
            System.err.println(this.getClass().getName() + " No session defined");
            return resultName;
        } 
        return invocation.invoke();
    }
}

