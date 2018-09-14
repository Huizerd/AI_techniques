package ai2018.group31;

// Acceptance Strategy
// decides whether the opponent’s bid is acceptable

public class Group31_AS {
	
	public void init(NegotiationSession negotiationSession,
            Group31_BS offeringStrategy,
            Group31_OM opponentModel,
            java.util.Map<java.lang.String,java.lang.Double> parameters) 
            throws java.lang.Exception {
		
		//	negotiationSession - state of the negotiation.
		//	offeringStrategy - of the agent.
		//	parameters - of the acceptance strategy		
            		
		//TODO 
		
	}
	
	public java.lang.String printParameters(){
		
		//	Returns: string representation of the parameters supplied to the model.
		
		//TODO
		
		return null;
	}
	
//	public void setOpponentUtilitySpace(BilateralAtomicNegotiationSession fNegotiation) {
//		//	Method which may be overwritten to get access to the opponent's utilityspace in an experimental setup.
//		//	Parameters:
//		//	fNegotiation - reference to negotiation setting.
	
		//TODO 

//	}
	
	public /*abstract*/ Actions determineAcceptability() {
		
		//	Determines to either to either accept or reject the opponent's bid or even quit the negotiation.
		//	Returns: one of three possible actions: Actions.Accept, Actions.Reject, Actions.Break.
		
		//TODO 
		
		return null;
	}
	
	public final void storeData(java.io.Serializable object) {
		
		//	Description copied from class: BOA
		//	Method used to store data that should be accessible in the next negotiation session on the same scenario. 
		//  This method can be called during the negotiation, but it makes more sense to call it in the endSession method.
		//	Specified by: storeData in class BOA
		//	Parameters: object - to be saved by this component.

		//TODO 
	}	

	public final java.io.Serializable loadData() {

		//	Description copied from class: BOA
		//	Method used to load the saved object, possibly created in a previous negotiation session. The method returns null when such an object does not exist yet.
		//	Specified by: loadData in class BOA
		//	Returns: saved object or null when not available.

		//TODO 

		return null;
	}
	
	public boolean isMAC() {
		
		//	Method which states if the current acceptance strategy is the Multi-Acceptance Strategy. This method should always return false, except for the MAC.
		//	Returns: if AC is MAC.
		
		//TODO 

		return false;
	}
}
