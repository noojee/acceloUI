package au.com.noojee.acceloUI.views.ticketFilters;

public class NoContractDescFilter extends NoContractAscFilter
{

	@Override
	public String getName()
	{
		return "No Contracts Desc";
	}

	boolean isAscending()
	{
		return false;
	}
	
}
