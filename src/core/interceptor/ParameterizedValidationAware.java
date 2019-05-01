package core.interceptor;

public interface ParameterizedValidationAware {
	public String onFailedValidation(String method);
}
