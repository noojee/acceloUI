package au.com.noojee.acceloUI.reporting;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

import com.google.common.io.Files;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Notification;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

import au.com.noojee.acceloUI.authentication.CurrentUser;
import au.com.noojee.acceloUI.forms.AcceloContactProvider;
import au.com.noojee.acceloUI.forms.EmailForm;
import au.com.noojee.acceloUI.main.BroadcastMessage;
import au.com.noojee.acceloUI.main.Broadcaster;
import au.com.noojee.acceloUI.views.IconButton;
import au.com.noojee.acceloapi.dao.ContractPeriodDao;
import au.com.noojee.acceloapi.entities.Contract;
import au.com.noojee.acceloapi.entities.ContractPeriod;

public class ExportContractPeriodWindow extends Window
{
	private static final long serialVersionUID = 1L;
	private ListSelect<ContractPeriod> periodSelection;
	private Button cancelButton;

	public ExportContractPeriodWindow(Contract contract)
	{
		super("Export Usage Report");
		
		VerticalLayout layout = new VerticalLayout();
		
		layout.addComponent(new Label("Select an Contract Period to export"));

		List<ContractPeriod> periods = new ContractPeriodDao().getByContract(contract);

		periodSelection = new ListSelect<ContractPeriod>();
		periodSelection.setItems(periods);
		periodSelection.setItemCaptionGenerator(ContractPeriod::getPeriodRange);
		periodSelection.setRows(6);
		periodSelection.setWidth("100%");

		layout.addComponent(periodSelection);

		HorizontalLayout buttons = new HorizontalLayout();
		cancelButton = new Button("Cancel", e -> this.close());
		buttons.addComponent(cancelButton);
		
		periodSelection.addValueChangeListener(event -> {
			exportSelectedPeriod(buttons);
		});
		
		layout.addComponent(buttons);
		
		setClosable(false);
		center();
		
		setContent(layout);

	}

	private void exportSelectedPeriod(HorizontalLayout buttons)
	{
		Set<ContractPeriod> contractPeriods = periodSelection.getSelectedItems();
		
		if (contractPeriods.size() == 0)
			Notification.show("You must select one Contract Period to export");
		else
		{
			ContractPeriod contractPeriod = (ContractPeriod) contractPeriods.toArray()[0];
			
			buttons.removeAllComponents();
			buttons.addComponent(cancelButton);
			
			Button emailButton = new Button("Send Email");
			emailButton.addClickListener(l -> {
				try {
					sendEmail(contractPeriod);
				} catch (IOException e) {
					Broadcaster.broadcast(new BroadcastMessage(e));
				}
			});
			buttons.addComponent(emailButton);
			
			// We have to dynamically add this button due to a requirement of the FileDownloader (gets around browser security issues).
			IconButton exportButton = getExportButton(contractPeriod);
			buttons.addComponent(exportButton);
			
		}
	}

	private void sendEmail(ContractPeriod contractPeriod) throws IOException
	{
		AcceloContactProvider contactProvider = new AcceloContactProvider(contractPeriod); 
		
		EmailForm form = new EmailForm(CurrentUser.get(), contactProvider, contactProvider.getDefaultContact().getEmail());
		form.setSubject("Noojee work report for " + contractPeriod.getPeriodRange());
		
		String body = "Please find the attached work report for the period " + contractPeriod.getPeriodRange() + ".<br></br>"
				+ "The attached Excel report contains <b>two sheets</b>. An Overview sheet and a details sheet for all work performed in the noted period.</br>"
				+ "The report highlights the total work performed against your 'included' work allowance and the remaining work allowance.</br></br>"
				+ "Any excess over and above your included work allowance will be billed on a separate invoice.</br></br></br>"
				+ "If you have any questions please call Accounts on 03 8320 8100.</br>";
		
		form.setBody(body);
		
		ContractReport contractReport = new ContractReport();
		ExcelReport excel = contractReport.generate(contractPeriod);
		
		File tmpDir = Files.createTempDir();
		File excelFile = new File(tmpDir, contractReport.generateSpreadsheetName(contractPeriod));
		
		excel.save(excelFile);
		
		form.attachFile(excelFile, true);
		
		form.show();
	}

	IconButton getExportButton(ContractPeriod contractPeriod)
	{
		IconButton exportButton = new IconButton("Download", VaadinIcons.DOWNLOAD);

		exportButton.setStyleName(ValoTheme.BUTTON_LINK);
		exportButton.setDescription("Download this Usage Report");

		ContractReport contractReport = new ContractReport();
		FileDownloader fileDownloader = new FileDownloader(new StreamResource(new StreamSource()
		{
			private static final long serialVersionUID = 1L;

			ExcelReport excel = null;

			@Override
			public InputStream getStream()
			{
				excel = contractReport.generate(contractPeriod);

				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				try
				{

					excel.getWorkBook().write(stream);
					return new ByteArrayInputStream(stream.toByteArray());
				}
				catch (IOException e)
				{
					ContractReport.logger.error(e, e);
					Notification.show("An error occurred.", Notification.Type.ERROR_MESSAGE);
				}
				return null;

			}
		}, contractReport.generateSpreadsheetName(contractPeriod)));
		fileDownloader.setOverrideContentType(false);
		fileDownloader.extend(exportButton);

		return exportButton;

	}
}