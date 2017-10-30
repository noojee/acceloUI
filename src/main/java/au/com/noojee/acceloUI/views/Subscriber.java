package au.com.noojee.acceloUI.views;

public interface Subscriber<T>
{

	void onNext(T container);

	void onComplete();

	void onError(Exception e);

}
