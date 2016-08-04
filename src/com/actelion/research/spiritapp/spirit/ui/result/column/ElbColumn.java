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

package com.actelion.research.spiritapp.spirit.ui.result.column;

import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.util.ui.exceltable.Column;

public class ElbColumn extends Column<Result, String> {
	public ElbColumn() {
		super("Result\nELB", String.class);
		setHideable(true);
	}
	
	@Override
	public float getSortingKey() {
		return 1f;
	}
	
	@Override
	public String getValue(Result row) {
		return row.getElb();
	}
	
	@Override
	public boolean isEditable(Result row) {return false;}
	

}
