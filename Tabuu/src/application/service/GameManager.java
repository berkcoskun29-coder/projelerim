package application.service;
import java.util.Collections; 
import java.util.List;
import application.model.TabuCard; 
public class GameManager {
	private List<TabuCard> cards; 
	private int currentCardIndex = 0;
	private String team1Name =	"Takım 1 ";
	private String team2Name =	"Takım 2  ";
	private int team1Score = 0;
	private int team2Score = 0;
	private boolean team1Turn =true;
	private int passRemaining= 3;
	public GameManager(List<TabuCard> cards) {
		this.cards = cards;
		Collections.shuffle(this.cards);
		
	}
	public TabuCard getCurrentCard() {
		if (cards.isEmpty())return null;
		return cards.get(currentCardIndex);
		
	}
	public void nextCard() {
		currentCardIndex++;
		if (currentCardIndex >=cards.size()) {
			currentCardIndex =0;
			Collections.shuffle(cards);
		}
	}
	public void correctAnswer() {
		if (team1Turn) {
			team1Score++;
			}else {
				team2Score++;
			}
		nextCard();
	
	}
	public void tabooPenalty() {
		if (team1Turn) {
			team1Score--;
		}else {
			team2Score--;
		}
		nextCard();
	}
	public boolean usePass() {
		if (passRemaining >0) {
			passRemaining--;
			nextCard();
			return true;
			}
		return false;
	}
	public void nextTurn() {
		team1Turn = !team1Turn;
		passRemaining =3;
	}
	public boolean isTeam1Turn() {
		return team1Turn;
	}
	public String getCurrentTeamName() {
		return team1Turn ? team1Name : team2Name;
	}
	public int getTeam1Score() {
		return team1Score;
	}
	public int getTeam2Score() {
		return team2Score;
}
	public int getPassRemaining() { 
        // Kalan pas hakkını verir.
        return passRemaining; 
    }
	public String getTeam1Name() { 
        // 1. takım adını verir.
        return team1Name; 
    }

    public String getTeam2Name() { 
        // 2. takım adını verir.
        return team2Name; 
    }

    public void setTeamNames(String team1Name, String team2Name) { 
        // Takım isimlerini dışarıdan belirlememizi sağlar.

        this.team1Name = team1Name; 
        // 1. takım adını güncelle

        this.team2Name = team2Name; 
        // 2. takım adını güncelle
    }
}


