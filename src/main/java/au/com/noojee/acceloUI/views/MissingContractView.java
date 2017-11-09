package au.com.noojee.acceloUI.views;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.grid.ColumnResizeMode;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import au.com.noojee.acceloapi.dao.TicketDao;
import au.com.noojee.acceloapi.entities.Ticket;
import au.com.noojee.acceloapi.entities.meta.Ticket_;
import au.com.noojee.acceloapi.filter.AcceloFilter;

/**
 * Show all companies that have been active in the last two months (i.e. a ticket raised) that don't have a contract.
 * 
 * @author bsutton
 *
 */
public class MissingContractView extends VerticalLayout implements View // , Subscriber<ContractLine>
{
	private static final long serialVersionUID = 1L;

	public static final String VIEW_NAME = "MissingContracts";

	static Logger logger = LogManager.getLogger();

	Grid<MissingContractLine> grid;
	ListDataProvider<MissingContractLine> contractProvider;
	private List<MissingContractLine> contractLines;

	private Label loading;

	public MissingContractView()
	{
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

			this.addComponent(grid);
			this.setExpandRatio(grid, 1);

			grid.setColumnResizeMode(ColumnResizeMode.SIMPLE);
			
			loadData();

			logger.error("Finished.");
		}

	}

	private void loadData()
	{
		LocalDate now = LocalDate.now();
		
		LocalDate lastMonth = now.minusMonths(1).withDayOfMonth(1);
				
		// Find the list of tickets that are not assigned to a contract in the last two months.
		AcceloFilter<Ticket> filter = new AcceloFilter<>();
		filter.where(filter.eq(Ticket_.contract, 0).and(filter.afterOrEq(Ticket_.date_submitted, lastMonth)));
		List<Ticket> unassignedTickets = new TicketDao().getByFilter(filter);
		
		// dedup the list based on the company
		// int[] companyIds = unassignedTickets.stream().mapToInt(ticket -> { return ticket.getCompanyId();} ).distinct().toArray();
		
		// create a list of tickets for each company
		Map<Integer, List<Ticket>> ticketMap = new HashMap<>();

		// build a map of tickets for each company.
		unassignedTickets.stream().forEach(ticket -> { 
			List<Ticket> tickets = ticketMap.get(ticket.getCompanyId());
			if (tickets == null)
				tickets = new ArrayList<>();
			tickets.add(ticket);
			ticketMap.put(ticket.getCompanyId(), tickets);
		} );
		
		
		// Now check that each of those companies has an active contract.
	//	for (int companyId : companyIds)
		{
			// find the first/last date for the tickets we have
			
			filter = new AcceloFilter<>();
	//		filter.where(new Eq("company", companyId).and(new Before("start_date", )));
			
			// List<Contract> companies = new ContractDao().getByFilter(filter);
			
		}
		
		
//		contractLines = contract.stream().map(e -> {
//			return e.getContractLine();
//		}).collect(Collectors.toList());
//		
//		
//		AcceloFilter filter = new AcceloFilter();
//		filter.where(new Eq("contract", 0).and(new Eq("company", this.contract.getCompanyId())));
//		List<Ticket> unassignedTickets = new TicketDao().getByFilter(filter);
//		
	

		
	}

	/*
	 * @Override public void unassignedTicketsLoaded(ContractContainer
	 * contractContainer) { // Update the UI UI.getCurrent().access(() -> {
	 * CompanyView.logger.error(contractContainer.toString() +
	 * " Refresh UI starting.");
	 * this.contractProvider.refreshItem(contractContainer.getContractLine());
	 * CompanyView.logger.error(contractContainer.toString() +
	 * " Refesh UI complete"); });
	 * 
	 * }
	 * 
	 * @Override public void ticketsLoaded(ContractContainer contractContainer)
	 * { // TODO Auto-generated method stub
	 * 
	 * }
	 * 
	 * @Override public void contractLoaded(ContractLine contractLine) {
	 * this.contractLines.add(contractLine);
	 * this.contractProvider.refreshItem(contractLine);
	 * 
	 * }
	 */

//	@Override
//	public void onNext(ContractLine contractLine)
//	{
//		this.ui.access(() -> {
//			this.contractLines.add(contractLine);
//			this.contractProvider.refreshItem(contractLine);
//			this.contractProvider.refreshAll();
//		});
//
//	}
//
//	@Override
//	public void onComplete()
//	{
//		this.ui.access(() -> {
//			this.contractProvider.refreshAll();
//			loading.setValue("Load Complete.");
//		});
//	}
//
//	@Override
//	public void onError(Exception e)
//	{
//		// TODO Auto-generated method stub
//
//	}

}
