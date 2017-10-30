package au.com.noojee.acceloUI.views;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.grid.ColumnResizeMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.LocalDateRenderer;

import au.com.noojee.acceloUI.reporting.ExportContractPeriodWindow;
import au.com.noojee.acceloapi.AcceloException;
import au.com.noojee.acceloapi.Formatters;
import au.com.noojee.acceloapi.dao.ContractDao;
import au.com.noojee.acceloapi.entities.Contract;
import au.com.noojee.acceloapi.filter.AcceloCache;
import au.com.noojee.acceloapi.filter.AcceloFilter;
import au.com.noojee.acceloapi.filter.expressions.Eq;

/**
 * Show all companies with a retainer.
 * 
 * @author bsutton
 *
 */
public class CompanyView extends VerticalLayout implements View
{
	private static final long serialVersionUID = 1L;

	public static final String VIEW_NAME = "Company";

	static Logger logger = LogManager.getLogger();

	Grid<ContractLine> grid;
	final ListDataProvider<ContractLine> contractProvider;
	private List<ContractLine> contractLines = new ArrayList<>();

	private Label loading;

	private UI ui;

	private int loadCount = 0;

	private TextField filter;

	public CompanyView()
	{
		this.contractProvider = new ListDataProvider<>(contractLines);
		this.ui = UI.getCurrent();
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		View.super.enter(event);
		this.setSizeFull();

		try
		{
			initialiseGrid();
		}
		catch (AcceloException e)
		{
			logger.error(e, e);
		}
	}

	void initialiseGrid() throws AcceloException
	{
		if (grid == null)
		{

			this.setMargin(true);
			this.setSpacing(true);
			this.setSizeFull();

			HorizontalLayout topLine = new HorizontalLayout();
			topLine.setWidth("100%");
			this.addComponent(topLine);
			
			loading = new Label("Loading Contracts ...");
			topLine.addComponent(loading);
			topLine.setComponentAlignment(loading, Alignment.MIDDLE_LEFT);
			
			Button flush = new Button("Flush Cache");
			topLine.addComponent(flush);
			topLine.setComponentAlignment(flush, Alignment.MIDDLE_RIGHT);
			flush.addClickListener(l -> AcceloCache.getInstance().flushCache());
			
			

			grid = new Grid<>();

			grid.setSizeFull();
			grid.addColumn(ContractLine::getCompanyName).setCaption("Company").setExpandRatio(1).setId("CompanyName");
			grid.addColumn(ContractLine::getContractTitle).setCaption("Contract").setId("Contract");
			grid.addColumn(ContractLine::getDateStarted, new LocalDateRenderer("dd/MM/yyyy")).setCaption("Start");
			grid.addColumn(ContractLine::getDateExpires, new LocalDateRenderer("dd/MM/yyyy")).setCaption("End");
			grid.addColumn(contractLine -> Formatters.format(contractLine.getContractValue())).setCaption("Value")
					.setStyleGenerator(contractLine -> "align-right");

			grid.addColumn(contractLine -> Formatters.format(contractLine.getRemainingValue())).setCaption("Remaining")
					.setStyleGenerator(contractLine -> "align-right");

			// grid.addColumn(contract ->
			// formatDuration(contract.getUnassignedWork())).setCaption("Unassigned")
			// .setStyleGenerator(contract -> "align-right");

			grid.addColumn(contractLine -> contractLine.getUnassignedTicketCount()).setCaption("Unassigned")
					.setStyleGenerator(contractLine -> "align-right");

			grid.addComponentColumn(contractLine -> new IconButton("Tickets", VaadinIcons.SEARCH, e -> UI.getCurrent().getNavigator()
					.navigateTo(TicketView.VIEW_NAME + "/" + contractLine.getContract().getId())))
			.setWidth(80)
			.setCaption("Tickets");
			
			
			grid.addComponentColumn(contractLine -> new IconButton("Export", VaadinIcons.DOWNLOAD, e -> ui.addWindow(new ExportContractPeriodWindow(contractLine.getContract()))))
			.setWidth(80)
			.setCaption("Report");


			grid.addColumn(contract -> Formatters.format(contract.getMtdWork())).setCaption("MTD Work")
					.setStyleGenerator(contract -> "align-right");

			grid.addColumn(contract -> Formatters.format(contract.getLastMonthWork())).setCaption("Last Month Work")
					.setStyleGenerator(contract -> "align-right");

			// grid.addComponentColumn(contract -> new Button("Refresh", e ->
			// contract.refresh()));

			this.grid.setDataProvider(contractProvider);

			this.addComponent(createFilter());
			this.addComponent(grid);
			this.setExpandRatio(grid, 1);

			grid.setColumnResizeMode(ColumnResizeMode.SIMPLE);

			logger.error("Finished.");

			loadContracts();
		}

	}

	private Component createFilter()
	{
		HorizontalLayout layout = new HorizontalLayout();
		layout.setWidth("100%");
		Button clear = new Button("X");
		clear.addClickListener(l -> clearFilter());

		layout.addComponent(clear);
		filter = new TextField();
		filter.addValueChangeListener(l -> setFilter());
		filter.setWidth("100%");
		layout.addComponent(filter);
		layout.setExpandRatio(filter, 1);

		contractProvider.addFilter(contractLine -> filterContracts(contractLine, filter.getValue()));

		return layout;

	}

	private void clearFilter()
	{
		filter.clear();
		contractProvider.refreshAll();
	}

	private void setFilter()
	{
		contractProvider.refreshAll();
	}

	private boolean filterContracts(ContractLine contractLine, String filter)
	{
		return contractLine.getCompanyName().toLowerCase().contains(filter.toLowerCase());
	}

	void updateLoading(String message, ContractLine l)
	{
		ui.access(() -> {
			if (l != null)
				this.contractProvider.refreshItem(l);
			loading.setValue(message);
		});
	}

	private void loadContracts()
	{
		new Thread(() -> {
			// This list may be empty but if it is the loadCache will background
			List<Contract> contracts;
			try
			{
				this.loadCount = 0;
				updateLoading("Loading Contracts...", null);
				// contracts = new ContractDao().getActiveContracts();
				
				// dev optimisation.
				AcceloFilter filter = new AcceloFilter();
				filter.where(new Eq("against_id", 3787));
				contracts = new ContractDao().getByFilter(filter);

				List<ContractLine> lines = contracts.parallelStream().map(c -> {

					this.loadCount++;

					ContractLine line = new ContractLine(c);
					line.getCompanyName();
					line.getContractTitle();
					line.getContractValue();
					line.getRemainingValue();
					line.getUnassignedTicketCount();
					line.getContract();
					line.getMtdWork();
					line.getLastMonthWork();

					updateLoading("Loaded " + this.loadCount + " Contracts", line);

					return line;
				}).collect(Collectors.toList());

				contractLines.addAll(lines.stream().sorted().collect(Collectors.toList()));

				this.ui.access(() -> {
					contractProvider.refreshAll();
					updateLoading("Loading Work...", null);

				});
				loadWork();

			}
			catch (AcceloException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}).start();

	}

	private void loadWork()
	{
		// background load the activity no.s
		this.loadCount = 0;
		new Thread(() -> {
			contractLines.parallelStream().distinct().forEach(l -> {
				this.loadCount++;

				l.loadWork();
				updateLoading("Loaded Work for " + this.loadCount + " Contracts", l);

			});

			new UIAccessor(this.ui, true, () -> {
				this.contractProvider.refreshAll();
				updateLoading("Work Load Complete: api calls=" + AcceloCache.getInstance().getMissCounter(), null);
			}).run();

		}).start();

	}

}
