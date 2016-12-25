import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.IndexRange;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Note{ 
	private static final String CONTROL_CHARS = " 0123456789!@#$%^&*()~`-_=+[]{}\\|\"\r\n\t;:?/,.<>";
	/** The filename of this note. */
	private String fileName;
	/** The name part of the filename (just the last part, not containing the path)*/
	private String shortName;
	/** The tab for this note */
	private Tab ti;
	/** The text area for this note */
	private TextArea txt;
	/** The total length of bytes to read */
	private long totLength = 0;
	/** The current amount of bytes read. */
	private long currentRead = 0;
	/** Whether or not this note is reading */
	private boolean isReading = false;
	/** The primary stage for this program. */
	private Stage primaryStage;
	/** Whether or not there are usaved changes to this note. */
	private boolean needsSaved = false;
	/** The text of this note */
	private String text = "";
	/** Holds the words in this note that are not in the dictionary.
	 * This is used so that misspelled words are not tried to define more than once. */
	private HashSet<String> wordsNotInDict = new HashSet<String>();
	/**
	 * Holds the reference to the definitions.
	 */
	private HashMap<String, Definition> definitions;

	/**
	 * Constructor for a note that is from an existing file.
	 * @param file The File representing the file of this note
	 * @param ti The Tab on the UI
	 * @param primaryStage The window to show alerts on.
	 * @param definitions the definitions in the main program.
	 * @throws IOException This will throw an IOException if there is an error 
	 * in creating the FileReader, BufferedReader, or if there is an issue decoding the file.
	 */
	public Note(File file, Tab ti, Stage primaryStage, 
			HashMap<String, Definition> definitions) throws IOException{
		
		System.out.println("Log: " + file.getName() + " file opened @" + new Date().toString());
		this.totLength = file.length();
		this.currentRead = 0L;
		//use multithreading to load from a file as it could potentially be very large
		this.fileName = file.getAbsolutePath();
		this.shortName = file.getName();
		this.ti = ti;
		this.primaryStage = primaryStage;
		this.definitions = definitions;
		setStyledText();
		new Thread(() -> {
			System.out.println("Log: " + getFileName() + " note thread started @" + new Date());
			long ct = System.currentTimeMillis();
			//read the file
			try {
				this.isReading = true;
				Platform.runLater(() -> {
					txt.setEditable(false);
				});

				FileReader fr = new FileReader(this.fileName);
				BufferedReader br = new BufferedReader(fr);
				//read the first part by 2000 chars
				char[] line = new char[5000]; //read 2000 chars at a time
				int num = 0;
				String content = "";
				
				while((num = br.read(line)) != -1){
					//build up the string
					String temp = "";
					for(int i = 0; i < num; i++){
						temp += line[i];
					}
					//add to the content
					content += temp;
					this.currentRead += temp.length();
				}
				this.text = content;
				//done reading, update UI
				Platform.runLater(() -> {
					txt.setText(text);
					txt.setEditable(true);
				});

				NotesMain.printTime(ct, "mills, finished reading file: " + this.fileName);
				br.close();
				fr.close();
			} catch (IOException e) {
				Platform.runLater(() -> {
					Alert a = new Alert(AlertType.ERROR);
					a.setTitle("Error");
					a.setContentText("There was an issue opening file: " 
							+ Note.this.fileName + ".\n" + e.getMessage());
					a.showAndWait();
				});
			}
			this.isReading = false;

			//this thread now deals with adding the words to dictionary
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

				//process the current word - only the lower case is stored
				if(!this.definitions.containsKey(currentWord.toLowerCase())){ 
					//if there is not a dictionary entry for word and if it is 
					//not a roman numeral then it is misspelled
					if(!Definition.isRomanNumeral(currentWord)){
						//try to define the word
						if(!wordsNotInDict.contains(currentWord)){
							try {
								Definition d = new Definition(currentWord);
								this.definitions.put(d.getWord(), d);
							} catch (Exception e) {
								System.out.println("Could not define word: " + currentWord);
								wordsNotInDict.add(currentWord);
							}
						}
					}
				}
				i += currentWord.length(); //add on the length of the current word
			}

			//end of this thread running
			System.out.println(this.getFileName() + " thread stopped normally");
		}).start();
	}

	/**
	 * Constructor for a note that is newly created (not from an existing file). 
	 * You <b><u>MUST</b></u> call the run method of this object in order to add dictionary items.
	 * @param ti The TabItem on the UI
	 */
	public Note(Tab ti, Stage primaryStage, HashMap<String, Definition> definitions){
		System.out.println("Log: new note started @" + new Date().toString());
		this.definitions = definitions;
		this.ti = ti;
		this.primaryStage = primaryStage;
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
	 * @return The text of this note. 
	 * Note that this call is thread-safe, so it may be called from a thread other than the UI.
	 */
	public String getText(){
		return this.text;
	}

	/**
	 * Gets the file name of this note
	 * @return The file name of this note, or <b>null</b> 
	 * if it was created using the Note(TabItem, TabFolder)
	 */
	public String getFileName() {
		return this.fileName;
	}
	/**
	 * Gets the tab item associated with this note
	 * @return The reference to the tab item
	 */
	public Tab getTab() {
		return this.ti;
	}
	/**
	 * Saves the note to file. If the note was created using the file -> new, 
	 * then this calls the saveAs method
	 * @return true if the file saves properly, otherwise false
	 * @see Note.saveAs
	 */
	public void save(){
		if(this.fileName == null){
			//prompt the user for the file to pick
			FileChooser fd = new FileChooser();
			fd.setTitle("Select the file(s) to open");
			fd.getExtensionFilters().add(new ExtensionFilter("Text files (*.txt)", "*.txt"));
			File f = fd.showSaveDialog(primaryStage);
			saveAs(f.getAbsolutePath());
			return;
		}
		//just save to the filename
		saveAs(this.fileName);
	}
	/**
	 * Saves the note to the file specified. 
	 * <br>The filename for this note and the text at the tab item 
	 * are changed to match the selected file name.
	 * @param filename The full path of the file to save to
	 * @return true if the file saves properly, otherwise false
	 */
	public void saveAs(String filename){
		this.fileName = filename;
		this.shortName = new File(filename).getName();
		this.needsSaved = false;
		//save to the fileName using mutilthreads
		new Thread(() -> {
			try {
				FileWriter fw = new FileWriter(this.fileName);
				BufferedWriter bw = new BufferedWriter(fw);
				bw.write(this.text);
				bw.flush();
				bw.close();
				fw.close();
				Platform.runLater(() -> {
					this.ti.setText(shortName);
				});
			} catch (IOException e){
				Alert a = new Alert(AlertType.ERROR);
				a.setTitle("Error");
				a.setContentText("There was an error saving the file.\nMessage: " + e.getMessage());
			}
			
		}).start();
	}
	
	/**
	 * Sets the styled text used by this Note. 
	 * Calling this method will reset the text, so only call it on creation in the constructor constructor
	 */
	private void setStyledText(){
		txt = new TextArea();
		txt.setFont(new Font("Courier New", 12L));
		ti.setContent(txt);

		txt.setOnKeyPressed(event -> {
			text = txt.getText();
			if(event.getCode() == KeyCode.TAB){
				System.out.println("Tab pressed");
				//consume so that the tab character is not inserted
				event.consume();
				
				//set the tabs
				setTab(event);
				//since this changes the note, update
				this.needsSaved();
				
				//use Ctrl (windows) or command (mac os) combinations
			} else if(event.isShortcutDown()){ 
				//shortcut S is save
				if(event.getCode() == KeyCode.S){
					save(); 
				}
				//shortcut A is select all
				if(event.getCode() == KeyCode.A){
					txt.selectAll();
				}

			} else {
				//the text is edited
				this.text = txt.getText();
				this.needsSaved();
			}
		});
	}
	/**
	 * Internal method used for formatting. 
	 * This is only called when a tab is pressed.
	 * @param The KeyEvent that contains the data.
	 */
	private void setTab(KeyEvent e){

		//save this text so it can be acessed by another thread 
		text = txt.getText();
		//keep track of bullets
		int currentSelection = txt.getSelection().getStart();

		//handle the tabs
		if(e.isShiftDown()){
			//shift + tab should remove the first tab
			int temp = getLineIndex(txt.getText(), currentSelection);
			txt.selectRange(temp, temp + 1);
			char c = txt.getText().charAt(txt.getSelection().getStart());
			//if the tab is the first character, delete it
			if(c == '\t'){
				txt.deleteNextChar();
				txt.selectRange(currentSelection - 1, currentSelection - 1);
			}
		} else {
			//regular tab should place the tab at the start and the text
			//go through and find the bullet text and place the bullets if necessary 
			int lineIndex = getLineIndex(text, currentSelection);

			boolean needsBullet = true;
			for(int i = lineIndex; i < text.length(); i++){
				//if a ~ is not found before new line, then it needs the character
				if(text.charAt(i) == '\n'){
					break;
				} else if(text.charAt(i) == '~'){
					needsBullet = false;
					break;
				}
			}

			txt.insertText(lineIndex, "\t");
			txt.positionCaret(lineIndex);
			
			//while the text is a tab, increase by 1
			while(txt.getText(txt.getCaretPosition(), txt.getCaretPosition() + 1) == "\t"){
				txt.selectForward();
			}
			
			
			if(needsBullet){
				txt.insertText(txt.getCaretPosition(), "~");
			}

			txt.positionCaret(txt.getCaretPosition() + 1);
		}
	}
	
	/**
	 * Checks the spelling from start to end in the index range.
	 * @param start The start index.
	 * @param end The end index (must be > start and < txt.getText().length()
	 * @param showCount to show an alert displaying the number of misspelled words.
	 */
	public void checkSpelling(){
		//check spelling
		int spellingIndex = txt.getSelection().getStart();

		for(int i = 0; i < txt.getText().length(); i++){//loop through every character
			if(CONTROL_CHARS.contains(txt.getText(i, i + 1))){
				continue; //go on to the next letter
			}
			String currentWord = "";
			for(int j = i; j < txt.getText().length(); j++){
				//go until a control char is reached
				if(CONTROL_CHARS.contains(txt.getText(j, j + 1))){
					break;
				} else {
					currentWord += txt.getText(j, j + 1);
				}

			}
			currentWord = currentWord.replace("'s", ""); //remove the possessives
			//process the current word - the lower case is stored
			if(!this.definitions.containsKey(currentWord.toLowerCase())){ 
				//if there is not a dictionary entry for word and if it is 
				//not a roman numeral then it is misspelled
				if(!Definition.isRomanNumeral(currentWord)){
					//try to define the word
					boolean showAlert = false;
					if(!wordsNotInDict.contains(currentWord)){
						try {
							Definition d = new Definition(currentWord);
							this.definitions.put(d.getWord(), d);
						} catch (Exception e) {
							System.out.println("Could not define word: " + currentWord);
							wordsNotInDict.add(currentWord);
							//also not a word
							txt.selectRange(i + currentWord.length(), i);
							showAlert = true;
						}
					} else {
						//definitely not a word
						txt.selectRange(i + currentWord.length(), i);
						showAlert = true;
					}
					if(showAlert){
						Alert a = new Alert(AlertType.NONE, "Do you wish to edit word: "
							+ currentWord + "?", new ButtonType("Yes"), new ButtonType("No"));
						Optional<ButtonType> b = a.showAndWait();
						if(b.isPresent() && b.get().getText().equals("Yes")){
							//change the text to the input
							TextInputDialog tid = new TextInputDialog(currentWord);
							tid.setGraphic(null);
							tid.setContentText("Enter the new word: ");
							Optional<String> value = tid.showAndWait();
							if(value.isPresent()){
								txt.deleteText(i, i + currentWord.length());
								txt.insertText(i, value.get());
							}
							this.text = txt.getText();
						}
						
					}
					
				}
			}
			//add on the length of the current word so that "you" won't be "you","ou","u"
			i += currentWord.length(); 
		}

		//done, so set the selection back, and show alert
		txt.positionCaret(spellingIndex);
		
	}

	/**
	 * Gets the index of the start of the line in caret positions
	 * @param contents The String that is the entire text of the Text
	 * @param currentPosition the int that is the current caret position
	 * @return
	 */
	private int getLineIndex(String contents, int currentPosition){
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
			s = "Reading file: " + new File(fileName).getName() + " " 
					+ df.format(100*(double)currentRead/(double)totLength) + "%";
		}
		return s;
	}

	/**
	 * Shows that this note needs saved on the tab.
	 */
	public void needsSaved(){
		this.ti.setText(this.shortName + " *");
		this.needsSaved = true;
	}
	
	/**
	 * Gets if this note has unsaved changes.
	 * @return true iff this note's contents are different than the file.
	 */
	public boolean getNeedsSaved() { return this.needsSaved; }
	
	/**
	 * Finds the words and highlights occurences in green. 
	 * This uses a UI so only call from UI thread.
	 */
	public void findWords() {
		Stage stage = new Stage(StageStyle.DECORATED);
		stage.setWidth(400);
		stage.setHeight(150);
		stage.setTitle("Find text");
		stage.initOwner(primaryStage);
		stage.setX(primaryStage.getX());
		stage.setY(primaryStage.getY());
		
		VBox items = new VBox();
		
		HBox textFieldAndLabel = new HBox();
		//find label
		textFieldAndLabel.getChildren().add(new Label("Find: "));
		//the find text field
		TextField textField = new TextField();
		textField.setPrefWidth(200);
		textFieldAndLabel.getChildren().add(textField);
		items.getChildren().add(textFieldAndLabel);
		
		//the button
		Button button = new Button("Find next");
		button.setPrefSize(200, 20);
		button.setOnAction(event -> {
			int selection = txt.getCaretPosition();
			
			String textToSearch = txt.getText();
			String searchText = textField.getText();
			
			int i = textToSearch.indexOf(searchText, selection);
			if(i == -1){
				Alert a = new Alert(AlertType.ERROR);
				a.setContentText("The text could not be found after the caret.");
				a.setTitle("Error");
				a.showAndWait();
			} else {
				txt.selectRange(i, i + searchText.length());
			}
		});
		items.getChildren().add(button);
				
		Scene scene = new Scene(items);
		//if this loses focus, close
		stage.focusedProperty().addListener(value -> {
			if(!stage.isFocused()){
				stage.close();
			}
		});
		stage.setScene(scene);
		//show the window, allowing the main note to be used
		stage.show();
	}
	/**
	 * Finds words and replaces them. Uses a UI so only call from the UI thread
	 */
	public void findReplace(){
		Stage stage = new Stage(StageStyle.DECORATED);
		stage.setWidth(400);
		stage.setHeight(150);
		stage.setTitle("Find text");
		stage.initOwner(primaryStage);
		stage.setX(primaryStage.getX());
		stage.setY(primaryStage.getY());
		
		VBox items = new VBox();
		
		HBox find = new HBox();
		//find label
		find.getChildren().add(new Label("Find: "));
		//the find text field
		TextField findTextField = new TextField();
		findTextField.setPrefWidth(200);
		find.getChildren().add(findTextField);
		items.getChildren().add(find);

		HBox replace = new HBox();
		//replace label
		replace.getChildren().add(new Label("Replace: "));
		//the replace text field
		TextField replaceTextField = new TextField();
		replaceTextField.setPrefWidth(200);
		find.getChildren().add(replaceTextField);
		items.getChildren().add(replace);
		
		//the button
		Button button = new Button("Replace next");
		button.setPrefSize(200, 20);
		button.setOnAction(event -> {
			int selection = txt.getCaretPosition();
			
			String textToSearch = txt.getText();
			String searchText = findTextField.getText();
			String replaceText = replaceTextField.getText();
			
			int i = textToSearch.indexOf(searchText, selection);
			if(i == -1){
				Alert a = new Alert(AlertType.ERROR);
				a.setContentText("The text could not be found after the caret.");
				a.setTitle("Error");
				a.showAndWait();
			} else {
				txt.deleteText(new IndexRange(i, i + searchText.length()));
				txt.insertText(i, replaceText);
				txt.selectRange(i + searchText.length(), i);
			}
		});
		
		Scene scene = new Scene(items);
		//if this loses focus, close
		stage.focusedProperty().addListener(value -> {
			if(!stage.isFocused()){
				stage.close();
			}
		});
		stage.setScene(scene);
		//show the window, allowing the main note to be used
		stage.show();
	}
}
