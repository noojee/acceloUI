package au.com.noojee.acceloUI.authentication;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;

import au.com.noojee.acceloUI.forms.UserProvider;
import au.com.noojee.acceloapi.entities.Staff;

/**
 * Class for retrieving and setting the name of the current user of the current
 * session (without using JAAS). All methods of this class require that a
 * {@link VaadinRequest} is bound to the current thread.
 * 
 * 
 * @see com.vaadin.server.VaadinService#getCurrentRequest()
 */
public final class CurrentUser implements UserProvider
{

	Staff staff;
	
	/**
	 * The attribute key used to store the username in the session.
	 */
	public static final String CURRENT_USER_SESSION_ATTRIBUTE_KEY = CurrentUser.class.getCanonicalName();

	private CurrentUser(Staff staff)
	{
		this.staff = staff;
	}
	
	

	/**
	 * Returns the name of the current user stored in the current session, or an
	 * empty string if no user name is stored.
	 * 
	 * @throws IllegalStateException
	 *             if the current session cannot be accessed.
	 */
	public static CurrentUser get()
	{
		CurrentUser currentUser = (CurrentUser) getCurrentRequest().getWrappedSession()
				.getAttribute(CURRENT_USER_SESSION_ATTRIBUTE_KEY);
		if (currentUser == null)
		{
			return null;
		}
		else
		{
			return currentUser;
		}
	}

	/**
	 * Sets the name of the current user and stores it in the current session.
	 * Using a {@code null} username will remove the username from the session.
	 * 
	 *             if the current session cannot be accessed.
	 */
	public static void set(Staff staff)
	{
		
		if (staff == null)
		{
			getCurrentRequest().getWrappedSession().removeAttribute(CURRENT_USER_SESSION_ATTRIBUTE_KEY);
		}
		else
		{
			CurrentUser user = new CurrentUser(staff);
			getCurrentRequest().getWrappedSession().setAttribute(CURRENT_USER_SESSION_ATTRIBUTE_KEY, user);
		}
	}

	private static VaadinRequest getCurrentRequest()
	{
		VaadinRequest request = VaadinService.getCurrentRequest();
		if (request == null)
		{
			throw new IllegalStateException("No request bound to current thread");
		}
		return request;
	}



	public String getEmailAddress()
	{
		return staff.getEmail();
	}



	public String getEmailSignature()
	{
		return "Noojee Accounts accounts@noojee.com.au";
	}



	static public boolean isUserSignedIn()
	{
		return CurrentUser.get() != null && CurrentUser.get().staff != null;
	}
}
