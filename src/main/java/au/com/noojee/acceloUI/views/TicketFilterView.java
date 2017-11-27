package au.com.noojee.acceloUI.views;

import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MultiSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.LocalDateRenderer;

import au.com.noojee.acceloUI.util.SMNotification;
import au.com.noojee.acceloUI.views.ticketFilters.ErrorFilter;
import au.com.noojee.acceloUI.views.ticketFilters.NoContractFilter;
import au.com.noojee.acceloUI.views.ticketFilters.TicketFilter;
import au.com.noojee.acceloUI.views.ticketFilters.UnapprovedTicketFilter;
import au.com.noojee.acceloapi.AcceloException;
import au.com.noojee.acceloapi.cache.AcceloCache;
import au.com.noojee.acceloapi.dao.TicketDao;
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
				HorizontalLayout filtersLayout = new HorizontalLayout();
				this.addComponent(filtersLayout);

				initialiseGrid();

				List<TicketFilter> filters = Arrays.asList(new UnapprovedTicketFilter(), new NoContractFilter(),
						new ErrorFilter());
				filtersBox = new ComboBox<>();
				filtersBox.setWidth("200");
				filtersBox.setDataProvider(new ListDataProvider<TicketFilter>(filters));
				filtersBox.setItemCaptionGenerator(TicketFilter::getName);
				filtersBox.addValueChangeListener(l -> loadTickets(l.getValue(), false));
				filtersBox.setSelectedItem(filters.get(1));
				filtersBox.setEmptySelectionAllowed(false);

				Button refreshButton = new Button("Refresh");
				refreshButton.addClickListener(l -> refreshList());
				filtersLayout.addComponent(refreshButton);
				filtersLayout.addComponent(filtersBox, 0);

				HorizontalLayout buttons = new HorizontalLayout();
				// buttons.setWidth("100%");
				this.addComponent(buttons);

				Button delete = new Button("Delete");
				delete.addClickListener(l -> deleteTickets());
				buttons.addComponent(delete);

				Button editTicket = new Button("Edit Ticket(s)");
				editTicket.addClickListener(l -> editTicket());
				buttons.addComponent(editTicket);

				initialised = true;
			}

		}
		catch (AcceloException e)
		{
			logger.error(e, e);
		}

		return this;
	}

	private void refreshList()
	{
		Optional<TicketFilter> item = filtersBox.getSelectedItem();
		item.ifPresent(filter -> loadTickets(filter, true));
		ticketProvider.refreshAll();
	}

	private void deleteTickets()
	{
		MultiSelect<TicketLine> selections = this.grid.asMultiSelect();
		selections.getValue().parallelStream().forEach(line ->
			{

				new TicketDao().delete(line.getTicket());
				this.ticketLines.remove(line);
			});

		// remove all selections as we have just deleted them.
		selections.deselectAll();

		this.ticketProvider.refreshAll();
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
			heading = new Label();
			heading.setContentMode(ContentMode.HTML);

			setHeading("Tickets Filters");

			this.addComponent(heading);

			grid = new Grid<>();
			grid.setSelectionMode(Grid.SelectionMode.MULTI);

			ticketProvider = new ListDataProvider<>(ticketLines);
			this.grid.setDataProvider(ticketProvider);

			grid.setSizeFull();
			grid.addColumn(TicketLine::getId).setCaption("No.").setWidth(80);
			grid.addColumn(TicketLine::getCompanyName).setCaption("Company").setWidth(200);
			grid.addColumn(TicketLine::getTitle).setCaption("Title").setExpandRatio(1);
			grid.addColumn(TicketLine::getDateStarted, new LocalDateRenderer("dd/MM/yyyy")).setCaption("Created")
					.setWidth(120);
			grid.addColumn(TicketLine::getDateLastInteracted, new LocalDateRenderer("dd/MM/yyyy")).setCaption("Updated")
					.setWidth(120);
			grid.addColumn(TicketLine::getAssignee).setCaption("Engineer").setWidth(120);

			grid.addComponentColumn(ticketLine -> new IconButton("View Activities", VaadinIcons.SEARCH, e ->
				{
					UI.getCurrent().getNavigator().navigateTo(ActivityView.VIEW_NAME + "/" + ticketLine.getId());
				})).setWidth(80).setCaption("Details");

			// grid.addComponentColumn(ticketLine -> new Button("Refresh", e -> ticketLine.refresh()));

			this.addComponent(grid);
			this.setExpandRatio(grid, 1);

			HorizontalLayout bottomLine = new HorizontalLayout();
			bottomLine.setWidth("100%");
			this.addComponent(bottomLine);

			loading = new Label("Loading Contracts ...");
			bottomLine.addComponent(loading);
			bottomLine.setComponentAlignment(loading, Alignment.MIDDLE_LEFT);

			Button flush = new Button("Flush Cache");
			bottomLine.addComponent(flush);
			bottomLine.setComponentAlignment(flush, Alignment.MIDDLE_RIGHT);
			flush.addClickListener(l -> flushCache());
		}

	}

	private void loadTickets(TicketFilter ticketFilter, boolean refresh) throws AcceloException
	{

		logger.error("Start fetch Tickets");
		updateLoading("Loading...");

		ticketLines.clear();
		ticketProvider.refreshAll();
		grid.getSelectionModel().deselectAll();

		heading.setValue(ticketFilter.getName());

		new Thread(() ->
			{

				this.loadCount = 0;
				try
				{
					List<Ticket> tickets = ticketFilter.getTickets(refresh);

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
							lines.stream().forEach(line -> grid.getSelectionModel().select(line));
							updateLoading("Load Complete");
						});

				}
				catch (NumberFormatException | AcceloException e)
				{
					logger.error(e, e);
				}
			}).start();

	}

	private void editTicket()
	{
		try
		{
			MultiSelect<TicketLine> selections = this.grid.asMultiSelect();

			Optional<TicketFilter> ticketFilter = filtersBox.getSelectedItem();

			if (!ticketFilter.isPresent())
				SMNotification.show("Please select a filter first.");
			else
			{
				
				int pageCount = 0;
				Set<TicketLine> list = selections.getValue();

				for (TicketLine ticketLine : list)
				{
					try
					{
						String action = ticketFilter.get().buildURL(ticketLine.ticket);

						URL acceloApprovalPage;
						acceloApprovalPage = new URL("https", "noojee.accelo.com", 443, action);
						Page.getCurrent().open(acceloApprovalPage.toExternalForm(),
								"_accelo" + (pageCount == 0 ? "" : pageCount), true);

						pageCount++;
					}
					catch (Throwable e)
					{
						SMNotification.show("Error opening Accelo", e.getMessage(), Notification.Type.ERROR_MESSAGE);
					}

				}
			}

		}
		catch (Exception e)
		{
			SMNotification.show("Error", e.getMessage(), Notification.Type.ERROR_MESSAGE);
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
