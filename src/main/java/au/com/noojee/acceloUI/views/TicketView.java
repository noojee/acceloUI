package au.com.noojee.acceloUI.views;

import java.net.URL;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.vaadin.dialogs.ConfirmDialog;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
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
import au.com.noojee.acceloUI.views.TicketView.ParamArg.ArgType;
import au.com.noojee.acceloapi.AcceloException;
import au.com.noojee.acceloapi.cache.AcceloCache;
import au.com.noojee.acceloapi.dao.CompanyDao;
import au.com.noojee.acceloapi.dao.ContractDao;
import au.com.noojee.acceloapi.dao.TicketDao;
import au.com.noojee.acceloapi.entities.Company;
import au.com.noojee.acceloapi.entities.Contract;
import au.com.noojee.acceloapi.entities.Ticket;
import au.com.noojee.acceloapi.entities.meta.Ticket_;
import au.com.noojee.acceloapi.entities.types.AgainstType;
import au.com.noojee.acceloapi.filter.AcceloFilter;
import au.com.noojee.acceloapi.util.Formatters;

/**
 * Show all companies with a retainer.
 * 
 * @author bsutton
 */
public class TicketView extends VerticalLayout implements View
{
	private static final long serialVersionUID = 1L;

	public static final String VIEW_NAME = "Ticket";

	static Logger logger = LogManager.getLogger();

	Grid<TicketLine> grid;
	ListDataProvider<TicketLine> ticketProvider;
	private List<TicketLine> ticketLines = new ArrayList<>();

	private Label loading;
	private Label companyName;
	private Label contractTitle;
	private Label contractStartDate;
	private Label contractEndDate;
	private Label contractValue;
	private Label contractRemaining;

	private ParamArg currentPArg = null;

	private UI ui;

	private int loadCount = 0;

	public TicketView()
	{
		this.ui = UI.getCurrent();

	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		View.super.enter(event);

		String contractId = event.getParameters();

		try
		{
			initialiseGrid();
			loadTickets(contractId);
		}
		catch (AcceloException e)
		{
			logger.error(e, e);
		}
	}

	private void loadTickets(String urlParam) throws AcceloException
	{
		MultiSelect<TicketLine> selections = this.grid.asMultiSelect();
		selections.deselectAll();
		
		ParamArg pArg = validParamId(urlParam);

		// Do we need to load data
		if (pArg != null && pArg != currentPArg)
		{
			currentPArg = pArg;

			Contract contract = new ContractDao().getById(pArg.id);
			if (pArg.type == ArgType.contract)
				currentPArg.id = contract.getCompanyId();

			Company company = new CompanyDao().getById(currentPArg.id);

			companyName.setValue("Company: " + company.getName());
			if (contract != null)
			{
				contractTitle.setValue("Contract: " + contract.getTitle());
				contractStartDate.setValue("Start: " + Formatters.format(contract.getDateStarted()));
				contractEndDate.setValue("Expires: " + Formatters.format(contract.getDateExpires()));
				contractValue.setValue("Value: " + Formatters.format(contract.getValue()));
				contractRemaining.setValue("Remaining: " + Formatters.format(contract.getRemainingValue()));
			}

			logger.error("Start fetch Tickets");

			ticketLoader(contract);

		}
		else
			updateLoading("No Data found.");

	}

	private void ticketLoader(Contract contract)
	{
		new Thread(() ->
			{
				this.loadCount = 0;
				ticketLines.clear();
				updateLoading("Loading");

				try
				{
					List<Ticket> tickets = getTickets(contract);

					List<TicketLine> lines = tickets.parallelStream().map(t ->
						{
							updateLoading("Loading " + this.loadCount++);
							TicketLine l = new TicketLine(t);
							// force some caching.
							l.getAssignee();
							l.getBillable();
							l.getNonBillable();
							l.isWorkApproved();
							l.getContact();
							l.isOpen();
							return l;
						}).collect(Collectors.toList());

					ticketLines.addAll(lines.stream().sorted().collect(Collectors.toList()));

					ui.access(() ->
						{
							ticketProvider.refreshAll();
						});
					updateLoading("Load Complete");

					loadWork();

				}
				catch (NumberFormatException | AcceloException e)
				{
					logger.error(e, e);
				}
			}).start();
	}

	static class ParamArg
	{
		enum ArgType
		{
			company, contract
		};

		ArgType type;
		int id;
		public boolean allTickets = true;
	}

	private ParamArg validParamId(String urlParam)
	{
		ParamArg parg = null;

		// Do we need to load data
		if (urlParam != null && !urlParam.isEmpty())
		{
			String[] args = urlParam.split("=");
			if (args.length >= 2)
			{
				parg = new ParamArg();
				parg.id = Integer.valueOf(args[1]);
				parg.type = ArgType.valueOf(args[0]);
				
				if (args.length == 3)
					parg.allTickets = false;
			}
		}
		else
			// if nothing was passed show the current data.
			parg = currentPArg;
		return parg;
	}

	private List<Ticket> getTickets(Contract contract) throws AcceloException
	{
		int companyId = this.currentPArg.id;
		
		// all tickets assigned to this contract
		// CONSIDER restricting the date range.
		List<Ticket> tickets = new ArrayList<>();
		if (contract != null && this.currentPArg.allTickets == true)
		{
			tickets.addAll(new TicketDao().getByContract(contract));
			companyId = contract.getCompanyId();
		}

		AcceloFilter<Ticket> filter = new AcceloFilter<>();

		// Add all tickets which belong to the company but haven't been
		// assigned.
		filter.where(filter.against(AgainstType.company, companyId).and(filter.eq(Ticket_.contract, 0))
				.and(filter.after(Ticket_.date_started, LocalDateTime.of(2017, 03, 01, 0, 0, 0))));

		List<Ticket> unassigned = new TicketDao().getByFilter(filter);
		tickets.addAll(unassigned);

		return tickets;
	}

	private void loadWork()
	{
		// background load the activity no.s
		new Thread(() ->
			{
				ticketLines.parallelStream().distinct().forEach(l -> new UIAccessor(this.ui, false, () ->
					{
						l.loadWork();
						new UIAccessor(this.ui, true, () ->
							{
								this.ticketProvider.refreshAll();
							}).run();

					}).run());

				new UIAccessor(this.ui, true, () ->
					{
						loading.setValue("Work Load Complete");
					}).run();

			}).start();

	}

	void updateLoading(String message)
	{
		ui.access(() ->
			{
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
			Label heading = new Label("<H2><b>Contract Tickets</b></H2>");
			heading.setContentMode(ContentMode.HTML);
			this.addComponent(heading);

			this.addComponent(createContractDetails());

			grid = new Grid<>();
			grid.setSelectionMode(Grid.SelectionMode.MULTI);


			ticketProvider = new ListDataProvider<>(ticketLines);
			this.grid.setDataProvider(ticketProvider);

			grid.setSizeFull();
			grid.addColumn(TicketLine::getId).setCaption("No.").setWidth(80);
			grid.addColumn(TicketLine::getTitle).setCaption("Title").setExpandRatio(1);
			grid.addColumn(TicketLine::getDateStarted, new LocalDateRenderer("dd/MM/yyyy")).setCaption("Created")
					.setWidth(120);
			grid.addColumn(TicketLine::getDateLastInteracted, new LocalDateRenderer("dd/MM/yyyy")).setCaption("Updated")
					.setWidth(120);
			grid.addColumn(TicketLine::getAssignee).setCaption("Engineer").setWidth(120);
			grid.addColumn(TicketLine::getContact).setCaption("Contact").setWidth(160);
			grid.addColumn(TicketLine::isOpen).setCaption("Open");
			grid.addColumn(TicketLine::isWorkApproved).setCaption("Approved");

			grid.addComponentColumn(ticketLine -> new IconButton("View Activities", VaadinIcons.SEARCH, e ->
				{
					UI.getCurrent().getNavigator().navigateTo(ActivityView.VIEW_NAME + "/" + ticketLine.getId());
				})).setWidth(80).setCaption("Details");

			grid.addColumn(ticketLine -> formatDuration(ticketLine.getBillable())).setCaption("Billable")
					.setStyleGenerator(ticketLine -> "align-right");

			grid.addColumn(ticketLine -> formatDuration(ticketLine.getNonBillable())).setCaption("Non Billable")
					.setStyleGenerator(ticketLine -> "align-right");

			grid.addComponentColumn(ticketLine -> new Button("Refresh", e -> ticketLine.refresh()));

			this.addComponent(grid);
			this.setExpandRatio(grid, 1);

			HorizontalLayout bottomLine = new HorizontalLayout();
			bottomLine.setWidth("100%");
			this.addComponent(bottomLine);

			loading = new Label("Loading Contracts ...");
			bottomLine.addComponent(loading);
			bottomLine.setComponentAlignment(loading, Alignment.MIDDLE_LEFT);
			
			Button delete = new Button("Delete");
			delete.addClickListener(l -> deleteTickets());
			bottomLine.addComponent(delete);

			Button editTicket = new Button("Edit Ticket(s)");
			editTicket.addClickListener(l -> editTicket());
			bottomLine.addComponent(editTicket);


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
		 * // Flush the tickets and activities current on display. for ( TicketLine line : ticketLines) { Ticket ticket
		 * = line.ticket; AcceloCache.getInstance().flushEntities(new TicketDao().getActivities(ticket));
		 * AcceloCache.getInstance().flushEntity(ticket); }
		 */
		ticketProvider.refreshAll();
	}

	private Component createContractDetails()
	{
		VerticalLayout layout = new VerticalLayout();
		layout.setSpacing(false);
		layout.setMargin(new MarginInfo(false, false));

		HorizontalLayout overviewLayout = new HorizontalLayout();
		overviewLayout.setMargin(new MarginInfo(false, false));

		layout.addComponent(overviewLayout);
		companyName = new Label();
		overviewLayout.addComponent(companyName);
		contractTitle = new Label();
		overviewLayout.addComponent(contractTitle);

		HorizontalLayout detailsLayout = new HorizontalLayout();
		detailsLayout.setMargin(new MarginInfo(false, false));

		layout.addComponent(detailsLayout);
		contractStartDate = new Label();
		detailsLayout.addComponent(contractStartDate);
		contractEndDate = new Label();
		detailsLayout.addComponent(contractEndDate);
		contractValue = new Label();
		detailsLayout.addComponent(contractValue);
		contractRemaining = new Label();
		detailsLayout.addComponent(contractRemaining);

		return layout;
	}

	// private void attachToContract(TicketLine ticketLine)
	// {
	// try
	// {
	// Ticket ticket = ticketLine.ticket;
	// ticket.setContractId(this.currentPArg.);
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
									TicketView.this.ticketLines.remove(line);
								});

							// remove all selections as we have just deleted them.
							selections.deselectAll();

							TicketView.this.ticketProvider.refreshAll();
						}
					}
				});
	}

	
	private void editTicket()
	{
		try
		{
			MultiSelect<TicketLine> selections = this.grid.asMultiSelect();

				int pageCount = 0;
				Set<TicketLine> list = selections.getValue();

				for (TicketLine ticketLine : list)
				{
					try
					{
						URL acceloApprovalPage = new TicketDao().getEditURL("noojee.accelo.com", ticketLine.getTicket());
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
		catch (Exception e)
		{
			SMNotification.show("Error", e.getMessage(), Notification.Type.ERROR_MESSAGE);
		}
	}


}
