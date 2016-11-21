/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2016 Actelion Pharmaceuticals Ltd., Gewerbestrasse 16,
 * CH-4123 Allschwil, Switzerland.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 * @author Joel Freyss
 */

package com.actelion.research.spiritcore.business;


import java.text.SimpleDateFormat;

public enum DataType {	
	ALPHA("Alphanumeric", null),
	AUTO("Autocomplete", null),
	NUMBER("Numeric", null),		
	LIST("OneChoice", "List of options"),
	MULTI("MultiChoice", "List of options"),
	DATE("Date/Time", null),	
	D_FILE("File", null),
	LARGE("LargeText", null),
	FORMULA("Formula", "Formula"),	
	BIOSAMPLE("Biosample", "Biotype")
	;
	
	public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy"); 
	public static SimpleDateFormat DATETIME_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm"); 	
	private final String name;
	private final String parametersDescription;
	
	private DataType(String name, String parametersDescription) {
		this.name = name;
		this.parametersDescription = parametersDescription;
	}
	
	public String getDescription() {
		return name;
	}
	
	public String getParametersDescription() {
		return parametersDescription;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
