package mip.ilp;

public class Summand {
	protected double factor;
	protected String variableName;

	/**
	 * @param factor
	 * @param var
	 */
	public Summand(double factor, String var) {
		this.factor = factor;
		variableName = var;
	}

	/**
	 * 
	 */
	public Summand() {
		factor = 1;
		variableName = null;
	}

	/**
	 * @return Returns the factor.
	 */
	public double getFactor() {
		return factor;
	}

	/**
	 * @param factor
	 *            The factor to set.
	 */
	public void setFactor(double factor) {
		this.factor = factor;
	}

	/**
	 * @return Returns the var.
	 */
	public String getVariableName() {
		return variableName;
	}

	/**
	 * @param var
	 *            The var to set.
	 */
	public void setVariableName(String var) {
		variableName = var;
	}
}
