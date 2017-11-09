package au.com.noojee.acceloUI.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.data.provider.ListDataProvider;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.renderers.LocalDateRenderer;
import com.vaadin.ui.themes.ValoTheme;

import au.com.noojee.acceloapi.AcceloException;
import au.com.noojee.acceloapi.Formatters;
import au.com.noojee.acceloapi.dao.ActivityDao;
import au.com.noojee.acceloapi.entities.Activity;
import au.com.noojee.acceloapi.filter.AcceloFilter;

/**
 * Show all companies with a retainer.
 * 
 * @author bsutton
 */
public class CleanupView extends VerticalLayout implements View
{
	private static final long serialVersionUID = 1L;

	public static final String VIEW_NAME = "Cleanup";

	static Logger logger = LogManager.getLogger();

	private Label loading;

//	private Label ticketId = new Label();
//	private Label ticketOpenDate = new Label();
//	private Label ticketContact = new Label();
//	private Label ticketStatus = new Label();
//	private Label ticketSubject = new Label();

	private Label activityViewSubject;
	private Label activityViewDetails;

	private TextField activityPattern;

	private Label activitiesLabel;

	private ComboBox<String> commonPatterns;

	private Button showActivities;

	private Button deleteActivities;

	private Grid<ActivityLine> activityGrid;
	private ListDataProvider<ActivityLine> activityProvider;
	private List<ActivityLine> activityLines;

	private VerticalLayout resultsLayout = new VerticalLayout();

	public CleanupView()
	{
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		View.super.enter(event);
		this.setSizeFull();

		HorizontalLayout searchLayout = new HorizontalLayout();
		searchLayout.setWidth("100%");

		List<String> patterns = new ArrayList<>();
		patterns.add("has not progressed in three days.");

		activitiesLabel = new Label("Activities");
		searchLayout.addComponent(activitiesLabel);
		commonPatterns = new ComboBox<>();
		searchLayout.addComponent(commonPatterns);
		commonPatterns.setItems(patterns);
		commonPatterns.setPopupWidth("200px");
		commonPatterns.addSelectionListener(l -> activityPattern.setValue(l.getSelectedItem().orElse("")));

		activityPattern = new TextField();
		searchLayout.addComponent(activityPattern);
		activityPattern.setPlaceholder("Enter regex match Pattern");
		showActivities = new Button("Show", l -> doShowActivities());
		searchLayout.addComponent(showActivities);
		deleteActivities = new Button("Delete", l -> doDeleteActivities());
		searchLayout.addComponent(deleteActivities);

		this.addComponent(searchLayout);

		this.addComponent(resultsLayout);

	}

	private void doDeleteActivities()
	{
		// get the list of selected activities and delete them.
		Set<ActivityLine> activities = activityGrid.getSelectedItems();

		for (ActivityLine activityLine : activities)
		{
			Activity activity = activityLine.getActivity();
			new ActivityDao().delete(activity);
			activityLines.remove(activityLine);
		}
		
		activityProvider.refreshAll();
	}

	private void doShowActivities()
	{
		if (resultsLayout != null)
			this.removeComponent(resultsLayout);

		resultsLayout = new VerticalLayout();
		this.addComponent(resultsLayout);
		this.setExpandRatio(resultsLayout, 2); // 2/3 of the screen.

		activityGrid = new Grid<ActivityLine>();

		resultsLayout.addComponent(activityGrid);
		activityGrid.setSizeFull();

		activityGrid.setSelectionMode(Grid.SelectionMode.MULTI);
		activityGrid.setSizeFull();
		activityGrid.addColumn(ActivityLine::getSubject).setCaption("Subject").setExpandRatio(1);

		activityGrid.addColumn(ActivityLine::getStanding).setCaption("Standing");
		activityGrid.addColumn(ticketLine -> ticketLine.getAssignee()).setCaption("Engineer");

		activityGrid.addColumn(ActivityLine::getDateCreated, new LocalDateRenderer("dd/MM/yyyy")).setCaption("Created");

		activityGrid.addColumn(activityLine -> Formatters.format(activityLine.getBillable())).setCaption("Billable")
				.setStyleGenerator(activityLine -> "align-right");

		activityGrid.addComponentColumn(activityLine ->
			{
				IconButton link = new IconButton("", VaadinIcons.ARROW_LEFT,
						e -> changeBillable(activityLine));

				link.addStyleName(ValoTheme.BUTTON_SMALL);
				link.addStyleName(ValoTheme.BUTTON_BORDERLESS);
				boolean isBillable = activityLine.getBillable().getSeconds() > 0;
				link.setIcon(isBillable ? VaadinIcons.ARROW_CIRCLE_RIGHT : VaadinIcons.ARROW_LEFT);
				link.setDescription(isBillable ? "Move to Non Billable" : "Move to Billable");
				return link;
			}).setWidth(80).setCaption("Move");

		activityGrid.addColumn(activityLine -> Formatters.format(activityLine.getNonBillable()))
				.setCaption("NonBillable")
				.setStyleGenerator(activityLine -> "align-right");

		activityGrid.addColumn(activityLine -> Formatters.format(activityLine.getRateCharged())).setCaption("Rate")
				.setStyleGenerator(activityLine -> "align-right");

		activityGrid.addColumn(activityLine -> activityLine.isApproved()).setCaption("Approved");

		activityGrid.addItemClickListener(l -> showActivity(l.getItem()));

		activityViewSubject = new Label();
		activityViewSubject.setWidth("100%");
		activityViewSubject.setContentMode(ContentMode.HTML);
		resultsLayout.addComponent(activityViewSubject);

		Panel panel = new Panel();

		activityViewDetails = new Label();

		activityViewDetails.setContentMode(ContentMode.PREFORMATTED);
		// activityViewDetails.setSizeFull();

		panel.setSizeFull();
		panel.setContent(activityViewDetails);

		resultsLayout.addComponent(panel);
		// resultsLayout.setExpandRatio(panel, 1); // 1/3 of the screen.

		loading = new Label("Loading...");
		resultsLayout.addComponent(loading);

		loadData();

	}

	private void loadData()
	{
		try
		{
			logger.error("Start fetch Activitys");
			AcceloFilter<Activity> filter = new AcceloFilter<>();
			filter.refreshCache();
			
			filter.where(filter.search(activityPattern.getValue()));
			

			List<Activity> activities = new ActivityDao().getByFilter(filter);

			activityLines = activities.parallelStream().sorted().map(t -> new ActivityLine(t))
					.collect(Collectors.toList());
			activityProvider = new ListDataProvider<>(activityLines);
			this.activityGrid.setDataProvider(activityProvider);

			if (!activityLines.isEmpty())
			{
				this.activityGrid.select(activityLines.get(0));
				showActivity(activityLines.get(0));
			}

			logger.error("Finished.");
			loading.setValue("Load Complete");
		}
		catch (AcceloException e)
		{
			logger.error(e, e);
		}

	}

	private void changeBillable(ActivityLine activityLine)
	{
		/*
		 * Activity activity = activityLine.getActivity(); boolean isBillable = activity.getBillable().getSeconds() > 0;
		 * if (isBillable) { activity.setNonBillable(activity.getBillable());
		 * activity.setBillable(Duration.ofSeconds(0)); } else { activity.setNonBillable(Duration.ofSeconds(0));
		 * activity.setBillable(activity.getBillable()); } new ActivityDao().update(activity);
		 * this.activityProvider.refreshItem(activityLine);
		 */

	}

	private void showActivity(ActivityLine l)
	{
		activityViewSubject.setValue("<b>Activity: " + l.getSubject() + "</b>");
		activityViewDetails.setValue(l.getBody());

	}

}
