package com.dynatrace.diagnostics.plugins.utils;

public enum PURGE_AFTER {
    NEVER("never"),
    ONE_WEEK("1 week"),
    TWO_WEEKS("2 weeks"),
    THREE_WEEKS("3 weeks"),
    FOUR_WEEKS("4 weeks"),
    ONE_MONTH("1 month"),
    TWO_MONTHS("2 months"),
    THREE_MONTHS("3 months"),
    SIX_MONTHS("6 months"),
    ONE_YEAR("1 year"),
    TWO_YEARS("2 years"),
    THREE_YEARS("3 years"),
    FIVE_YEARS("5 years");
    
    private String name;
    
    PURGE_AFTER(String name) {
    	this.name = name;
    }
    
    public static PURGE_AFTER valueOfIgnoreCase(String name) {
        for(PURGE_AFTER pa : PURGE_AFTER.values()) {
            if (pa.name.equalsIgnoreCase(name)) {
                return pa;
            }
        }
        throw new IllegalArgumentException("There is no value with name '" + name + "' in the enum " + PURGE_AFTER.class.getName());        
    }
    
    public String toString() {
    	return name;
    }

}
