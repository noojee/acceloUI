package au.com.noojee.acceloUI.views;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import javax.money.CurrencyUnit;
import javax.money.Monetary;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.javamoney.moneta.Money;

import au.com.noojee.acceloapi.AcceloException;
import au.com.noojee.acceloapi.dao.CompanyDao;
import au.com.noojee.acceloapi.dao.TicketDao;
import au.com.noojee.acceloapi.entities.Contract;
import au.com.noojee.acceloapi.entities.Ticket;
import au.com.noojee.acceloapi.entities.meta.AgainstType_;
import au.com.noojee.acceloapi.entities.meta.Ticket_;
import au.com.noojee.acceloapi.filter.AcceloFilter;

class ContractLine implements Comparable<ContractLine>
{
	static Logger logger = LogManager.getLogger();
	static CurrencyUnit currencyUnit = Monetary.getCurrency(Locale.getDefault());

	Contract contract;
	String companyName;
//	String contractTitle;
//	LocalDate dateStarted;
//	LocalDate dateExpires;

//	Money contractValue;
//	Money remainingValue;
	
	// The total billable amount for the current month.
//	Money billable;

	// This two values are background calculated by a call to loadWork.
	Duration mtdWork; // The amount work done so far this month.
	Duration lastMonthWork; // The amount of work done last month on this
							// ticket.
	private int unassignedTickets; // no. of unassigned tickets

	public ContractLine(Contract contract)
	{
		super();

		this.contract = contract;

		try
		{
			this.companyName = new CompanyDao().getById(contract.getCompanyId()).getName();
		}
		catch (AcceloException e)
		{
			CompanyView.logger.error(e, e);
		}

	}

	public Contract getContract()
	{
		return this.contract;
	}

	public String getCompanyName()
	{
		return companyName;
	}

	public String getContractTitle()
	{
		return contract.getTitle();
	}

	public LocalDate getDateStarted()
	{
		return this.contract.getDateStarted();
	}

	public LocalDate getDateExpires()
	{
		return this.contract.getDateExpires();
	}

	public Money getContractValue()
	{
		return this.contract.getValue();
	}

	public Money getRemainingValue()
	{
		return this.contract.getRemainingValue();
	}

	public Duration getMtdWork()
	{
		return mtdWork;
	}

	public Duration getLastMonthWork()
	{
		return lastMonthWork;
	}
	
//	public Money getBillable()
//	{
//		return this.billable;
//	}

	void loadWork()
	{
		try
		{
			TicketDao daoTicket = new TicketDao();
			List<Ticket> tickets = daoTicket.getByContract(this.contract);

			ContractLine.this.mtdWork = Duration.ofSeconds(tickets.stream().mapToLong(t -> daoTicket.sumMTDWork(t).getSeconds()).sum());
			ContractLine.this.lastMonthWork = Duration.ofSeconds(tickets.stream().mapToLong(t -> daoTicket.sumLastMonthWork(t).getSeconds()).sum());

		}
		catch (AcceloException e)
		{
			logger.error(e, e);
		}
	}

	public int getUnassignedTicketCount()
	{
		
		AcceloFilter<Ticket> filter = new AcceloFilter<>();
		filter.where(filter.eq(Ticket_.contract, 0).and(filter.against(AgainstType_.company, this.contract.getCompanyId())));
		List<Ticket> unassignedTickets = new TicketDao().getByFilter(filter);
		
		return unassignedTickets.size();
	
	}

//	public void setRemainingValue(Money remainingValue)
//	{
//		this.remainingValue = remainingValue;
//	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((companyName == null) ? 0 : companyName.hashCode());
		result = prime * result + ((contract == null) ? 0 : contract.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContractLine other = (ContractLine) obj;
		if (companyName == null)
		{
			if (other.companyName != null)
				return false;
		}
		else if (!companyName.equals(other.companyName))
			return false;
		if (contract == null)
		{
			if (other.contract != null)
				return false;
		}
		else if (!contract.equals(other.contract))
			return false;
		return true;
	}

	public String toString()
	{
		return this.companyName;
	}

	@Override
	public int compareTo(ContractLine arg0)
	{
		return this.companyName.compareTo(arg0.companyName);
	}

}