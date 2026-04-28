package application.model;
import java.util.List;

public class TabuCard {
	private String mainWord;
	private List<String> forbiddenWords;
	public TabuCard(String mainWord, List<String> fotbiddenWords) {
		this.mainWord = mainWord;
		this.forbiddenWords = forbiddenWords;
	}
	public String getMainWord() {
		return mainWord;
	}
	public List<String> getForbiddenWords(){
		return forbiddenWords;
	}

	
}
