package com.hazmit.nas_runner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ShowOrMovie {

	public ShowOrMovie(String name) {
		super();
		this.name = name;
	}

	private String name;
	private Map<String, File> components = new HashMap<String, File>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, File> getComponents() {
		return components;
	}

	public void setComponents(Map<String, File> components) {
		this.components = components;
	}
	
	@Override
	public String toString() {
		return "ShowOrMovie [name=" + name + ", components=" + components + "]";
	}
	
}
