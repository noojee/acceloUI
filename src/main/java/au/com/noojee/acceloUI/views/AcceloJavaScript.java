package au.com.noojee.acceloUI.views;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AcceloJavaScript
{
	final String script;
	
	AcceloJavaScript(String filename) throws IOException, URISyntaxException
	{
		ClassLoader classLoader = AcceloJavaScript.class.getClassLoader();
		
		// The file must be located in the resource directory.
		URL resource = classLoader.getResource(new File("javascript/" , filename).getPath());
		
//		if (resource == null)
//			throw new FileNotFoundException(filename);
		
//		File file = new File(resource.getFile());
		
		Path path = Paths.get(resource.toURI());
		List<String> readAllLines = Files.readAllLines(path);
		Stream<String> stream = readAllLines.stream();
		// FileReader fr = new FileReader(file);
		
		script = stream
				.collect(Collectors.joining("\n"));
	}
	
	public String toString()
	{
		return script;
	}

}
