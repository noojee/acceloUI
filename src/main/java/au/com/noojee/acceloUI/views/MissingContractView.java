package au.com.noojee.acceloUI.views;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.VaadinSession;
import com.vaadin.shared.ui.grid.ColumnResizeMode;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

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

	private UI ui;

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

			this.setMargin(true);
			this.setSpacing(true);
			this.setSizeFull();

			loading = new Label("Loading...");
			this.addComponent(loading);

			grid = new Grid<>();

//			// This list may be empty but if it is the loadCache will background
//			// fill it.
//			List<Contract> contract = Cache.getCacheSingleton().getContracts();
//			contractLines = contract.stream().map(e -> {
//				return e.getContractLine();
//			}).collect(Collectors.toList());
//
//			contractLines = new ArrayList<>();
//			contractProvider = new ListDataProvider<>(contractLines);
//			this.grid.setDataProvider(contractProvider);
//
//			Cache.getCacheSingleton().loadCache();
//			Cache.getCacheSingleton().subscribe(this);
//
//			grid.setSizeFull();
//			grid.addColumn(ContractLine::getCompanyName).setCaption("Company").setExpandRatio(1);
//			grid.addColumn(ContractLine::getContractTitle).setCaption("Contract");
//			grid.addColumn(ContractLine::getDateStarted, new LocalDateRenderer("dd/MM/yyyy")).setCaption("Start");
//			grid.addColumn(ContractLine::getDateExpires, new LocalDateRenderer("dd/MM/yyyy")).setCaption("End");
//			grid.addColumn(contractLine -> Formatters.format(contractLine.getContractValue())).setCaption("Value")
//					.setStyleGenerator(contractLine -> "align-right");
//
//			grid.addColumn(contractLine -> Formatters.format(contractLine.getRemainingValue())).setCaption("Remaining")
//					.setStyleGenerator(contractLine -> "align-right");
//
//			// grid.addColumn(contract ->
//			// formatDuration(contract.getUnassignedWork())).setCaption("Unassigned")
//			// .setStyleGenerator(contract -> "align-right");
//
//			grid.addColumn(contractLine -> contractLine.getUnassignedTicketCount()).setCaption("Unassigned")
//					.setStyleGenerator(contractLine -> "align-right");
//
//			grid.addComponentColumn(contractLine -> new Button("View", e -> UI.getCurrent().getNavigator()
//					.navigateTo(TicketView.VIEW_NAME + "/" + contractLine.getContract().getId())));
//
//			grid.addColumn(contract -> Formatters.format(contract.getMtdWork())).setCaption("MTD Work")
//					.setStyleGenerator(contract -> "align-right");
//
//			grid.addColumn(contract -> Formatters.format(contract.getLastMonthWork())).setCaption("Last Month Work")
//					.setStyleGenerator(contract -> "align-right");
//
//			grid.addComponentColumn(contract -> new Button("Refresh", e -> contract.refresh()));

			this.addComponent(grid);
			this.setExpandRatio(grid, 1);

			grid.setColumnResizeMode(ColumnResizeMode.SIMPLE);

			logger.error("Finished.");
		}

	}

	class MYUI
	{
		UI ui;
		VaadinSession session;

		MYUI()
		{
			this.ui = UI.getCurrent();
			this.session = VaadinSession.getCurrent();
		}

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
