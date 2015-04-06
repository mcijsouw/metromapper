package mip.ilp;

public class Variable {
	public static final int REAL = 0;
	public static final int BINARY = 1;
	public static final int GENERAL = 2;

	protected double lower, upper; // bounds for the variable's value
	protected int type; // none, binary or general according to the constants defined above

	/**
	 * 
	 */
	public Variable() {
		lower = Integer.MIN_VALUE;
		upper = Integer.MAX_VALUE;
		type = GENERAL;
	}

	public boolean isBinary() {
		return (type == BINARY);
	}

	public boolean isGeneral() {
		return (type == GENERAL);
	}

	public boolean isReal() {
		return (type == REAL);
	}

	public void setBinary() {
		type = BINARY;
	}

	public void setGeneral() {
		type = GENERAL;
	}

	public void setReal() {
		type = REAL;
	}

	/**
	 * @return Returns the lower.
	 */
	public double getLower() {
		return lower;
	}

	/**
	 * @param lower
	 *            The lower to set.
	 */
	public void setLower(double lower) {
		this.lower = lower;
	}

	/**
	 * @return Returns the upper.
	 */
	public double getUpper() {
		return upper;
	}

	/**
	 * @param upper
	 *            The upper to set.
	 */
	public void setUpper(double upper) {
		this.upper = upper;
	}

}
