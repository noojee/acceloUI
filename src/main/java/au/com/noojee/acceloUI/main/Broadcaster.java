package au.com.noojee.acceloUI.main;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Broadcaster
{

	private static final List<BroadcastListener> listeners = new CopyOnWriteArrayList<BroadcastListener>();

	public static void register(BroadcastListener listener)
	{
		listeners.add(listener);
	}

	public static void unregister(BroadcastListener listener)
	{
		listeners.remove(listener);
	}

	public static void broadcast(final BroadcastMessage message)
	{
		for (BroadcastListener listener : listeners)
		{
			listener.receiveBroadcast(message);
		}
	}

	public interface BroadcastListener
	{

		void receiveBroadcast(BroadcastMessage message);
	}

}