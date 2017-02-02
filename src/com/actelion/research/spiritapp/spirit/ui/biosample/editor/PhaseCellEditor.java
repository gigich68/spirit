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

package com.actelion.research.spiritapp.spirit.ui.biosample.editor;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import com.actelion.research.spiritapp.spirit.ui.biosample.edit.EditBiosampleTable;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.util.ui.JTextComboBox;


/**
 * Editor for a Phase, modeled as a Phase
 * @author J
 *
 */
public class PhaseCellEditor extends AbstractCellEditor implements TableCellEditor {

	private JTextComboBox textComboBox;
	private Biosample b;
	
	public PhaseCellEditor() {
	}
	
	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, final int row, int column) {
		final EditBiosampleTable t = (EditBiosampleTable) table;
		if(row>=0 && row<t.getRows().size()) {
			b = t.getRows().get(row);
		}
		textComboBox = new JTextComboBox(false) {
			@Override
			public Collection<String> getChoices() {
				List<String> choices = new ArrayList<>();
				if(b!=null && b.getInheritedStudy()!=null) {
					for(Phase s : b.getInheritedStudy().getPhases()) {
						choices.add(s.getShortName());
					}
				}
				return choices;
			}
		};
		textComboBox.setMargin(null);
		textComboBox.setBorder(BorderFactory.createMatteBorder(1,1,1,1, Color.BLUE));
		if(value==null) {
			textComboBox.setText("");
		} else if(value instanceof Phase) {
			textComboBox.setText(((Phase)value).getShortName());
		}
		
		System.out.println("PhaseCellEditor.getTableCellEditorComponent() "+value);
		
		textComboBox.selectAll();
		return textComboBox;
	}

	@Override
	public Phase getCellEditorValue() {
		Biosample b = JPAUtil.reattach(this.b);
		Phase phase = b==null || b.getInheritedStudy()==null? null: b.getInheritedStudy().getPhase(textComboBox.getText());
		return phase;
	}				
}