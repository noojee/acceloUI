package au.com.noojee.acceloUI.reporting;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import au.com.noojee.acceloapi.AcceloException;
import au.com.noojee.acceloapi.dao.CompanyDao;
import au.com.noojee.acceloapi.dao.ContractDao;
import au.com.noojee.acceloapi.dao.TicketDao;
import au.com.noojee.acceloapi.entities.Company;
import au.com.noojee.acceloapi.entities.Contract;
import au.com.noojee.acceloapi.entities.ContractPeriod;
import au.com.noojee.acceloapi.entities.Ticket;

/**
 * Hello world!
 *
 */
public class ContractReport
{

	static Logger logger = LogManager.getLogger();

	public ExcelReport generate(ContractPeriod contractPeriod) throws AcceloException
	{
		ExcelReport excel = new ExcelReport();

		Contract contract = new ContractDao().getById(contractPeriod.getContractId());

		Company company = new CompanyDao().getById(contract.getCompanyId());

		logger.error("Period: " + contractPeriod.getDateCommenced() + " - " + contractPeriod.getDateExpires());

		// find the issues attached to the parent contract.
		List<Ticket> tickets = new TicketDao().getByContract(contract);

		tickets = filterForCurrentPeriod(contractPeriod, tickets);

		excel.writeUsageSummary(company, contract, contractPeriod);
		excel.writeTicketSheet(tickets);

		for (Ticket ticket : tickets)
		{
			excel.writeTicketActivities(ticket);
		}

		// excel.save();

		return excel;
	}

	String generateSpreadsheetName(ContractPeriod contractPeriod)
	{
		Contract contract = new ContractDao().getById(contractPeriod.getContractId());
		Company company = new CompanyDao().getById(contract.getCompanyId());

		return new File(company.getName() + "-SLA Usage Report-" + contractPeriod.getPeriodRange()  + ".xlsx").getName().replaceAll(" ", "");
	}

	// Remove any tickets which aren't for the current period.
	private static List<Ticket> filterForCurrentPeriod(ContractPeriod period, List<Ticket> tickets)
	{
		List<Ticket> periodTickets = new ArrayList<>();

		for (Ticket ticket : tickets)
		{
			logger.error("Found Ticket: " + ticket.getId() + " Closed:" + ticket.getDateClosed());
			if (ticket.getDateClosed() != null && ticket.getDateClosed().isAfter(period.getDateCommenced())
					&& ticket.getDateClosed().isBefore(period.getDateExpires()))
			{
				logger.error("Ticket is in current period");
				periodTickets.add(ticket);

			}

		}

		return periodTickets;
	}

	static class ContractPeriodComparator implements Comparator<ContractPeriod>
	{

		@Override
		public int compare(ContractPeriod lhs, ContractPeriod rhs)
		{

			return (lhs.getDateCommenced().isBefore(rhs.getDateCommenced()) ? 1 : -1);
		}

	}

}
