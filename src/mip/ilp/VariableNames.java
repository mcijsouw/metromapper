package mip.ilp;

import java.util.HashMap;

public class VariableNames {
	protected HashMap nameToVariable, variableToName;

	/**
	 * 
	 */
	public VariableNames() {
		nameToVariable = new HashMap();
		variableToName = new HashMap();
	}

	public String getName(Variable var) {
		return (String) variableToName.get(var);
	}

	public Variable getVariable(String name) {
		return (Variable) nameToVariable.get(name);
	}

	public boolean contains(String name) {
		return nameToVariable.containsKey(name);
	}

	public boolean contains(Variable var) {
		return variableToName.containsKey(var);
	}

	public void add(Variable var, String name) throws UniqueNameException {
		if (nameToVariable.containsKey(name)) {
			throw new UniqueNameException("A variable with name " + name + " already exists.");
		} else {
			// make sure both maps are consistent
			if (variableToName.containsKey(var)) {
				String oldName = (String) variableToName.get(var);
				nameToVariable.remove(oldName);
			}
			nameToVariable.put(name, var);
			variableToName.put(var, name);
		}
	}

	public static class UniqueNameException extends Exception {
		public UniqueNameException(String s) {
			super(s);
		}
	}
}
