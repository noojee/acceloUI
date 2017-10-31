package au.com.noojee.acceloUI.views;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.data.provider.ListDataProvider;
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
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.LocalDateRenderer;

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

	Grid<ActivityLine> grid;
	ListDataProvider<ActivityLine> ticketProvider;
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
					ticketProvider = new ListDataProvider<>(activityLines);
					this.grid.setDataProvider(ticketProvider);
					
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

			grid = new Grid<>();
			grid.setSelectionMode(Grid.SelectionMode.MULTI);
			grid.setSizeFull();
			grid.addColumn(ActivityLine::getSubject).setCaption("Subject").setExpandRatio(1);
			grid.addColumn(ActivityLine::getStanding).setCaption("Standing");
			grid.addColumn(ticketLine -> ticketLine.getAssignee()).setCaption("Engineer");

			grid.addColumn(ActivityLine::getDateCreated, new LocalDateRenderer("dd/MM/yyyy")).setCaption("Created");

			grid.addColumn(activityLine -> Formatters.format(activityLine.getBillable())).setCaption("Billable")
					.setStyleGenerator(activityLine -> "align-right");

			grid.addColumn(activityLine -> Formatters.format(activityLine.getNonBillable())).setCaption("NonBillable")
					.setStyleGenerator(activityLine -> "align-right");

			grid.addColumn(activityLine -> Formatters.format(activityLine.getRateCharged())).setCaption("Rate")
					.setStyleGenerator(activityLine -> "align-right");

			grid.addColumn(activityLine -> activityLine.isApproved()).setCaption("Approved");
			
			grid.addItemClickListener(l -> showActivity(l.getItem()));

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

	private void deleteActivities()
	{
		MultiSelect<ActivityLine> selections = this.grid.asMultiSelect();
		
		selections.getValue().stream().forEach(line -> {

			new ActivityDao().delete(line.getActivity());
		});
		
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
		activityViewSubject.setValue("<b>Activity: " + l.getSubject() + "</b>");
		activityViewDetails.setValue(l.getBody());


	}

}
