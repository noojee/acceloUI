package au.com.noojee.acceloUI.authentication.oauth;

import java.net.URL;
import java.util.List;

public class Person
{
	String kind;
	String etag;
	List<Email> emails;
	

	String objectType;
	String id;
	String displayName;
	Name name;
	Image image;
	boolean isPlusUser;
	int circledByCount;
	boolean verified;
	String domain;
	
	
	public List<Email> getEmails()
	{
		return emails;
	}

	
	@Override
	public String toString()
	{
		return "Person [kind=" + kind + ", etag=" + etag + ", emails=" + emails + ", objectType=" + objectType + ", id="
				+ id + ", displayName=" + displayName + ", name=" + name + ", image=" + image + ", isPlusUser="
				+ isPlusUser + ", circledByCount=" + circledByCount + ", verified=" + verified + ", domain=" + domain
				+ "]";
	}

	static class Name
	{
		String familyName;
		String givenName;

		
		@Override
		public String toString()
		{
			return "Name [familyName=" + familyName + ", givenName=" + givenName + "]";
		}
	}
	
	static class Image
	{
		URL url;
		boolean isDefault;
		
		@Override
		public String toString()
		{
			return "Image [url=" + url + ", isDefault=" + isDefault + "]";
		}
	}
	
	public static class Email
	{
		private String value;
		String type;
		
		@Override
		public String toString()
		{
			return "Email [value=" + getValue() + ", type=" + type + "]";
		}

		public String getValue()
		{
			return value;
		}
	}


}
