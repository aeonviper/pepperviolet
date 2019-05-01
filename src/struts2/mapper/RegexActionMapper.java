package struts2.mapper;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.dispatcher.mapper.ActionMapping;
import org.apache.struts2.dispatcher.mapper.DefaultActionMapper;

import com.opensymphony.xwork2.config.ConfigurationManager;

import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.*;
import java.util.regex.*;
import org.apache.commons.collections.*;

public class RegexActionMapper extends DefaultActionMapper {
			
	protected Pattern[] patternList = new Pattern[0];
	protected Map<Pattern, String> actionNameMap = new HashMap<Pattern, String>();
	protected Map<Pattern, String[]> parameterMap = new HashMap<Pattern, String[]>();
	protected Map<String, String[]> actionPatternMap = new HashMap<String, String[]>();
	protected Map<String, String[]> actionParamsMap = new HashMap<String, String[]>();
	
	public RegexActionMapper() {
		
		String actionName;
		String[] params;
		String pattern;
		String[] fields;
		Map<String, String> parameters;		
		
		URL url;
		InputStreamReader inputStreamReader = null;
		BufferedReader reader = null;
		String line;
		List<String> lines = new ArrayList<String>();
		try {			
			url = Thread.currentThread().getContextClassLoader().getResource("regex.mapping");
			inputStreamReader = new InputStreamReader(url.openStream());
			reader = new BufferedReader(inputStreamReader);
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() > 0 && !line.startsWith("#")) {
					lines.add(line);
				}
			}			
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("Error loading regex.mapping. Cause: " + e);
		} finally {
			try {
				if (reader != null) { reader.close(); }				
			} catch (IOException eio) {
				eio.printStackTrace();
			}
			
			try {
				if (inputStreamReader != null) { inputStreamReader.close(); }				
			} catch (IOException eio) {
				eio.printStackTrace();
			}			
		}
		
		patternList = new Pattern[lines.size()];
		for (int i=0;i<lines.size();i++) {
			
			fields = lines.get(i).split(":");
			
			actionName = fields[0];
			params = fields[1].split(",");
			pattern = fields[2];
			
			patternList[i] = Pattern.compile(pattern);
			actionNameMap.put(patternList[i], actionName);			
			parameterMap.put(patternList[i], params);
			
			actionPatternMap.put(actionName, excludeParenthesis(pattern));
			actionParamsMap.put(actionName, params);
			
		}
		//System.out.println(">> " + this.getClass().getSimpleName() + " initialised");
	}

	public ActionMapping getMapping(HttpServletRequest request, ConfigurationManager configManager) {
        ActionMapping mapping = new ActionMapping();
        String uri = getUri(request);
        
        Map params;
        int i;
        boolean matchFound = false;
        
        Matcher matcher;
        for (Pattern pattern : patternList) {
        	if ((matcher = pattern.matcher(uri)).matches()) {
        		//System.out.println(">> " + uri + " matches " + pattern.toString());
        		        		
        		mapping.setExtension("");
        		mapping.setNamespace("/");
        		mapping.setName(actionNameMap.get(pattern));
        		mapping.setParams(new HashMap());
        		
        		for (i=1;i<=matcher.groupCount();i++) {
        			mapping.getParams().put(parameterMap.get(pattern)[i-1], new String[]{matcher.group(i)});
        		}
        		matchFound = true;
        		break;
        	}
        }
        
        if (!matchFound) {
        	 mapping = super.getMapping(request, configManager);
        } 
        
        return mapping;
	}
	
	public ActionMapping getMappingFromActionName(String actionName) {
		//System.out.println(">> getMappingFromActionName " + actionName);
		
		ActionMapping mapping = new ActionMapping();
		
		String[] paramValue = null;		
        String paramsQuery = "";
        String [] test;
        Map<String, Object> paramsMap = new HashMap<String, Object>();
        
        if (actionName.indexOf('?') != -1) {
        	paramsQuery = actionName.substring(actionName.indexOf('?'));
        	for (String s : paramsQuery.split("[?&]")) {
        		if (s.length()>0) {
        			paramValue = s.split("=");
        			if (paramValue.length == 1) {
        				paramsMap.put(paramValue[0], "");
        			} else if (paramValue.length == 2) {
        				paramsMap.put(paramValue[0], paramValue[1]);
        			}
        		}
        	}
            actionName = actionName.substring(0, actionName.indexOf('?'));
            mapping.setParams(paramsMap);
        }
        
        mapping.setName(actionName);        
        
		return mapping;
	}
	
	public String getUriFromActionMapping(ActionMapping mapping) {		
		//System.out.println(">> getUriFromActionMapping " + mappingToString(mapping));
		boolean matchFound = false;
		
		StringBuilder sb = null;
		String[] parts = null;
		String[] params = null;
		String[] paramValue = null;
		Object o = null;
		Map<String, String> paramsMap = new HashMap<String, String>();
		int i = 0;
		
		String name = mapping.getName();
        String paramsQuery = "";
        if (name.indexOf('?') != -1) {
        	paramsQuery = name.substring(name.indexOf('?'));
        	for (String s : paramsQuery.split("[?&]")) {
        		paramValue = s.split("=");
    			if (paramValue.length == 1) {
    				paramsMap.put(paramValue[0], "");
    			} else if (paramValue.length == 2) {
    				paramsMap.put(paramValue[0], paramValue[1]);
    			}
        	}
            name = name.substring(0, name.indexOf('?'));
        }
		
		if (mapping != null) {
			sb = new StringBuilder();
			parts = actionPatternMap.get(mapping.getName());
			params = actionParamsMap.get(mapping.getName());
			for (i=0;parts != null && i<parts.length;i++) {
				sb.append(parts[i]);
				if (params != null && i<params.length && mapping.getParams() != null) {
					sb.append((o = mapping.getParams().get(params[i])) == null ? "" : o);
				}
			}
			if (sb.length() > 0 ) {
				matchFound = true;
			}
		}
		
		if (matchFound) {
			return sb.toString();
		} else {
			return super.getUriFromActionMapping(mapping);
		}
	}
	
	private String mappingToString(ActionMapping mapping) {
		StringBuilder sb = new StringBuilder();
		sb
		.append(mapping.getNamespace() + " ")
		.append(mapping.getName() + " ")
		.append(mapping.getMethod() + " ");
		return sb.toString();
	}
	
	private String[] excludeParenthesis(String str) {
		String OPEN = "(";
		String CLOSE = ")";
		ArrayStack stack = new ArrayStack();
		CharacterIterator it = new StringCharacterIterator(str);
		StringBuilder sb;		
		char prevCh1, prevCh2, ch;
		prevCh1 = ' ';
		prevCh2 = ' ';
		String[] array;
		int i;

		List<String> list = new ArrayList<String>();
		sb = new StringBuilder();

		for (ch=it.first();ch!=CharacterIterator.DONE;prevCh1=prevCh2,prevCh2=ch,ch=it.next()) {

			if (ch=='(' && prevCh1 != '\\' && prevCh2 != '\\') {
				stack.push(OPEN);
				continue;
			} else if (ch==')' && prevCh1 != '\\' && prevCh2 != '\\') {
				stack.pop();
				if (stack.empty() && n(sb).length()>0) {
					list.add(sb.toString());
					sb = new StringBuilder();
				}
				continue;
			}

			if (stack.empty()) {
				sb.append(ch);
			}
		}

		if (n(sb).length()>0) {
			list.add(sb.toString());
		}
		
		i = 0;		
		array = new String[list.size()];
		for (String s : list) {
			array[i++] = s;
		}

		return array;
	}

	private StringBuilder n(StringBuilder sb) {

		if (sb.length()>0 && "^".equals(sb.substring(0,1))) {
			sb.deleteCharAt(0);
		}
		if (sb.length()>0 && "$".equals(sb.substring(sb.length()-1,sb.length()))) {
			sb.deleteCharAt(sb.length()-1);
		}

		return sb;
	}
	
}
