package au.com.noojee.acceloUI.authentication.oauth;

import java.io.IOException;

import org.vaadin.addon.oauthpopup.OAuthListener;
import org.vaadin.addon.oauthpopup.OAuthPopupConfig;

import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.Token;
import com.google.gson.JsonSyntaxException;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

public class GoogleAuthorizer<E> extends HorizontalLayout implements OAuthListener
{
	private static final long serialVersionUID = 1L;
	static ApiInfo apiInfo = null;

	private Label message;
	
	OAuthDataFetcher<E> responder;

	public GoogleAuthorizer(OAuthDataFetcher<E> responder)
	{
		this.responder = responder;
		
		OAuthPopupConfig config = OAuthPopupConfig.getStandardOAuth20Config(getApiKey(), getApiSecret());

		// config.setGrantType("authorization_code");
		// config.setScope("https://www.googleapis.com/auth/plus.login");

		config.setScope("https://www.googleapis.com/auth/userinfo.email");

		// config.setCallbackUrl("urn:ietf:wg:oauth:2.0:oob");
		GoogleOAuthButton button = new GoogleOAuthButton(config);

		button.addOAuthListener(this);

		// In most browsers "resizable" makes the popup
		// open in a new window, not in a tab.
		// You can also set size with eg. "resizable,width=400,height=300"
		button.setPopupWindowFeatures("resizable,width=600,height=500");
		button.setWidth("150px");

		this.setSpacing(true);
		this.addComponent(button);

		message = new Label("");
		this.addComponent(message);
		this.setComponentAlignment(message, Alignment.MIDDLE_CENTER);

	}

	static private String getApiKey()
	{
		return ApiInfo.getInstance().getClientKey();
	}

	static private String getApiSecret()
	{
		return ApiInfo.getInstance().getClientSecret();
	}

	@Override
	public void authSuccessful(final Token token, final boolean isOAuth20)
	{
		message.setValue("Authorized.");

		OAuth2AccessToken oauth2Token = (OAuth2AccessToken) token;
		oauth2Token.getAccessToken();

		
		((OAuth2AccessToken) token).getAccessToken();
		((OAuth2AccessToken) token).getRefreshToken();
		((OAuth2AccessToken) token).getExpiresIn();
		
		try
		{
			E entity = responder.get(oauth2Token);
			responder.onComplete(entity);
		}
		catch (JsonSyntaxException | IOException e)
		{
			responder.onError(e.getMessage());
		}
	}

	@Override
	public void authDenied(String reason)
	{
		responder.onError(reason);
	}

}
