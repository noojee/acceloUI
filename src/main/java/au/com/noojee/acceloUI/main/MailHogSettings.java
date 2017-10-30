package au.com.noojee.acceloUI.main;

import au.com.noojee.acceloUI.forms.SMTPServerSetting;

public class MailHogSettings extends SMTPServerSetting
{

	
	MailHogSettings()
	{
		super();
		setSmtpFQDN("localhost");
		setSmtpPort(1025);
	}
}
