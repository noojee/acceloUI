package au.com.noojee.acceloUI.forms;

import java.util.List;

import au.com.noojee.acceloUI.forms.EmailForm.Contact;

public interface ContactProvider
{

	Contact getDefaultContact();

	List<Contact> getAlternateContacts();

}
