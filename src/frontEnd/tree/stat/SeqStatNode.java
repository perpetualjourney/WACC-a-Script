package tree.stat;

import visitor.WACCTreeVisitor;
import assembly.Register;
import assembly.TokenSequence;

/**
 * Class to represent sequential statements
 * Rule: stat ; stat
 */

public class SeqStatNode extends StatNode {
	
	private StatNode lhs;
	private StatNode rhs;
	
	public SeqStatNode(StatNode lhs, StatNode rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}
	
	public TokenSequence toAssembly(Register register) {
		return lhs.toAssembly(register).appendAll(rhs.toAssembly(register));
	}

	@Override
	public <T> T accept(WACCTreeVisitor<T> visitor) {
		return visitor.visitSeqStatNode(this);
	}

	public StatNode getLhs() {
		return lhs;
	}
	
	public StatNode getRhs() {
		return rhs;
	}
	
	@Override
	public boolean checkExpectation() {
		return lhs.checkExpectation() || rhs.checkExpectation();
	}
	
	@Override
	public int getVarCounter() {
		return lhs.getVarCounter() + rhs.getVarCounter();
	}
}
