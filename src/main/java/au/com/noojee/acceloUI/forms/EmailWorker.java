package au.com.noojee.acceloUI.forms;


import java.util.ArrayList;

import org.apache.commons.mail.EmailException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.ui.UI;

import au.com.noojee.acceloUI.forms.EmailForm.TargetLine;
import au.com.noojee.acceloUI.main.BroadcastMessage;
import au.com.noojee.acceloUI.main.Broadcaster;

public class EmailWorker extends RunnableUI
{

	private Logger logger = LogManager.getLogger();
	private EmailForm emailForm;
	private UserProvider userProvider;

	public EmailWorker(UI ui, EmailForm form, UserProvider userProvider)
	{
		super(ui);
		this.emailForm = form;
		this.userProvider = userProvider;
	}

	@Override
	public void run(UI ui)
	{
		try
		{
			final SMTPServerSetting settings = SMTPServerSetting.getInstance();

			final ArrayList<SMTPServerSetting.EmailTarget> targets = new ArrayList<>();

			// First add in the primary address.
			if (!isEmpty(emailForm.getPrimaryTargetAddress()))
			{
				targets.add(
						new SMTPServerSetting.EmailTarget(emailForm.getPrimaryType(),
								emailForm.getPrimaryTargetAddress()));
			}

			for (final TargetLine line : emailForm.lines)
			{
				if (line.targetAddress.getValue() != null && !isEmpty(line.targetAddress.getValue().getEmail()))
				{
					targets.add(new SMTPServerSetting.EmailTarget((EmailAddressType) line.targetTypeCombo.getValue(),
						line.targetAddress.getValue().getEmail()));
				}
			}

			assert targets.size() != 0 : "Empty list of email targets";
			settings.sendEmail(settings, emailForm.getSender().getEmailAddress(),
					this.userProvider.getEmailAddress(), targets, emailForm.getSubject(),
					emailForm.getBody(), emailForm.getAttachements());
		}
		catch (final EmailException e)
		{
			logger.error(e, e);
			emailForm.enableSendButton();
			Broadcaster.broadcast(new BroadcastMessage(e));
		}
		finally
		{
			 
		}
		

	}

	private boolean isEmpty(final String value)
	{
		return value == null || value.length() == 0;
	}

}
