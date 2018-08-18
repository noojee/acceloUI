package au.com.noojee.acceloUI.reporting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.joda.money.Money;

import au.com.noojee.acceloapi.AcceloException;
import au.com.noojee.acceloapi.dao.ActivityDao;
import au.com.noojee.acceloapi.dao.InvoiceDao;
import au.com.noojee.acceloapi.dao.StaffDao;
import au.com.noojee.acceloapi.dao.TicketDao;
import au.com.noojee.acceloapi.entities.Activity;
import au.com.noojee.acceloapi.entities.Company;
import au.com.noojee.acceloapi.entities.Contact;
import au.com.noojee.acceloapi.entities.Contract;
import au.com.noojee.acceloapi.entities.ContractPeriod;
import au.com.noojee.acceloapi.entities.Invoice;
import au.com.noojee.acceloapi.entities.Staff;
import au.com.noojee.acceloapi.entities.Ticket;
import au.com.noojee.acceloapi.entities.types.TicketStatus;
import au.com.noojee.acceloapi.util.Constants;
import au.com.noojee.acceloapi.util.Conversions;
import au.com.noojee.acceloapi.util.Format;

/**
 * A very simple program that writes some data to an Excel file using the Apache
 * POI library.
 * 
 * @author www.codejava.net
 *
 */
public class ExcelReport
{

	private int ticketRowCount = 0;
	private Workbook workbook;
	private final Sheet ticketSheet;
	private final CellStyle dateStyle;
	private final CellStyle dollarStyle;
	private final CellStyle dollarAndCentsStyle;
	private int descriptionColumn;
	private int resolutionColumn;
	private CellStyle wrapStyle;
	private CellStyle boldStyle;
	private int chargeColumn;
	private Money totalCharge; // in cents

	ExcelReport()
	{
		workbook = new XSSFWorkbook();
		ticketSheet = workbook.createSheet("Tickets");

		dateStyle = workbook.createCellStyle();
		dateStyle.setDataFormat(workbook.createDataFormat().getFormat("d/m/yyyy"));

		dollarStyle = workbook.createCellStyle();
		DataFormat df = workbook.createDataFormat();
		dollarStyle.setDataFormat(df.getFormat("$#,##0"));

		dollarAndCentsStyle = workbook.createCellStyle();
		df = workbook.createDataFormat();
		dollarAndCentsStyle.setDataFormat(df.getFormat("$#,##0.00"));

		wrapStyle = workbook.createCellStyle();
		wrapStyle.setWrapText(true); // Set wordwrap

		boldStyle = workbook.createCellStyle();

		Font font = workbook.createFont();
		font.setFontHeightInPoints((short) 11);
		font.setFontName(HSSFFont.FONT_ARIAL);
		font.setBold(true);

		boldStyle.setFont(font);
	}

	public void writeUsageSummary(Company company, Contract contract, ContractPeriod contractPeriod)
	{
		Row row = ticketSheet.createRow(ticketRowCount++);
		row.setHeight((short) (row.getHeight() * 2));

		int columnCount = 0;

		Cell cell = row.createCell(columnCount++);
		cell.setCellValue("SLA Usage Summary for the period: " + Format.format(contractPeriod.getDateCommenced())
				+ " - " + Format.format(contractPeriod.getDateExpires()));
		cell.setCellStyle(boldStyle);
		ticketSheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 4));

		row = ticketSheet.createRow(ticketRowCount++);

		// opening balance
		cell = row.createCell(columnCount++);
		cell.setCellValue("Contract Value: ");
		cell.setCellStyle(boldStyle);
		cell = row.createCell(columnCount++);
		cell.setCellValue(Conversions.asDouble(contract.getValue()));
		cell.setCellType(CellType.NUMERIC);
		cell.setCellStyle(dollarStyle);

		// closing balance
		cell = row.createCell(columnCount++);
		cell.setCellValue("Period Value: ");
		cell.setCellStyle(boldStyle);
		cell = row.createCell(columnCount++);
		cell.setCellValue(contractPeriod.getBudget());
		cell.setCellType(CellType.NUMERIC);
		cell.setCellStyle(dollarStyle);

	}

	public void writeTicketSheet(List<Ticket> tickets) throws AcceloException
	{
		int columnCount = 0;

		Row row = ticketSheet.createRow(ticketRowCount++);
		Cell cell = row.createCell(columnCount++);
		cell.setCellValue("Ticket Id");
		cell.setCellStyle(boldStyle);

		cell = row.createCell(columnCount++);
		cell.setCellValue("Date Opened");
		ticketSheet.setColumnWidth(columnCount - 1, PixelUtil.pixel2WidthUnits(60));
		cell.setCellStyle(boldStyle);
		cell = row.createCell(columnCount++);
		cell.setCellValue("Date Closed");
		ticketSheet.setColumnWidth(columnCount - 1, PixelUtil.pixel2WidthUnits(60));
		cell.setCellStyle(boldStyle);
		cell = row.createCell(columnCount++);
		cell.setCellValue("Assigned Engineer");
		ticketSheet.setColumnWidth(columnCount - 1, PixelUtil.pixel2WidthUnits(100));
		cell.setCellStyle(boldStyle);
		cell = row.createCell(columnCount++);
		cell.setCellValue("Contact");
		cell.setCellStyle(boldStyle);
		cell = row.createCell(columnCount++);
		cell.setCellValue("Title");
		cell.setCellStyle(boldStyle);
		cell = row.createCell(columnCount++);
		cell.setCellValue("Status");
		cell.setCellStyle(boldStyle);

		descriptionColumn = columnCount;
		cell = row.createCell(columnCount++);
		cell.setCellValue("Description");
		cell.setCellStyle(boldStyle);

		resolutionColumn = columnCount;
		cell = row.createCell(columnCount++);
		cell.setCellValue("Resolution");
		cell.setCellStyle(boldStyle);

		for (Ticket ticket : tickets)
		{
			row = ticketSheet.createRow(ticketRowCount++);

			columnCount = 0;

			cell = row.createCell(columnCount++);
			cell.setCellValue(ticket.getId());
			cell = row.createCell(columnCount++);
			cell.setCellValue(Conversions.toDate(ticket.getDateTimeOpened().toLocalDate()));
			cell.setCellStyle(dateStyle);
			cell = row.createCell(columnCount++);
			cell.setCellValue(Conversions.toDate(ticket.getDateTimeClosed().toLocalDate()));
			cell.setCellStyle(dateStyle);
			cell = row.createCell(columnCount++);
			cell.setCellValue(getAssignee(ticket));
			cell = row.createCell(columnCount++);
			Contact contact = new TicketDao().getContact(ticket);
			if (contact != null)
				cell.setCellValue(contact.getFullName());
			cell = row.createCell(columnCount++);
			cell.setCellValue(ticket.getTitle());
			cell = row.createCell(columnCount++);
			TicketStatus status = ticket.getStatus();
			cell.setCellValue(status.getTitle());

			cell = row.createCell(columnCount++);
			cell.setCellValue(ticket.getDescription());
			cell.setCellStyle(wrapStyle);

			cell = row.createCell(columnCount++);
			cell.setCellValue(ticket.getResolutionDetail());
			cell.setCellStyle(wrapStyle);

			row.setHeight((short) (row.getHeight() * 3)); // 60 twips (three
															// rows high).

		}

		for (int i = 0; i < columnCount; i++)
		{
			if (i == descriptionColumn)
				ticketSheet.setColumnWidth(i, PixelUtil.pixel2WidthUnits(300));
			else if (i == resolutionColumn)
				ticketSheet.setColumnWidth(i, PixelUtil.pixel2WidthUnits(300)); // 256*100);
			else
				ticketSheet.autoSizeColumn(i);
		}
	}

	private String getAssignee(Ticket ticket)
	{
		String assignee = "None";
		
		if (ticket.getAssignee() != 0)
			assignee = new StaffDao().getById(ticket.getAssignee()).getFullName();
			
		return assignee;
	}

	void writeTicketActivities(Ticket ticket) throws AcceloException
	{
		Sheet activitySheet = workbook.createSheet("Ticket-" + ticket.getId());
		
		TicketDao daoTicket = new TicketDao();
		ActivityDao daoActivity = new ActivityDao();

		List<Activity> activities = daoActivity.getByTicket(ticket, true);

		int rowCount = 0;
		int columnCount = 0;

		// Insert ticket summary details

		// Ticket ID
		Row row = activitySheet.createRow(rowCount++);
		Cell cell = row.createCell(columnCount++);
		activitySheet.setColumnWidth(columnCount - 1, PixelUtil.pixel2WidthUnits(80));
		cell.setCellValue("Ticket Id");
		cell.setCellStyle(boldStyle);
		cell = row.createCell(columnCount++);
		cell.setCellValue(ticket.getId());

		cell = row.createCell(columnCount++);
		cell.setCellValue("Date Opened");
		cell.setCellStyle(boldStyle);
		activitySheet.setColumnWidth(columnCount - 1, PixelUtil.pixel2WidthUnits(80));

		cell = row.createCell(columnCount++);
		cell.setCellValue(Conversions.toDate(ticket.getDateTimeOpened().toLocalDate()));
		cell.setCellStyle(dateStyle);

		cell = row.createCell(columnCount++);
		cell.setCellValue("Date Closed");
		cell.setCellStyle(boldStyle);
		activitySheet.setColumnWidth(columnCount - 1, PixelUtil.pixel2WidthUnits(80));
		cell = row.createCell(columnCount++);
		cell.setCellValue(Conversions.toDate(ticket.getDateTimeClosed().toLocalDate()));
		cell.setCellStyle(dateStyle);

		cell = row.createCell(columnCount++);
		cell.setCellValue("Assigned Engineer");
		cell.setCellStyle(boldStyle);
		activitySheet.setColumnWidth(columnCount - 1, PixelUtil.pixel2WidthUnits(100));
		cell = row.createCell(columnCount++);
		Staff engineer = new StaffDao().getById(ticket.getAssignee());
		cell.setCellValue(engineer != null ? engineer.getFullName() : "");

		cell = row.createCell(columnCount++);
		cell.setCellValue("Contact");
		cell.setCellStyle(boldStyle);

		// Contact Full Name
		cell = row.createCell(columnCount++);
		activitySheet.setColumnWidth(columnCount - 1, PixelUtil.pixel2WidthUnits(80));
		Contact contact = daoTicket.getContact(ticket);
		if (contact != null)
			cell.setCellValue(contact.getFullName());

		// status
		cell = row.createCell(columnCount++);
		cell.setCellValue("Status");
		cell.setCellStyle(boldStyle);

		// Status value
		cell = row.createCell(columnCount++);
		TicketStatus status = ticket.getStatus();
		cell.setCellValue(status.getTitle());

		// Billable
		row = activitySheet.createRow(rowCount++);
		columnCount = 0;
		Duration billable = daoTicket.getBillable(ticket);
		cell = row.createCell(columnCount++);
		cell.setCellValue("Billable");
		cell.setCellStyle(boldStyle);
		cell = row.createCell(columnCount++);
		cell.setCellValue(formatDuration(billable));

		// Title
		row = activitySheet.createRow(rowCount++);
		columnCount = 0;
		cell = row.createCell(columnCount++);
		cell.setCellValue("Title");
		cell.setCellStyle(boldStyle);
		cell = row.createCell(columnCount++);
		cell.setCellValue(ticket.getTitle());

		// Description
		row = activitySheet.createRow(rowCount++);
		columnCount = 0;
		cell = row.createCell(columnCount++);
		cell.setCellValue("Description");
		cell.setCellStyle(boldStyle);
		row.setHeight((short) (row.getHeight() * 10));

		// Description Value
		cell = row.createCell(columnCount++);
		cell.setCellValue(ticket.getDescription());
		cell.setCellStyle(wrapStyle);
		// activitySheet.setColumnWidth(columnCount-1,PixelUtil.pixel2WidthUnits(400));
		activitySheet
				.addMergedRegion(new CellRangeAddress(rowCount - 1, rowCount - 1, columnCount - 1, columnCount + 10));

		// Resolution
		row = activitySheet.createRow(rowCount++);
		row.setHeight((short) (row.getHeight() * 6));
		columnCount = 0;

		cell = row.createCell(columnCount++);
		cell.setCellValue("Resolution");
		cell.setCellStyle(boldStyle);
		cell = row.createCell(columnCount++);
		cell.setCellValue(ticket.getResolutionDetail());
		cell.setCellStyle(wrapStyle);
		// activitySheet.setColumnWidth(columnCount-1,PixelUtil.pixel2WidthUnits(300));
		activitySheet
				.addMergedRegion(new CellRangeAddress(rowCount - 1, rowCount - 1, columnCount - 1, columnCount + 10));

		// skip a row.
		row = activitySheet.createRow(rowCount++);
		row = activitySheet.createRow(rowCount++);

		// Insert the headings.
		columnCount = 0;
		cell = row.createCell(columnCount++);
		cell.setCellValue("Created");
		cell.setCellStyle(boldStyle);
		cell = row.createCell(columnCount++);
		cell.setCellValue("Subject");
		cell.setCellStyle(boldStyle);
		columnCount += 6;
		cell = row.createCell(columnCount++);
		cell.setCellValue("Engineer");
		cell.setCellStyle(boldStyle);
		cell = row.createCell(columnCount++);
		cell.setCellValue("Billable Time");
		cell.setCellStyle(boldStyle);
		activitySheet.setColumnWidth(columnCount - 1, PixelUtil.pixel2WidthUnits(80));
		cell = row.createCell(columnCount++);
		cell.setCellValue("Non Billable Time");
		cell.setCellStyle(boldStyle);
		activitySheet.setColumnWidth(columnCount - 1, PixelUtil.pixel2WidthUnits(100));
		cell = row.createCell(columnCount++);
		cell.setCellValue("Rate");
		cell.setCellStyle(boldStyle);

		// Charged
		cell = row.createCell(columnCount++);
		cell.setCellValue("Charged");
		cell.setCellStyle(boldStyle);

		// Invoice
		cell = row.createCell(columnCount++);
		cell.setCellValue("Invoice");
		cell.setCellStyle(boldStyle);

		// Body
		cell = row.createCell(columnCount++);
		cell.setCellValue("Body");
		cell.setCellStyle(boldStyle);

		totalCharge = Constants.MONEY_ZERO;

		boolean foundActivities = false;

		for (Activity activity : activities)
		{


			// Only output the activity if some time has been lodged against it.
			if (!activity.getBillable().isZero() || !activity.getNonBillable().isZero())
			{
				foundActivities = true;

				row = activitySheet.createRow(rowCount++);

				columnCount = 0;

				cell = row.createCell(columnCount++);
				cell.setCellValue(Conversions.toDate(activity.getDateTimeCreated().toLocalDate()));
				cell.setCellStyle(dateStyle);

				// Subject value
				cell = row.createCell(columnCount++);
				cell.setCellValue(activity.getSubject());
				activitySheet.addMergedRegion(
						new CellRangeAddress(rowCount - 1, rowCount - 1, columnCount - 1, columnCount + 5));
				columnCount += 6;

				cell = row.createCell(columnCount++);
				Staff staff = new StaffDao().getById(activity.getStaff());
				cell.setCellValue(staff != null ? staff.getFullName() : "");
				cell.setCellStyle(dateStyle);
				cell = row.createCell(columnCount++);
				cell.setCellValue(formatDuration(activity.getBillable()));
				cell = row.createCell(columnCount++);
				cell.setCellValue(formatDuration(activity.getNonBillable()));
				// Rate Charged
				cell = row.createCell(columnCount++);
				cell.setCellValue(Format.format(activity.getRateCharged()));
				cell.setCellType(CellType.NUMERIC);
				cell.setCellStyle(dollarStyle);

				// Amount Charged
				chargeColumn = columnCount;
				cell = row.createCell(columnCount++);
				Money charge = calculateCharge(activity.getRateCharged(), activity.getBillable());
				cell.setCellValue(Format.format(charge));
				cell.setCellType(CellType.NUMERIC);
				cell.setCellStyle(dollarAndCentsStyle);
				totalCharge.plus(charge);

				// Invoice Id
				cell = row.createCell(columnCount++);
				Invoice invoice = new InvoiceDao().getById(activity.getInvoiceId());
				cell.setCellValue(invoice != null ? "" + invoice.getId() : "");

				// Body Value
				cell = row.createCell(columnCount++);
				cell.setCellValue(activity.getBody());
			}

		}

		if (foundActivities)
		{
			// Total Charges
			row = activitySheet.createRow(rowCount++);

			// Total Label
			cell = row.createCell(chargeColumn - 1);
			cell.setCellValue("Total");
			cell.setCellStyle(boldStyle);

			// Total amount
			cell = row.createCell(chargeColumn);
			cell.setCellValue(Format.format(totalCharge)); // convert to
																// dollars
			cell.setCellType(CellType.NUMERIC);
			cell.setCellStyle(dollarAndCentsStyle);

			for (int i = 0; i < columnCount; i++)
			{
				// activitySheet.autoSizeColumn(i);
			}
		}

	}

	/**
	 * calculates charge in cents.
	 * 
	 * @param rate
	 * @param billableSeconds
	 * @return chargeable amount in cents.
	 */
	private Money calculateCharge(Money rate, Duration billable)
	{
		long hours = billable.toHours();
		long minutes = billable.toMinutes() % 60;
		long seconds = billable.getSeconds() % 60;
		// rate *= 100;

		Money charge = (rate.multipliedBy(hours).plus(rate.multipliedBy(minutes).dividedBy(60, RoundingMode.HALF_UP))
				.plus(rate.multipliedBy(seconds).dividedBy(3600, RoundingMode.HALF_UP)));

		// round up
		// charge = (int) (((float) charge) + 0.5f);
		// return (int) (((float) charge) + 0.5f);

		return charge;
	}

	String formatTime(int seconds)
	{
		String formatted;

		// Hours and minutes.
		formatted = String.format("%02d:%02d", seconds / 3600, seconds / 60);

		if (seconds == 0)
			formatted = "";

		return formatted;
	}

	public void save(File excelFile) throws IOException, FileNotFoundException
	{

		try (FileOutputStream outputStream = new FileOutputStream(excelFile))
		{
			workbook.write(outputStream);
		}
	}

	String formatDuration(Duration duration)
	{

		return (duration == null ? "" : DurationFormatUtils.formatDuration(duration.toMillis(), "H:mm"));
	}

	public Workbook getWorkBook()
	{
		return workbook;
	}

}