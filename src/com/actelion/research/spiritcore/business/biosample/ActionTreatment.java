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

import java.awt.Color;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.actelion.research.spiritcore.business.study.NamedTreatment;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.spiritcore.business.study.StudyAction;
import com.actelion.research.spiritcore.util.Pair;

@Entity
@DiscriminatorValue("Treatment")
@Audited
/**
 * This is used to save the treatment applied to an animal
 * This class evolves from the beginning as it it not used anymore to "store" the weight but only to track what has been done.
 * @author freyssj
 *
 */
public class ActionTreatment extends ActionBiosample {
	
	/** The link to the treatment, can be null for pure weighing */
	@ManyToOne(cascade=CascadeType.REFRESH, fetch=FetchType.LAZY)
	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	@JoinColumn(name="treatment_id")
	@BatchSize(size=100)
	private NamedTreatment namedTreatment;
	
	@Column(name="treatment_weight", precision=9, scale=3)
	private Double weight;

	@Column(name="treatment_calcdose", precision=9, scale=3)
	private Double calculatedDose;

	@Column(name="treatment_effdose", precision=9, scale=3)
	private Double effectiveDose;
		
	@Column(name="treatment_calcdose2", precision=9, scale=3)
	private Double calculatedDose2;

	@Column(name="treatment_effdose2", precision=9, scale=3)
	private Double effectiveDose2;
		
	
	@Column(name="treatment_formulation", length=20)
	private String formulation;
	
	@Column(nullable=true)
	private String updUser = "??";

	
	public ActionTreatment() {		
	}
	public ActionTreatment(Biosample animal, Phase phase, Double weight, NamedTreatment nt, Double eff1, Double eff2, String formulation, String comments, String user) {
		this.biosample = animal;
		this.phase = phase;
		this.weight = weight;
		this.namedTreatment = nt;
		this.calculatedDose = nt==null? null: nt.getCalculatedDose1(weight);
		this.calculatedDose2 = nt==null? null: nt.getCalculatedDose2(weight);
		this.effectiveDose = eff1;
		this.effectiveDose2 = eff2;
		this.formulation = formulation;
		this.comments = comments;
		this.updUser = user;
		this.updDate = new Date();
	}
	
	public NamedTreatment getNamedTreatment() {
		return namedTreatment;
	}

	public void setNamedTreatment(NamedTreatment namedTreatment) {
		this.namedTreatment = namedTreatment;
	}

	public Double getCalculatedDose1() {
		return calculatedDose;
	}

	public void setCalculatedDose1(Double calculatedDose) {
		this.calculatedDose = calculatedDose;
	}

	public Double getEffectiveDose1() {
		return effectiveDose;
	}

	public void setEffectiveDose1(Double effectiveDose) {
		this.effectiveDose = effectiveDose;
	}

	public void setWeight(Double weight) {
		this.weight = weight;
	}

	public Double getWeight() {
		return weight;
	}	
			
	@Override
	public String getDetails() {
		String s = (getNamedTreatment()!=null? "Treatment: " : "Weighing: ") + 
			(getWeight()==null?"": getWeight()+"g ") +
			(getNamedTreatment()==null? 
				"": 
				(getCalculatedDose1()==null?"": (getEffectiveDose1()==null?getCalculatedDose1(): getEffectiveDose1()) + (getNamedTreatment().getUnit1()!=null? getNamedTreatment().getUnit1().getNumerator():"") + " ") +
				(getCalculatedDose2()==null?"": (getEffectiveDose2()==null?getCalculatedDose1(): getEffectiveDose2()) + (getNamedTreatment().getUnit2()!=null? getNamedTreatment().getUnit2().getNumerator():"") + " ") +
				(getFormulation()!=null? getFormulation(): (getNamedTreatment()==null?"": "[" + getNamedTreatment().getName() + "]"))) + 		
			(getComments()!=null?": "+getComments():"");		
		return s;

	}
	
	/**
	 * @param formulation the formulation to set
	 */
	public void setFormulation(String formulation) {
		this.formulation = formulation;
	}

	/**
	 * @return the formulation
	 */
	public String getFormulation() {
		return formulation;
	}
		
	public boolean isWeighingRequired() {		
		if(getBiosample()==null || getPhase()==null) return false;
		StudyAction a = getBiosample().getStudyAction(getPhase());
		if(a==null) return false;
		if(a.isMeasureWeight() || (a.getNamedTreatment()!=null && a.getNamedTreatment().isWeightDependant())) {
			return true;
		} else {
			return false;
		}	
	}
	
	public Pair<Double, Phase> calculateWeightPercentIncrease() {
		if(getWeight()==null || getWeight()<=0) return null;
		
		
		if(getPhase()==null) {
			System.err.println("ActionTreatment: " + this+" has a null phase");
			return null;
		}

		List<ActionTreatment> list = getBiosample().getActions(ActionTreatment.class);
		if(list==null) return null;
		ActionTreatment sel = null;
		for (ActionTreatment fw : list) {
			if(fw.getPhase()==null) {
				System.err.println(fw+" has a null phase");
				continue;
			}
			if(fw.getPhase().getTime()>=getPhase().getTime()) continue;
			if(fw.getWeight()==null || fw.getWeight()<=0) continue;
			
			if(sel==null || sel.getPhase().getTime()<fw.getPhase().getTime()) {
				sel = fw;
			}
		}
		
		if(sel==null) return null;
		return new Pair<Double,Phase> (100*(getWeight() - sel.getWeight())/sel.getWeight(), sel.getPhase());
		
	}
	

	
	public ActionTreatment getPrevious() {
		return getPreviousMeasureFromList(getBiosample().getActions(ActionTreatment.class));
		
	}
	public ActionTreatment getPreviousMeasureFromList(List<ActionTreatment> list) {
		ActionTreatment sel = null;
		if(list!=null) {
			for (ActionTreatment fw : list) {
				if(fw.getBiosample().getId()!=getBiosample().getId()) continue;
				if(fw.getPhase()==null) {
					System.err.println(fw+" has a null phase");
					continue;
				}
				if(getPhase()==null) {
					System.err.println(this+" has a null phase");
					continue;
				}
				if(fw.getPhase().getTime()>=getPhase().getTime()) continue;
				
				if(sel==null || sel.getPhase().getTime()<fw.getPhase().getTime()) {
					sel = fw;
				}
			}
		}
		return sel;
	}
	
	
	public Double getCalculatedDose2() {
		return calculatedDose2;
	}

	public void setCalculatedDose2(Double calculatedDose2) {
		this.calculatedDose2 = calculatedDose2;
	}

	public Double getEffectiveDose2() {
		return effectiveDose2;
	}

	public void setEffectiveDose2(Double effectiveDose2) {
		this.effectiveDose2 = effectiveDose2;
	}

	public boolean isEmpty() {
		return weight==null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj==null || !(obj instanceof ActionTreatment)) return false;
		ActionTreatment t = (ActionTreatment) obj;
		return getPhase().equals(t.getPhase()) && getBiosample().equals(t.getBiosample());
		
	}

		
	@Override
	public Color getColor() {
		return new Color(225,255,195);
	}


	public String getUpdUser() {
		return updUser;
	}

	public void setUpdUser(String updUser) {
		this.updUser = updUser;
	}

}
