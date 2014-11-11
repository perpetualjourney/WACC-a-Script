package tree.stat;

import org.antlr.v4.runtime.RuleContext;

import WACCExceptions.InvalidTypeException;
import symboltable.SymbolTable;
import tree.ExprNode;
import tree.type.WACCType;

public class IfStatNode extends StatNode {
	
	private ExprNode ifCond;
	
	public IfStatNode(ExprNode expr) {
		this.ifCond = expr;
	}
	
	@Override
	public boolean check(SymbolTable st, RuleContext ctx) {
		if (ifCond.getType() == WACCType.BOOL) {
			return true;
		} else {
			el.record(new InvalidTypeException("If statements should have an expr of type BOOL", ctx));
		    return false;
		}
	}
	

}
