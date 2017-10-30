package au.com.noojee.acceloUI.authentication.oauth;

import java.io.IOException;

import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth20Service;
import com.github.scribejava.core.oauth.OAuthService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public abstract class OAuthDataFetcher<E>
{
	abstract String getURL();

	abstract Class<E> getEntityClass();
	
	AuthListener<E> authListener;
	
	protected OAuthDataFetcher(AuthListener<E> authListener)
	{
		this.authListener = authListener;
		
	}

	public E get(OAuth2AccessToken oauth2Token) throws JsonSyntaxException, IOException
	{
		final OAuthService service = createOAuthService();
		final OAuthRequest request = new OAuthRequest(Verb.GET, getURL(), service);
		((OAuth20Service) service).signRequest((OAuth2AccessToken) oauth2Token, request);

		Response resp = request.send();
		E entity = new Gson().fromJson(resp.getBody(), getEntityClass());
		
		return entity;
	}

	private OAuthService createOAuthService()
	{
		final ServiceBuilder sb = new ServiceBuilder().apiKey(ApiInfo.getInstance().getClientKey())
				.apiSecret(ApiInfo.getInstance().getClientSecret()).callback("http://www.google.fi");
		return sb.build((DefaultApi20) ApiInfo.getInstance().getScribeApi());

	}

	public void onComplete(E entity)
	{
		this.authListener.onAuthenticated(entity);
		// Notification.show("Complete", entity.toString(), Notification.Type.ERROR_MESSAGE);
		
	}

	public void onError(String error)
	{
		this.authListener.onError(error);
		// Notification.show("Error", error, Notification.Type.ERROR_MESSAGE);
		
	}

}
