import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Scanner;

public class Definition implements Comparable<Definition>, Serializable {
	
	/** The version ID for this definition */
	private static final long serialVersionUID = 1L;
	
	private String word;
	private String definition;
	
	/**
	 * Creates the definition of a word from an online dictionary.
	 * @param word The String that is the word to define.
	 * @throws IOException This will throw an IOException if the word is not found online, 
	 * no netword connection, or any other I/O error.
	 */
	public Definition(String word) throws IOException {
		this.word = word;
		this.definition = defineWord(word);
	}
	
	/**
	 * Creates a definition of a word give both the word and the definition.
	 * This is used for allowing the user to define words.
	 * @param word The String that is the word to define.
	 * @param definition The definition of the word.
	 */
	public Definition(String word, String definition){
		this.word = word;
		this.definition = definition;
	}
	
	/**
	 * Gets the word for this definition.
	 * @return The word that this definition defines.
	 */
	public String getWord(){
		return this.word;
	}
	
	/**
	 * Gets the definition of the word.
	 * @return The string that is the definition of the word.
	 */
	public String getDefinition(){
		return this.definition;
	}
	
	/**
	 * Downloads a page as a String from the URL
	 * @param url The page location
	 * @return A string that is the contents of the page
	 * @throws IOException If there is an error either discovering the page or reading from it
	 */
	private static String downloadPage(URL url) throws IOException{
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
	private static String downloadPage(String fileName) throws IOException{
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
	private static String defineWord(String word) throws IOException {
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
	 * Gets if a word is a roman numeral.
	 * A roman numeral does not have to be properly formatter, however it must only contain:
	 * I, V, X, L, C, D, and M characters.
	 * @param word The string that is the word to check.
	 * @return true iff word is a roman numeral
	 */
	public static boolean isRomanNumeral(String word){
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
	 * Required for the natural ordering of Definitions.
	 * This compares the words of the definitons, ignoring case.
	 * @param o The other definition to compare to.
	 * @return a negative integer, zero, or a positive integer as the specified Definition is 
	 * greater than, equal to, or less than this Definition, ignoring case considerations.
	 */
	@Override
	public int compareTo(Definition o) {
		return this.word.compareToIgnoreCase(o.word);
	}
}
