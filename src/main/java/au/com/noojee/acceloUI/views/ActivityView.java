package au.com.noojee.acceloUI.views;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MultiSelect;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.LocalDateRenderer;
import com.vaadin.ui.themes.ValoTheme;

import au.com.noojee.acceloapi.AcceloException;
import au.com.noojee.acceloapi.Formatters;
import au.com.noojee.acceloapi.dao.ActivityDao;
import au.com.noojee.acceloapi.dao.TicketDao;
import au.com.noojee.acceloapi.entities.Activity;
import au.com.noojee.acceloapi.entities.Ticket;

/**
 * Show all companies with a retainer.
 * 
 * @author bsutton
 *
 */
public class ActivityView extends VerticalLayout implements View
{
	private static final long serialVersionUID = 1L;

	public static final String VIEW_NAME = "Activity";

	static Logger logger = LogManager.getLogger();

	private Grid<ActivityLine> grid;
	private ListDataProvider<ActivityLine> activityProvider;
	private List<ActivityLine> activityLines;

	private Label loading;

	private String currentActivityId;

	private Label activityViewSubject;
	private Label activityViewDetails;
	
	private Label ticketId = new Label();
	private Label ticketOpenDate = new Label();
	private Label ticketContact = new Label();
	private Label ticketStatus = new Label();
	private Label ticketSubject = new Label();
	
	private TextField filter;




	public ActivityView()
	{
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		View.super.enter(event);
		this.setSizeFull();

		String ticketId = event.getParameters();

		initialiseGrid();
		loadData(ticketId);
	}

	private void loadData(String ticketId)
	{
		try
		{
			if (ticketId != currentActivityId)
			{
				
				
				currentActivityId = ticketId;
				logger.error("Start fetch Activitys");
				if (ticketId != null && ticketId.length() > 0)
				{
					Ticket ticket = new TicketDao().getById(Integer.valueOf(ticketId));
					this.ticketId.setValue("Ticket: " + ticket.getId());
					this.ticketOpenDate.setValue("Open: " + ticket.getDateStarted()); 
					this.ticketContact.setValue("Contact: " + new TicketDao().getContact(ticket).getFullName());
					this.ticketStatus.setValue("Status: " + ticket.getStatus());
					this.ticketSubject.setValue("<b>Subject: " + ticket.getTitle() + "</b>");


					List<Activity> activities = new TicketDao().getActivities(ticket);

					logger.error("End fetch activities");

					activityLines = activities.parallelStream().sorted().map(t -> new ActivityLine(t))
							.collect(Collectors.toList());
					activityProvider = new ListDataProvider<>(activityLines);
					this.grid.setDataProvider(activityProvider);
					
					if (!activityLines.isEmpty())
					{
						this.grid.select(activityLines.get(0));
						showActivity(activityLines.get(0));
					}
					
				}

				logger.error("Finished.");
				loading.setValue("Load Complete");
			}
		}
		catch (AcceloException e)
		{
			logger.error(e, e);
		}

	}

	void initialiseGrid()
	{
		if (grid == null)
		{

			this.setMargin(true);
			this.setSpacing(true);
			this.setSizeFull();

			Label heading = new Label("<H2><b>Ticket Activities</b></H2>");
			heading.setContentMode(ContentMode.HTML);
			this.addComponent(heading);

			
			
			this.addComponent(createTicketDetails());
			
			this.addComponent(createUserFilter());


			grid = new Grid<>();
			grid.setSelectionMode(Grid.SelectionMode.MULTI);
			grid.setSizeFull();
			grid.addColumn(ActivityLine::getSubject).setCaption("Subject").setExpandRatio(1);
			grid.addColumn(ActivityLine::getStanding).setCaption("Standing");
			grid.addColumn(ticketLine -> ticketLine.getAssignee()).setCaption("Engineer");

			grid.addColumn(ActivityLine::getDateCreated, new LocalDateRenderer("dd/MM/yyyy")).setCaption("Created");

			grid.addColumn(activityLine -> Formatters.format(activityLine.getBillable())).setCaption("Billable")
					.setStyleGenerator(activityLine -> "align-right");
			
			grid.addComponentColumn(activityLine -> {
				IconButton link = new IconButton("", VaadinIcons.ARROW_LEFT,
						e -> changeBillable(activityLine));
				
				link.addStyleName(ValoTheme.BUTTON_SMALL);
				link.addStyleName(ValoTheme.BUTTON_BORDERLESS);
				boolean isBillable = activityLine.getBillable().getSeconds() > 0; 
				link.setIcon(isBillable ? VaadinIcons.ARROW_CIRCLE_RIGHT : VaadinIcons.ARROW_LEFT);
				link.setDescription(isBillable ? "Move to Non Billable" : "Move to Billable");
				return link;
			}).setWidth(80).setCaption("Move");


			grid.addColumn(activityLine -> Formatters.format(activityLine.getNonBillable())).setCaption("NonBillable")
					.setStyleGenerator(activityLine -> "align-right");

			grid.addColumn(activityLine -> Formatters.format(activityLine.getRateCharged())).setCaption("Rate")
					.setStyleGenerator(activityLine -> "align-right");

			grid.addColumn(activityLine -> activityLine.isApproved()).setCaption("Approved");
			
			grid.addItemClickListener(l -> showActivity(l.getItem()));
			
			grid.addSelectionListener(event -> {
				Optional<ActivityLine> oLine = event.getFirstSelectedItem();
				if (!oLine.isPresent())
					showActivity(null);
				});

			// grid.addComponentColumn(activityLine -> new Button("View", e
			// -> UI.getCurrent().getNavigator()
			// .navigateTo(ActivityView.VIEW_NAME + "/" +
			// activityLine.getId())));
			//

			// grid.addComponentColumn(
			// activityLine -> new Button("Refresh",
			// e -> activityLine.refresh(acceloApi)
			// )
			// );

			// // background load the activity no.s
			// MYUI myUI = new MYUI();
			// new Thread(() -> {
			// activityLines.parallelStream().distinct().forEach(c -> new
			// UIAccessor(myUI, false, () -> {
			// c.loadWork(acceloApi);
			// }).run());

			// new UIAccessor(myUI, true, () -> loading.setValue("Load
			// Complete")).run();

			// }).start();

			this.addComponent(grid);
			this.setExpandRatio(grid, 2); // 2/3 of the screen.
			

			
			Button delete = new Button("Delete");
			delete.addClickListener(l -> deleteActivities());
			this.addComponent(delete);
			
			activityViewSubject = new Label();
			activityViewSubject.setWidth("100%");
			activityViewSubject.setContentMode(ContentMode.HTML);
			this.addComponent(activityViewSubject);

			
			Panel panel = new Panel();
			
			
			activityViewDetails = new Label();
			
			activityViewDetails.setContentMode(ContentMode.PREFORMATTED);
			//activityViewDetails.setSizeFull();
			
			panel.setSizeFull();
			panel.setContent(activityViewDetails);
			
			this.addComponent(panel);
			this.setExpandRatio(panel, 1); // 1/3 of the screen.
			
			loading = new Label("Loading...");
			this.addComponent(loading);
	
		}
	}

	private void changeBillable(ActivityLine activityLine)
	{
		Activity activity = activityLine.getActivity();
		
		boolean isBillable = activity.getBillable().getSeconds() > 0; 

		if (isBillable)
		{
			activity.setNonBillable(activity.getBillable());
			activity.setBillable(Duration.ofSeconds(0));
		}
		else
		{
			activity.setBillable(activity.getNonBillable());
			activity.setNonBillable(Duration.ofSeconds(0));
		}
		
		activity.setTimeAllocationId(0);
		
		new ActivityDao().replace(activity);
		
		this.activityProvider.refreshItem(activityLine);

			
	}

	private void deleteActivities()
	{
		MultiSelect<ActivityLine> selections = this.grid.asMultiSelect();
		
		
		selections.getValue().parallelStream().forEach(line -> {

			new ActivityDao().delete(line.getActivity());
			this.activityLines.remove(line);
		});
		
		// remove all selections as we have just deleted them.
		selections.deselectAll();

		this.activityProvider.refreshAll();
		
	}
	
	

	private Component createTicketDetails()
	{
		VerticalLayout vLayout = new VerticalLayout();
		vLayout.setSpacing(false);
		vLayout.setMargin(new MarginInfo(false, true));
		
		
		HorizontalLayout layout = new HorizontalLayout();
		layout.addComponent(ticketId);
		layout.addComponent(ticketContact);
		layout.addComponent(ticketOpenDate);
		layout.addComponent(ticketStatus);
		
		vLayout.addComponent(layout);
		
		vLayout.addComponent(ticketSubject);
		ticketSubject.setContentMode(ContentMode.HTML);
		ticketSubject.setWidth("100%");
		
		return vLayout;
	}

	private void showActivity(ActivityLine l)
	{
		if (l != null)
		{
		activityViewSubject.setValue("<b>Activity: " + l.getSubject() + "</b>");
		activityViewDetails.setValue(l.getBody());
		}
		else
		{
			activityViewSubject.setValue("<b>Activity: None selected</b>");
			activityViewDetails.setValue("");
		}


	}
	
	
	/**
	 * Create a grid filter to allow the user to search through the list of contracts.
	 * @return
	 */
	private Component createUserFilter()
	{
		HorizontalLayout layout = new HorizontalLayout();
		layout.setWidth("100%");
		Button clear = new Button("X");
		clear.addClickListener(l -> clearUserFilter());

		layout.addComponent(clear);
		filter = new TextField();
		filter.addValueChangeListener(l -> setUserFilter());
		filter.setWidth("100%");
		layout.addComponent(filter);
		layout.setExpandRatio(filter, 1);

		

		return layout;

	}

	private void clearUserFilter()
	{
		filter.clear();
		activityProvider.refreshAll();
	}

	private void setUserFilter()
	{
		activityProvider.setFilter(activityLine -> filterActivities(activityLine, filter.getValue()));
		
		MultiSelect<ActivityLine> selections = this.grid.asMultiSelect();
		
		selections.deselectAll();
		
		activityProvider.refreshAll();
	}

	/**
	 * Match any activites thats name contains the filter expression typed by the user.
	 * @param contractLine
	 * @param filter
	 * @return
	 */
	private boolean filterActivities(ActivityLine activityLine, String filter)
	{
		return activityLine.getSubject().toLowerCase().contains(filter.toLowerCase());
	}



}
