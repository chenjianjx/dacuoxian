import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

//TODO: add more info for every throw 
public class Dacuoxian {

	private static final String TEMP_FILE_PREFIX = "dacuoxian";

	public static void main(String[] args) {
		if (args == null || args.length < 1) {
			printUsage();
			return;
		}

		// assign arguments to variables
		String mode = "enable";
		String snippetFileName = null;
		if (args[0].equals("disable")) {
			mode = "disable";
			if (args.length < 2) {
				printUsage();
				return;
			}
			snippetFileName = args[1];
		} else {
			snippetFileName = args[0];
		}

		System.out.println("mode is " + mode + ". snippetFile is " + snippetFileName + "\n");

		// validation
		File hostsFile = new File("/etc/hosts");
		File snippetFile = new File(snippetFileName);
		doValidation(hostsFile, snippetFile);

		// back-up existing hostsFile
		backupHosts(hostsFile);

		// clean snippet file
		snippetFile = cleanSnippet(snippetFile);

		// rearrange snippet file
		snippetFile = rearrangeSnippet(snippetFile);

		// remove occurrences of snippet domains in the existing hosts file
		List<String> snipptDomains = extractDomains(snippetFile);
		String existingHosts = removeDomainsFromExistingHosts(hostsFile, snipptDomains);

		// do the combination
		String finalVersionStr = null;
		if (mode.equals("disable")) {

			finalVersionStr = existingHosts;
		} else {
			finalVersionStr = combine(existingHosts, snippetFile);
		}

		System.out.println("The final version: \n" + finalVersionStr);

		// write to the hosts
		writeFinalVersion(hostsFile, finalVersionStr);

	}

	private static void writeFinalVersion(File hostsFile, String finalVersionStr) {
		try {
			writeStringToFile(hostsFile, finalVersionStr);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String combine(String existingHosts, File snippetFile) {
		try {
			StringBuffer finalVersion = new StringBuffer();
			finalVersion.append(existingHosts + "\n");
			for (String line : readLines(snippetFile)) {
				finalVersion.append(line + "\t\t\t#added by dacuoxian-java \n");
			}
			String finalVersionStr = finalVersion.toString();
			return finalVersionStr;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static String removeDomainsFromExistingHosts(File hostsFile, List<String> snipptDomains) {
		String currentHostsStr = null;
		try {
			currentHostsStr = readFileToString(hostsFile);

			for (String domain : snipptDomains) {
				currentHostsStr = replaceAll(currentHostsStr, domain, "");
			}
			// after removal, there may be lines which has only ip no domains.
			// Remove them
			StringBuffer result = new StringBuffer();
			String[] lines = currentHostsStr.split("\\n+");
			for (String line : lines) {
				line = line.trim();
				line = removeHostLineComment(line);
				if (line.matches("\\d.*") && line.split("\\s+").length == 1) {
					continue; // that's what we are talking about: orphan IP
								// line
				}
				result.append(line + "\n");
			}
			String resultStr = result.toString();
			File newFile = File.createTempFile(TEMP_FILE_PREFIX, "anything");
			writeFinalVersion(newFile, resultStr);
			System.out.println("Existing file with new domains removed turns out to be  " + newFile);
			return resultStr;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private static void writeStringToFile(File file, String resultStr) throws IOException {
		PrintWriter writer = new PrintWriter(new FileWriter(file));
		writer.print(resultStr);
		writer.close();
	}

	private static String replaceAll(String text, String repl, String with) {
		return replace(text, repl, with, -1);
	}

	private static String replace(String text, String repl, String with, int max) {
		if (isEmpty(text) || isEmpty(repl) || with == null || max == 0) {
			return text;
		}
		int start = 0;
		int end = text.indexOf(repl, start);
		if (end == -1) {
			return text;
		}
		int replLength = repl.length();
		int increase = with.length() - replLength;
		increase = (increase < 0 ? 0 : increase);
		increase *= (max < 0 ? 16 : (max > 64 ? 64 : max));
		StringBuffer buf = new StringBuffer(text.length() + increase);
		while (end != -1) {
			buf.append(text.substring(start, end)).append(with);
			start = end + replLength;
			if (--max == 0) {
				break;
			}
			end = text.indexOf(repl, start);
		}
		buf.append(text.substring(start));
		return buf.toString();
	}

	private static boolean isEmpty(String str) {
		return str == null || str.length() == 0;
	}

	private static String readFileToString(File file) throws IOException {

		Reader reader = new FileReader(file);
		Writer writer = new StringWriter();

		// Transfer bytes from in to out
		char[] buf = new char[1024];
		int len;
		while ((len = reader.read(buf)) > 0) {
			writer.write(buf, 0, len);
		}
		reader.close();
		writer.close();

		return writer.toString();

	}

	private static List<String> extractDomains(File file) {
		try {
			List<String> lines = readLines(file);
			List<String> domains = new ArrayList<String>();
			for (String line : lines) {
				String domain = line.split("\\s+")[1];
				domains.add(domain);
			}
			return domains;
		} catch (IOException e) {
			throw new IllegalArgumentException();
		}
	}

	private static File rearrangeSnippet(File snippetFile) {
		try {
			List<String> lines = readLines(snippetFile);
			List<String> resultLines = new ArrayList<String>();
			for (String line : lines) {
				line = removeHostLineComment(line);

				String[] segs = line.split("\\s+");

				for (int i = 1; i < segs.length; i++) {
					resultLines.add(segs[0] + "\t" + segs[i]);
				}

			}
			File newFile = File.createTempFile(TEMP_FILE_PREFIX, "anything");
			writeLines(newFile, resultLines);
			System.out.println("Snippet file rearranged  as " + newFile);
			return newFile;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

	}

	private static String removeHostLineComment(String line) {
		int sharpIndex = line.indexOf("#");
		if (sharpIndex >= 0) {
			line = line.substring(0, sharpIndex); // leave out comments
		}
		return line;
	}

	private static File cleanSnippet(File snippetFile) {
		try {
			List<String> lines = readLines(snippetFile);
			List<String> cleanedLines = new ArrayList<String>();
			for (String line : lines) {
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				if (!line.matches("\\d.*")) {
					continue;
				}
				cleanedLines.add(line);
			}
			File newFile = File.createTempFile(TEMP_FILE_PREFIX, "anything");
			writeLines(newFile, cleanedLines);
			System.out.println("Snippet file cleaned  as " + newFile);
			return newFile;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

	}

	private static void backupHosts(File hostsFile) {
		try {
			File newFile = File.createTempFile(TEMP_FILE_PREFIX, "anything");
			copyFile(hostsFile, newFile);
			System.out.println("Hosts file backed up as " + newFile);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}

	}

	private static void doValidation(File hostsFile, File snippetFile) {
		// validation
		if (!snippetFile.exists()) {
			throw new IllegalArgumentException("File doesn't exist: " + snippetFile);
		}

		if (snippetFile.isDirectory()) {
			throw new IllegalArgumentException("File should not be a directory: " + snippetFile);
		}

		if (!snippetFile.canRead()) {
			throw new IllegalArgumentException("Cannot read file : " + snippetFile);
		}

		if (!hostsFile.canRead()) {
			throw new IllegalArgumentException("Cannot read file : " + hostsFile);
		}

		if (!hostsFile.canWrite()) {
			throw new IllegalArgumentException("Cannot write file : " + hostsFile);
		}
	}

	private static void printUsage() {
		System.err.println("Usage: java dacuoxian new-hosts-snippet-file-path");
		System.err.println("Usage: java dacuoxian disable hosts-snippet-file-path");
	}

	// TODO: use finally
	private static void copyFile(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	// TODO: use finally
	private static List<String> readLines(File file) throws IOException {
		List<String> lines = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		while (true) {
			String line = reader.readLine();
			if (line == null) {
				break;
			}
			lines.add(line);
		}
		reader.close();
		return lines;
	}

	// TODO: use finally
	private static void writeLines(File file, List<String> lines) throws IOException {
		PrintWriter writer = new PrintWriter(new FileWriter(file));
		for (String line : lines) {
			writer.println(line);
		}
		writer.close();

	}

}
