package assembly;

import java.util.ArrayList;
import java.util.List;

public abstract class InstrToken {
	private InstrToken next;
	
	public void setNext(InstrToken t) {
		if (next == null) {
			next = t;
		} else {
			throw new IllegalAccessError("You can't set the next token more than once.\n"
					+ "Token given: " + t.toString());
		}
	}
	
	public List<InstrToken> flatten() {
		List<InstrToken> tokens = new ArrayList<>();
		tokens.add(this);
		tokens.addAll(next.flatten());
		return tokens;
	}

}