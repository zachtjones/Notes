import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

public class NotesMain extends Application {
	/** This is the list of notes that are open */
	private ArrayList<Note> notes = new ArrayList<Note>();
	/** This is the tab pane that has the tabs for the individual notes*/
	private TabPane tabs;
	/** This is the label that displays the message at the bottom*/
	private Label lblBottom;
	/**
	 * This is the map of String, Definition that is the word, and it's corresponding definition.
	 * The get, put, and contains methods are constant time
	 */
	private HashMap<String, Definition> definitions = new HashMap<String, Definition>();

	public static final String base = thisBaseDir();
	public final static String defFile = base + "definitions.defnar";

	public static void main(String[] args) {
		System.out.println("Log: Notes process started in main (UI thread) @ " + new Date().toString());
		System.out.println("Log: Base directory : " + base);
		System.out.println("Log: Definitions file : " + defFile);
		System.out.println("Closing this command line window will terminate this process immediately and can cause you to lose unsaved progress");

		Application.launch(args);
		System.out.println("Log: main thread shut down normally @ " + new Date().toString());
	}
	
	@Override
	public void init(){
		//load in the definitions
		//the definitions are all serialized to the definitions file
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		try {
			fis = new FileInputStream(defFile);
			ois = new ObjectInputStream(fis);
			while(true){
				Definition d = (Definition)ois.readObject();
				this.definitions.put(d.getWord(), d);
			}
		} catch (EOFException e){
			//don't need to do anything, end of file reached.
			//this part of code will be run once the definitions are all loaded.
		} catch (ClassNotFoundException | IOException e) {
			System.out.println("Error: " + e.getMessage());
		} finally {
			try {
				if(ois != null) { ois.close(); }
				if(fis != null) { fis.close(); }
			} catch (IOException e) {
				System.out.println("Error: " + e.getMessage());
			}
		
		}
		
	}
	
	/**
	 * Saves the definitions to file
	 */
	private void saveDefinitions(){
		//save the definitions to file
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try{
			fos = new FileOutputStream(defFile);
			oos = new ObjectOutputStream(fos);
			for( Definition d : this.definitions.values()){
				oos.writeObject(d);
			}
		} catch (IOException e){
			System.out.println("Error: " + e.getMessage());
		} finally {
			try {
				if(oos != null) { oos.close(); }
				if(fos != null) { fos.close(); }
			} catch (IOException e) {
				System.out.println("Error: " + e.getMessage());
			}
		}
		
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		MenuBar menu = new MenuBar();

		//file menu
		Menu fileMenu = new Menu("File");
		
		//file new
		MenuItem fileMenuNew = new MenuItem("New");
		fileMenuNew.setOnAction(event -> {
			FileChooser fd = new FileChooser();
			fd.setTitle("Select the file(s) to open");
			fd.getExtensionFilters().add(new ExtensionFilter("Text files (*.txt)", "*.txt"));
			File file = fd.showSaveDialog(primaryStage);
			
			if(file == null){
				return; //cancel was clicked
			}
			try {
				file.createNewFile();
				Tab tempTI = new Tab(file.getName());
				Note tempNote = new Note(file, tempTI, primaryStage, definitions);
				notes.add(tempNote);
				tabs.getTabs().add(tempTI);
			} catch (IOException e1) {
				Alert a = new Alert(AlertType.ERROR);
				a.setContentText("There was an issue creating file: " 
						+ file.getAbsolutePath() + ".\n" + e1.getMessage());
				a.setTitle("Error");
				a.showAndWait();
			}

		});
		fileMenu.getItems().add(fileMenuNew);

		//file open
		MenuItem fileMenuOpen = new MenuItem("Open");
		fileMenuOpen.setOnAction(event -> {
			FileChooser fd = new FileChooser();
			fd.setTitle("Select the file(s) to open");
			fd.getExtensionFilters().add(new ExtensionFilter("Text files (*.txt)", "*.txt"));
			List<File> files = fd.showOpenMultipleDialog(primaryStage);

			if(files == null){
				return; //none were selected
			}
			for(File fn : files){
				try {
					Tab tempTI = new Tab(fn.getName());
					Note tempNote = new Note(fn, tempTI, primaryStage, definitions);
					tabs.getTabs().add(tempTI);
					notes.add(tempNote);
				} catch (IOException e1) {
					Alert a = new Alert(AlertType.ERROR);
					a.setContentText("There was an issue opening file: " + fn + ".\n" + e1.getMessage());
					a.setTitle("Error");
					a.showAndWait();
				}
			}
		});
		fileMenu.getItems().add(fileMenuOpen);

		//file save
		MenuItem fileMenuSave = new MenuItem("Save");
		fileMenuSave.setOnAction(event -> {
			//save the current note
			int temp = tabs.getSelectionModel().getSelectedIndex();
			if(temp == -1){return;}
			notes.get(temp).save();
		});
		fileMenu.getItems().add(fileMenuSave);

		//file save as
		MenuItem fileMenuSaveAs = new MenuItem("Save As");
		fileMenuSaveAs.setOnAction(event -> {
			//save the current note
			int temp = tabs.getSelectionModel().getSelectedIndex();
			if(temp == -1){return;}
			FileChooser fd = new FileChooser();
			fd.setTitle("Select the file(s) to open");
			fd.getExtensionFilters().add(new ExtensionFilter("Text files (*.txt)", "*.txt"));
			File f = fd.showSaveDialog(primaryStage);
			if(f == null) { return; } //no file selected
			notes.get(temp).saveAs(f.getAbsolutePath());
		});
		fileMenu.getItems().add(fileMenuSaveAs);

		//file close
		MenuItem fileMenuClose = new MenuItem("Close");
		fileMenuClose.setOnAction(event -> {
			//close the selected tab
			closeSelected();
		});
		fileMenu.getItems().add(fileMenuClose);

		MenuItem fileMenuCloseAll = new MenuItem("Close all");
		fileMenuCloseAll.setOnAction(event -> {
			for(Note n : notes){
				//save the note if needed
				if(n.getNeedsSaved()){
					Alert a = new Alert(AlertType.NONE, "Do you want to save changes to: "
							+ n.getFileName() + "?", ButtonType.YES, ButtonType.NO);
					Optional<ButtonType> b = a.showAndWait();
					if(b.isPresent() && b.get().getText().equals("Yes")){
						n.save();
					}
				}
			}
			//remove all notes
			notes.removeAll(notes);
			tabs.getTabs().clear();
		});
		fileMenu.getItems().add(fileMenuCloseAll);

		//edit menu
		Menu editMenu = new Menu("Edit");

		//check spelling button
		MenuItem editMenuCheckSpelling = new MenuItem("Check Spelling");
		editMenuCheckSpelling.setOnAction(event -> {
			int temp = tabs.getSelectionModel().getSelectedIndex();
			if(temp == -1){return;}
			notes.get(temp).checkSpelling();
		});
		editMenu.getItems().add(editMenuCheckSpelling);

		//find button
		MenuItem editMenuFind = new MenuItem("Find");
		editMenuFind.setOnAction(event -> {
			int temp = tabs.getSelectionModel().getSelectedIndex();
			if(temp == -1){return;}
			notes.get(temp).findWords();
		});
		editMenu.getItems().add(editMenuFind);

		//replace button
		MenuItem editMenuReplace = new MenuItem("Replace");
		editMenuReplace.setOnAction(event -> {
			int temp = tabs.getSelectionModel().getSelectedIndex();
			if(temp == -1){return;}
			notes.get(temp).findReplace();
		});
		editMenu.getItems().add(editMenuReplace);


		//definitions menu
		Menu defMenu = new Menu("Definitions");

		//define word
		MenuItem defMenuDefine = new MenuItem("Defined words");
		defMenuDefine.setOnAction(event -> {
			DefinitionShower ds = new DefinitionShower(this.definitions);
			ds.show(primaryStage);
		});
		defMenu.getItems().add(defMenuDefine);

		menu.getMenus().addAll(fileMenu, editMenu, defMenu);

		BorderPane bp = new BorderPane();
		bp.setTop(menu);
		
		//bottom label
		lblBottom = new Label();
		bp.setBottom(lblBottom);

		bp.setCenter(tabs);

		Timer t = new Timer();
		//update the status periodically
		t.schedule(new TimerTask(){
			@Override
			public void run() {
				String stats = getStatus();
				Platform.runLater(() -> {
					lblBottom.setText(stats.equals("") ? "Ready" : stats);
				});				
			}
		}, 0, 1000);
		
		Scene scene = new Scene(bp);
		primaryStage.setScene(scene);
		primaryStage.setTitle("Notes");
		primaryStage.setWidth(600);
		primaryStage.setHeight(400);

		//prompt for the save when all tabs are closed
		primaryStage.setOnCloseRequest(event -> {
			//just fire the fileMenuClose
			fileMenuCloseAll.fire();
			//cancel the timer
			t.cancel();
			//save the definitions
			this.saveDefinitions();
		});
		primaryStage.show();

	}

	/**
	 * Gets the combined status of all the notes.
	 * @return A string concatenation of the statuses for each note.
	 */
	public String getStatus() {
		String retVal = "";
		for(Note n : notes){
			retVal += n.getStatus();
		}
		return retVal;
	}

	/** Returns true if any of the notes are being read. */
	public boolean isReadingToAny() {
		for(Note n : notes){
			if(n.getIsReading()){
				return true;
			}
		}
		return false;
	}

	/**
	 * Closes the selected tab
	 */
	private void closeSelected(){
		int index = tabs.getSelectionModel().getSelectedIndex();
		if(index >= 0){
			//get the selected tab
			Tab temp = tabs.getTabs().get(index);
			//close it
			Note n = notes.get(index);
			if(n.getNeedsSaved()){
				Alert a = new Alert(AlertType.NONE, "Do you want to save changes to: "
						+ n.getFileName() + "?", ButtonType.YES, ButtonType.NO);
				Optional<ButtonType> b = a.showAndWait();
				if(b.isPresent() && b.get().getText().equals("Yes")){
					n.save();
				}
			}
			notes.remove(index);
			tabs.getTabs().remove(temp);
		}
	}

	/**
	 * Prints to the console how much time has passed since start in milliseconds with the message. <br>
	 * Use this method commonly for finding the time it takes for methods to run.
	 * @param start a long value representing the start of the elapsed time
	 * @param message The message to show along with the time
	 */
	public static void printTime(long start, String message){
		System.out.print(System.currentTimeMillis() - start);
		System.out.println(" : " + message);
	}
	/**
	 * Gets the base directory of this jar
	 * @return
	 */
	private static String thisBaseDir(){
		try {
			return new File(new File(".txt").getAbsolutePath()).getParent() + File.separator;
		} catch (Exception e) {

		}
		return "";
	}

}