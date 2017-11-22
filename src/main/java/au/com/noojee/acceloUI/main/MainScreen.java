package au.com.noojee.acceloUI.main;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;

import au.com.noojee.acceloUI.about.AboutView;
import au.com.noojee.acceloUI.views.ActivityView;
import au.com.noojee.acceloUI.views.CleanupView;
import au.com.noojee.acceloUI.views.CompanyView;
import au.com.noojee.acceloUI.views.ErrorView;
import au.com.noojee.acceloUI.views.Menu;
import au.com.noojee.acceloUI.views.TicketView;
import au.com.noojee.acceloUI.views.UnapprovedTicketView;
import au.com.noojee.acceloUI.views.UnassignedTicketView;

/**
 * Content of the UI when the user is logged in.
 * 
 * 
 */
public class MainScreen extends HorizontalLayout
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Menu menu;

	public MainScreen(AcceloUI ui)
	{

		setSpacing(false);
		setStyleName("main-screen");

		CssLayout viewContainer = new CssLayout();
		viewContainer.addStyleName("valo-content");
		viewContainer.setSizeFull();

		CompanyView companyView = new CompanyView();
		final Navigator navigator = new Navigator(ui, viewContainer);
		// set default view.
		navigator.addView("", companyView);
		navigator.setErrorView(ErrorView.class);
		menu = new Menu(navigator);
		menu.addView(companyView, CompanyView.VIEW_NAME, CompanyView.VIEW_NAME, VaadinIcons.BUILDING);
		menu.addView(new TicketView(), TicketView.VIEW_NAME, TicketView.VIEW_NAME, VaadinIcons.TICKET);
		menu.addView(new ActivityView(), ActivityView.VIEW_NAME, ActivityView.VIEW_NAME, VaadinIcons.HOURGLASS);
		menu.addView(new UnassignedTicketView(), UnassignedTicketView.VIEW_NAME, UnassignedTicketView.VIEW_NAME, VaadinIcons.AMBULANCE);
		menu.addView(new UnapprovedTicketView(), UnapprovedTicketView.VIEW_NAME, UnapprovedTicketView.VIEW_NAME, VaadinIcons.ALARM);
		menu.addView(new CleanupView(), CleanupView.VIEW_NAME, CleanupView.VIEW_NAME, VaadinIcons.CROSSHAIRS);
		menu.addView(new AboutView(), AboutView.VIEW_NAME, AboutView.VIEW_NAME, VaadinIcons.INFO_CIRCLE);


		navigator.addViewChangeListener(viewChangeListener);

		addComponent(menu);
		addComponent(viewContainer);
		setExpandRatio(viewContainer, 1);
		setSizeFull();
	}

	// notify the view menu about view changes so that it can display which view
	// is currently active
	@SuppressWarnings("serial")
	ViewChangeListener viewChangeListener = new ViewChangeListener()
	{

		@Override
		public boolean beforeViewChange(ViewChangeEvent event)
		{
			return true;
		}

		@Override
		public void afterViewChange(ViewChangeEvent event)
		{
			menu.setActiveView(event.getViewName());
		}

	};
}
