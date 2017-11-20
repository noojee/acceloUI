package au.com.noojee.acceloUI.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

public class JobService
{
	static Logger logger = LogManager.getLogger();

	private UI ui;
	private ConcurrentHashMap<String, Job<?>> activeJobs = new ConcurrentHashMap<>();
	private ExecutorService executor;

	static private JobService self;

	private JobService(UI ui)
	{
		this.ui = ui;

		executor = Executors.newCachedThreadPool();
	}

	static public void init(UI ui)
	{
		self = new JobService(ui);
	}

	static public JobService getInstance()
	{
		return self;
	}

	public <R> Job<R> newJob(String jobName, Callable<R> callable)
	{
		Job<R> job = new Job<R>(jobName, callable);
		activeJobs.put(job.jobName, job);

		return job;
	}

	public <R> Job<R> newJob(String jobName, Callable<R> callable, JobCompleteListener<R> listener)
	{
		Job<R> job = new Job<R>(jobName, callable);
		job.addCompleteListener(listener);
		activeJobs.put(job.jobName, job);

		return job;
	}

	public <R> void complete(Job<R> job)
	{
		activeJobs.remove(job.jobName);
	}

	public void shutdown()
	{

		try
		{
			System.out.println("attempt to shutdown executor");
			executor.shutdown();
			executor.awaitTermination(5, TimeUnit.SECONDS);
		}
		catch (InterruptedException e)
		{
			System.err.println("tasks interrupted");
		}
		finally
		{
			if (!executor.isTerminated())
			{
				System.err.println("cancel non-finished tasks");
			}
			executor.shutdownNow();
			System.out.println("shutdown finished");
		}

	}

	public class Job<R> implements Callable<R>
	{
		private String jobName;
		private Callable<R> callable;
		private JobCompleteListener<R> listener;

		private Job(String jobName, Callable<R> callable2)
		{
			this.jobName = jobName;
			this.callable = callable2;
		}

		public void addCompleteListener(JobCompleteListener<R> listener)
		{
			this.listener = listener;
		}

		@Override
		public R call() throws Exception
		{
			R result = null;
			try
			{
				result = callable.call();

				if (this.listener != null)
				{
					JobService.this.complete(this);
					final R localResult = result;
					ui.access(() ->
						{
							this.listener.onComplete(localResult);
							SMNotification.show("Job Complete", jobName, Notification.Type.TRAY_NOTIFICATION);
						});
				}

			}
			catch (Throwable e)
			{
				logger.error(e,e);
				SMNotification.show("Error processing Job", e.getMessage(), Notification.Type.ERROR_MESSAGE);
			}
			return result;
		}

		public void submit()
		{
			// submit the job for execution.
			self.executor.submit(this);
		}
	}

}
