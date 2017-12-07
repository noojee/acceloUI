package au.com.noojee.acceloUI.views.ticketFilters;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import au.com.noojee.acceloapi.dao.TicketDao;
import au.com.noojee.acceloapi.entities.Ticket;
import au.com.noojee.acceloapi.filter.AcceloFilter;

public class ErrorFilter extends TicketFilter
{

	@Override
	public String getName()
	{
		return "Error Reports";
	}

	public List<Ticket> getTickets(LocalDate cutoffDate, boolean refresh)
	{
		List<Ticket> errorTickets = new ArrayList<>();
		AcceloFilter<Ticket> filter = new AcceloFilter<>();
		if (refresh)
			filter.refreshCache();
		filter.limit(1);
		
		int maxTickets = 100;
		
		String searchString = "ERROR ERROR ERROR - Data migration";
				
		int page = 0;
		while (errorTickets.size() < maxTickets)
		{
			filter.offset(page++);
			
			// Find all tickets without a contract.
			// assigned.
			
			filter.where(
					filter.search(searchString));
			List<Ticket>list = new TicketDao().getByFilter(filter);
			
			if (list.isEmpty())
				break; // no more tickets.
			
			// we don't trust the accuracy of the accelo search so we double check.
			Stream<Ticket> stream = list.stream().filter(ticket -> ticket.getTitle().startsWith(searchString));
			
			errorTickets.addAll(stream.collect(Collectors.toList()));
		}

		return errorTickets.subList(0, Math.min(maxTickets, errorTickets.size()));
	}
	
	@Override
	public String buildURL(Ticket ticket)
	{
		return  "?action=edit_support_issue&id=" + ticket.getId();
	}

}
