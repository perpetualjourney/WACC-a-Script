package tree.stat;

import org.antlr.v4.runtime.RuleContext;

import WACCExceptions.InvalidTypeException;
import symboltable.SymbolTable;
import tree.ExprNode;
import tree.type.WACCType;

public class WhileStatNode extends StatNode {
	
	private ExprNode expr;
	
	public WhileStatNode(ExprNode expr) {
		this.expr = expr;
	}
	
	@Override
	public boolean check(SymbolTable st, RuleContext ctx) {
		if (expr.getType() == WACCType.BOOL) {
			return true;
		} else {
			el.record(new InvalidTypeException("While statement should have an expr of type BOOL", ctx));
			return false;
		}
	}

}
