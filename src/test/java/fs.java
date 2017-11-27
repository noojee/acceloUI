import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

class fs
{

	static String getExtension(Path path)
	{
		return getExtension(path.toFile());
	}

	static String getExtension(File file)
	{
		String[] tokens = file.getName().split("\\.(?=[^\\.]+$)");

		return (tokens.length == 2 ? tokens[1] : "");
	}

	List<File> list(String root, String extension)
      {
    	  try
    	  {
          List<File> files;
			files = Files.list(FileSystems.getDefault().getPath(root)).filter(p -> getExtension(p)
			      .equals(extension)).map(p -> p.toFile()).collect(Collectors.toList());
			return files;
			
	  }
	  catch (java.lang.Exception e)
	  {
		  throw new fs.Exception(e);
	  }

      }

	Path getPath(String path)
	{
		try
		{
			try (FileSystem af = FileSystems.getDefault())
			{
				return af.getPath(".");
			}
		}
		catch (java.lang.Exception e)
		{
			throw new fs.Exception(e);
		}
	}
	
	static String owner(String filePath)
	{
		return owner(new File(filePath));
	}

	/**
	 * returns the name of the unix owner
	 * 
	 * @param file
	 * @return
	 */
	static String owner(File file)
	{
		try
		{
			return java.nio.file.Files.getOwner(file.toPath()).getName();
		}
		catch (IOException e)
		{
			throw new fs.Exception(e);
		}
	}
	
	/**
	 * Run a command passing any args.
	 * Returns the processes exit code.
	 * 
	 * @param command
	 * @return
	 */
	static int runBash(String... command)
	 {
		String[] args = command.clone();
		
		List<String> bashArgs = new ArrayList<>();
		bashArgs.add("/bin/bash");
		bashArgs.add( "-c");
		
		for (String arg : args)
		{
			bashArgs.add(arg);
		}
		try
		{
			Process p = new ProcessBuilder().inheritIO().command(bashArgs).start();
			return p.waitFor();
		}
		catch (java.lang.Exception e)
		{
			throw new fs.Exception(e);
		}
	 }


	static class Exception extends java.lang.Error
	{
		private static final long serialVersionUID = 1L;

		public Exception(java.lang.Exception e)
		{
			super(e);
		}

	}
	

}
