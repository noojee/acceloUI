package au.com.noojee.acceloUI.views;

import com.vaadin.icons.VaadinIcons;
import com.vaadin.ui.Button;

public class IconButton extends Button
{
	private static final long serialVersionUID = 1L;

	IconButton(String description, VaadinIcons icon, ClickListener listener)
	{
		super(icon);
		this.setDescription(description);

		this.addClickListener(listener);
	}

	public IconButton(String description, VaadinIcons icon)
	{
		super(icon);
		this.setDescription(description);

	}
}