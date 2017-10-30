package au.com.noojee.acceloUI.util;

import com.vaadin.shared.Registration;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.UI;

import rx.Observable;
import rx.Observer;
import rx.Subscription;

public enum ButtonEventSource
{
	; // no instances

	static public class ClickSubscription implements Subscription
	{
		private final Observer<? super ClickEvent> observer;
		Registration buttonClickRegistration;

		ClickSubscription(final Observer<? super ClickEvent> observer, final Button button)
		{
			this.observer = observer;
			buttonClickRegistration = button.addClickListener(this.listener);
		}

		final ClickListener listener = new ClickListener()
		{
			private static final long serialVersionUID = 1L;

			@Override
			public void buttonClick(ClickEvent event)
			{
				ClickSubscription.this.observer.onNext(event);
				
			}
		};

		@Override
		public void unsubscribe()
		{
			UI.getCurrent().access(() -> buttonClickRegistration.remove());
			// ClickSubscription.this.button.removeClickListener(ClickSubscription.this.listener));
		}
	}

	public static Observable<ClickEvent> fromActionOf(final Button button)
	{
		return Observable.create(observer -> new ClickSubscription(observer, button));
	}
}