package tree.stat;

import org.antlr.v4.runtime.ParserRuleContext;

import symboltable.SymbolTable;
import tree.expr.ExprNode;
import tree.type.WACCType;
import visitor.WACCTreeVisitor;
import WACCExceptions.InvalidTypeException;
import assembly.ImmValue;
import assembly.LabelCounter;
import assembly.Register;
import assembly.TokenSequence;
import assembly.tokens.BranchToken;
import assembly.tokens.CompareToken;
import assembly.tokens.LabelToken;

/**
 * Class to represent if statements 
 * Rule: 'if' expr 'then' stat 'else' stat 'fi'
 *
 */

public class IfStatNode extends StatNode {
	
	private ExprNode ifCond;
	private StatNode thenStat;
	private StatNode elseStat;
	
	public IfStatNode(ExprNode expr, StatNode thenStat, StatNode elseStat) {
		this.ifCond = expr;
		this.thenStat = thenStat;
		this.elseStat = elseStat;
	}
	
	@Override
	public boolean check(SymbolTable funcSt, SymbolTable st, ParserRuleContext ctx) {
		if (ifCond.getType() == WACCType.BOOL) {
			return true;
		} else {
			new InvalidTypeException("If statements should have an expr of type BOOL", ctx);
		    return false;
		}
	}
	 
	public TokenSequence toAssembly(Register register) {
		TokenSequence ifStat = ifCond.toAssembly(register);
		String l0 = "l" + LabelCounter.counter.getLabel();
		String l1 = "l" + LabelCounter.counter.getLabel();		
		ifStat.appendAll(new TokenSequence(
				new CompareToken(register, ImmValue.zero),
				new BranchToken("EQ", l0)));
		ifStat.appendAll(thenStat.toAssembly(register));
		ifStat.appendAll(new TokenSequence(
				new BranchToken(l1),
				new LabelToken(l0)));
		ifStat.appendAll(elseStat.toAssembly(register));
		ifStat.append(new LabelToken(l1));
		return ifStat;
	}

	@Override
	public boolean checkExpectation() {
		return thenStat.checkExpectation() && elseStat.checkExpectation();
	}
	
	@Override
	public <T> T accept(WACCTreeVisitor<T> visitor) {
		return visitor.visitIfStatNode(this);
	}

	public ExprNode getIfCond() {
		return ifCond;
	}

	public StatNode getThenStat() {
		return thenStat;
	}

	public StatNode getElseStat() {
		return elseStat;
	}
	
	@Override
	public int getVarCounter() {
		return thenStat.getVarCounter() + elseStat.getVarCounter();
	}

}
