import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javafx.application.Application;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
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

	public static final String base = thisBaseDir();
	public final static String defDir = base + "definitions" + File.separator;

	public static void main(String[] args) {
		System.out.println("Log: Notes process started in main (UI thread) @ " + new Date().toString());
		System.out.println("Log: Base directory : " + base);
		System.out.println("Log: Definitions directory : " + defDir);
		System.out.println("Closing this command line window will terminate this process immediately and can cause you to lose unsaved progress");

		Application.launch(args);
		//TODO
		/*
		Add more options with the dictionary (change local definitions, add user-defined definitions)
		Add the ability to add names to dictionary
		add the option to export/import definitions to/from 1 file


		keep a recents list
		if a tab is still open when the application shuts down, re-open it (if the filename is available) when the application starts

		Add more edit options to find, find & replace -use parameters like whole words only, case sensitive, current selection/current document/all open documents
			-also add the option to iterate (only go one occurrence at a time) - add the required buttons to the existing UI
			-for the not case sensitive-make a string = txt.getText() and set that and the find string to lower

		 */

		System.out.println("Log: main thread shut down normally @ " + new Date().toString());
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO make this MVC behavior
		MenuBar menu = new MenuBar();

		//file menu
		Menu fileMenu = new Menu("File");
		
		//file new
		MenuItem fileMenuNew = new MenuItem("New");
		fileMenuNew.setOnAction(event -> {
			FileChooser fd = new FileChooser();
			fd.setTitle("Select the file(s) to open");
			fd.getExtensionFilters().add(new ExtensionFilter("Any file (*.*)", "*.*"));
			File file = fd.showSaveDialog(primaryStage);
			
			if(file == null){
				return; //cancel was clicked
			}
			try {
				file.createNewFile();
				Tab tempTI = new Tab(file.getName());
				Note tempNote = new Note(file.getAbsolutePath(), tempTI, tabs);
				tempNote.start();
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
			fd.getExtensionFilters().add(new ExtensionFilter("Any file (*.*)", "*.*"));
			List<File> files = fd.showOpenMultipleDialog(primaryStage);

			if(files == null){
				return; //none were selected
			}
			for(File fn : files){
				try {
					Tab tempTI = new Tab(fn.getName());
					Note tempNote = new Note(fn.getAbsolutePath(), tempTI, tabs);
					tempNote.start();
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
			notes.get(temp).saveAs();
		});

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
				if(n.needsSaved()){
					Alert a = new Alert(AlertType.NONE, "Do you want to save changes to: "
							+ n.getName() + "?", ButtonType.YES, ButtonType.NO);
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
		MenuItem defMenuDefine = new MenuItem("Define word");
		defMenuDefine.setOnAction(event -> {
			//TODO
			Definition.defineWord();
		});
		defMenu.getItems().add(defMenuDefine);

		//create archive
		MenuItem defMenuCreateArchive = new MenuItem("Create Archive");
		defMenuCreateArchive.setOnAction(event -> {
			//TODO
			Definition.createDefArchive();
		});
		defMenu.getItems().add(defMenuCreateArchive);

		//load archive
		MenuItem defMenuLoadArchive = new MenuItem("Load Archive");
		defMenuLoadArchive.setOnAction(event -> {
			//TODO
			Definition.loadArchive();
		});
		defMenu.getItems().add(defMenuLoadArchive);


		menu.getMenus().addAll(fileMenu, editMenu, defMenu);

		BorderPane bp = new BorderPane();
		bp.setTop(menu);
		
		//bottom label
		lblBottom = new Label();
		bp.setBottom(lblBottom);

		//the close context-menu
		ContextMenu cm = new ContextMenu();
		MenuItem close = new MenuItem("Close");
		close.setOnAction(event -> {
			closeSelected();
		});
		cm.getItems().add(close);

		//the tab pane
		tabs = new TabPane();
		tabs.setOnMouseClicked(event -> {
			if(event.getButton() == MouseButton.SECONDARY){
				//show close context menu
				cm.show(primaryStage);
			}
		});

		bp.setCenter(tabs);


		/*


		tabFolder.addMouseListener(new MouseListener() {

			@Override
			public void mouseDoubleClick(MouseEvent e) {
				// maybe do somthing later with this

			}

			@Override
			public void mouseDown(MouseEvent e) {
				//System.out.println(tabFolder.getSelection()[0].getText() + " selected");
				if(tabFolder.getSelection()[0].equals(tiNew)){
					TabItem tempTI = new TabItem(tabFolder, SWT.NONE);
					Note tempNote = new Note(tempTI, tabFolder);
					tempNote.start();
					notes.add(tempNote);
					tempTI.setText("New " + notes.size());
					tabFolder.setSelection(tempTI);
				}
			}

			@Override
			public void mouseUp(MouseEvent e) {

			}

		});

		shell.addControlListener(new ControlListener() {
			@Override
			public void controlMoved(ControlEvent e) {
				//nothing needed to do here
			}

			@Override
			public void controlResized(ControlEvent e) {
				tabFolder.setSize(shell.getSize().x - 35, shell.getSize().y - 100);
				lblBottom.setLocation(5, shell.getSize().y - 84);
				lblBottom.setSize(shell.getSize().x, lblBottom.getSize().y);
			}

		});

		lblBottom = new Label(shell, SWT.NONE);
		lblBottom.setSize(500, 25);

		lblBottom.setBounds(50, 600, 500, 25);
		lblBottom.moveAbove(null);
		lblBottom.setText("");
		display.timerExec(100, new Runnable() {

			@Override
			public void run() {
				if(shell.isDisposed()) return;
				//System.out.println("Is reading to any: " + isReadingToAny());
				String stats = getStatus();
				lblBottom.setText(stats.equals("") ? "Ready" : stats);
				display.timerExec(100, this);
			}

		});
		shell.open();
		System.out.println("Log: UI opened successfully.");
		while(!shell.isDisposed()){
			if(!display.readAndDispatch()){
				display.sleep();
			}
		}
		display.dispose();

		shell = new Shell(display);
		shell.setText("ZJ Notes");
		shell.setSize(800, 600);
	}

	public static String getStatus() {
		String retVal = "";
		for(Note n : notes){
			retVal += (n.getStatus() == "" ? "" : n.getStatus());
		}
		return retVal;
	}

	public static boolean isReadingToAny() {
		for(Note n : notes){
			if(n.getIsReading()){
				return true;
			}
		}
		return false;
		 */

		Scene scene = new Scene(bp);
		primaryStage.setScene(scene);
		primaryStage.setTitle("Notes");

		//add the close save prompt

		primaryStage.setOnCloseRequest(event -> {
			//TODO
		});
		primaryStage.show();

	}

	/**
	 * Closes the selected tab
	 */
	private void closeSelected(){
		// TODO
		int index = tabs.getSelectionModel().getSelectedIndex();
		if(index > 1){
			//get the selected tab
			Tab temp = tabs.getTabs().get(index);
			//close it
			Note n = notes.get(index - 1);
			if(n.needsSaved()){
				Alert a = new Alert(AlertType.NONE, "Do you want to save changes to: "
						+ n.getName() + "?", ButtonType.YES, ButtonType.NO);
				Optional<ButtonType> b = a.showAndWait();
				if(b.isPresent() && b.get().getText().equals("Yes")){
					n.save();
				}
			}
			notes.remove(index - 1);
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