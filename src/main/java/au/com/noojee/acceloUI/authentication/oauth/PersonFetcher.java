package au.com.noojee.acceloUI.authentication.oauth;

public class PersonFetcher extends OAuthDataFetcher<Person>
{

	public PersonFetcher(AuthListener<Person> authListener)
	{
		super(authListener);
	}

	@Override
	String getURL()
	{
		return "https://www.googleapis.com/plus/v1/people/me";
	}

	@Override
	Class<Person> getEntityClass()
	{
		return Person.class;
	}

	
}