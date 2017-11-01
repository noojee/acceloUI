package au.com.noojee.acceloUI.views;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import au.com.noojee.acceloapi.AcceloException;
import au.com.noojee.acceloapi.dao.CompanyDao;
import au.com.noojee.acceloapi.dao.StaffDao;
import au.com.noojee.acceloapi.dao.TicketDao;
import au.com.noojee.acceloapi.entities.Contact;
import au.com.noojee.acceloapi.entities.Staff;
import au.com.noojee.acceloapi.entities.Ticket;
import au.com.noojee.acceloapi.entities.meta.Ticket_;
import au.com.noojee.acceloapi.filter.AcceloFilter;

public class TicketLine implements Comparable<TicketLine>
{
	static Logger logger = LogManager.getLogger();

	Ticket ticket;

	public TicketLine(Ticket ticket)
	{
		this.ticket = ticket;
	}

	int getId()
	{
		return ticket.getId();
	}

	String getTitle()
	{
		return ticket.getTitle();
	}

	String getAssignee()
	{
		int staffId = ticket.getAssignee();

		String staffName = "Error";
		try
		{
			Staff staff = new StaffDao().getById(staffId);
			staffName = (staff == null ? "" : staff.getFullName());
		}
		catch (AcceloException e)
		{
			logger.error(e, e);
		}
		return staffName;
	}

	String getContact()
	{

		String contactName = "Error";
		try
		{
			Contact contact = new TicketDao().getContact(ticket);
			contactName = (contact == null ? "Null Contact" : contact.getFullName());
		}
		catch (AcceloException e)
		{
			logger.error(e, e);

		}

		return contactName;
	}
	
	String getCompanyName()
	{
		return new CompanyDao().getById(ticket.getCompanyId()).getName();
	}
	
	/**
	 * Is the ticket Attached to a contract.
	 * @return
	 */
	boolean isAttached()
	{
		return ticket.isAttached();
	}

	void loadWork()
	{
		TicketDao daoTicket = new TicketDao();
		daoTicket.getBillable(ticket);
		daoTicket.sumMTDWork(ticket);
		daoTicket.sumLastMonthWork(ticket);
	}

	Duration getBillable()
	{
		return new TicketDao().getBillable(ticket);
	}

	Duration getNonBillable()
	{
		return new TicketDao().getNonBillable(ticket);
	}

	LocalDate getDateStarted()
	{
		return ticket.getDateStarted();
	}

	LocalDate getDateLastInteracted()
	{
		return ticket.getDateLastInteracted();
	}
	public boolean isOpen()
	{
		return ticket.isOpen();
	}

	public boolean isFullyApproved()
	{
		boolean approved = false;
		try
		{
			approved = new TicketDao().isFullyApproved(ticket);
		}
		catch (AcceloException e)
		{
			logger.error(e, e);
		}
		return approved;
	}

	@Override
	public int compareTo(TicketLine o)
	{
		return this.ticket.getId() - o.ticket.getId();
	}

	public void refresh()
	{
		try
		{
			AcceloFilter<Ticket> filter = new AcceloFilter<>();
			filter.where(filter.eq(Ticket_.id, ticket.getId()));
			filter.refreshCache();

			List<Ticket> tickets = new TicketDao().getByFilter(filter);
			if (tickets != null)
				ticket = tickets.get(0);
		}
		catch (AcceloException e)
		{
			logger.error(e, e);
		}

	}

}
