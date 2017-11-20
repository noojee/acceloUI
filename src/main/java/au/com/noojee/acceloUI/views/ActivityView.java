package au.com.noojee.acceloUI.views;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
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
import com.vaadin.server.Page;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MultiSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.LocalDateRenderer;
import com.vaadin.ui.themes.ValoTheme;

import au.com.noojee.acceloUI.util.JobService;
import au.com.noojee.acceloUI.util.SMNotification;
import au.com.noojee.acceloapi.AcceloException;
import au.com.noojee.acceloapi.dao.ActivityDao;
import au.com.noojee.acceloapi.dao.TicketDao;
import au.com.noojee.acceloapi.entities.Activity;
import au.com.noojee.acceloapi.entities.Activity.Standing;
import au.com.noojee.acceloapi.entities.Ticket;
import au.com.noojee.acceloapi.util.Formatters;

/**
 * Show all companies with a retainer.
 * 
 * @author bsutton
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
	private Label ticketTotalBilled = new Label();
	private Label ticketSubject = new Label();

	private TextField filter;

	private Ticket ticket;

	private TicketDao daoTicket;

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
					daoTicket = new TicketDao();
					ticket = daoTicket.getById(Integer.valueOf(ticketId));
					this.ticketId.setValue("Ticket: " + ticket.getId());
					this.ticketOpenDate.setValue("Open: " + ticket.getDateStarted());
					this.ticketContact.setValue("Contact: " + new TicketDao().getContact(ticket).getFullName());
					this.ticketStatus.setValue("Status: " + ticket.getStatus());
					updateTotalBilledLabel();

					this.ticketSubject.setValue("<b>Subject: " + ticket.getTitle() + "</b>");

					List<Activity> activities = new TicketDao().getActivities(ticket, true);

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
			grid.addColumn(ActivityLine::getDateStarted, new LocalDateRenderer("dd/MM/yyyy")).setCaption("Started");

			grid.addColumn(activityLine -> Formatters.format(activityLine.getBillable())).setCaption("Billable")
					.setStyleGenerator(activityLine -> "align-right");

			grid.addComponentColumn(activityLine ->
				{
					IconButton link = new IconButton("", VaadinIcons.ARROW_CIRCLE_LEFT,
							e -> changeBillable(e, activityLine));

					link.addStyleName(ValoTheme.BUTTON_SMALL);
					link.addStyleName(ValoTheme.BUTTON_BORDERLESS);
					boolean isBillable = activityLine.getBillable().getSeconds() > 0;
					link.setIcon(isBillable ? VaadinIcons.ARROW_CIRCLE_RIGHT : VaadinIcons.ARROW_CIRCLE_LEFT);
					link.setDescription(isBillable ? "Move to Non Billable" : "Move to Billable");
					return link;
				}).setWidth(80).setCaption("Move");

			grid.addColumn(activityLine -> Formatters.format(activityLine.getNonBillable())).setCaption("NonBillable")
					.setStyleGenerator(activityLine -> "align-right");

			grid.addColumn(activityLine -> Formatters.format(activityLine.getRateCharged())).setCaption("Rate")
					.setStyleGenerator(activityLine -> "align-right");

			grid.addColumn(activityLine -> activityLine.isApproved()).setCaption("Approved");

			grid.addItemClickListener(l -> showActivity(l.getItem()));

			grid.addSelectionListener(event ->
				{
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

			HorizontalLayout buttons = new HorizontalLayout();
			// buttons.setWidth("100%");
			this.addComponent(buttons);

			Button delete = new Button("Delete");
			delete.addClickListener(l -> deleteActivities());
			buttons.addComponent(delete);

//			Button approve = new Button("Approve");
//			approve.addClickListener(l -> approveTicket());
//			buttons.addComponent(approve);
			
			buttons.addComponent(createApproval());

			Button setMin = new Button("Set Min Bill");
			setMin.addClickListener(l -> roundBilling(l));
			buttons.addComponent(setMin);

			activityViewSubject = new Label();
			activityViewSubject.setWidth("100%");
			activityViewSubject.setContentMode(ContentMode.HTML);
			this.addComponent(activityViewSubject);

			Panel panel = new Panel();

			activityViewDetails = new Label();

			activityViewDetails.setContentMode(ContentMode.PREFORMATTED);
			// activityViewDetails.setSizeFull();

			panel.setSizeFull();
			panel.setContent(activityViewDetails);

			this.addComponent(panel);
			this.setExpandRatio(panel, 1); // 1/3 of the screen.

			loading = new Label("Loading...");
			this.addComponent(loading);

		}
	}

	private void changeBillable(ClickEvent e, ActivityLine activityLine)
	{
		// stop the button being clicked again until the db update is complete.
		e.getButton().setEnabled(false);
		// this.deleteButton.setEnabled(false);

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

		JobService.Job<Activity> job = JobService.getInstance()
				.newJob("Change Billing for Activity " + activity.getId(), () -> new ActivityDao().replace(activity));

		job.addCompleteListener(newActivity ->
			{
				e.getButton().setEnabled(true);
				activityLine.setActivity(newActivity);

				// The next line actually replaces the button
				this.activityProvider.refreshItem(activityLine);
				updateTotalBilledLabel();
			});

		job.submit();

	}

	private void deleteActivities()
	{
		MultiSelect<ActivityLine> selections = this.grid.asMultiSelect();

		selections.getValue().parallelStream().forEach(line ->
			{

				new ActivityDao().delete(line.getActivity());
				this.activityLines.remove(line);
			});

		// remove all selections as we have just deleted them.
		selections.deselectAll();

		this.activityProvider.refreshAll();
		updateTotalBilledLabel();

	}

	private Button createApproval()
	{
		Button approveButton = new Button("Approve");

		approveButton.addClickListener(l ->
			{

				// https://noojee.accelo.com/?action=approve_object&object_id=10683&object_table=issue&amp;referring_link=action%3Dview_issue%26id%3D10683

				try
				{

					String action = "?action=approve_object&object_id=" + this.ticket.getId()
							+ "&object_table=issue&referring_link=action%3Dview_issue%26id%3D" + this.ticket.getId();

					URL acceloApprovalPage = new URL("https", "noojee.accelo.com", 443, action);

					Page.getCurrent().open(acceloApprovalPage.toExternalForm(), "_accelo", true);
				}
				catch (Exception e)
				{
					SMNotification.show("Error", e.getMessage(), Notification.Type.ERROR_MESSAGE);
				}

			});

		return approveButton;
	}

	private void approveTicket()
	{
		try
		{
			String fqdn = "noojee.accelo.com"; // UI.getCurrent().getPage().getLocation().getHost();

			// Approve activities
			// https://noojee.accelo.com/api/0.5/key/activity/list
			// form data activity_ids=209671&activity_ids=209708&activity_ids=209710&activity_ids=209711&approve=1
			//
			String activityIds = activityLines.stream().map(l -> "activity_ids=" + l.getActivity().getId())
					.collect(Collectors.joining("&"));
			// String formData = activityIds + "&approve=1";
			//
			// URL urlApproveActivities = new URL("https", fqdn, 443,
			// "/api/0.5/key/activity/list" + "?" + formData);
			//
			// Page.getCurrent().open(urlApproveActivities.toExternalForm(), "_accelo", true);

			// Now approve the ticket

			// https://noojee.accelo.com/api/0.5/key/issue/10683/approval
			// Referer:https://noojee.accelo.com/?action=approve_object&object_id=10683&object_table=issue&amp;referring_link=action%3Dview_issue%26id%3D10683
			// X-API-Key:03341ef66bc8ea0a28f07ca4f8e74c101f7001aa

			// URL urlApproveTicket = new URL("https", fqdn, 443, "/api/0.5/key/issue/" + this.ticket.getId() +
			// "/approval");
			//
			// Page.getCurrent().open(urlApproveTicket.toExternalForm(), "_accelo1", true);

			Page.getCurrent().getJavaScript().execute(getApproveScript(this.ticket.getId(), activityIds));

		}
		catch (Exception e)
		{
			SMNotification.show("Error", e.getMessage(), Notification.Type.ERROR_MESSAGE);
		}

	}

	private String getApproveScript(int ticketId, String activity_ids)
	{
		String script = "";
		try
		{
			script = new AcceloJavaScript("approveTicket.js").toString();

			script += "\n\nnoojeeApproveTicket(" + ticketId + ",\"" + activity_ids + "\");";
		}
		catch (IOException | URISyntaxException e)
		{
			SMNotification.show("Error reading javascript file", e.getMessage(), Notification.Type.ERROR_MESSAGE);
		}
		return script;
	}

	// You can't approve activities via the UI.
//	private void approveActivities()
//	{
//
//		// https://noojee.accelo.com/?action=approve_object&object_id=10683&object_table=issue
//		MultiSelect<ActivityLine> selections = this.grid.asMultiSelect();
//
//		selections.getValue().parallelStream().forEach(line ->
//			{
//				// We ignore any lines that are already approved.
//				if (!line.isApproved())
//				{
//					Activity activity = line.getActivity();
//					activity.setStanding(Standing.approved);
//
//					JobService.Job<Activity> job = JobService.getInstance()
//							.newJob("Approve Activity " + activity.getId(), () -> new ActivityDao().replace(activity));
//
//					job.addCompleteListener(newActivity ->
//						{
//							line.setActivity(newActivity);
//						});
//
//					job.submit();
//				}
//			});
//
//		this.activityProvider.refreshAll();
//	}

	/**
	 * Rounds billing up to the nearest 15 min block as per our standard contracts.
	 * 
	 * @param l
	 */
	private void roundBilling(ClickEvent l)
	{
		l.getButton().setEnabled(false);
		MultiSelect<ActivityLine> selections = this.grid.asMultiSelect();

		if (selections.getSelectedItems().size() != 1)
		{
			l.getButton().setEnabled(true);
			SMNotification.show("Set Minimum Billable", "You must select only one activity",
					Notification.Type.ERROR_MESSAGE);
		}
		else
		{
			// calculate the current total billable.
			Duration totalBillable = this.activityLines.stream().map(ActivityLine::getBillable).reduce(Duration.ZERO,
					(lhs, rhs) -> lhs.plus(rhs));

			//
			long minutes = totalBillable.toMinutes();
			long rounded = (long) (Math.ceil(minutes / 15.0f) * 15);

			if (rounded != minutes)
			{
				// Update the selected activity so the total billable for the ticket is rounded up to the nearest
				// fifteen minutes.
				ActivityLine selectedActivity = (ActivityLine) selections.getSelectedItems().toArray()[0];
				Activity activity = selectedActivity.getActivity();

				Duration roundedBillable = Duration.ofMinutes(rounded).minus(totalBillable)
						.plus(activity.getBillable());
				activity.setBillable(roundedBillable);

				JobService.Job<Activity> job = JobService.getInstance()
						.newJob("Set Minimum Billable " + activity.getId(), () -> new ActivityDao().replace(activity),
								newActivity ->
									{
										selectedActivity.setActivity(newActivity);
										this.activityProvider.refreshItem(selectedActivity);
										updateTotalBilledLabel();
										l.getButton().setEnabled(true);
									});

				;
				job.submit();

			}
			else
				l.getButton().setEnabled(true);
		}
	}

	private void updateTotalBilledLabel()
	{
		this.ticketTotalBilled.setValue("Total Billed: " + Formatters.format(this.daoTicket.getBillable(this.ticket)));

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
		layout.addComponent(ticketTotalBilled);

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
	 * 
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
	 * 
	 * @param contractLine
	 * @param filter
	 * @return
	 */
	private boolean filterActivities(ActivityLine activityLine, String filter)
	{
		return activityLine.getSubject().toLowerCase().contains(filter.toLowerCase());
	}

}
