package au.com.noojee.acceloUI.views.ticketFilters;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import au.com.noojee.acceloapi.dao.TicketDao;
import au.com.noojee.acceloapi.entities.Ticket;
import au.com.noojee.acceloapi.entities.meta.Ticket_;
import au.com.noojee.acceloapi.entities.meta.fieldTypes.OrderByField.Order;
import au.com.noojee.acceloapi.filter.AcceloFilter;

public class UnapprovedTicketFilter extends TicketFilter
{

	@Override
	public String getName()
	{
		return "Unapproved Tickets";
	}

	@Override
	public List<Ticket> getTickets(boolean refresh)
	{
		// get all unapproved tickets
		// LocalDate lastMonth = now.minusMonths(1).withDayOfMonth(1);

		List<Ticket> unapproved = new ArrayList<>();

		AcceloFilter<Ticket> filter = new AcceloFilter<>();
		if (refresh) filter.refreshCache();
		filter.limit(1);

		int offset = 0;

		while (unapproved.size() < 50)
		{

			filter.offset(offset);

			// All closed tickets
			filter.where(filter.eq(Ticket_.contract, 0).and(filter.eq(Ticket_.standing, Ticket.Standing.closed))
					.and(filter.after(Ticket_.date_started, LocalDate.of(2017, 03, 01))))
					.orderBy(Ticket_.id, Order.DESC);

			filter.showHashCode();

			// filter.where(filter.eq(Activity_.against_type, AgainstType.ticket).and(Activity_.));
			TicketDao daoTicket = new TicketDao();

			List<Ticket> closedTickets = daoTicket.getByFilter(filter);

			if (closedTickets.isEmpty() || offset == 2)
				break; // no more closed tickets.

			// Now just those that have unapproved work.
			unapproved.addAll(closedTickets.parallelStream().filter(ticket -> daoTicket.isWorkApproved(ticket))
					.collect(Collectors.toList()));

			offset += 1;
		}

		return unapproved;
	}
	
	@Override
	public String buildURL(Ticket ticket)
	{
		return "?action=approve_object&object_id=" + ticket.getId() + "&object_table=issue";
	}

}
