package au.com.noojee.acceloUI.authentication.oauth;

public interface AuthListener<E>
{
	/**
	 * The entity is now available.
	 * @param entity
	 */
	abstract public void onAuthenticated(E entity);
	
	/**
	 * We failed to fetch the entity
	 * @param entity
	 */
	abstract public void onError(String error);

}
