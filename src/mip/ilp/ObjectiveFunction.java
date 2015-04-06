package mip.ilp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ObjectiveFunction {
	protected boolean minimize;
	protected Set summands;
	protected String name;
	protected final int LINESPLIT = 200;

	/**
	 * 
	 */
	public ObjectiveFunction() {
		this.minimize = true;
		this.summands = new HashSet();
		this.name = "";
	}

	/**
	 * @return Returns the minimize.
	 */
	public boolean isMinimize() {
		return minimize;
	}

	/**
	 * @param minimize
	 *            The minimize to set.
	 */
	public void setMinimize(boolean minimize) {
		this.minimize = minimize;
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
	 * @return Returns the summands.
	 */
	public Set getSummands() {
		return summands;
	}

	/**
	 * Adds a new summand to the objective function.
	 * 
	 * @param s
	 *            The summand
	 */
	public void addSummand(Summand s) {
		summands.add(s);
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
		return str;
	}

}
