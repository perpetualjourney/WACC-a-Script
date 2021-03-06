package tree.stat;

import org.antlr.v4.runtime.ParserRuleContext;

import symboltable.SymbolTable;
import tree.WACCTree;
import tree.type.WACCType;
/**
 * Abstract class used to group together all WACCTree statement nodes
 * 
 */

public abstract class StatNode extends WACCTree {

	@Override
	public boolean check(SymbolTable funcSt, SymbolTable st, ParserRuleContext ctx ) {
		return true;
	}


	@Override
	public WACCType getType() {
		throw new UnsupportedOperationException("It's not possible to call getType() on a StatNode.");
	}
	
	public boolean checkExpectation() {
		return false;
	}


	/**
	 * 
	 * @return This method will return the number of variables declared 
	 * in the Statement.
	 * 
	 * Most statements will return 0, so this method is only overridden in VarDecNode,
	 * and statements with other nested statements (e.g. seqStatNode)
	 * 
	 */
	public int getVarCounter() {
		return 0;
	}

}
