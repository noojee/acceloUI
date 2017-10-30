package au.com.noojee.acceloUI.main;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Label;

public class ErrorLabel extends Label {

	private static final long serialVersionUID = 1L;

	public ErrorLabel(String message) {
		super(message);
		
	}

	public void setContentMode(ContentMode html) {
		super.setContentMode(html);

	}

}
