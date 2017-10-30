package au.com.noojee.acceloUI.main;

import au.com.noojee.acceloUI.forms.SMTPServerSetting;

public class GMailSettings extends SMTPServerSetting
{
	GMailSettings()
	{
		super();
		
		SMTPServerSetting.getInstance().setSmtpFQDN("smtp.gmail.com");

		//SMTPServerSetting.getInstance().setSmtpPort(587);
		SMTPServerSetting.getInstance().setSmtpPort(465);
		SMTPServerSetting.getInstance().setUseSSL(true);
		SMTPServerSetting.getInstance().setUseStartTLS(true);
		SMTPServerSetting.getInstance().setAuthRequired(true);
		SMTPServerSetting.getInstance().setUsername("bsutton@noojee.com.au");
		SMTPServerSetting.getInstance().setPassword("XXXXX");

	}

}
