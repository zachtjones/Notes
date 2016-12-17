import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public class Note extends Thread{ 
	public static final String BULLET_STRING = "" + (char)126;
	public static final String CONTROL_CHARS = " 0123456789!@#$%^&*()~`-_=+[]{}\\|\"\r\n\t;:?/,.<>";
	private boolean needsBullet = false;
	private String fileName;
	private TabItem ti;
	private TabFolder tf;
	private StyledText txt;
	
	private long totLength = 0;
	private long currentRead = 0;
	private boolean isReading = false;
	
	private SaveThread st;
	
	private String text = "";
	private ArrayList<String> wordsNotInDict = new ArrayList<String>();

	/**
	 * Constructor for a note that is from an existing file. You <b><u>MUST</b></u> call the start method of this object in order to read the file and add dictionary items.
	 * @param fileName The string representing the full file name of this note
	 * @param ti The TabItem on the UI
	 * @param tf The TabFolder on the UI
	 * @throws IOException This will throw an IOException if there is an error in creating the FileReader, BufferedReader, or if there is an issue decoding the file.
	 */
	public Note(String fileName, Tab ti, TabPane tf) throws IOException{
		System.out.println("Log: " + fileName + " file opened @" + new Date().toString());
		this.totLength = new File(fileName).length();
		this.currentRead = 0L;
		//use multithreading to load from a file as it could potentially be very large
		this.fileName = fileName;
		this.ti = ti;
		this.tf = tf;
		setStyledText();
		this.setName("Note thread - " + this.fileName);
	}
	@Override
	public void run(){
		System.out.println("Log: " + this.getName() + " note thread started @" + new Date().toString());
		long ct = System.currentTimeMillis();
		if(this.fileName != null){
			try {
				Display display = Display.getDefault();
				try {
					this.isReading = true;
					display.syncExec(new Runnable(){
						@Override
						public void run(){
							txt.setEditable(false);
						}
					});
					FileReader fr = new FileReader(this.fileName);
					BufferedReader br = new BufferedReader(fr);
					//read the first part by 2000 chars
					char[] line = new char[5000]; //read 2000 chars at a time
					int num = 0;
					while((num = br.read(line)) != -1){
						//build up the string
						String temp = "";
						for(int i = 0; i < num; i++){
							temp += line[i];
						}
						//shortcut for beating the 'can't refer to a non-final local variable in an enclosing scope'
						final String tempLine = temp;
						if(ti.isDisposed()) {
							br.close();
							fr.close();
							System.out.println(this.getName() + " thread stopped normally");
							return;
						}
						display.syncExec(new Runnable() {
							@Override
							public void run() {
								//add the text
								try {
									txt.append(tempLine);
								} catch (Exception e) {
								}
							}
						});
						this.currentRead += temp.length();
					}
						
					display.syncExec(new Runnable(){
						@Override
						public void run(){
							txt.setEditable(true);
							text = txt.getText();
						}
					});
					NotesMain.printTime(ct, "mills, finished reading file: " + this.fileName);
					br.close();
					fr.close();
				} catch (final IOException e) {
					try {
						display.syncExec(new Runnable() {
							@Override
							public void run() {
								MessageBox mb = new MessageBox(tf.getShell(), SWT.OK);
								mb.setMessage("There was an issue opening file: " + Note.this.fileName + ".\n" + e.getMessage());
								mb.setText("Error");
								mb.open();
							}
						});
					} catch (SWTException e1) {
						System.out.println("Display was disposed before it was done reading in a file.");
					}
				}
			} catch (Exception e) {
				System.out.println("The display was disposed before " + this.getName() + " was able to finish.");
			}
			this.isReading = false;
			
		}
		
		if(this.fileName == null){
			
		} else if(this.fileName.endsWith(".txt")){
			
		} else {
			System.out.println(this.getName() + " thread stopped normally");
			return; //if the filename is not from a new one or if it is not ending with txt then return
		}
		//this thread now deals with adding the words to dictionary
		while(true){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			String t = "" + text;//want a new instance of the String
			for(int i = 0; i < t.length(); i++){//loop through every character
				if(CONTROL_CHARS.contains("" + t.charAt(i))){
					continue; //go on to the next letter
				}
				String currentWord = "";
				for(int j = i; j < t.length(); j++){
					//go until a control char is reached
					if(CONTROL_CHARS.contains("" + t.charAt(j))){
						break;
					} else {
						currentWord += t.charAt(j);
					}
					
				}
				currentWord = currentWord.replace("'s", ""); //remove the possessives
				//process the current word
				File newFile = new File(NotesMain.defDir + currentWord + ".txt");
				if(!newFile.exists()){ //if there is not a dictionary entry for word and if it is not a roman numeral then it is misspelled
					if(!Definition.isRomanNumeral(currentWord)){
						//try to define the word
						if(!wordsNotInDict.contains(currentWord)){
							try {
								Definition.defineWord(currentWord);
							} catch (Exception e) {
								System.out.println("Could not define word: " + currentWord);
								wordsNotInDict.add(currentWord);
							}
						}
					}
				}
				i += currentWord.length(); //add on the length of the current word so that "you" won't be "you","ou","u"
			}
			
			//terminate this thread if the display is disposed
			if(ti.isDisposed()){
				System.out.println(this.getName() + " thread stopped normally");
				return;
			}
		}
	}
	/**
	 * Constructor for a note that is newly created (not from an existing file). 
	 * You <b><u>MUST</b></u> call the run method of this object in order to add dictionary items.
	 * @param ti The TabItem on the UI
	 * @param tf The TabFolder on the UI
	 */
	public Note(Tab ti, TabPane tf){
		System.out.println("Log: new note started @" + new Date().toString());
		this.setName("New note thread");
		this.ti = ti;
		this.tf = tf;
		setStyledText();
	}
	/**
	 * Gets the total file length of this note. Should only be used when this is reading
	 * @return The file length as a long data type
	 * @see getIsReading()
	 */
	public long getTotalFileLength(){
		return this.totLength;
	}
	/**
	 * Gets the current amount of characters/bytes read. Should only be used when this is reading
	 * @return The current amount of data read as a long data type
	 * @see getIsReading()
	 */
	public long getCurrentLengthRead(){
		return this.currentRead;
	}
	/**
	 * Gets whether this Note's run thread is currently reading from the file
	 * @return True if this is reading, else false
	 */
	public boolean getIsReading() {
		return this.isReading;
	}
	/**
	 * Gets this Note's text
	 * @return The text of this note. Note that this call is thread-safe, so it may be called from a thread other than the UI.
	 */
	public String getText(){
		return this.text;
	}
	
	/**
	 * Gets the file name of this note
	 * @return The file name of this note, or <b>null</b> if it was created using the Note(TabItem, TabFolder)
	 */
	public String getFileName() {
		return this.fileName;
	}
	/**
	 * Gets the tab item associated with this note
	 * @return The reference to the tab item
	 */
	public TabItem getTabItem() {
		return this.ti;
	}
	/**
	 * Saves the note to file. If the note was created using the + tab, then this calls the saveAs method
	 * @return true if the file saves properly, otherwise false
	 * @see Note.saveAs
	 */
	public void save(){
		if(this.fileName == null){
			saveAs();
			return;
		}
		//save to the fileName using mutilthreads
		st = new SaveThread(text, fileName, ti, tf);
		st.start();
	}
	/**
	 * Saves the note to the file specified by the fileDialog. 
	 * <br>The filename for this note and the text at the tab item are changed to match the selected file name
	 * @return true if the file saves properly, otherwise false
	 */
	public void saveAs(){
		FileDialog fd = new FileDialog(tf.getShell(), SWT.SAVE);
		fd.setText("Type the name of the file to save to");
		String[] temp = {"*.txt", "*.*"};
		String[] temp2 = {"Text files (*.txt)", "Any file type (*.*)"};
		fd.setFilterExtensions(temp);
		fd.setFilterNames(temp2);
		String result = fd.open();
		if(result == null){return;}
		fileName = result;
		//save to the fileName using mutilthreads
		st = new SaveThread(text, fileName, ti, tf);
		st.start();
	}
	/**
	 * Sets the styled text used by this Note. 
	 * Calling this method will reset the text, so only call it on creation in the constructor constructor
	 */
	private void setStyledText(){
		txt = new StyledText(tf, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		final Font thisFont = new Font(Display.getDefault(), "Courier New", 12, SWT.NORMAL);
		txt.setFont(thisFont);
		txt.setLineSpacing(8);
		txt.addDisposeListener(new DisposeListener(){

			@Override
			public void widgetDisposed(DisposeEvent e) {
				//dispose of the font
				thisFont.dispose();
			}
			
		});
		ti.setControl(txt);
		txt.addTraverseListener(new TraverseListener(){

			@Override
			public void keyTraversed(TraverseEvent e) {
				e.doit = false;
			}
			
		});
		/*
		txt.addMouseListener(new MouseListener(){

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				//don't need to do anything here
			}

			@Override
			public void mouseDown(MouseEvent e) {
				checkSpellingNearSelection();
			}

			@Override
			public void mouseUp(MouseEvent e) {
				//don't need to do anything here
				
			}
			
		});*/
		txt.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {
				if(isReading){return;}
				
				int thisLineIndex = txt.getLineAtOffset(txt.getSelection().x);
				if(fileName == null){ //only format if it is .txt or new
					needsBullet = (txt.getLine(thisLineIndex).indexOf(BULLET_STRING) == -1);
					format(e);
				} else if(fileName.endsWith(".txt")){
					needsBullet = (txt.getLine(thisLineIndex).indexOf(BULLET_STRING) == -1);
					format(e);
				}
				needsBullet = false;
				
				//check the spelling near the selection if a space was pressed
				if(e.character == ' '){
					checkSpellingNearSelection();
				}
				
				if(e.stateMask == SWT.CONTROL){ //use Ctrl combinations
					if((char)e.keyCode == 's'){
						save(); //if it is Ctrl s, then save
					}
					if((char)e.keyCode == 'e'){
						checkSpelling();
					}
					if((char)e.keyCode == 'a'){
						txt.selectAll();
					}
					
				}
				if(e.character != 0){
					if(!ti.getText().contains("*")){
						ti.setText(ti.getText() + "*");
					}
				}
				
			}

			@Override
			public void keyReleased(KeyEvent e) {	}
		});
	}
	/**
	 * Internal method used for formatting
	 * @param e
	 */
	private void format(KeyEvent e){
		try {
			//save this text so it can be acessed by another thread 
			text = txt.getText();
			//keep track of bullets
			switch(e.character){
			case '\r':
				//Do something with the enter
				txt.insert(" ");
				txt.setSelection(txt.getSelection().x + 1);
				break;
			case '\t':
				int currentSelection = txt.getSelection().x;
				e.doit = false;
				
				//handle the tabs
				if(e.stateMask == SWT.SHIFT){
					e.doit = false;
					int temp = getLineIndex(txt.getText(), currentSelection);
					txt.setSelection(temp, temp+1);
					char c = txt.getText().charAt(txt.getSelection().x);
					if(c != '\t'){
						txt.setSelection(currentSelection-1);
						return;
					} else {
						txt.insert("");
						txt.setSelection(currentSelection-2, currentSelection-1);
						txt.insert("");
						txt.setSelection(currentSelection-1);
						break;
					}
				} else {
					//go through and find the bullet text and place the bullets if necessary (character 149 is bullet)
					
					
					e.doit = false;
					int temp = getLineIndex(txt.getText(), currentSelection);
					
					txt.setSelection(temp);
					txt.insert("\t");
					txt.setSelection(currentSelection+1);
					if(temp == currentSelection){
						txt.setSelection(currentSelection+1);
					}
					
					int tempPos = getLineIndex(txt.getText(), txt.getSelection().x);
					String thisTab = "\t";
					while(thisTab.equalsIgnoreCase("\t")){
						txt.setSelection(tempPos, tempPos +1);
						thisTab = txt.getSelectionText();
						tempPos ++;
					}
					txt.setSelection(txt.getSelection().x - 1);

					txt.setSelection(tempPos-1, tempPos-1);
					if(needsBullet){
						txt.insert(BULLET_STRING);
						currentSelection ++;
					}

					txt.setSelection(currentSelection, currentSelection + 1);
					txt.insert("");
					
					break;
					
				}
			}
			
		} catch (Exception e1) {
			System.out.println("Exception caught: " + e1.getMessage());
		}
	}
	/**
	 * Call this method to check the spelling of this note. This highlights all words in red that are not spelled correctly
	 */
	public void checkSpelling() {
		//check spelling
		int spellingIndex = txt.getSelection().x;
		txt.selectAll();
		fgColorAll(null); //clears the color changes
		Display display = Display.getDefault();
		int numMissed = 0;
		boolean spellchecking = false;
		if(this.getFileName() != null){
			if(this.getFileName().endsWith(".txt")){
				spellchecking = true;
			}
		} else {
			spellchecking = true;
		}
		if(!spellchecking){return;}
		//this part works just like the StringTokenizer, but i is the index of the start of the word
		String t = txt.getText();
		for(int i = 0; i < t.length(); i++){//loop through every character
			if(CONTROL_CHARS.contains("" + t.charAt(i))){
				continue; //go on to the next letter
			}
			String currentWord = "";
			for(int j = i; j < t.length(); j++){
				//go until a control char is reached
				if(CONTROL_CHARS.contains("" + t.charAt(j))){
					break;
				} else {
					currentWord += t.charAt(j);
				}
				
			}
			currentWord = currentWord.replace("'s", ""); //remove the possessives
			//process the current word
			File newFile = new File(NotesMain.defDir + currentWord + ".txt");
			if(!newFile.exists()){ //if there is not a dictionary entry for word and if it is not a roman numeral then it is misspelled
				if(!Definition.isRomanNumeral(currentWord)){
					txt.setSelectionRange(i, currentWord.length());
					fgColor(display.getSystemColor(SWT.COLOR_RED));
					numMissed ++;
				}
			}
			
			i += currentWord.length(); //add on the length of the current word so that "you" won't be "you","ou","u"
		}
		
		//done, so set the selection back, and show messagebox
		txt.setSelection(spellingIndex, spellingIndex);
		txt.setSelectionBackground(null);
		MessageBox mb = new MessageBox(tf.getShell(), SWT.OK);
		mb.setMessage("Completed spell checking and found " + numMissed + " misspelled words");
		mb.setText("Spell check");
		mb.open();
	}
	/**
	 * Call this method to check the spelling of this note within 100 chars of the cursor position. This method is faster than checking the whole note. This highlights all words in red that are not spelled correctly
	 */
	private void checkSpellingNearSelection(){
		boolean spellchecking = false;
		if(this.getFileName() != null){
			if(this.getFileName().endsWith(".txt")){
				spellchecking = true;
			}
		} else {
			spellchecking = true;
		}
		if(!spellchecking){return;}
		//check spelling
		int spellingIndexStart = txt.getSelection().x;
		int spellingIndexEnd = txt.getSelection().y;
		int st = spellingIndexStart - 100;
		if(st < 0) st = 0;
		int ed = spellingIndexStart + 100;
		if(ed > txt.getText().length()) ed = txt.getText().length();
		
		Display display = Display.getDefault();
		//this part works just like the StringTokenizer, but i is the index of the start of the word
		String t = txt.getText();
		if(t.length() == 0){return;}
		
		while(!CONTROL_CHARS.contains("" + t.charAt(st)) && st < t.length()){
			//go ahead one until a control character is found (not part of a word)
			//this allows a word not to be clipped, causing it to appear to be wrong
			st++;
		}
		try {
			txt.setSelection(st, ed);
		} catch (IllegalArgumentException e) {
			//this exception is only thrown when the start or end is in the middle of a \r\n thing-don't need to do anything
		}
		fgColor(null); //clears the color changes
		for(int i = st; i < t.length(); i++){//loop through every character
			if(i < 0){//if i is less than 0, automatically go to the next i
				continue;
			}
			if(i > spellingIndexStart + 100){
				break;//if the index is over 100 away, stop
			}
			if(CONTROL_CHARS.contains("" + t.charAt(i))){
				continue; //go on to the next letter
			}
			String currentWord = "";
			for(int j = i; j < t.length(); j++){
				//go until a control char is reached
				if(CONTROL_CHARS.contains("" + t.charAt(j))){
					break;
				} else {
					currentWord += t.charAt(j);
				}

			}
			currentWord = currentWord.replace("'s", ""); //remove the possessives
			//process the current word
			File newFile = new File(NotesMain.defDir + currentWord + ".txt");
			if(!newFile.exists()){ //if there is not a dictionary entry for word and if it is not a roman numeral then it is misspelled
				if(!Definition.isRomanNumeral(currentWord)){
					txt.setSelectionRange(i, currentWord.length());
					fgColor(display.getSystemColor(SWT.COLOR_RED));
				}
			}

			i += currentWord.length(); //add on the length of the current word so that "you" won't be "you","ou","u"
		}

		//done, so set the selection back
		txt.setSelection(spellingIndexStart, spellingIndexEnd);
		txt.setSelectionBackground(null);
	}

	private void fgColorAll(Color fg) {
		if(txt.getCharCount() == 0){return;}
		StyleRange style, range;
		range = txt.getStyleRangeAtOffset(0);
		if (range != null) {
			style = (StyleRange) range.clone();
			style.start = 0;
			style.length = txt.getText().length();
			style.foreground = fg;
		} else {
			style = new StyleRange(0, txt.getText().length(), fg, null, SWT.NORMAL);
		}
		txt.setStyleRange(style);
	}

	private void fgColor(Color fg) {
		if(txt.getCharCount() == 0){return;}
	    Point sel = txt.getSelectionRange();
	    if ((sel == null) || (sel.y == 0))
	    	return;
	    StyleRange style, range;
	    range = txt.getStyleRangeAtOffset(sel.x);
	    if (range != null) {
	    	style = (StyleRange) range.clone();
	    	style.start = sel.x;
	    	style.length = sel.y;
	    	style.foreground = fg;
	    } else {
	    	style = new StyleRange(sel.x, sel.y, fg, null, SWT.NORMAL);
	    }
	    txt.setStyleRange(style);
	    txt.setSelectionRange(sel.x + sel.y, 0);
	  }
	/**
	 * Gets the index of the start of the line in caret positions
	 * @param contents The String that is the entire text of the Text
	 * @param currentPosition the int that is the current caret position
	 * @return
	 */
	public static int getLineIndex(String contents, int currentPosition){
		if(contents == ""){return 0;}
		char tempChar = ' ';
		while(tempChar != '\n' && tempChar != '\r'){
			currentPosition --;
			try {
				tempChar = contents.charAt(currentPosition);
			} catch (Exception e) {
				return 0;
			}
			if(currentPosition == 0) {return 0;}
		}
		return currentPosition+1;
	}
	/**
	 * Gets the status of this note as a String 
	 * @return The String that represents what this note is doing (read/write) or the empty String if not doing anything
	 */
	public String getStatus() {
		String s = "";
		if(isReading){ 
			DecimalFormat df = new DecimalFormat("##.##");
			s = "Reading file: " + new File(fileName).getName() + " " + df.format(100*(double)currentRead/(double)totLength) + "%";
		}
		if(st == null) return s;
		if(st.getState() != Thread.State.TERMINATED){
			s += st.getStatus();
		}
		return s;
	}
	
	public boolean needsSaved(){
		//TODO
		return false;
	}
	/**
	 * Finds the words and highlights occurences in green. This uses a UI so only call from UI thread.
	 */
	public void findWords() {
		final String s = "" + text;
		final Shell newShell = new Shell(tf.getShell(), SWT.APPLICATION_MODAL | SWT.BORDER | SWT.CLOSE);
		final Display display = tf.getDisplay();
		newShell.setSize(400, 150);
		newShell.setText("Find text");
		newShell.setLocation(tf.getShell().getLocation());
		Label newLabel = new Label(newShell, SWT.NONE);
		newLabel.setText("Enter the word and then click find");
		newLabel.setBounds(5, 5, 300, 25);
		final Text txtT = new Text(newShell, SWT.SINGLE);
		txtT.setBounds(5, 30, 200, 20);
		Button newButton = new Button(newShell, SWT.PUSH);
		newButton.setBounds(210, 30, 100, 20);
		newButton.setText("Find");
		newButton.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetSelected(SelectionEvent e) {
				Point sel = txt.getSelectionRange();
				String textToSearchFor = txtT.getText();
				if(textToSearchFor.length() == 0){return;}
				int index = 0;
				
				while(index > -1){
					index = s.indexOf(textToSearchFor, index+1);
					if(index != -1){
						//System.out.println("Found word @" + index);
						txt.setSelection(index, textToSearchFor.length() + index);
						fgColor(display.getSystemColor(SWT.COLOR_GREEN));
					}
				}
				txt.setSelectionRange(sel.x, sel.y);
				txt.showSelection();
				newShell.close();
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
	/**
	 * Finds words and replaces them. Uses a UI so only call from the UI thread
	 */
	public void findReplace(){
		final Shell newShell = new Shell(tf.getShell(), SWT.APPLICATION_MODAL | SWT.BORDER | SWT.CLOSE);
		final Display display = tf.getDisplay();
		newShell.setSize(400, 150);
		newShell.setText("Find text");
		newShell.setLocation(tf.getShell().getLocation());
		Label lblF = new Label(newShell, SWT.NONE);
		lblF.setText("Find:");
		lblF.setBounds(5, 5, 40, 25);
		Label lblR = new Label(newShell, SWT.NONE);
		lblR.setText("Replace:");
		lblR.setBounds(5, 30, 50, 25);
		final Text txtF = new Text(newShell, SWT.SINGLE);
		txtF.setBounds(55, 5, 200, 20);
		final Text txtR = new Text(newShell, SWT.SINGLE);
		txtR.setBounds(55, 30, 200, 20);
		Button newButton = new Button(newShell, SWT.PUSH);
		newButton.setBounds(5, 65, 100, 20);
		newButton.setText("Go");
		newButton.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetSelected(SelectionEvent e) {
				String findText = txtF.getText();
				String replaceText = txtR.getText();
				if(findText.length() == 0){return;} //can't replace "" with stuff
				
				int index = 0;
				int numReplaces = 0;
				while(index > -1){
					index = txt.getText().indexOf(findText, index);
					if(index != -1){
						txt.setSelectionRange(index, findText.length());
						txt.insert(replaceText);
						txt.setSelectionRange(index, 0);
						index+= findText.length();
						//this will change the text, so mark it as edited
						if(!ti.getText().contains("*")){
							ti.setText(ti.getText() + "*");
						}
					}
					numReplaces++;
				}
				MessageBox mb = new MessageBox(newShell, SWT.OK);
				mb.setMessage("Found and replaced \"" + findText + "\" with \"" + replaceText + "\" " + numReplaces + " times.");
				mb.setText("Replace done");
				mb.open();
				newShell.close();
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
}
