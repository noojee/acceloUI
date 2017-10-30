package au.com.noojee.acceloUI.views;

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.UI;

public class UIAccessor
{
	private final UI ui;
	private VaadinSession session;
	private final Runnable runnable;
	/*
	 * if true the Vaadin UI is locked whilst the run method executes. If false
	 * then you are responsible for locking the UI. If the thread takes an
	 * extended period of time to run you don't want to lock up the vaadin UI
	 * for the whole time so you should pass false and then call ui.access
	 * yourself when you are ready to update the UI.
	 */
	private boolean lockUI;

	UIAccessor(boolean lockUI, Runnable runnable)
	{
		// Save the UI
		this.ui = UI.getCurrent();
		this.session = VaadinSession.getCurrent();
		this.runnable = runnable;
		this.lockUI = lockUI;
	}

	
	public UIAccessor(UI ui, boolean lockUI,  Runnable runnable)
	{
		// Save the UI
		this.ui = ui;
		this.session = this.ui.getSession();
		this.runnable = runnable;
		this.lockUI = lockUI;
	}

	/** 
	 * Use this method to run the runnable on the current thread (handy for parallelStreams).
	 */
	public void run()
	{
		// Push the current UI into the background thread.
		UI.setCurrent(ui);
		VaadinSession.setCurrent(session);

		if (lockUI)
			ui.access(runnable);
		else
			runnable.run();

	}

	/**
	 * Use this method to start the runnable on a new thread.
	 * @param lockUI
	 */
	public void start()
	{

		new Thread(() -> UIAccessor.this.run()).start();

	}
}
