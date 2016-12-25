import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class DefinitionShower {
	/** The definitions on this. this is sorted by the word */
	private TreeMap<String, Definition> definitions;
	/** the reference to the main's definitions */
	private HashMap<String, Definition> mainDefs;
	/** The definition text field*/
	private TextArea tfDef;
	/** The word text field*/
	private TextField tfWord;
	
	public DefinitionShower(HashMap<String, Definition> definitions){
		this.mainDefs = definitions;
		//want this to be sorted
		this.definitions = new TreeMap<String, Definition>(new Comparator<String>(){
			@Override
			public int compare(String o1, String o2) {
				return o1.compareToIgnoreCase(o2);
			}
		});
		this.definitions.putAll(definitions);
	}
	
	public void show(Stage primaryStage){
		BorderPane bp = new BorderPane();
		
		//left side is the list of words
		VBox left = new VBox();
		left.getChildren().add(new Label("Words: "));
		
		ListView<String> lw = new ListView<>();
		lw.setPrefSize(100, 400);
		//add all items
		lw.getItems().addAll(this.definitions.keySet());
		
		lw.getSelectionModel().selectedItemProperty().addListener(event -> {
			String selected = lw.getSelectionModel().getSelectedItem();
			if(selected == null){ return; }
			Definition d = this.definitions.get(selected);
			if(d != null){
				tfWord.setText(d.getWord());
				tfDef.setText(d.getDefinition());
			}
		});
		left.getChildren().add(lw);
		bp.setLeft(left);
		
		//center is the edit for specific word
		VBox center = new VBox();
		//edit definition label
		center.getChildren().add(new Label("Edit definition / add definition"));
		
		HBox h = new HBox();
		//word label
		h.getChildren().add(new Label("Word: "));
		//word text field
		tfWord = new TextField();
		tfWord.setPrefWidth(300);
		h.getChildren().add(tfWord);
		center.getChildren().add(h);
		//definition label
		center.getChildren().add(new Label("Definition"));
		//definition text field
		tfDef = new TextArea();
		
		center.getChildren().add(tfDef);
		
		//update button
		Button update = new Button("Update / Add");
		update.setPrefWidth(340);
		update.setOnAction(event -> {
			//get the word and definition
			String word = tfWord.getText();
			String definition = tfDef.getText();
			Definition d = new Definition(word, definition);
			
			if(word.isEmpty()){
				Alert a = new Alert(AlertType.ERROR);
				a.setContentText("Please enter a word to define.");
				a.setTitle("Error");
				a.showAndWait();
				tfWord.requestFocus();
				return;
			}
			if(definition.isEmpty()){
				Alert a = new Alert(AlertType.ERROR);
				a.setContentText("Please enter a definition for the word.");
				a.setTitle("Error");
				a.showAndWait();
				tfDef.requestFocus();
				return;
			}
			//update value / add value
			this.definitions.put(word, d);
			lw.getItems().clear();
			lw.getItems().addAll(this.definitions.keySet());
		});
		center.getChildren().add(update);
		bp.setCenter(center);
		
		Scene scene = new Scene(bp);
		Stage stage = new Stage();
		stage.setScene(scene);
		stage.setX(primaryStage.getX());
		stage.setY(primaryStage.getY());
		stage.initStyle(StageStyle.DECORATED);
		stage.initOwner(primaryStage);
		stage.setOnCloseRequest(event -> {
			//update the main dictionary
			this.mainDefs.clear();
			this.mainDefs.putAll(definitions);
			//update the main dictionary
		});
		stage.showAndWait();
	}
}
