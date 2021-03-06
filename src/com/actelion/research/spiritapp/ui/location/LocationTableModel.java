/*
 * Spirit, a study/biosample management tool for research.
 * Copyright (C) 2018 Idorsia Pharmaceuticals Ltd., Hegenheimermattweg 91,
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

package com.actelion.research.spiritapp.ui.location;

import java.util.ArrayList;
import java.util.List;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.location.LocationTable.LocationTableMode;
import com.actelion.research.spiritapp.ui.location.column.LocationDescriptionColumn;
import com.actelion.research.spiritapp.ui.location.column.LocationFlagColumn;
import com.actelion.research.spiritapp.ui.location.column.LocationFreeColumn;
import com.actelion.research.spiritapp.ui.location.column.LocationNameColumn;
import com.actelion.research.spiritapp.ui.location.column.LocationOccupiedColumn;
import com.actelion.research.spiritapp.ui.location.column.LocationPrivacyColumn;
import com.actelion.research.spiritapp.ui.location.column.LocationSizeColumn;
import com.actelion.research.spiritapp.ui.location.column.LocationStudyColumn;
import com.actelion.research.spiritapp.ui.location.column.LocationTypeColumn;
import com.actelion.research.spiritcore.business.location.Location;
import com.actelion.research.spiritcore.business.location.LocationType.LocationCategory;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.util.ui.exceltable.Column;
import com.actelion.research.util.ui.exceltable.ExtendTableModel;

public class LocationTableModel extends ExtendTableModel<Location> {

	private final LocationTableMode mode;

	public LocationTableModel(LocationTableMode mode) {
		this.mode = mode;

		List<Column<Location, ?>> columns = new ArrayList<>();
		Column<Location, ?> nameColumn = new LocationNameColumn();
		if(SpiritProperties.getInstance().isAdvancedMode()) {
			columns.add(new LocationFlagColumn());
		}
		columns.add(nameColumn);
		columns.add(new LocationTypeColumn());
		columns.add(new LocationDescriptionColumn());
		columns.add(new LocationOccupiedColumn());
		columns.add(new LocationFreeColumn());
		columns.add(new LocationSizeColumn());
		if(SpiritProperties.getInstance().isAdvancedMode()) {
			columns.add(new LocationPrivacyColumn());
		}
		columns.add(new LocationStudyColumn());
		setColumns(columns);
		setTreeColumn(nameColumn);
	}

	@Override
	public Location getTreeParent(Location row) {
		if(row==null) {
			return null;
		}
		try {
			row = JPAUtil.reattach(row);
			return row.getParent();
		} catch(Exception e) {
			System.err.println("Lazy loading error: ");
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<Location> getTreeChildren(Location row) {
		List<Location> res = new ArrayList<>();
		if(row==null) return res;
		try {
			row = JPAUtil.reattach(row);
			for (Location loc: row.getChildren()) {
				if(!SpiritRights.canRead(loc, SpiritFrame.getUser())) continue;
				res.add(loc);
			}
		} catch(Exception e) {
			System.err.println("Lazy loading error: ");
			e.printStackTrace();
		}

		//Filter unwanted items
		if(mode==LocationTableMode.ADMIN_LOCATION) {
			List<Location> filtered = new ArrayList<>();
			for (Location location : res) {
				if(location.getLocationType().getCategory()==LocationCategory.MOVEABLE) continue;
				filtered.add(location);
			}
			res = filtered;
		}

		return res;
	}



}
