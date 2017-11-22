package au.com.noojee.acceloUI.views;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.LocalDateRenderer;

import au.com.noojee.acceloapi.AcceloException;
import au.com.noojee.acceloapi.cache.AcceloCache;
import au.com.noojee.acceloapi.dao.TicketDao;
import au.com.noojee.acceloapi.entities.Ticket;
import au.com.noojee.acceloapi.entities.meta.Ticket_;
import au.com.noojee.acceloapi.filter.AcceloFilter;

/**
 * Show all companies with a retainer.
 * 
 * @author bsutton
 *
 */
public class UnassignedTicketView extends VerticalLayout implements View
{
	private static final long serialVersionUID = 1L;

	public static final String VIEW_NAME = "Unassigned Ticket";

	static Logger logger = LogManager.getLogger();

	Grid<TicketLine> grid;
	ListDataProvider<TicketLine> ticketProvider;
	private List<TicketLine> ticketLines = new ArrayList<>();

	private Label loading;

	private UI ui;

	private int loadCount = 0;

	public UnassignedTicketView()
	{
		this.ui = UI.getCurrent();

	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		View.super.enter(event);

		try
		{
			initialiseGrid();
			loadTickets();
		}
		catch (AcceloException e)
		{
			logger.error(e, e);
		}
	}

	private void loadTickets() throws AcceloException
	{

		logger.error("Start fetch Tickets");

		new Thread(() -> {

			this.loadCount = 0;
			ticketLines.clear();
			updateLoading("Loading");

			try
			{
				List<Ticket> tickets = getTickets();

				List<TicketLine> lines = tickets.parallelStream().map(t -> {
					updateLoading("Loading " + this.loadCount++);
					TicketLine l = new TicketLine(t);
					return l;
				}).collect(Collectors.toList());

				ticketLines.addAll(lines.stream().sorted().collect(Collectors.toList()));

				ui.access(() -> {
					ticketProvider.refreshAll();
				});
				updateLoading("Load Complete");

			}
			catch (NumberFormatException | AcceloException e)
			{
				logger.error(e, e);
			}
		}).start();

	}

	private List<Ticket> getTickets() throws AcceloException
	{
		// get all unassigned tickets
		//LocalDate lastMonth = now.minusMonths(1).withDayOfMonth(1);

		AcceloFilter<Ticket> filter = new AcceloFilter<>();
		filter.limit(1);

		// Add all tickets which belong to the company but haven't been
		// assigned.
		filter.where(filter.eq(Ticket_.contract, 0).and(filter.after(Ticket_.date_started, LocalDate.of(2017, 03, 01))));

		List<Ticket> unassignedTickets = new TicketDao().getByFilter(filter);

		return unassignedTickets;
	}

	

	void updateLoading(String message)
	{
		ui.access(() -> {
			loading.setValue(message);
		});
	}

	void initialiseGrid() throws AcceloException
	{
		if (grid == null)
		{
			this.setMargin(true);
			this.setSpacing(true);
			this.setSizeFull();
			Label heading = new Label("<H2><b>UnAssigned Tickets</b></H2>");
			heading.setContentMode(ContentMode.HTML);
			this.addComponent(heading);

			grid = new Grid<>();

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
			// grid.addColumn(TicketLine::getContact).setCaption("Contact").setWidth(160);
//
//			grid.addComponentColumn(ticketLine -> {
//				IconButton link = new IconButton("Attach To Contract", VaadinIcons.LINK,
//						e -> attachToContract(ticketLine));
//				link.setEnabled(!ticketLine.isAttached());
//				if (!ticketLine.isAttached())
//				{
//					link.setStyleName(ValoTheme.BUTTON_FRIENDLY);
//				}
//				return link;
//			}).setWidth(80).setCaption("Link");

			grid.addComponentColumn(ticketLine -> new IconButton("View Activities", VaadinIcons.SEARCH, e -> {
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

	private void flushCache()
	{
		AcceloCache.getInstance().flushCache();
		/*
		 * // Flush the tickets and activities current on display.
		 * 
		 * for ( TicketLine line : ticketLines) { Ticket ticket = line.ticket;
		 * 
		 * AcceloCache.getInstance().flushEntities(new
		 * TicketDao().getActivities(ticket));
		 * AcceloCache.getInstance().flushEntity(ticket); }
		 */
		ticketProvider.refreshAll();
	}

	
//	private void attachToContract(TicketLine ticketLine)
//	{
//		try
//		{
//			Ticket ticket = ticketLine.ticket;
//			ticket.setContractId(this.currentContractId);
//			new TicketDao().update(ticket);
//			this.ticketProvider.refreshItem(ticketLine);
//		}
//		catch (AcceloException e)
//		{
//			Notification.show("Failed to update ticket", e.getMessage(), Notification.Type.ERROR_MESSAGE);
//		}
//	}

	String formatDuration(Duration duration)
	{

		return (duration == null ? "" : DurationFormatUtils.formatDuration(duration.toMillis(), "H:mm"));
	}

}
