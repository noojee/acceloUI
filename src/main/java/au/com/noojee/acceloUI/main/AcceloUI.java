package au.com.noojee.acceloUI.main;

import java.io.FileNotFoundException;

import javax.servlet.annotation.WebServlet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Viewport;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

import au.com.noojee.acceloUI.authentication.AccessControl;
import au.com.noojee.acceloUI.authentication.BasicAccessControl;
import au.com.noojee.acceloUI.authentication.LoginScreen;
import au.com.noojee.acceloUI.authentication.LoginScreen.LoginListener;
import au.com.noojee.acceloUI.main.Broadcaster.BroadcastListener;
import au.com.noojee.acceloapi.AcceloApi;
import au.com.noojee.acceloapi.AcceloException;
import au.com.noojee.acceloapi.AcceloSecret;

/**
 * Main UI class of the application that shows either the login screen or the
 * main view of the application depending on whether a user is signed in.
 *
 * The @Viewport annotation configures the viewport meta tags appropriately on
 * mobile devices. Instead of device based scaling (default), using responsive
 * layouts.
 */
@SuppressWarnings("serial")
@Viewport("user-scalable=no,initial-scale=1.0")
@Theme("acceloUI")
@Widgetset("AppWidgetset")
@Push(PushMode.AUTOMATIC)
public class AcceloUI extends UI implements BroadcastListener
{
	static Logger logger = LogManager.getLogger();

	private AccessControl accessControl = new BasicAccessControl();

	@Override
	protected void init(VaadinRequest vaadinRequest)
	{
		Responsive.makeResponsive(this);
		setLocale(vaadinRequest.getLocale());
		getPage().setTitle("Accelo Contracts");
		
		Broadcaster.register(this);
		
		new MailHogSettings();

		//new GMailSettings();
		
		
		try
		{
			AcceloApi.getInstance().connect(AcceloSecret.load());
		}
		catch (FileNotFoundException | AcceloException e)
		{
			logger.error(e,e);
		}
		
		if (!accessControl.isUserSignedIn())
		{
			setContent(new LoginScreen(accessControl, new LoginListener()
			{
				@Override
				public void loginSuccessful()
				{
					showMainView();
				}
			}));
		}
		else
		{
			showMainView();
		}
	}

	protected void showMainView()
	{
		addStyleName(ValoTheme.UI_WITH_MENU);
		setContent(new MainScreen(AcceloUI.this));
		getNavigator().navigateTo(getNavigator().getState());
	}

	public static AcceloUI get()
	{
		return (AcceloUI) UI.getCurrent();
	}

	public AccessControl getAccessControl()
	{
		return accessControl;
	}
	
	@Override
	public void receiveBroadcast(final BroadcastMessage message)
	{
		access(new Runnable()
		{
			@Override
			public void run()
			{
				message.displayMessage(UI.getCurrent());
			}
		});
	}


	@WebServlet(urlPatterns = "/*", name = "retainerUIServlet", asyncSupported = true)
	@VaadinServletConfiguration(ui = AcceloUI.class, productionMode = false)
	public static class retainerUIServlet extends VaadinServlet
	{
	}

}
