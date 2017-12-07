package au.com.noojee.acceloUI.views;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.DateField;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MultiSelect;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.LocalDateRenderer;

import au.com.noojee.acceloUI.util.SMNotification;
import au.com.noojee.acceloUI.views.ticketFilters.BillingAdjustmentRequired;
import au.com.noojee.acceloUI.views.ticketFilters.ErrorFilter;
import au.com.noojee.acceloUI.views.ticketFilters.NoContractAscFilter;
import au.com.noojee.acceloUI.views.ticketFilters.NoContractDescFilter;
import au.com.noojee.acceloUI.views.ticketFilters.TicketFilter;
import au.com.noojee.acceloUI.views.ticketFilters.UnapprovedTicketFilter;
import au.com.noojee.acceloapi.AcceloException;
import au.com.noojee.acceloapi.cache.AcceloCache;
import au.com.noojee.acceloapi.dao.TicketDao;
import au.com.noojee.acceloapi.entities.Priority;
import au.com.noojee.acceloapi.entities.Ticket;

/**
 * Show all companies with a retainer.
 * 
 * @author bsutton
 */
public class TicketFilterView extends VerticalLayout implements View
{
	private static final long serialVersionUID = 1L;

	public static final String VIEW_NAME = "Ticket Filters";

	static Logger logger = LogManager.getLogger();

	Grid<TicketLine> grid;
	ListDataProvider<TicketLine> ticketProvider;
	private List<TicketLine> ticketLines = new ArrayList<>();

	private Label loading;

	private UI ui;

	private int loadCount = 0;

	private Label heading;

	private boolean initialised = false;

	private ComboBox<TicketFilter> filtersBox;

	private CheckBox refreshList;

	private DateField cutoffDate;

	public TicketFilterView()
	{
		this.ui = UI.getCurrent();

	}

	@Override
	public Component getViewComponent()
	{
		try
		{
			if (!initialised)
			{
				layoutHeader();

				initialiseGrid();

				layoutFooter();

				initialised = true;
			}

		}
		catch (AcceloException e)
		{
			logger.error(e, e);
		}

		return this;
	}

	private void layoutFooter()
	{

		VerticalLayout footer = new VerticalLayout();
		this.addComponent(footer);
		
		HorizontalLayout buttonLine = new HorizontalLayout();
		footer.addComponent(buttonLine);
		buttonLine.setWidth("100%");
		

		Button delete = new Button("Delete");
		delete.addClickListener(l -> deleteTickets());
		buttonLine.addComponent(delete);
		buttonLine.setComponentAlignment(delete, Alignment.TOP_LEFT);

		Button editTicket = new Button("Edit Ticket(s)");
		editTicket.addClickListener(l -> editSelectedTickets());
		buttonLine.addComponent(editTicket);
		buttonLine.setComponentAlignment(editTicket, Alignment.TOP_LEFT);

		Button approveTicket = new Button("Approve Ticket(s)");
		approveTicket.addClickListener(l -> approveSelectedTickets());
		buttonLine.addComponent(approveTicket);
		buttonLine.setComponentAlignment(approveTicket, Alignment.TOP_LEFT);

		Button roundBillingButton = new Button("Round Billing Data");
		buttonLine.addComponent(roundBillingButton);
		roundBillingButton.addClickListener(l -> roundBilling(cutoffDate.getValue()));
		buttonLine.setComponentAlignment(roundBillingButton, Alignment.TOP_LEFT);

		Button flush = new Button("Flush Cache");
		buttonLine.addComponent(flush);
		buttonLine.setComponentAlignment(flush, Alignment.TOP_RIGHT);
		flush.addClickListener(l -> flushCache());

		HorizontalLayout dateLine = new HorizontalLayout();
		footer.addComponent(dateLine);
		dateLine.setWidth("100%");
		loading = new Label("Select a Filter and click Load");
		dateLine.addComponent(loading);
		
		Label dateLabel = new Label("Cutoff Date");
		dateLine.addComponent(dateLabel);
		dateLine.setComponentAlignment(dateLabel, Alignment.MIDDLE_RIGHT);
		cutoffDate = new DateField();
		dateLine.addComponent(cutoffDate);
		cutoffDate.setDateFormat("dd/MMM/yy");
		LocalDate lastMonth = LocalDate.now().minusMonths(1).withDayOfMonth(1);
		cutoffDate.setValue(lastMonth);
		dateLine.setComponentAlignment(cutoffDate, Alignment.MIDDLE_RIGHT);
		
		

	}

	private void layoutHeader()
	{
		HorizontalLayout headerLayout = new HorizontalLayout();
		this.addComponent(headerLayout);

		List<TicketFilter> filters = Arrays.asList(new UnapprovedTicketFilter(), new NoContractAscFilter(),
				new NoContractDescFilter(),
				new ErrorFilter(), new BillingAdjustmentRequired());
		filtersBox = new ComboBox<>();
		filtersBox.setWidth("200");
		filtersBox.setDataProvider(new ListDataProvider<TicketFilter>(filters));
		filtersBox.setItemCaptionGenerator(TicketFilter::getName);
		// filtersBox.addValueChangeListener(l -> loadTickets(l.getValue(), false));
		// filtersBox.setSelectedItem(filters.get(1));
		filtersBox.setEmptySelectionAllowed(true);

		Button loadButton = new Button("Load");
		loadButton.addClickListener(l -> loadList());
		headerLayout.addComponent(loadButton);
		refreshList = new CheckBox("Refresh Cache");
		headerLayout.addComponent(refreshList);
		headerLayout.addComponent(filtersBox, 0);

		heading = new Label();
		heading.setContentMode(ContentMode.HTML);
		setHeading("Tickets Filters");

		headerLayout.addComponent(heading);

	}

	private void loadList()
	{
		
		Optional<TicketFilter> item = filtersBox.getSelectedItem();
		boolean refreshList = this.refreshList.getValue();
		item.ifPresent(filter -> loadTickets(filter, cutoffDate, refreshList));
		ticketProvider.refreshAll();
	}

	private void deleteTickets()
	{
		MultiSelect<TicketLine> selections = this.grid.asMultiSelect();

		ConfirmDialog.show(UI.getCurrent(), "Please Confirm:", "Are you really sure?",
				"Yes Delete it!", "No, Save me.", new ConfirmDialog.Listener()
				{
					private static final long serialVersionUID = 1L;

					public void onClose(ConfirmDialog dialog)
					{
						if (dialog.isConfirmed())
						{
							// Confirmed to continue
							selections.getValue().parallelStream().forEach(line ->
								{
									new TicketDao().delete(line.getTicket());
									TicketFilterView.this.ticketLines.remove(line);
								});

							// remove all selections as we have just deleted them.
							selections.deselectAll();

							TicketFilterView.this.ticketProvider.refreshAll();
						}
					}
				});
	}

	void updateLoading(String message)
	{
		ui.access(() ->
			{
				loading.setValue(message);
			});
	}

	private void setHeading(String heading)
	{
		this.heading.setValue("<H2><b>" + heading + "</b></H2>");
	}

	void initialiseGrid() throws AcceloException
	{
		if (grid == null)
		{
			this.setMargin(true);
			this.setSpacing(true);
			this.setSizeFull();

			grid = new Grid<>();
			grid.setSelectionMode(Grid.SelectionMode.MULTI);

			ticketProvider = new ListDataProvider<>(ticketLines);
			this.grid.setDataProvider(ticketProvider);

			grid.setSizeFull();
			grid.addComponentColumn(ticketLine -> new IconButton("View Activities", VaadinIcons.SEARCH, e ->
				{
					UI.getCurrent().getNavigator().navigateTo(ActivityView.VIEW_NAME + "/" + ticketLine.getId());
				})).setWidth(80).setCaption("Details");

			grid.addColumn(TicketLine::getId).setCaption("No.").setWidth(80);
			grid.addColumn(TicketLine::getCompanyName).setCaption("Company").setWidth(200);
			grid.addColumn(TicketLine::getDateStarted, new LocalDateRenderer("dd/MM/yyyy")).setCaption("Created")
					.setWidth(120);
			grid.addColumn(TicketLine::getDateLastInteracted, new LocalDateRenderer("dd/MM/yyyy")).setCaption("Updated")
					.setWidth(120);
			grid.addColumn(TicketLine::getAssignee).setCaption("Engineer").setWidth(120);
			grid.addColumn(TicketLine::getPriority).setCaption("Priority").setWidth(100);

			// grid.addComponentColumn(ticketLine -> new IconButton("Change Priority", VaadinIcons.CHEVRON_CIRCLE_UP, e
			// ->
			// {
			// selectPriority(ticketLine.ticket);
			// })).setWidth(80).setCaption("");

			grid.addColumn(TicketLine::getTitle).setCaption("Title").setExpandRatio(1);

			// grid.addComponentColumn(ticketLine -> new Button("Refresh", e -> ticketLine.refresh()));

			this.addComponent(grid);
			this.setExpandRatio(grid, 1);

		}

	}

	// private void selectPriority(Ticket ticket)
	// {
	// VerticalLayout popupContent = new VerticalLayout();
	//
	// ListSelect<Priority.NoojeePriority> selectList = new ListSelect<>();
	// selectList.setDataProvider(new ListDataProvider<>(Arrays.asList(Priority.NoojeePriority.values())));
	// popupContent.addComponent(selectList);
	//
	// HorizontalLayout buttons = new HorizontalLayout();
	//
	// Button ok = new Button("OK");
	// buttons.addComponent(ok);
	// Button cancel = new Button("Cancel");
	// buttons.addComponent(cancel);
	//
	// popupContent.addComponent(buttons);
	//
	// final Window dialog = new Window("Select Priority");
	// dialog.setModal(true);
	// ui.addWindow(dialog);
	// dialog.setContent(popupContent);
	//
	// ok.addClickListener(l -> { updatePriority(dialog, ticket, selectList.getValue()); });
	// cancel.addClickListener(l -> dialog.close());
	//
	// }

	// private void updatePriority(Window dialog, Ticket ticket, Set<NoojeePriority> prioritySet)
	// {
	//
	// if (prioritySet.size() != 1)
	// SMNotification.show("Invalid Selection", "Please select one and only one Priority",Type.ERROR_MESSAGE);
	// else
	// {
	// TicketDao daoTicket = new TicketDao();
	//
	// ticket.setPriority(prioritySet.stream().findFirst().get());
	// daoTicket.update(ticket);
	// dialog.close();
	// }
	//
	// }

	private void loadTickets(TicketFilter ticketFilter, DateField cutoffDate, boolean refresh) throws AcceloException
	{

		logger.error("Start fetch Tickets");
		updateLoading("Loading " + ticketFilter.getName() + "... ");

		ticketLines.clear();
		ticketProvider.refreshAll();
		grid.getSelectionModel().deselectAll();

		new Thread(() ->
			{
				this.loadCount = 0;
				try
				{
					List<Ticket> tickets = ticketFilter.getTickets(cutoffDate.getValue(), refresh);

					List<TicketLine> lines = tickets.parallelStream().map(t ->
						{
							updateLoading("Loading " + this.loadCount++);
							TicketLine l = new TicketLine(t);
							return l;
						}).collect(Collectors.toList());

					ticketLines.addAll(lines.stream().sorted((t1, t2) -> Long.compare(t2.getId(), t1.getId()))
							.collect(Collectors.toList()));

					ui.access(() ->
						{
							ticketProvider.refreshAll();
							// lines.stream().forEach(line -> grid.getSelectionModel().select(line));
							updateLoading("Load Complete");
						});

				}
				catch (NumberFormatException | AcceloException e)
				{
					logger.error(e, e);
				}
			}).start();

	}

	private void editSelectedTickets()
	{
		openPage(this::openEditPage);
	}

	private void approveSelectedTickets()
	{
		openPage(this::openApprovePage);
	}

	// void openPage(Predicate<TicketLine> pageOpener)
	void openPage(BiFunction<TicketLine, Integer, Void> pageOpener)
	{
		try
		{
			List<TicketLine> selected = getSelectedTickets();

			if (selected == null)
				SMNotification.show("Please select a filter first.");
			else
			{
				int pageCount = 0;

				for (TicketLine ticketLine : selected)
				{
					try
					{
						pageOpener.apply(ticketLine, pageCount);
						pageCount++;
					}
					catch (Throwable e)
					{
						SMNotification.show("Error opening Accelo", e.getMessage(), SMNotification.Type.ERROR_MESSAGE);
					}

				}
			}

		}
		catch (Exception e)
		{
			SMNotification.show("Error", e.getMessage(), SMNotification.Type.ERROR_MESSAGE);
		}
	}

	Void openEditPage(TicketLine ticketLine, int pageCount)
	{
		URL acceloApprovalPage = new TicketDao().getEditURL("noojee.accelo.com", ticketLine.ticket);

		Page.getCurrent().open(acceloApprovalPage.toExternalForm(),
				"_accelo" + (pageCount == 0 ? "" : pageCount), true);
		
		return null;

	}

	Void openApprovePage(TicketLine ticketLine, int pageCount)
	{
			
		URL acceloApprovalPage = new TicketDao().getApproveURL("noojee.accelo.com", ticketLine.ticket);

		Page.getCurrent().open(acceloApprovalPage.toExternalForm(),
				"_accelo" + (pageCount == 0 ? "" : pageCount), true);

		return null;


	}

	private List<TicketLine> getSelectedTickets()
	{
		List<TicketLine> ticketLines = null;
		MultiSelect<TicketLine> selections = this.grid.asMultiSelect();
		Optional<TicketFilter> ticketFilter = filtersBox.getSelectedItem();

		if (ticketFilter.isPresent())
			ticketLines = new ArrayList<>(selections.getValue());

		return ticketLines;
	}

	private void roundBilling(LocalDate cutoffDate)
	{
		TicketFilter filter = this.filtersBox.getValue();
		if (filter == null)
			SMNotification.show("Select a filter first.");
		else
		{
			List<Ticket> tickets = filter.getTickets(cutoffDate, false);
			AtomicInteger progressCount = new AtomicInteger(0);

			ConfirmDialog.show(ui, "Round Billing",
					"Clicking Run will round billing data up to the next 15 min block (60 min for Critical and Urgent) for all tickets of type "
							+ filter.getName(),
					"Run", "Cancel", () ->
						{
							tickets.stream()
									.forEach(ticket ->
										{
											BillingAdjustmentRequired.roundBilling(ticket);
											int count = progressCount.incrementAndGet();
											updateLoading("Processed " + count + " tickets of " + tickets.size());
										});
						});
		}
	}

	private void flushCache()
	{
		AcceloCache.getInstance().flushCache();
		/*
		 * // Flush the tickets and activities current on display. for ( TicketLine line : ticketLines) { Ticket ticket
		 * = line.ticket; AcceloCache.getInstance().flushEntities(new TicketDao().getActivities(ticket));
		 * AcceloCache.getInstance().flushEntity(ticket); }
		 */
		ticketProvider.refreshAll();
	}

	// private void attachToContract(TicketLine ticketLine)
	// {
	// try
	// {
	// Ticket ticket = ticketLine.ticket;
	// ticket.setContractId(this.currentContractId);
	// new TicketDao().update(ticket);
	// this.ticketProvider.refreshItem(ticketLine);
	// }
	// catch (AcceloException e)
	// {
	// Notification.show("Failed to update ticket", e.getMessage(), Notification.Type.ERROR_MESSAGE);
	// }
	// }

	String formatDuration(Duration duration)
	{

		return (duration == null ? "" : DurationFormatUtils.formatDuration(duration.toMillis(), "H:mm"));
	}

}
