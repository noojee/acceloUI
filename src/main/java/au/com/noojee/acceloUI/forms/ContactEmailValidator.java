package au.com.noojee.acceloUI.forms;

import com.vaadin.data.ValidationResult;
import com.vaadin.data.Validator;
import com.vaadin.data.ValueContext;

public class ContactEmailValidator implements Validator<EmailForm.Contact>
{
	private static final long serialVersionUID = 1L;
	private String error;

	ContactEmailValidator(String error)
	{
		this.error = error;
	}

	@Override
	public ValidationResult apply(EmailForm.Contact contact, ValueContext context)
	{
		ValidationResult result = ValidationResult.ok();
		org.apache.commons.validator.routines.EmailValidator validator = org.apache.commons.validator.routines.EmailValidator
				.getInstance();

		String email = contact.getEmail();

		if (email != null && email.trim().length() > 0 && !validator.isValid(email))
			ValidationResult.error(error);

		return result;
	}

}
