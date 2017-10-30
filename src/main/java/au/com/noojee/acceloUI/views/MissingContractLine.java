package au.com.noojee.acceloUI.views;

import java.util.List;

import au.com.noojee.acceloapi.entities.Company;
import au.com.noojee.acceloapi.entities.Ticket;

public class MissingContractLine
{
	Company company;
	
	// Tickets from the last two months.
	List<Ticket> tickets;
	
	MissingContractLine()
	{
		
	}
	
	
	String getCompanyName()
	{
		return company.getName();
	}
	
	int getTicketCount()
	{
		return tickets.size();
	}
	
//	Duration getTotalWork()
//	{
//		tickets.stream().
//	}
	
}
