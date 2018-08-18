package au.com.noojee.acceloUI.views;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.data.Binder;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MultiSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.renderers.LocalDateTimeRenderer;
import com.vaadin.ui.themes.ValoTheme;

import au.com.noojee.acceloUI.util.JobService;
import au.com.noojee.acceloUI.util.SMNotification;
import au.com.noojee.acceloapi.AcceloException;
import au.com.noojee.acceloapi.cache.AcceloCache;
import au.com.noojee.acceloapi.dao.ActivityDao;
import au.com.noojee.acceloapi.dao.CompanyDao;
import au.com.noojee.acceloapi.dao.TicketDao;
import au.com.noojee.acceloapi.entities.Activity;
import au.com.noojee.acceloapi.entities.Company;
import au.com.noojee.acceloapi.entities.Ticket;
import au.com.noojee.acceloapi.util.Format;
import au.com.noojee.acceloapi.util.LocalDateTimeHelper;

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
	private Label companyLabel = new Label();
	private Label ticketStartDate = new Label();
	private Label ticketContact = new Label();
	private Label ticketStatus = new Label();
	private Label ticketPriority = new Label();
	private Label ticketTotalBilled = new Label();
	private Label ticketSubject = new Label();
	private TextArea ticketResolution = new TextArea();

	private TextField filter;

	private Ticket ticket;

	private TicketDao daoTicket;

	boolean layoutExists = false;

	public ActivityView()
	{
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		View.super.enter(event);
		this.setSizeFull();

		String ticketId = event.getParameters();

		if (!layoutExists)
		{
			layoutHeader();
			initialiseGrid();
			layoutFooter();

			layoutExists = true;
		}
		loadData(ticketId);
	}

	void initialiseGrid()
	{
		this.setMargin(true);
		this.setSpacing(true);
		this.setSizeFull();
		grid = new Grid<>();
		grid.setSelectionMode(Grid.SelectionMode.MULTI);
		grid.setSizeFull();
		grid.addColumn(ActivityLine::getSubject).setCaption("Subject").setExpandRatio(1);
		grid.addColumn(ActivityLine::getStanding).setCaption("Standing");
		grid.addColumn(ticketLine -> ticketLine.getAssignee()).setCaption("Engineer");

		grid.addColumn(ActivityLine::getDateTimeCreated, new LocalDateTimeRenderer("dd/MM/yy HH:mm"))
				.setCaption("Created");
		grid.addColumn(ActivityLine::getDateTimeStarted, new LocalDateTimeRenderer("dd/MM/yy HH:mm"))
				.setCaption("Started");

		grid.addColumn(activityLine -> Format.format(activityLine.getBillable())).setCaption("Billable")
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

		grid.addColumn(activityLine -> Format.format(activityLine.getNonBillable())).setCaption("NonBillable")
				.setStyleGenerator(activityLine -> "align-right");

		grid.addColumn(activityLine -> Format.format(activityLine.getRateCharged())).setCaption("Rate")
				.setStyleGenerator(activityLine -> "align-right");

		grid.addColumn(activityLine -> activityLine.isApproved()).setCaption("Approved");

		grid.addItemClickListener(l -> showActivity(l.getItem()));

		grid.addSelectionListener(event ->
			{
				Optional<ActivityLine> oLine = event.getFirstSelectedItem();
				if (!oLine.isPresent())
					showActivity(null);
			});

		this.addComponent(grid);
		this.setExpandRatio(grid, 2); // 2/3 of the screen.

	}

	private void layoutFooter()
	{
		HorizontalLayout buttons = new HorizontalLayout();
		// buttons.setWidth("100%");
		this.addComponent(buttons);

		Button delete = new Button("Delete");
		delete.addClickListener(l -> deleteActivities());
		buttons.addComponent(delete);

		// Button approve = new Button("Approve");
		// approve.addClickListener(l -> approveTicket());
		// buttons.addComponent(approve);

		buttons.addComponent(createApproval());

		Button setMin = new Button("Set Min Bill");
		setMin.addClickListener(l -> roundBilling(l));
		buttons.addComponent(setMin);

		Button setNonBillableButton = new Button("Set Non-Billable");
		setNonBillableButton.addClickListener(l -> setNonBillable(l));
		buttons.addComponent(setNonBillableButton);

		Button setBillableButton = new Button("Set Billable");
		setBillableButton.addClickListener(l -> setBillable(l));
		buttons.addComponent(setBillableButton);

		Button editButton = new Button("Edit");
		editButton.addClickListener(l -> onEdit());
		buttons.addComponent(editButton);

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

	private void layoutHeader()
	{
		VerticalLayout header = new VerticalLayout();
		this.addComponent(header);
		header.setMargin(false);

		HorizontalLayout headingLayout = new HorizontalLayout();
		header.addComponent(headingLayout);
		Label heading = new Label("<H2><b>Ticket Activities</b></H2>");
		heading.setContentMode(ContentMode.HTML);
		headingLayout.addComponent(heading);

		Button findButton = new Button("Find");
		headingLayout.addComponent(findButton);
		headingLayout.setComponentAlignment(findButton, Alignment.BOTTOM_LEFT);
		findButton.setDisableOnClick(true);
		findButton.addClickListener(l -> selectTicket());

		header.addComponent(createTicketDetails());

		header.addComponent(createUserFilter());
	}

	static class Form
	{
		Integer ticketNo = 0;

		static void setTicketNo(Form form, Integer ticketNo)
		{
			form.ticketNo = ticketNo;
		}

		Integer getTicketNo()
		{
			return this.ticketNo;
		}
	}

	private void selectTicket()
	{
		VerticalLayout popupContent = new VerticalLayout();
		TextField issueField = new TextField("Ticket No.:");
		popupContent.addComponent(issueField);
		issueField.focus();

		Form form = new Form();
		Binder<Form> binder = new Binder<Form>();
		binder.setBean(form);
		binder.forField(issueField)
				.withConverter(new StringToIntegerConverter("Must be Integer"))
				.asRequired("Please enter a ticket no.")
				.bind(Form::getTicketNo, Form::setTicketNo);

		HorizontalLayout buttons = new HorizontalLayout();

		Button cancel = new Button("Cancel");
		buttons.addComponent(cancel);
		Button ok = new Button("OK");
		ok.setClickShortcut(KeyCode.ENTER);
		ok.addStyleName(ValoTheme.BUTTON_PRIMARY);
		buttons.addComponent(ok);

		popupContent.addComponent(buttons);

		final Window dialog = new Window("Select Ticket");
		dialog.setModal(true);
		UI.getCurrent().addWindow(dialog);
		dialog.setContent(popupContent);

		ok.addClickListener(l ->
			{
				dialog.close();
				findTicket(binder.getBean());
			});
		cancel.addClickListener(l -> dialog.close());

	}

	private void findTicket(Form form)
	{
		loadData(form.getTicketNo().toString());
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
					displayTicketSummary(ticketId);

					ActivityDao daoActivity = new ActivityDao();
					List<Activity> activities = daoActivity.getByTicket(ticket, true);

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

	private void displayTicketSummary(String ticketId)
	{
		daoTicket = new TicketDao();

		ticket = daoTicket.getById(Integer.valueOf(ticketId));
		Company company = new CompanyDao().getById(ticket.getCompanyId());

		this.ticketId.setValue("Ticket: " + ticket.getId());
		this.companyLabel.setValue("Company: " + company.getName());
		this.ticketStartDate.setValue("Open: " + LocalDateTimeHelper.format(ticket.getDateTimeStarted()));
		this.ticketContact.setValue("Contact: " + new TicketDao().getContact(ticket).getFullName());
		this.ticketStatus.setValue("Status: " + ticket.getStatus().getTitle());
		this.ticketPriority.setValue("Priority: " + ticket.getPriority());
		this.ticketResolution.setValue(ticket.getResolutionDetail());

		updateTotalBilledLabel();

		this.ticketSubject.setValue("<b>Subject: " + ticket.getTitle() + "</b>");
	}

	private void setBillable(ClickEvent e)
	{

		JobService.Job<Ticket> job = JobService.getInstance()
				.newJob("Mark Ticket as Non Billable: " + ticket.getId(), () ->
					{
						for (ActivityLine activityLine : activityLines)
						{
							Activity activity = activityLine.getActivity();
							if (activity.getNonBillable().getSeconds() > 0)
							{

								activity.setBillable(activity.getNonBillable().plus(activity.getBillable()));
								activity.setNonBillable(Duration.ofSeconds(0));

								Activity newActivity = new ActivityDao().replace(activity);

								activityLine.setActivity(newActivity);

								// The next line actually replaces the button
								this.activityProvider.refreshItem(activityLine);
							}

						}
						return ticket;
					});

		job.addCompleteListener(ticket ->
			{
				e.getButton().setEnabled(true);

				AcceloCache.getInstance().flushEntity(ticket, true);
				updateTotalBilledLabel();
			});

		job.submit();

	}

	private void setNonBillable(ClickEvent e)
	{

		JobService.Job<Ticket> job = JobService.getInstance()
				.newJob("Mark Ticket as Non Billable: " + ticket.getId(), () ->
					{
						for (ActivityLine activityLine : activityLines)
						{
							Activity activity = activityLine.getActivity();
							if (activity.getBillable().getSeconds() > 0)
							{
								activity.setNonBillable(activity.getNonBillable().plus(activity.getBillable()));
								activity.setBillable(Duration.ofSeconds(0));
								Activity newActivity = new ActivityDao().replace(activity);

								activityLine.setActivity(newActivity);

								// The next line actually replaces the button
								this.activityProvider.refreshItem(activityLine);
							}

						}
						return ticket;
					});

		job.addCompleteListener(ticket ->
			{
				e.getButton().setEnabled(true);

				AcceloCache.getInstance().flushEntity(ticket, true);
				updateTotalBilledLabel();
			});

		job.submit();

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

				AcceloCache.getInstance().flushEntity(ticket, true);
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

	/**
	 * Rounds billing up to the nearest 5 min block as per our standard contracts.
	 * 
	 * @param l
	 */
	private void roundBilling(ClickEvent l)
	{
		l.getButton().setEnabled(false);
		// calculate the current total billable.
		Duration totalBillable = this.activityLines.stream().map(ActivityLine::getBillable).reduce(Duration.ZERO,
				(lhs, rhs) -> lhs.plus(rhs));

		//
		long minutes = totalBillable.toMinutes();
		long rounded = minutes;

		// We only round up if they are more than 5 minutes into the next block.
		long excess = minutes % TicketDao.MIN_BILL_INTERVAL;
		if (minutes < TicketDao.MIN_BILL_INTERVAL || excess >= TicketDao.BILLING_LEWAY)
			rounded = (long) (Math.ceil(minutes / (float)TicketDao.MIN_BILL_INTERVAL) * TicketDao.MIN_BILL_INTERVAL);

		if (rounded != minutes)
		{
			JobService.Job<Activity> job = JobService.getInstance()
					.newJob("Set Minimum Billable " + ticket.getId(), () -> new TicketDao().roundBilling(ticket, TicketDao.MIN_BILL_INTERVAL, TicketDao.BILLING_LEWAY),
							newActivity ->
								{
									this.activityLines.add(new ActivityLine(newActivity));
									this.activityProvider.refreshAll();
									updateTotalBilledLabel();
									l.getButton().setEnabled(true);
								});

			;
			job.submit();

		}
		else
			l.getButton().setEnabled(true);
	}

	private void updateTotalBilledLabel()
	{
		this.ticketTotalBilled.setValue("Total Billed: " + Format.format(this.daoTicket.getBillable(this.ticket)));

	}

	private Component createTicketDetails()
	{
		VerticalLayout vLayout = new VerticalLayout();
		vLayout.setSpacing(false);
		vLayout.setMargin(new MarginInfo(false, true));

		HorizontalLayout layout = new HorizontalLayout();
		layout.addComponent(ticketId);
		layout.addComponent(companyLabel);
		layout.addComponent(ticketContact);
		layout.addComponent(ticketStartDate);
		layout.addComponent(ticketStatus);
		layout.addComponent(ticketPriority);
		layout.addComponent(ticketTotalBilled);
		vLayout.addComponent(layout);
		vLayout.addComponent(ticketSubject);
		ticketSubject.setContentMode(ContentMode.HTML);
		ticketSubject.setWidth("100%");

		ticketResolution.setReadOnly(true);
		ticketResolution.setWordWrap(true);
		ticketResolution.setWidth("100%");
		ticketResolution.setHeight(6, Unit.EM);

		vLayout.addComponent(ticketResolution);
		return vLayout;
	}

	private void showActivity(ActivityLine l)
	{
		if (l != null)
		{
			activityViewSubject.setValue("<b>Activity: " + l.getSubject() + "</b>");
			activityViewDetails.setValue(reduceLines(l.getBody()));
		}
		else
		{
			activityViewSubject.setValue("<b>Activity: None selected</b>");
			activityViewDetails.setValue("");
		}

	}

	private String reduceLines(String body)
	{
		body = body.replaceAll("\r\n+", "\n");
		body = body.replaceAll("\n\n+", "\n");
		return body;
	}

	private void onEdit()
	{
		URL editURL = new TicketDao().getEditURL("noojee.accelo.com", ticket);

		Page.getCurrent().open(editURL.toExternalForm(),
				"_accelo", true);
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
