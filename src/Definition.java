import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Scanner;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class Definition {
	/**
	 * Downloads a page as a String from the URL
	 * @param url The page location
	 * @return A string that is the contents of the page
	 * @throws IOException If there is an error either discovering the page or reading from it
	 */
	static String downloadPage(URL url) throws IOException{
		String allText = "";
		InputStream reader = url.openStream();
		Scanner sc = new Scanner(reader);
		while(sc.hasNextLine()){
			allText += sc.nextLine() + "\n";
		}
		sc.close();
		return allText;
	}
	/**
	 * Downloads a page as a String from the String fileName that is the represetation of the URL (such as http://www.google.com)
	 * @param fileName The URL as a String
	 * @return A string that is the contents of the page
	 * @throws IOException If there is an error either discovering the page or reading from it
	 */
	static String downloadPage(String fileName) throws IOException{
		URL url = new URL(fileName);
		return downloadPage(url);
	}
	/**
	 * Gets the definition of a word. Also saves the word to file in the definitions folder if the definition exists
	 * @param word The String that is the word to define
	 * @return A String that is the definition of the word
	 * @throws IOException If an IOException occurs
	 * @throws Exception If another type of Exception occurs that is not an IOException
	 */
	static String defineWord(String word) throws IOException, Exception{
		String fileName = "http://dictionary.reference.com/browse/" + word;
		String allContents = downloadPage(fileName);
		int startPos = allContents.indexOf("<div class=\"source-data\">");
		String rest = allContents.substring(startPos);
		rest = rest.substring(rest.indexOf("1."), rest.indexOf("</section>"));
		rest = rest.replace("</span>", "");
		rest = rest.replace("</a>", "");
		while(rest.indexOf("<") != -1){
			rest = rest.replace(goUntilGT(rest), "");
		}
		rest = rest.replace(">", "");
		rest = rest.replace("<span class=\"def-number\">", "");
		rest = rest.replace("</div>", "");
		rest = rest.replace("<div class=\"def-content\">", "");
		rest = rest.replace("<div class=\"def-set\">", "");
		rest = rest.replace("<div class=\"def-block def-inline-example\">", "");
		rest = rest.replace("<span class>=\"dbox-example\">", "");
		rest = rest.replace("<span class=\"dbox-example\">", "");
		rest = rest.replace("<span class=\"dbox-italic\">", "");
		rest = rest.replace(".\n\n", ".\n");
		
		//remove all non-visble characters (the ones below 32) except for \n
		for(int i = 0; i<32; i++){
			if((char)i == '\n'){
				continue;
			}
			rest = rest.replace("" + (char)i, "");
		}
		while(rest.indexOf("\n\n") > -1){
			rest = rest.replace("\n\n", "\n");
		}
		while(rest.indexOf("  ") > -1){//double spaces
			rest = rest.replace("  ", " ");//replace double spaces with single ones
		}
		//replace \n with \r\n so it works on windows line terminators
		rest = rest.replace("\n", "\r\n");
		//save the def to file, even if there is already a definition for the word
		saveToFile(word, rest);
		return rest;
	}
	/**
	 * Private method that returns a string that goes from '<' to the next '>' char. Useful for removing HTML tags. <br>
	 * This method is used potentially many times in the defineWord method
	 * @param text The String to go through
	 * @return The String that is from '<' to '>', or "" if there is none.
	 */
	private static String goUntilGT(String text){
		int start = text.indexOf("<");
		if(start == -1){return "";}
		for(int i = start; i < text.length(); i++){
			if(("" + text.charAt(i)).equals(">")){
				return text.substring(start, i);
			}
		}
		return "";
	}
	/**
	 * Defines a word using the defineWord(String) method, showing a shell (window) that has the definition typed in.
	 * @param display The display that is for this program.
	 * @param shell Only used to set the location of this new window to the same as the other window. Must not be null
	 */
	static void defineWord(Display display, Shell shell){
		final Shell newShell = new Shell(display);
		newShell.setSize(600, 400);
		newShell.setText("Define a word");
		newShell.setLocation(shell.getLocation());
		Label newLabel = new Label(newShell, SWT.NONE);
		newLabel.setText("Enter the word and then click define");
		newLabel.setBounds(5, 5, 300, 25);
		final Text txt = new Text(newShell, SWT.SINGLE);
		final Text txtDef = new Text(newShell, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
		txt.setBounds(5, 30, 200, 20);
		txt.addKeyListener(new KeyListener(){

			@Override
			public void keyPressed(KeyEvent e) {
				if(e.keyCode == 13){
					String word = txt.getText();
					try {
						String result = defineWord(word);
						txtDef.setText(result);
					} catch (Exception e1) {
						MessageBox mb = new MessageBox(newShell, SWT.OK);
						mb.setMessage("Error downloading page");
						mb.setText("Error");
						mb.open();
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				//don't need to do anything here
			}
			
		});
		txtDef.setEditable(false);
		txtDef.setBounds(5, 60, 570, 290);
		
		Button newButton = new Button(newShell, SWT.PUSH);
		newButton.setBounds(210, 30, 100, 20);
		newButton.setText("Define");
		newButton.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetSelected(SelectionEvent e) {
				String word = txt.getText();
				try {
					String result = defineWord(word);
					txtDef.setText(result);
				} catch (Exception e1) {
					MessageBox mb = new MessageBox(newShell, SWT.OK);
					mb.setMessage("Error downloading page");
					mb.setText("Error");
					mb.open();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				//not called
			}
			
		});
		newShell.open();
		while(!newShell.isDisposed()){
			if(!display.readAndDispatch()){
				display.sleep();
			}
		}
	}
	
	static String defineWordUsingFileIfExists(String word) throws IOException, Exception{
		File tempFile = new File(NotesMain.defDir + word + ".txt");
		if(tempFile.exists()){
			try {
				String definition = readAll(word);
				return definition;
			} catch (IOException e) {
				
			}
		}
		//if the file doesn't exist then use the defineWord(String)
		return defineWord(word);
		
	}
	
	static String readAll(String fileName) throws IOException{
		FileReader fr = new FileReader(fileName);
		String result = "";
		int tChar = 0;
		while((tChar = fr.read()) != -1){
			result += "" + (char)tChar;
		}
		fr.close();
		return result;
	}
	
	static boolean isRomanNumeral(String word){
		//if all the characters in the word are roman numerals, then it is considered a Roman Numeral
		String possible_letters = "IVXLCDM";
		for(int i = 0; i < word.length(); i++){
			if(!possible_letters.contains("" + word.charAt(i))){
				return false;
			}
		}
		return true;
	}
	/**
	 * Saves the definition of a word to file, printing the error to the standard output stream (System.out) if any Exceptions occur.
	 * @param word The word that the definition is for. Note that the file that is saved to is the NotesMain.defDir + word + ".txt"
	 * @param contents The contents as a string of the definition.
	 */
	static void saveToFile(String word, String contents){
		//save to file
		try {
			File newFile = new File(NotesMain.defDir + word + ".txt");
			System.out.println("Log: adding a definition for word: " + newFile.getAbsolutePath());
			File dir = new File(NotesMain.defDir);
			if(!dir.isDirectory()){
				dir.mkdirs();
			}
			if(!newFile.exists()){
				FileWriter fw = new FileWriter(newFile);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(contents);
				bw.flush();
				bw.close();
				fw.close();
			}
		} catch (Exception e) {
			System.out.println("Error saving the definition of: " + word + " to file.");
		}
	}
	/**
	 * Creates an archive file of the definitions, prompting the user for the location to save the archive file
	 * <br>The archive file format is as follows:
	 * number of definitions + "\n"
	 * n lines consisting of "word.txt" + file.length(), where n is the number of definitions
	 * the contents of the files (no extra stuff added) the length devoted to each definition is the number at the header
	 * @param parent The parent shell to show both the FileDailog and MessageBox over
	 */
	static void createDefArchive(Shell parent){
		FileDialog fd = new FileDialog(parent, SWT.SAVE);
		String[] temp1 = {"*.defnar"};
		String[] temp2 = {"Definitions Archive file (*.defnar)"};
		fd.setFilterExtensions(temp1);
		fd.setFilterNames(temp2);
		String resultFn = fd.open();
		ArchiveThread at = new ArchiveThread(resultFn, ArchiveThread.Values.WRITE, parent);
		at.start();
	}
	/**
	 * Creates the definitions from the archive file
	 * @param parent The
	 */
	static void loadArchive(Shell parent){
		FileDialog fd = new FileDialog(parent, SWT.OPEN);
		String[] temp1 = {"*.defnar"};
		String[] temp2 = {"Definitions Archive file (*.defnar)"};
		fd.setFilterExtensions(temp1);
		fd.setFilterNames(temp2);
		String resultFn = fd.open();
		ArchiveThread at = new ArchiveThread(resultFn, ArchiveThread.Values.READ, parent);
		at.start();
	}
}
