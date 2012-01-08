package net.yapbam.relnotes;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//import net.yapbam.gui.LocalizationData;
import net.yapbam.util.StringUtils;

public class ReleaseNotesFormatter {
	private boolean ignoreNext;
	
	private boolean divOpened;
	private boolean needSeparator;
	private boolean firstOne;
	private List<String> known;
	private List<String> changes;
	private List<String> fixes;
	private List<String> currentList;
	private boolean ignoredVersion;
	
	private BufferedWriter writer;
	
	// Fields used to store the wordings
	private String next;
	private String knownSingular;
	private String knownPlural;
	private String changeSingular;
	private String changePlural;
	private String fixSingular;
	private String fixPlural;
	
	public ReleaseNotesFormatter() {
		this.ignoreNext = false;
		
		this.ignoredVersion = false; // True if the current version has to be ignored
		this.divOpened = false; // Has the version opened a div element (the "not yet released" version didn't open a div element)
		this.needSeparator = false; // Is a separator needed between current version and next one
		this.firstOne = true; // Is it the first already released version ?
		this.changes = new ArrayList<String>(); // The current version's changes
		this.fixes = new ArrayList<String>(); // The current version's bug fixes
		this.known = new ArrayList<String>(); // The current version's known bugs
		this.currentList = null; // The array (changes or fix) we are currently reading
	}
	
	/** Sets the property to ignore the next version in the generation.
	 * @param ignoreNext true to omit the next version and start the generated html to the first official release. False to include it.
	 */
	public void setIgnoreNext(boolean ignoreNext) {
		this.ignoreNext = ignoreNext;
	}
	
	private void echoHead() throws IOException {
		echo("<html>");
		echo("<head>");
	  echo("<style type=\"text/css\">");
	  echo(".relnotes-version {");
	  echo("background: #f0f0f0;");
	  echo("margin-bottom: 10px;");
	  echo("padding-left: 5px;");
	  echo("}");
	  echo(".relnotes-bugFix {");
	  echo("background: #f8fff8;");
	  echo("color: #202020;");
	  echo("padding-left: 10px;");
	  echo("margin-bottom: 5px;");
	  echo("margin-right: 5px;");
	  echo("}");
	  echo("h2 {");
	  echo("font-size: 1.2em;");
	  echo("}");
	  echo("</style>");
	  echo("</head>");
	  echo("<body>");
	}
	
	private void echoBottom() throws IOException {
	  echo("</body>");
		echo("</html>");
	}

	public synchronized void build(BufferedReader reader, BufferedWriter writer) throws IOException {
		this.writer = writer;
		this.echoHead();
		// Read the wordings (there at the first line)
		{
		String line = reader.readLine();
		if (line==null) throw new EOFException();
		String[] fields = StringUtils.split(line, '\t');
		if (fields.length<7) throw new EOFException();
		this.next=fields[0];
		this.knownSingular=fields[1];
		this.knownPlural=fields[2];
		this.changeSingular=fields[3];
		this.changePlural=fields[4];
		this.fixSingular=fields[5];
		this.fixPlural=fields[6];
		}
		int lineNumber = 1;
		for (String line=reader.readLine(); line!=null; line=reader.readLine()) {
			lineNumber++;
			String[] fields = StringUtils.split(line, '\t');
			String code = fields[0].trim();
			line = fields.length>1?fields[1].trim():"";
			if (code.equals("version")) {
				openVersion(line);
			} else if (code.equals("improvement")) {
				this.currentList = this.changes;
			} else if (code.equals("fix")) {
				this.currentList = this.fixes;
			} else if (code.equals("known")) {
				this.currentList = this.known;
			} else if (code.length()==0) {
				if (line.length()!=0) this.currentList.add(line);
			} else {
				wrongLine(lineNumber);
			}
			if (fields.length>2) {
				wrongLine(lineNumber);
			}
		}
		this.closeVersion();
		this.echoBottom();
		this.writer.flush();
	}

	private void wrongLine(int lineNumber) {
		System.err.println ("It seems, line "+lineNumber+" is wrong");
	}
	
	private void openVersion(String version) throws IOException {
		this.closeVersion();
		if (version.equals("next")) {
			if (this.ignoreNext) {
				this.ignoredVersion = true;
			} else {
				// Version "not yet released"
				echo ("<h2>"+this.next+"</h2>");
				this.needSeparator = true; // There's always a separator between next version and others
			}
		} else {
			// Other versions
			if (this.needSeparator) {
				echo ("<hr/>");
			}
			echo ("<div class=\"relnotes-version\"><h2>"+version+"</h2>");
			this.divOpened = true;
			this.needSeparator = this.firstOne;
			this.firstOne = false;
		}
	}
	
	private void closeVersion() throws IOException {
		if (!this.ignoredVersion) {
			this.output(this.known, "relnotes-knownBugs", this.knownSingular, this.knownPlural);
			this.output(this.changes, "relnotes-new", this.changeSingular, this.changePlural);
			this.output(this.fixes, "relnotes-bugFix", this.fixSingular, this.fixPlural);
			this.known.clear();
			if (this.divOpened) {
				echo ("</div>");
				this.divOpened = false;
			}
		}
		this.changes.clear();
		this.fixes.clear();
		// known bugs are propagated to next release
		this.ignoredVersion = false;
	}
	
	private void output(List<String> array, String className, String singular, String plural) throws IOException {
		if (array.size()!=0) {
			String title = (array.size()==1) ? singular : plural;
			echo ("<div class=\""+className+"\"><h3>"+title+"</h3><ul>");
			for (String line : array) {
				echo ("<li>"+line+"</li>");
			}
			echo ("</ul></div>");
		}
	}

	private void echo(String line) throws IOException {
		this.writer.write(line);
		this.writer.newLine();
	}
}
