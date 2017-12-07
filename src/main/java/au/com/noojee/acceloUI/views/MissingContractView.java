package au.com.noojee.acceloUI.views;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.shared.ui.grid.ColumnResizeMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import au.com.noojee.acceloapi.cache.AcceloCache;
import au.com.noojee.acceloapi.dao.CompanyDao;
import au.com.noojee.acceloapi.dao.TicketDao;
import au.com.noojee.acceloapi.entities.Company;
import au.com.noojee.acceloapi.entities.Ticket;
import au.com.noojee.acceloapi.entities.meta.Ticket_;
import au.com.noojee.acceloapi.filter.AcceloFilter;
import au.com.noojee.acceloapi.util.LocalDateTimeHelper;

/**
 * Show all companies that have been active in the last two months (i.e. a ticket raised) that don't have a contract.
 * 
 * @author bsutton
 */
public class MissingContractView extends VerticalLayout implements View // , Subscriber<ContractLine>
{
	private static final long serialVersionUID = 1L;

	public static final String VIEW_NAME = "Missing Contracts";

	static Logger logger = LogManager.getLogger();

	UI ui;

	Grid<MissingContractLine> grid;
	ListDataProvider<MissingContractLine> contractProvider;
	private List<MissingContractLine> contractLines;

	private Label loading;

	public MissingContractView()
	{
		this.ui = UI.getCurrent();
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		View.super.enter(event);

		initialiseGrid();
	}

	void initialiseGrid()
	{
		if (grid == null)
		{
			HorizontalLayout topLine = new HorizontalLayout();
			this.addComponent(topLine);
			topLine.addComponent(new Label("<H2>Missing Contracts</H2>", ContentMode.HTML));
			Button refresh = new Button("Refresh");
			refresh.addClickListener(l -> loadData(true));
			topLine.addComponent(refresh);

			this.setMargin(true);
			this.setSpacing(true);
			this.setSizeFull();

			loading = new Label("Loading...");
			this.addComponent(loading);

			grid = new Grid<>();

			contractLines = new ArrayList<>();
			contractProvider = new ListDataProvider<>(contractLines);
			this.grid.setDataProvider(contractProvider);

			grid.setSizeFull();
			grid.addColumn(MissingContractLine::getCompanyName).setCaption("Company").setExpandRatio(1);
			grid.addColumn(MissingContractLine::getFirstTicket).setCaption("First Ticket")
					.setWidth(120);
			grid.addColumn(MissingContractLine::getLastTicket).setCaption("Last Ticket")
					.setWidth(120);
			grid.addComponentColumn(contractLine -> new IconButton("Tickets", VaadinIcons.SEARCH,
					e -> UI.getCurrent().getNavigator()
							.navigateTo(TicketView.VIEW_NAME + "/company=" + contractLine.getCompany().getId()
									+ "=ignoreContractedTickets")))
					.setWidth(80)
					.setCaption("Tickets");

			this.addComponent(grid);
			this.setExpandRatio(grid, 1);

			grid.setColumnResizeMode(ColumnResizeMode.SIMPLE);

			loadData(false);

			logger.error("Finished.");
		}

	}

	private void loadData(boolean refresh)
	{
		// Find the list of tickets that are not assigned to a contract since the start of accelo
		AcceloFilter<Ticket> filter = new AcceloFilter<>();
		if (refresh)
			AcceloCache.getInstance().flushCache();
		logger.error("find tickets with no contract");
		filter.where(
				filter.eq(Ticket_.contract, 0)
						.and(filter.afterOrEq(Ticket_.date_submitted, LocalDateTime.of(2017, 3, 2, 0, 0))));
		List<Ticket> unassignedTickets = new TicketDao().getByFilter(filter);

		// dedup the list based on the company
		logger.error("Get list of companies for the tickets");
		List<Integer> companyIds = unassignedTickets.stream().map(ticket -> ticket.getCompanyId()).distinct()
				.collect(Collectors.toList());

		// List of companies that have tickets.
		logger.error("Generate company details");
		List<CompanyDetails> companies = companyIds.parallelStream()
				.map(id -> new CompanyDetails(id, unassignedTickets)).collect(Collectors.toList());

		// Find companies with no active filter.
		logger.error("Find companies with missing contracts");
		List<CompanyDetails> missingContracts = companies.parallelStream()
				.filter(c -> !c.hasActiveContract())
				.collect(Collectors.toList());

		// Create a missing contract line from each company details.
		logger.error("Create the contract lines");
		contractLines.clear();
		contractLines.addAll(missingContracts.stream()
				.map(cd -> cd.getContractLine())
				.collect(Collectors.toList()));

		this.contractProvider.refreshAll();

		loading.setValue("Load Complete.");

	}

	static class CompanyDetails
	{
		Company company;
		List<Ticket> tickets = new ArrayList<>();
		LocalDateTime firstTicket;
		LocalDateTime lastTicket;

		CompanyDetails(int companyId, List<Ticket> possibleTickets)
		{
			this.company = new CompanyDao().getById(companyId, true);

			// find tickets that belong to this company
			this.tickets = possibleTickets.parallelStream().filter(t -> t.getCompanyId() == company.getId())
					.collect(Collectors.toList());

			// determine first and last date.
			this.tickets.parallelStream()
					.forEach(t ->
						{
							firstTicket = LocalDateTimeHelper.Max(firstTicket, t.getDateTimeSubmitted());
							lastTicket = LocalDateTimeHelper.Min(lastTicket, t.getDateTimeSubmitted());
						});
		}

		public boolean hasActiveContract()
		{
			return new CompanyDao().hasActiveContract(company);
		}

		MissingContractLine getContractLine()
		{
			return new MissingContractLine(this);
		}

		public Company getCompany()
		{
			return company;
		}

		public String toString()
		{
			return this.company.getName() + " first: " + this.firstTicket + " last: " + this.lastTicket;
		}
	}

	/*
	 * @Override public void unassignedTicketsLoaded(ContractContainer contractContainer) { // Update the UI
	 * UI.getCurrent().access(() -> { CompanyView.logger.error(contractContainer.toString() + " Refresh UI starting.");
	 * this.contractProvider.refreshItem(contractContainer.getContractLine());
	 * CompanyView.logger.error(contractContainer.toString() + " Refesh UI complete"); }); }
	 * @Override public void ticketsLoaded(ContractContainer contractContainer) { // TODO Auto-generated method stub }
	 * @Override public void contractLoaded(ContractLine contractLine) { this.contractLines.add(contractLine);
	 * this.contractProvider.refreshItem(contractLine); }
	 */

	// @Override
	// public void onNext(ContractLine contractLine)
	// {
	// this.ui.access(() -> {
	// this.contractLines.add(contractLine);
	// this.contractProvider.refreshItem(contractLine);
	// this.contractProvider.refreshAll();
	// });
	//
	// }
	//
	// @Override
	// public void onComplete()
	// {
	// this.ui.access(() -> {
	// this.contractProvider.refreshAll();
	// loading.setValue("Load Complete.");
	// });
	// }
	//
	// @Override
	// public void onError(Exception e)
	// {
	// // TODO Auto-generated method stub
	//
	// }

	void updateLoading(String message)
	{
		ui.access(() ->
			{
				loading.setValue(message);
			});
	}

}
