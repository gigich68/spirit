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

package com.actelion.research.spiritcore.business.pivot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Aggregation;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Deviation;
import com.actelion.research.spiritcore.business.pivot.PivotTemplate.Where;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.study.Phase;
import com.actelion.research.util.CompareUtils;


/**
 * A PivotCell is a container for a list of ResultValue
 * @author freyssj
 *
 */
public class PivotCell implements Comparable<PivotCell> {
	

	//Underlying data
	private final PivotDataTable table;
	private List<ResultValue> values = new ArrayList<ResultValue>();
	private List<PivotCellKey> orderedNestedKeys = new ArrayList<PivotCellKey>();		
	private Map<PivotCellKey, PivotCell> nestedMap;
	
	//
	private Margins margin;
	private boolean calculated = false;

	//Data to display
	private Comparable<?> aggregated = null;
	private Double calculatedValue = null;
	private Double std = null;
	private Integer coeff = null;
	private int N;
	
	
	/**
	 * Margins showing where to display the values:
	 * <pre> 
	 * 0...........m1...........m2...........m3...........m4...........m5
	 *   label             value  computed     std                  (N)
	 * </pre>
	 * 
	 * The margin is used to know the best preferred size (max(m5)), but we still have to transform the margins to perform right-alignment 
	 *
	 * <pre> 
	 * 0...........m1...........WIDTH-m52....WIDTH-m53....WIDTH-m54....WIDTH
	 *   label             value  computed     std                  (N)
	 * </pre>
	 * 
	 * @author freyssj
	 *
	 */
	public static class Margins {
		public int m1Value = -1;
		public int m2Computed = -1;
		public int m3Std = 0;
		public int m4N = 0;
		public int m5Width = 0;
		
		@Override
		public String toString() {
			return m1Value+" "+m2Computed+" "+m3Std+" "+m4N+" "+m5Width;
		}
	}
	
	public PivotCell(PivotDataTable table) {
		this.table = table;
	}
		
	private void calculateStats() {
		if(calculated) return;
		
		PivotTemplate tpl = table.getTemplate();
		
		calculated = true;		
		aggregated = null;
		std = null;
		coeff = null;
		N=0;
		if(values.size()==0) return;


		//Calculate quality
		Quality res = null;
		for (ResultValue r : values) {
			Quality q = r.getResult().getQuality();
			if(q!=null) {
				if(res==null || res.getId()>q.getId()) {
					res = q;
				}
			}
		}
		quality = res;

		
		//Calculate number of non null values		
		for (ResultValue value: values) {
			if(value.getValue()!=null) {
				N++;
			}
		}
		
		if(tpl.getAggregation()==Aggregation.HIDE) {
			//Count of all results, empty or non empty
			aggregated = "";
		} else if(tpl.getAggregation()==Aggregation.COUNT) {
			//Count of all results, empty or non empty
			aggregated = values.size();			
		} else if(tpl.getAggregation()==Aggregation.ALL_VALUES) {
			//All Values, separated by "; "
			Collections.sort(values);
			StringBuilder sb = new StringBuilder();			
			for (ResultValue value: values) {
				if(sb.length()>0) sb.append("; ");
				sb.append(value.getValue()==null?"": value.getValue());
			}
			aggregated = sb.toString();
		} else if(values.size()==1) {
			//One value-> don't compute but display
			aggregated = values.get(0).getDoubleValue()==null? values.get(0).getValue(): values.get(0).getDoubleValue();
		} else {	
			
			//Calculate Display1, Display2
			boolean hasNonDoubles = false;
			List<Double> doubles = new ArrayList<Double>();
			List<String> texts = new ArrayList<String>();
			for (ResultValue value: values) {
				if(value.getAttribute().getDataType()==DataType.NUMBER) {
					Double v = value.getDoubleValue();
					if(v!=null) doubles.add(v);
					else if(value.getValue()!=null) hasNonDoubles = true;					
				} else {
					String t = value.getValue();
					if(t!=null) texts.add(t);
				}
			}
			
			if(texts.size()>0) {
				if(doubles.size()>0) {
					//If we have a mix of doubles and texts, there is nothing to display
					aggregated = "?";
				} else {
					//If we have texts, display it, if all values are the same
					boolean allEquals = true;
					for (int i = 1; i < texts.size(); i++) {
						if(!texts.get(i).equals(texts.get(0))) {
							allEquals = false; break;
						}
					}
					if(allEquals) {
						aggregated = texts.get(0);
					} else {
						aggregated = "?";
					}
					
				}
			} else if(doubles.size()>0) {
				if(tpl.getAggregation()==Aggregation.AVERAGE) {
					aggregated = getAverage(doubles);
				} else if(tpl.getAggregation()==Aggregation.MEDIAN) {
					aggregated = getMedian(doubles);
				} else if(tpl.getAggregation()==Aggregation.GEOMETRIC_MEAN) {
					aggregated = getGeometricMean(doubles);
				} else if(tpl.getAggregation()==Aggregation.MINIMUM) {
					aggregated = getMin(doubles);
				} else if(tpl.getAggregation()==Aggregation.MAXIMUM) {
					aggregated = getMax(doubles);
				} else if(hasNonDoubles) {
					aggregated = "?";
				} else {
					aggregated = "??";
				}
				

				Double s  = getStd(doubles, getAverage(doubles));
				if(s==null) {
					std = null;
					coeff = null;
				} else {
					int c = (int) (s / getAverage(doubles) * 100); 
					coeff = c;
					if(tpl.getDeviation()==Deviation.COEFF_VAR) {
						std = (double) c;
					} else if(tpl.getDeviation()==Deviation.STD) {
						std = s;					
					}
				}
				
			} else {
				aggregated = "";
			}
			
		}
		//Calculate ComputedValue
		if(tpl.getComputed()!=null) {
			List<Double> doubles = new ArrayList<Double>();
			for (ResultValue value: values) {
				Double v = value.getCalculatedValue();
				if(v!=null) doubles.add(v);
			}

			calculatedValue = getAverage(doubles);
		} else {
			calculatedValue = null;
		}

	}
	
	public Comparable<?> getValue() {
		calculateStats();
		return aggregated;
	}
	
	public Double getStd(){
		calculateStats();
		return std;		
	}
	public Integer getCoeffVariation() {
		calculateStats();
		return coeff;		
	}
	
	public Double getComputed() {
		calculateStats();
		return calculatedValue;
	}
	
	private static PivotCellKey getCellKey(PivotTemplate template, ResultValue rv) {
		StringBuilder sb = new StringBuilder();
		Result r = rv.getResult();
		if(r==null) return new PivotCellKey(null, "");
		
		
		Phase phase = null;
		for (PivotItem item : template.getPivotItems(Where.ASCELL)) {
			if(phase==null && item.getClassifier()==PivotItemClassifier.STUDY_PHASE) {
				phase = rv.getResult().getInheritedPhase(); 
			}
						
			//Append the item's title
			String title = item.getTitle(rv);			
			if(title!=null) {
				if(title.length()>=3 && title.charAt(0)=='<' && title.charAt(2)=='>') {
					title = title.substring(3);
				}
				if(title!=null && title.length()>0) {
					if(sb.length()>0) sb.append("; ");
					sb.append(title);
				}
			}
		}
		
				
		return new PivotCellKey(phase, sb.toString());
			 
	}
	
//	public String getNestedKey(int subRow) {
//		calculateSplit();
//		return subRow>=0 && subRow<orderedPivotCellKeys.size()? orderedPivotCellKeys.get(subRow).getKey(): null;		
//	}
//	
//	public PivotCell getNestedPivotCell(int subRow) {
//		calculateSplit();
//		return subRow>=0 && subRow<orderedPivotCellKeys.size()? pivotCellMap.get(orderedPivotCellKeys.get(subRow)): null;		
//	}
	
	public List<PivotCellKey> getNestedKeys() {
		calculateNested();
		return orderedNestedKeys;
	}
	
	public PivotCell getNested(PivotCellKey key) {
		calculateNested();
		PivotCell pc = nestedMap.get(key);
		if(pc!=null) return pc;
		return new PivotCell(table);
	}
		
	/**
	 * Split the cell based on the Template.Cell parameters.
	 * For each ResultValue included in this cell, we calculate the key and create a Map<key, new sub PivotCell>
	 */
	private void calculateNested() {
		if(nestedMap==null) {
			nestedMap = new TreeMap<PivotCellKey, PivotCell>();
			
			for (int i = 0; i < values.size(); i++) {
				ResultValue v = values.get(i);
				PivotCellKey key = getCellKey(table.getTemplate(), v);
				PivotCell values = nestedMap.get(key);
				if(values==null) {
					values = new PivotCell(table);
					nestedMap.put(key, values);
				}
				values.values.add(v);			
			}
			
			orderedNestedKeys.clear();
			orderedNestedKeys.addAll(nestedMap.keySet());
		}
	}
	
	@Override
	public String toString() {
		calculateStats();
		boolean hasKeys = table.getTemplate().getPivotItems(Where.ASCELL).size()>0;
		StringBuilder sb = new StringBuilder();
		for(PivotCellKey key: getNestedKeys()) {
			String s = key.getKey();
//			PivotCell c = getPivotCell(key);
			if(sb.length()>0) sb.append("\r\n");
			if(hasKeys && table.getTemplate().getAggregation()==Aggregation.COUNT && (aggregated instanceof Integer) && 1==Integer.valueOf((Integer)aggregated)) {
				sb.append(s);
			} else { 
				sb.append((s.length()==0?"":s+": ") + (aggregated==null? "": aggregated.toString()));
			}
		}
		return sb.toString();

//		return getValuesAsString() + " " +getComments() + (display1!=null? " ("+display1+")":"");
	}
	
//	public String getValuesAsString() {
//		StringBuilder sb = new StringBuilder();		
//		for (int i = 0; i < values.size(); i++) {
//			if(i>0) sb.append("; ");
//			sb.append(values.get(i)==null? "": values.get(i));			
//		}
//		return sb.toString().trim();
//	}
	
	@Override
	public int compareTo(PivotCell o) {
		if(o==null) return -1;
		return CompareUtils.compare(getValue(), o.getValue());
	}

	
	/**
	 * Returns results associated with this cell in the appropriate order (according to split)
	 * @return
	 */
	public List<Result> getResults() {
		calculateNested();
		calculateStats();
		List<Result> results = new ArrayList<Result>();
		for(PivotCell c2: nestedMap.values()) {
			for (ResultValue value : c2.values) {
				results.add(value.getResult());
			}
		}
		
		return results;
	}
	
	public int getN() {
		return N;
	}
	
	private Quality quality = null;
	
	public Quality getQuality() {
		return quality;
	}
	
	public PivotDataTable getTable() {
		return table;
	}
	
	private static final Double getMedian(List<Double> doubles) {
		if(doubles.size()==0) return null;
		Collections.sort(doubles);
		Double median;
		if(doubles.size()%2==0) {
			median = (doubles.get(doubles.size()/2-1) + doubles.get(doubles.size()/2)) / 2;  
		} else {
			median = doubles.get(doubles.size()/2);
		}
		return median;
	}
	
	private static final Double getMin(List<Double> doubles) {
		if(doubles.size()==0) return null;
		double min = Double.MAX_VALUE;
		for (Double d : doubles) {
			if(d<min) min = d;
		}
		return min;
	}
	private static final Double getMax(List<Double> doubles) {
		if(doubles.size()==0) return null;
		double max = -Double.MAX_VALUE;
		for (Double d : doubles) {
			if(d>max) max = d;
		}
		return max;
	}
	
	private static final Double getAverage(List<Double> doubles) {
		if(doubles.size()==0) return null;
		double avg = 0;
		for (Double d : doubles) {
			avg += d;
		}
		avg/=doubles.size();
		return avg;
	}
	private static final Double getGeometricMean(List<Double> doubles) {
		if(doubles.size()==0) return null;
		double product = 1;
		boolean hasNeg = false;
		for (Double d : doubles) {
			product *=d ;
			if(d<0) hasNeg = true;
		}
		if(hasNeg) {
			return Double.NaN;
		} else {
			return Math.pow(product, 1.0/doubles.size());				
		}
	}
	
	private static Double getStd(List<Double> doubles, Double average) {
		Double stdAverage;
		
		if(average==null || doubles.size()<=1) {
			stdAverage = null;
		} else {
			double s = 0;
			for (Double d : doubles) s += (d-average) * (d-average);
			s = Math.sqrt(s / (doubles.size()-1));
			stdAverage = s;
		}
		return stdAverage;
	}
	
	public Margins getMargins() {
		return margin;
	}
	public void setMargin(Margins margin) {
		this.margin = margin;
	}
	
	public void addValue(ResultValue value) {
		values.add(value);
	}
	
}
		