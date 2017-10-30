package au.com.noojee.acceloUI.forms;

public interface ProgressListener<T>
{

	void progress(int count, int max, String message);

	void complete(int sent);

	void itemError(Exception e, String status);

	void exception(Exception e);

}
