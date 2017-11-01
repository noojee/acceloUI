package au.com.noojee.acceloUI.forms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;

public class SMTPServerSetting
{
	/**
	 * The FQDN or IP address of the SMTP server.
	 */
	private String smtpFQDN;

	/**
	 * The port used to connect to the SMTP server.
	 */

	private Integer smtpPort;

	/**
	 * The username used to authenticate against the SMTP server if authRequired
	 * is true.
	 */
	private String username;

	/**
	 * The password used to authenticate against the SMTP server if authRequired
	 * is true.
	 */
	private String password;

	/**
	 * A default from address which is used when the 'system' is sending out
	 * emails.
	 */
	private String fromEmailAddress;

	/**
	 * The default email address that bounce messages should be sent to. As this
	 * is a system wide variable we usually just passing in
	 * bounce@scoutmaster.org.au and they are ignored. emails.
	 */
	private String bounceEmailAddress;

	/**
	 * If true then the email server requires authentication.
	 */
	private Boolean authRequired = false;

	/**
	 * If set we will use SSL when connecting to the email server if it is
	 * supported.
	 */
	private Boolean useSSL = false;

	private boolean useStartTLS = false;

	private static SMTPServerSetting self;

	public static SMTPServerSetting getInstance()
	{
		if (self == null)
		{
			new SMTPServerSetting();
		}
		return self;
	}

	protected SMTPServerSetting()
	{
		// If not initialised before then do it now.
		this.setAuthRequired(false);
		this.setSmtpPort(25);
		this.setSmtpFQDN("localhost");
		
		self = this;
	}

	public Integer getSmtpPort()
	{
		return this.smtpPort;
	}

	public void setSmtpPort(final Integer smtpPort)
	{
		this.smtpPort = smtpPort;
	}

	public String getUsername()
	{
		return this.username;
	}

	public void setUsername(final String username)
	{
		this.username = username;
	}

	public String getPassword()
	{
		return this.password;
	}

	public void setPassword(final String password)
	{
		this.password = password;
	}

	public String getFromEmailAddress()
	{
		return this.fromEmailAddress;
	}

	public void setFromEmailAddress(final String fromEmailAddress)
	{
		this.fromEmailAddress = fromEmailAddress;
	}

	public String getSmtpFQDN()
	{
		return this.smtpFQDN;
	}

	public void setSmtpFQDN(final String smtpFQDN)
	{
		this.smtpFQDN = smtpFQDN;
	}

	public Boolean isAuthRequired()
	{
		return this.authRequired;
	}

	public void setAuthRequired(final Boolean authRequired)
	{
		this.authRequired = authRequired;

	}

	public Boolean getUseSSL()
	{
		return this.useSSL;
	}

	public void setUseSSL(final Boolean useSSL)
	{
		this.useSSL = useSSL;
	}

	public String getName()
	{
		return this.smtpFQDN;
	}

	public void sendEmail(final SMTPServerSetting settings, final String fromAddress, final String bounceEmailAddress,
			final EmailTarget target, final String subject, final String body,
			final HashSet<? extends DataSource> attachedFiles) throws EmailException
	{
		final ArrayList<EmailTarget> list = new ArrayList<>();
		list.add(target);

		sendEmail(settings, fromAddress, bounceEmailAddress, list, subject, body, attachedFiles);

	}

	public void sendEmail(SMTPServerSetting settings, String fromEmailAddress, String bounceEmailAddress,
			EmailTarget emailTarget, String subject, String body) throws EmailException
	{
		sendEmail(settings, fromEmailAddress, bounceEmailAddress, emailTarget, subject, body,
				new HashSet<DataSource>());

	}

	public void sendEmail(SMTPServerSetting settings, String fromAddress, String bounceEmailAddress, EmailTarget target,
			String subject, String bodyText, ByteArrayDataSource byteArrayDataSource)
	{
		final HashSet<DataSource> attachedFiles = new HashSet<>();
		attachedFiles.add(byteArrayDataSource);

		sendEmail(settings, fromAddress, bounceEmailAddress, target, subject, bodyText, byteArrayDataSource);

	}

	/**
	 * Simple method to send a single email using the EMailServerSettings.
	 *
	 * @param settings
	 * @param fromAddress
	 * @param firstAddress
	 * @param object2
	 * @param object
	 * @param subject
	 * @param body
	 * @param attachedFiles
	 * @param string
	 * @throws EmailException
	 */
	public void sendEmail(final SMTPServerSetting settings, final String fromAddress, String bounceEmailAddress,
			final List<EmailTarget> targets, final String subject, final String body,
			final HashSet<? extends DataSource> attachedFiles) throws EmailException
	{
		final HtmlEmail email = new HtmlEmail();

		email.setDebug(true);
		email.setHostName(settings.getSmtpFQDN());
		email.setSmtpPort(settings.getSmtpPort());
		email.setSSLCheckServerIdentity(false);
		if (settings.isAuthRequired())
		{
			//email.setAuthentication(settings.getUsername(), settings.getPassword());
			email.setAuthenticator(new DefaultAuthenticator(settings.getUsername(), settings.getPassword()));
		}
		if (settings.getUseSSL())
		{
			email.setSslSmtpPort(settings.getSmtpPort().toString());
			email.setSSLOnConnect(true);
			// email.setSSLCheckServerIdentity(false);
			// email.setStartTLSEnabled(settings.getUseStartTLS());
		}
		email.setFrom(fromAddress);
		email.setBounceAddress(bounceEmailAddress);

		for (final EmailTarget target : targets)
		{
			addEmailAddress(email, target.emailAddress, target.type);
		}

		email.setSubject(subject);
		email.setHtmlMsg(body);
		email.setTextMsg("Your email client does not support HTML messages");
		if (attachedFiles != null)
		{
			for (final DataSource attachedFile : attachedFiles)
			{
				email.attach(attachedFile, attachedFile.getName(), attachedFile.getContentType());
			}
		}

		email.send();

	}

	public void setUseStartTLS(boolean useStartTLS)
	{
		this.useStartTLS = useStartTLS;
	}

	private boolean getUseStartTLS()
	{
		return this.useStartTLS;
	}

	private void addEmailAddress(final HtmlEmail email, final String firstAddress, final EmailAddressType firstType)
			throws EmailException
	{
		switch (firstType)
		{
			case To:
				email.addTo(firstAddress);
				break;
			case BCC:
				email.addBcc(firstAddress);
				break;
			case CC:
				email.addCc(firstAddress);
				break;
		}
	}

	public String getBounceEmailAddress()
	{
		return bounceEmailAddress;
	}

	public void setBounceEmailAddress(String bounceEmailAddress)
	{
		this.bounceEmailAddress = bounceEmailAddress;
	}

	/**
	 * Helper class to store an emailAddress and the Address Type (To, BCC, CC).
	 *
	 * @author bsutton
	 *
	 */
	static public class EmailTarget
	{

		private final String emailAddress;
		private final EmailAddressType type;

		public EmailTarget(final EmailAddressType type, final String emailAddress)
		{
			this.type = type;
			this.emailAddress = emailAddress;
		}
	}
}
