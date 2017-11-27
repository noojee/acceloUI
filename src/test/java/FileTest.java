import java.io.IOException;
import java.math.BigInteger;

import org.junit.Test;

public class FileTest
{

	@Test
	public void test() throws IOException
	{
		String[] tokens = getCommandLineArgs();

		String[] args = tokens;

		System.out.println("args[0]" + args[0]);
		int p = Integer.parseInt(args[0]);

		BigInteger two = new BigInteger("2");
		System.out.print("n=" + p + ": 2^n - 1 = ");
		System.out.println(two.pow(p).subtract(BigInteger.ONE));
		
		doit();

		println("done");


	}
	
	String[] getCommandLineArgs()
	{
	String line = System.getProperty("args");

	String[] tokens = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

	return tokens;
	}

	void println(String line)
	{
	System.out.println(line);
	}

	int run(String... command)
	{
		try
		{
		ProcessBuilder pb = new ProcessBuilder().command(command);
		
		// Inherit System.out as redirect output stream
		pb.redirectErrorStream(true);
		pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
		
		Process p = pb.start();

		return p.waitFor();
		}
		catch (java.lang.Exception e)
		{
		}
		return 0;
	}

	void doit()
	{
		println("starting");
		String[] args = getCommandLineArgs();
		
		println("starting");
		if (args.length != 1)
		{
			println("Usage: listTrunks <nodename>");
			println("e.g.");
			println("./jshell listTrunks comshapers");
		}
		else
		{
		println("running");
			println("knife node attribute get " + args[0]);
			run("knife node attribute get " + args[0]);
		}
	}



}
