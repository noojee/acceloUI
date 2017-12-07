package au.com.noojee.acceloUI.views.ticketFilters;

import java.time.LocalDate;
import java.util.List;

import au.com.noojee.acceloapi.entities.Ticket;

public abstract class TicketFilter
{
	abstract public String getName();

	abstract public List<Ticket> getTickets(LocalDate cutoffDate, boolean refresh);
	
	abstract public String buildURL(Ticket ticket);

}
