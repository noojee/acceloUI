package au.com.noojee.acceloUI.forms;


import java.io.File;

import javax.activation.FileDataSource;

import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.VerticalLayout;


public class AttachedFileLayout implements AttachedFile
{
	/**
	 *
	 */
	private final VerticalLayout attachedFilesLayout;
	private final File file;
	private final AbstractLayout line;
	private final boolean deleteAfterSend;

	public AttachedFileLayout(final VerticalLayout attachedFilesLayout, final File file, final AbstractLayout line, boolean deleteAfterSend)
	{
		this.attachedFilesLayout = attachedFilesLayout;
		this.file = file;
		this.line = line;
		this.deleteAfterSend = deleteAfterSend;
	}

	public void remove()
	{
		this.attachedFilesLayout.removeComponent(this.line);
		
		if (this.deleteAfterSend)
			this.file.delete();
	}

	@Override
	public File getFile()
	{
		return this.file;
	}

	public FileDataSource getDataSource()
	{
		return new FileDataSource(this.file);
	}
}