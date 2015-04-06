package mip.ilp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class Constraint {
	public static final int LESS = 0;
	public static final int EQUAL = 1;
	public static final int GREATER = 2;

	protected final int LINESPLIT = 200;

	protected Set summands;
	protected int sense;
	protected double rhs;
	protected String name;
	protected boolean lazy;

	/**
	 * 
	 */
	public Constraint() {
		summands = new HashSet();
		sense = LESS;
		rhs = 0;
		name = "";
		lazy = false;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the rhs.
	 */
	public double getRhs() {
		return rhs;
	}

	/**
	 * @param rhs
	 *            The rhs to set.
	 */
	public void setRhs(double rhs) {
		this.rhs = rhs;
	}

	public boolean isLess() {
		return (sense == LESS);
	}

	public boolean isEqual() {
		return (sense == EQUAL);
	}

	public boolean isGreater() {
		return (sense == GREATER);
	}

	public void setLess() {
		sense = LESS;
	}

	public void setEqual() {
		sense = EQUAL;
	}

	public void setGreater() {
		sense = GREATER;
	}

	/**
	 * @return Returns the summandSet.
	 */
	public Set getSummands() {
		return summands;
	}

	public void addSummand(Summand s) {
		summands.add(s);
	}

	public boolean isLazy() {
		return lazy;
	}

	public void setLazy(boolean lazy) {
		this.lazy = lazy;
	}

	public String toString() {
		String str = name;
		int currentLineLength = str.length();
		if (!name.equals("")) {
			str = str.concat(": ");
			currentLineLength += 2;
		}

		Iterator it = summands.iterator();
		while (it.hasNext()) {
			Summand summand = (Summand) it.next();
			int sign = (int) Math.signum(summand.getFactor());
			if (sign == -1) {
				String append = summand.getFactor() + " " + summand.getVariableName() + " ";
				if (currentLineLength + append.length() > LINESPLIT) {
					str = str.concat("\n");
					currentLineLength = 0;
				}
				str = str.concat(append);
				currentLineLength += append.length();
			} else if (sign == 1) {
				String append = "+" + summand.getFactor() + " " + summand.getVariableName() + " ";
				if (currentLineLength + append.length() > LINESPLIT) {
					str = str.concat("\n");
					currentLineLength = 0;
				}
				str = str.concat(append);
				currentLineLength += append.length();
			}
		}
		if (("" + rhs).length() + 2 + currentLineLength > LINESPLIT) {
			str = str.concat("\n");
			currentLineLength = 0;
		}
		if (sense == LESS) {
			str = str.concat("< ");
		} else if (sense == EQUAL) {
			str = str.concat("= ");
		} else if (sense == GREATER) {
			str = str.concat("> ");
		}
		str = str.concat("" + rhs);
		currentLineLength += (("" + rhs).length() + 2);
		return str;
	}
}
