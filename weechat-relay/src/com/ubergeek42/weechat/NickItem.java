package com.ubergeek42.weechat;

public class NickItem {
	int group;
	int visible;
	int level;
	String name;
	String color;
	String prefix;
	String prefixColor;
	
	@Override
	public String toString() {
		return String.format("%s%s",prefix,name);
	}
}
