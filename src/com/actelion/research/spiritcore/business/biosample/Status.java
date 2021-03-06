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

package com.actelion.research.spiritcore.business.biosample;

import java.awt.Color;

public enum Status {

	//Default status
	INLAB("Available", true, Color.BLACK, null),

	//Normal status
	PLANNED("Planned", false, new Color(0, 0, 120), new Color(180, 180, 255)),
	//	RECEIVED("Received", true, new Color(0, 120, 0), new Color(180, 255, 180)),
	STORED("Stored", true, Color.BLACK, new Color(180, 255, 180)),
	INPREP("In Preparation", true, new Color(0, 0, 150), new Color(180, 255, 180)),
	TRANSFERRED("Transferred", true, new Color(0, 0, 150), new Color(180, 180, 255)),



	USEDUP("Used Up", true, new Color(120, 70, 0), new Color(255, 180, 255)),
	LOWVOL("Low Volume", true, new Color(120, 70, 0), new Color(160, 160, 80)),

	//no more available status
	TRASHED ("Disposed", false, new Color(180, 0, 0), new Color(255, 80, 80)),
	NECROPSY("Necropsied", false, new Color(100, 70, 0), new Color(255, 225, 225)),

	//problematic status
	DEAD("Found Dead", false, new Color(100, 0, 0), new Color(255, 100, 100)),
	KILLED("Killed", false, new Color(100, 0, 0), new Color(255, 100, 100)),
	;

	private final String name;
	private final boolean available;
	private final Color foreground;
	private final Color background;

	private Status(String name, boolean available, Color foreground, Color background) {
		this.name = name;
		this.available = available;
		this.foreground = foreground;
		this.background = background;

	}

	public boolean isAvailable() {
		return available;
	}

	public String getName() {
		return name;
	}

	public Color getForeground() {
		return foreground;
	}

	public Color getBackground() {
		return background;
	}

	@Override
	public String toString() {
		return name;
	}
}
