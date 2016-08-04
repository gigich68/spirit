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

package com.actelion.research.spiritcore.business.biosample;

import com.actelion.research.util.CompareUtils;

public class Amount implements Comparable<Amount> {
	private Double quantity;
	private AmountUnit unit;
	
	public Amount(Double quantity, AmountUnit unit) {
		if(unit==null) throw new IllegalArgumentException("Unit cannot be null");
		this.quantity = quantity;
		this.unit = unit;
	}
	

	public Double getQuantity() {
		return quantity;
	}
	
	public AmountUnit getUnit() {
		return unit;
	}
	
	@Override
	public String toString() {
		return quantity==null?"": quantity + unit.getUnit();
	}
	
	@Override
	public int compareTo(Amount a) {
		int c = unit.compareTo(a.unit);
		if(c!=0) return c;
		return CompareUtils.compare(quantity, a.quantity);
	}
	
	
}
