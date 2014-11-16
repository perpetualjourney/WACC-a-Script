package WACCExceptions;

import org.antlr.v4.runtime.ParserRuleContext;

public class UndeclaredIdentifierException extends WACCException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2038560013979288674L;

	public UndeclaredIdentifierException(String exceptionMessage, ParserRuleContext ctx) {
		super(exceptionMessage, ctx);
	}

}