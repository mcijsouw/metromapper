package io;

public class KeyElement {
	private String id;
	private String forType;
	private String name;
	private String attrType;
	private String defaultValue;
	private String line = "";

	public KeyElement(String id, String forType, String name, String attrType) {
		this.id = id;
		this.forType = forType;
		this.name = name;
		this.attrType = attrType;
	}

	public KeyElement(String id, String forType, String name, String attrType, String defaultValue, String line) {
		this.id = id;
		this.forType = forType;
		this.name = name;
		this.attrType = attrType;
		this.defaultValue = defaultValue;
		this.line = line;
	}

	/**
	 * @return Returns the attrType.
	 */
	public String getAttrType() {
		return attrType;
	}

	/**
	 * @return Returns the defaultValue.
	 */
	public String getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @return Returns the forType.
	 */
	public String getForType() {
		return forType;
	}

	/**
	 * @return Returns the id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return Returns the line.
	 */
	public String getLine() {
		return line;
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}
}
