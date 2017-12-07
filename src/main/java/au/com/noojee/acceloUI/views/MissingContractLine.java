package au.com.noojee.acceloUI.views;

import java.time.LocalDateTime;
import java.util.List;

import au.com.noojee.acceloUI.views.MissingContractView.CompanyDetails;
import au.com.noojee.acceloapi.entities.Company;
import au.com.noojee.acceloapi.entities.Ticket;

public class MissingContractLine
{
	CompanyDetails cd;
	
	// Tickets from the last two months.
	List<Ticket> tickets;
	
	MissingContractLine(CompanyDetails cd)
	{
		this.cd = cd;
	}
	
	
	String getCompanyName()
	{
		return cd.getCompany().getName();
	}
	
	LocalDateTime getFirstTicket()
	{
		return cd.firstTicket;
	}
	
	LocalDateTime getLastTicket()
	{
		return cd.lastTicket;
	}
	int getTicketCount()
	{
		return tickets.size();
	}


	public Company getCompany()
	{
		return cd.getCompany();
	}
	
}
