package au.com.noojee.acceloUI.authentication.oauth;

import org.vaadin.addon.oauthpopup.OAuthPopupButton;
import org.vaadin.addon.oauthpopup.OAuthPopupConfig;

import com.github.scribejava.apis.GoogleApi20;
import com.vaadin.server.ClassResource;

public class GoogleOAuthButton extends OAuthPopupButton
{
	private static final long serialVersionUID = 1L;

	/**
	 * Vaadin {@link OAuthPopupButton} used to initiate OAuth authorization of Google API services. 
	 * 
	 * @param key Google API client ID.
	 * @param secret Google API client secret.
	 * @param scope Google API scope.
	 */
	public GoogleOAuthButton(OAuthPopupConfig config) {
		super(GoogleApi20.instance(), config);
		setIcon(new ClassResource("/org/vaadin/addon/oauthpopup/icons/google16.png"));
		setCaption("Google");
	}
}