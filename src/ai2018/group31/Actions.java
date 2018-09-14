package ai2018.group31;

public class Actions implements java.io.Serializable, java.lang.Comparable<Actions> {

	// Possible actions: 
	// - Accept: Accept the opponent's offer.
	// - Break: Walk away from the negotiation.
	// - Reject: Reject the opponent's offer.
	
	public enum Action {
	    ACCEPT, BREAK, REJECT
	}
	
	@Override
	public int compareTo(Actions o) {
		// TODO Auto-generated method stub
		return 0;
	}

}
