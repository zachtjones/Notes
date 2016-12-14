import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

public class ArchiveThread extends Thread{
	private String fileName;
	private Values val;
	private Shell parent;
	private Display display;
	
	enum Values {READ, WRITE};
	/**
	 * Constructs a new AchiveThread with the filename to read/write, 
	 * whether to read or write, and the Shell to show the messageboxes on.
	 * <br>Use .start() to start the process of creating/reading the definition archive
	 * @param fileName The filename of the definitions archive file to read/create
	 * @param val The Values enum value. Use ArhiveThread.Values.Write to signify creating the archive file, Read to import the defs from an archive
	 * @param parent The parent shell to show message boxes on
	 */
	public ArchiveThread(String fileName, Values val, Shell parent){
		this.fileName = fileName;
		this.val = val;
		this.parent = parent;
		this.display = parent.getDisplay();
	}
	
	@Override
	public void run(){
		if(this.val == Values.WRITE){ //create the file, reading the definitions
			String headerTable = "";
			final File[] allWords = new File(NotesMain.defDir).listFiles();
			headerTable += "" + allWords.length + "\n";
			for(File f : allWords){
				headerTable += f.getName().replace(".txt", "") + ":" + f.length() + "\n";
			}
			try {
				FileWriter fw = new FileWriter(this.fileName);
				fw.write(headerTable+"\n");
				//TODO write the definitions
				for(File f : allWords){
					fw.write(readAll(f.getAbsolutePath()));
				}
				fw.flush();
				fw.close();
				try {
					display.syncExec(new Runnable(){
						public void run(){
							MessageBox mb = new MessageBox(parent, SWT.OK);
							mb.setMessage("Finished creating the archive for " + allWords.length + " definitions.");
							mb.setText("Error");
							mb.open();
						}
					});
				} catch (Exception e1) {
					System.out.println("Finished creating the archive for " + allWords.length + " definitions");
				}
			} catch (IOException e) {
				try {
					display.syncExec(new Runnable(){
						public void run(){
							MessageBox mb = new MessageBox(parent, SWT.OK);
							mb.setMessage("Could not create the archive file");
							mb.setText("Error");
							mb.open();
						}
					});
				} catch (Exception e1) {
					System.out.println("Could not create the archive file");
				}
				System.out.println("Stack trace for caught exception: ");
				e.printStackTrace();
			}
			//System.out.println(headerTable);
		} else { //read the file and create the definitions
			File tempF = new File(NotesMain.defDir);
			if(!tempF.exists()){
				tempF.mkdir();
			}
			try {
				FileReader fr = new FileReader(this.fileName);//read the defnar archive
				BufferedReader br = new BufferedReader(fr);
				int length = Integer.parseInt(br.readLine());
				final ArrayList<String> names = new ArrayList<String>();
				ArrayList<Integer> lengths = new ArrayList<Integer>();
				for(int i = 0; i < length; i++){
					String temp = br.readLine();
					StringTokenizer st = new StringTokenizer(temp, ":");
					String name = st.nextToken();
					Integer l = Integer.parseInt(st.nextToken());
					names.add(name);
					lengths.add(new Integer(l));
				}
				for(int i = 0; i < length; i++){
					String temp = "";
					for(int j = 0; j < lengths.get(i); j++){
						temp += "" + (char)br.read();
					}
					FileWriter fw = new FileWriter(NotesMain.defDir + names.get(i) + ".txt");
					fw.write(temp);
					fw.flush();
					fw.close();
				}
				br.close();
				fr.close();
				try {
					display.syncExec(new Runnable(){
						public void run(){
							MessageBox mb = new MessageBox(parent, SWT.OK);
							mb.setMessage("Finished creating the " + names.size() + " definitions.");
							mb.setText("Error");
							mb.open();
						}
					});
				} catch (Exception e1) {
					System.out.println("Finished creating the " + names.size() + " definitions");
				}
			} catch (IOException e) {
				try {
					display.syncExec(new Runnable(){
						public void run(){
							MessageBox mb = new MessageBox(parent, SWT.OK);
							mb.setMessage("Could not read the archive file");
							mb.setText("Error");
							mb.open();
						}
					});
				} catch (Exception e1) {
					System.out.println("Could not read the archive file");
				}
				System.out.println("Stack trace for caught exception: ");
				e.printStackTrace();
			}
			
		}
		
	}
	
	private String readAll(String fileName) throws IOException{
		FileReader fr = new FileReader(fileName);
		String text = "";
		int temp = 0;
		while((temp = fr.read()) != -1){
			text += "" + (char)temp;
		}
		fr.close();
		return text;
	}
}
