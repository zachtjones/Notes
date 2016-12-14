import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

public class SaveThread extends Thread {
	private String text;
	private String fileName;
	private long totLength = 0;
	private long lengthSaved = 0;
	private Shell sh;
	private TabItem ti;
	private Display display;
	
	public SaveThread(String allContents, String fileName, TabItem ti, TabFolder tf){
		this.text = allContents;
		this.fileName = fileName;
		this.totLength = this.text.length();
		this.sh = tf.getShell();
		this.ti = ti;
		
		this.display = sh.getDisplay();
	}
	
	@Override
	public void run(){
		long ct = System.currentTimeMillis();
		try {
			FileWriter fw = new FileWriter(this.fileName);
			for(int i = 0; i < totLength; i++){
				fw.write(this.text.charAt(i));
				lengthSaved ++;
			}
			fw.flush();
			fw.close();
			System.out.println("Log: " + (System.currentTimeMillis() - ct) + " milliseconds to save file " + fileName);
			//done
			display.syncExec(new Runnable(){

				@Override
				public void run() {
					try {
						ti.setText(new File(fileName).getName());
						MessageBox mb = new MessageBox(sh, SWT.OK);
						mb.setMessage("File: " + fileName + " was saved properly");
						mb.setText("Save");
						mb.open();
					} catch (Exception e) {
					}
				}
				
			});
		} catch (IOException e) {
			System.out.println("Could not save " + this.fileName);
			display.syncExec(new Runnable(){

				@Override
				public void run() {
					MessageBox mb = new MessageBox(sh, SWT.OK);
					mb.setMessage("Could not save file: " + fileName);
					mb.setText("Save");
					mb.open();
				}
				
			});
		} catch (Exception e){
			//if there is a SWT exception due to disposed stuff
		}
	}
	
	public String getStatus() {
		return "File: " + this.fileName + " " + new DecimalFormat("##.##").format(100*(double)this.lengthSaved/(double)this.totLength) + "% saved";
	}
}
