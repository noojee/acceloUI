package au.com.noojee.acceloUI.forms;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;

public class EmailValidator implements Validator<String>
{
	private static final long serialVersionUID = 1L;
	private String errorMessage;

	public EmailValidator(String errorMessage)
	{
		this.errorMessage = errorMessage;
	}

	
	@Override
	public ValidationResult apply(String value, ValueContext context)
	{
		ValidationResult result = ValidationResult.ok();
		org.apache.commons.validator.routines.EmailValidator validator = org.apache.commons.validator.routines.EmailValidator.getInstance();
		
		if (value != null && ((String)value).trim().length() > 0 && !validator.isValid((String) value))
			result = ValidationResult.error(errorMessage);
		
		return result;
	}

}
