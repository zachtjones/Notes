import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

public class NotesMain {

	private static ArrayList<Note> notes = new ArrayList<Note>();
	private static Display display;
	private static Shell shell;
	private static Menu menu, fileMenu, tabMenu, editMenu, defMenu;
	private static MenuItem fileMenuHeader, fileMenuOpen, fileMenuSave, fileMenuSaveAs, fileMenuClose, fileMenuCloseAll, tabMenuClose;
	private static MenuItem editMenuHeader, editMenuCheckSpelling, editMenuFind, editMenuReplace;
	private static MenuItem definitionsHeader, defMenuDefine, defMenuCreateArchive, defMenuLoadArchive;
	private static TabFolder tabFolder;
	private static TabItem tiNew;
	private static Label lblBottom;
	public static final String base = thisBaseDir();
	public final static String defDir = base + "definitions" + File.separator;
	
	public static void main(String[] args) {
		System.out.println("Log: Notes process started in main (UI thread) @ " + new Date().toString());
		System.out.println("Log: Base directory : " + base);
		System.out.println("Log: Definitions directory : " + defDir);
		System.out.println("Closing this command line window will terminate this process immediately and can cause you to lose unsaved progress");
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
		Display.setAppName("Notes");
		display = new Display();
		shell = new Shell(display);
		shell.setText("ZJ Notes");
		shell.setSize(800, 600);
		
		menu = new Menu(shell, SWT.BAR);

		fileMenu = new Menu(menu);
		fileMenuHeader = new MenuItem(menu, SWT.CASCADE);
	    fileMenuHeader.setText("&File");
		fileMenuHeader.setMenu(fileMenu);
		
		editMenu = new Menu(menu);
		editMenuHeader = new MenuItem(menu, SWT.CASCADE);
		editMenuHeader.setText("&Edit");
		editMenuHeader.setMenu(editMenu);
		
		defMenu = new Menu(menu);
		definitionsHeader = new MenuItem(menu, SWT.CASCADE);
		definitionsHeader.setText("&Definitions");
		definitionsHeader.setMenu(defMenu);
		
		defMenuDefine = new MenuItem(defMenu, SWT.PUSH);
		defMenuDefine.setText("&Define");
		defMenuDefine.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetSelected(SelectionEvent e) {
				Definition.defineWord(display, shell);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				//not called
			}
			
		});
		
		defMenuCreateArchive = new MenuItem(defMenu, SWT.PUSH);
		defMenuCreateArchive.setText("&Create archive");
		defMenuCreateArchive.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				Definition.createDefArchive(shell);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				//not called
			}
		});
		
		defMenuLoadArchive = new MenuItem(defMenu, SWT.PUSH);
		defMenuLoadArchive.setText("&Load archive");
		defMenuLoadArchive.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				Definition.loadArchive(shell);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				//not called
			}
		});
		
		editMenuCheckSpelling = new MenuItem(editMenu, SWT.PUSH);
		editMenuCheckSpelling.setText("Ch&eck spelling");
		editMenuCheckSpelling.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				int temp = tabFolder.getSelectionIndex();
				if(temp == 0 || temp == -1){return;}
				notes.get(temp-1).checkSpelling();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				//not called
			}
		});
		
		editMenuFind = new MenuItem(editMenu, SWT.PUSH);
		editMenuFind.setText("&Find");
		editMenuFind.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				int temp = tabFolder.getSelectionIndex();
				if(temp == 0 || temp == -1){return;}
				notes.get(temp-1).findWords();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				//not called
			}
		});
		
		editMenuReplace = new MenuItem(editMenu, SWT.PUSH);
		editMenuReplace.setText("&Replace");
		editMenuReplace.addSelectionListener(new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				int temp = tabFolder.getSelectionIndex();
				if(temp == 0 || temp == -1){return;}
				notes.get(temp-1).findReplace();
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				//not called
			}
		});
		
		fileMenuOpen = new MenuItem(fileMenu, SWT.PUSH);
		fileMenuOpen.setText("&Open");
		fileMenuOpen.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
				fd.setText("Select the file(s) to open");
				String[] temp = {"All files (*.*)"};
				String[] temp2 = {"*.*"};
				fd.setFilterExtensions(temp2);
				fd.setFilterNames(temp);
				fd.open();
				String[] allSelectedFiles = fd.getFileNames();
				if(allSelectedFiles.length == 0){
					return; //none were selected, so there is no need to prompt the user
				}
				for(String fn : allSelectedFiles){
					try {
						TabItem tempTI = new TabItem(tabFolder, SWT.NONE);
						Note tempNote = new Note(fd.getFilterPath() + File.separator + fn, tempTI, tabFolder);
						tempNote.start();
						notes.add(tempNote);
						tempTI.setText(fn);//setStyledText will clear the StyledText
					} catch (IOException e1) {
						MessageBox mb = new MessageBox(shell, SWT.OK);
						mb.setMessage("There was an issue opening file: " + fn + ".\n" + e1.getMessage());
						mb.setText("Error");
						mb.open();
					}	
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				//not called
			}
			
		});
		
		fileMenuSave = new MenuItem(fileMenu, SWT.PUSH);
		fileMenuSave.setText("&Save");
		fileMenuSave.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetSelected(SelectionEvent e) {
				//save the current thing
				int temp = tabFolder.getSelectionIndex();
				if(temp == 0 || temp == -1){return;}
				notes.get(temp-1).save();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// not called
			}
			
		});
		fileMenuSaveAs = new MenuItem(fileMenu, SWT.PUSH);
		fileMenuSaveAs.setText("Save As");
		fileMenuSaveAs.addSelectionListener(new SelectionListener(){

			@Override
			public void widgetSelected(SelectionEvent e) {
				//save the current thing
				int temp = tabFolder.getSelectionIndex();
				if(temp == 0 || temp == -1){return;}
				notes.get(temp-1).saveAs();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// not called
			}
		});

		SelectionListener closeL = new SelectionListener(){
			@Override
			public void widgetSelected(SelectionEvent e) {
				if(tabFolder.getSelection()[0].equals(tiNew)){
					//do nothing
				} else {
					//close it
					TabItem tempTI = tabFolder.getSelection()[0];
					for(int i = 0; i < notes.size(); i++){
						if(notes.get(i).getTabItem().equals(tempTI)){
							if(tempTI.getText().contains("*")){
								MessageBox mb = new MessageBox(shell, SWT.YES | SWT.NO);
								mb.setText("Save");
								mb.setMessage("Do you want to save changes to: " + tempTI.getText().replace("*", "") + "?");
								int result = mb.open();
								if(result == SWT.YES){
									notes.get(i).save();
									notes.remove(i);
									tempTI.dispose();
									break;//exit the for loop
								} else if(result == SWT.NO){
									notes.remove(i);
									tempTI.dispose();
									break;//exit the for loop
								}
								break;
							} else { //just close it as no changes are necessary
								notes.remove(i);
								tempTI.dispose();
								break;
							}
							
							
						}
					}
					

				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				//not called
			}
		};
		fileMenuClose = new MenuItem(fileMenu, SWT.PUSH);
		fileMenuClose.setText("&Close");
		fileMenuClose.addSelectionListener(closeL);
		
		final SelectionListener closeAllListener = new SelectionListener(){

			@Override
			public void widgetSelected(SelectionEvent e) {
				for(Note n : notes){
					if(n.getTabItem().getText().contains("*")){
						MessageBox mb = new MessageBox(shell, SWT.YES | SWT.NO);
						mb.setText("Save");
						mb.setMessage("Do you want to save changes to: " + n.getTabItem().getText().replace("*", "") + "?");
						if(mb.open() == SWT.YES){
							n.save();
						}
					}
				}
				
				notes.removeAll(notes);
				for(TabItem ti : tabFolder.getItems()){
					if(!ti.equals(tiNew)){
						ti.dispose();
					}
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				//not called
			}
		};
		
		fileMenuCloseAll = new MenuItem(fileMenu, SWT.PUSH);
		fileMenuCloseAll.setText("Close All");
		fileMenuCloseAll.addSelectionListener(closeAllListener);
		
		shell.addDisposeListener(new DisposeListener(){

			@Override
			public void widgetDisposed(DisposeEvent e) {
				closeAllListener.widgetSelected(null);
			}
			
		});

		shell.setMenuBar(menu); //set menu is right-click on the control 

		tabFolder = new TabFolder(shell, SWT.BORDER);
		tabFolder.setBounds(5, 5, 765, 525);

		tabMenu = new Menu(tabFolder);
		tabMenuClose = new MenuItem(tabMenu, SWT.PUSH);
		tabMenuClose.setText("Close");
		tabMenuClose.addSelectionListener(closeL);

		tabFolder.setMenu(tabMenu);
		tabFolder.addKeyListener(new KeyListener() {

			@Override
			public void keyPressed(KeyEvent e) {
				e.doit = false;
			}

			@Override
			public void keyReleased(KeyEvent e) {
				e.doit = false;
			}

		});
		tiNew = new TabItem(tabFolder, SWT.NONE);
		tiNew.setText("+");

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
		System.out.println("Log: main thread shut down normally @ " + new Date().toString());
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