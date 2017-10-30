package au.com.noojee.acceloUI.main;

import org.apache.commons.mail.EmailException;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;


public class BroadcastMessage
{
	public enum BroadcastLevel
	{
		INFO
		{
			@Override
			public void displayMessage(UI ui, final String message)
			{
				ui.access(new Runnable()
				{

					@Override
					public void run()
					{
						Notification.show(message, Type.WARNING_MESSAGE);
					}
				});
			}
		},
		WARN
		{
			@Override
			public void displayMessage(UI ui, final String message)
			{
				ui.access(new Runnable()
				{

					@Override
					public void run()
					{
						Notification.show(message, Type.WARNING_MESSAGE);
					}
				});
			}
		},
		ERROR
		{
			@Override
			public void displayMessage(UI ui, final String message)
			{
				ui.access(new Runnable()
				{

					@Override
					public void run()
					{
						final Window window = new Window();

						VerticalLayout content = new VerticalLayout();
						ErrorLabel errorDetails = new ErrorLabel(message.replaceAll("\\n", "<br>"));
						errorDetails.setContentMode(ContentMode.HTML);
						content.addComponent(errorDetails);

						content.addComponent(new Label("Acknowledgement of this message will be audited"));

						final CheckBox ack = new CheckBox("I acknowledge that I have read and understood this message");
						content.addComponent(ack);

						Button ok = new Button("OK");
						ok.addClickListener(new Button.ClickListener()
						{

							private static final long serialVersionUID = 1L;

							@Override
							public void buttonClick(ClickEvent event)
							{
								if (ack.getValue())
								{

									window.close();
								}
								else
								{
									Notification.show("You must check 'I acknowledge...' befor you can continue.",
											Type.ERROR_MESSAGE);
								}

							}
						});
						content.addComponent(ok);
						content.setMargin(true);
						content.setSpacing(true);

						window.setModal(true);
						window.setContent(content);
						window.setClosable(false);
						window.setResizable(false);
						window.center();

						UI.getCurrent().addWindow(window);

					}
				});
			}
		};
		public abstract void displayMessage(UI ui, String message);

		public static void ERROR(EmailException e) {
			// TODO Auto-generated method stub
			
		}
	}

	private final String message;
	private final BroadcastLevel level;

	public BroadcastMessage(String message, BroadcastLevel level)
	{
		this.message = message;
		this.level = level;
	}

	public BroadcastMessage(Exception e)
	{
		this(e.getMessage(), BroadcastLevel.ERROR);
	}

	public void displayMessage(UI ui)
	{
		level.displayMessage(ui, message);
	}
}
