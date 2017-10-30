package au.com.noojee.acceloUI.authentication.oauth;

import java.io.InputStreamReader;

import com.github.scribejava.apis.GoogleApi20;
import com.github.scribejava.core.builder.api.DefaultApi20;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import au.com.noojee.acceloapi.AcceloException;

public class ApiInfo
{
	public final String name;
	public final GoogleApi20 scribeApi;
	public final String apiKey;
	public final String apiSecret;
	public final String exampleGetRequest;

	static private ApiInfo self = null;
	
	private  ApiInfo(String name, GoogleApi20 scribeApi, String apiKey, String apiSecret, String exampleGetRequest)
	{
		super();
		this.name = name;
		this.scribeApi = scribeApi;
		this.apiKey = apiKey;
		this.apiSecret = apiSecret;
		this.exampleGetRequest = exampleGetRequest;
	}

	static public ApiInfo getInstance()
	{
		if (self == null)
		{
			self = ApiInfo.readClientSecrets("/secrets/oauthclient-noojee-sla-management.json", "Google",
					GoogleApi20.instance(), "https://www.googleapis.com/plus/v1/people/me");
			if (self == null)
				throw new AcceloException("OAuth ApiInfo couldn't be loaded.");
		}
		
		return self;
		
	}
	
	// Client secrets are stored in a JSON file on the classpath in the
	// following format:
	// { "client_id": "my client id", "client_secret": "my client secret" }
	static private ApiInfo readClientSecrets(String resourcePath, String name, GoogleApi20 scribeApi, String getEndpoint)
	{
		
		if (ApiInfo.class.getResource(resourcePath) != null)
		{
			// JsonNode web = new
			// ObjectMapper().readTree(ApiInfo.class.getResourceAsStream(resourcePath));

			Gson gson = new Gson();
			JsonReader reader = new JsonReader(new InputStreamReader(ApiInfo.class.getResourceAsStream(resourcePath)));
			ClientSecret clientSecret = gson.fromJson(reader, ClientSecret.class);

			if (clientSecret != null)
			{
				self = new ApiInfo(name, scribeApi, clientSecret.client_id, clientSecret.client_secret, getEndpoint);
			}
		}
		return self;
	}

	
	public String getClientKey()
	{
		return self.apiKey;
	}
	
	public String getClientSecret()
	{
		return self.apiSecret;
	}
	

	public DefaultApi20 getScribeApi()
	{
		return self.scribeApi;
	}



	
	class ClientSecret
	{
		String client_id;
		String client_secret;

		public ClientSecret()
		{

		}
	}




	
}