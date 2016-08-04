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

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import com.actelion.research.util.CompareUtils;

/**
 * The BiosampleLinker is used to identify a field in the biosample hierary.
 * 
 * 				CellLine (parentBiotype)
 * 				/ \   \-Name (type=name)
 * 				 |
 * 				 Cell   <> ------ Plasmid1 (aggregatedMetadata)
 *                                     \-Name (type=name)
 *                                     
 *                                     
 * the linker can either be
 * - to itself
 * - to a parent (parentBiotype!=null)
 * - to an aggregated biosample (aggregatedMetadata!=null)
 * 
 * we cannot combine parent (vertical links) and aggregated (horizontal links) to limit complexity
 * 
 * @author freyssj
 *
 */
public class BiosampleLinker implements Comparable<BiosampleLinker> {
	
	public static enum LinkerType {
		SAMPLEID, 
		SAMPLENAME, 
		METADATA, 
		COMMENTS, 
	}
	
	private String label;
	private final LinkerType type;
	private final Biotype hierarchyBiotype;
	private final BiotypeMetadata aggregatedMetadata;
	
	/** Cannot be null if type=metadata*/
	private final BiotypeMetadata biotypeMetadata;
	
	/** Only used for the label (for amount and name), not for the actual linking*/
	private final Biotype biotypeForLabel;
	

	public enum LinkerMethod {
		/** No container, no sampleId, no links */
		DIRECT_LINKS, 
		/** No container, no sampleId, include links */
		ALL_LINKS,
		/** No container, no sampleId, include links */
		INDIRECT_LINKS;
				
	}

	public static Set<BiosampleLinker> getLinkers(Collection<Biosample> biosamples, LinkerMethod method) {
		Set<BiosampleLinker> res = new TreeSet<>();
		for (Biosample b : biosamples) {
			res.addAll(getLinkers(b, method));
		}
		return res;
	}
	

	/**
	 * Gets all available linkers
	 * @param biosample
	 * @param checkLinks
	 * @return
	 */
	public static Set<BiosampleLinker> getLinkers(Biosample biosample, LinkerMethod method) {
		Set<BiosampleLinker> res = new TreeSet<>();
		if(biosample==null || biosample.getBiotype()==null) return res; 
		
		boolean followLinks = method==LinkerMethod.ALL_LINKS || method==LinkerMethod.INDIRECT_LINKS;

		if(followLinks) {
			//Create aggregated links from this biosample
			for(Metadata m: biosample.getMetadataMap().values()) {
				Biosample b = m.getLinkedBiosample();
				if(b!=null) {
					res.add(new BiosampleLinker(m.getBiotypeMetadata(), LinkerType.SAMPLEID, m.getLinkedBiosample().getBiotype()));
					
					if(b.getBiotype().getSampleNameLabel()!=null && (b.getSampleName()!=null  && b.getSampleName().length()>0) && !b.getBiotype().isHideSampleId()) {
						res.add(new BiosampleLinker(m.getBiotypeMetadata(), LinkerType.SAMPLENAME, b.getBiotype()));
					}
					for(Metadata m2: b.getMetadataMap().values()) {
						if(m2.getValue()!=null && m2.getValue().length()>0) {
							res.add(new BiosampleLinker(m.getBiotypeMetadata(), m2.getBiotypeMetadata()));
						}
					}
					if(b.getComments()!=null && b.getComments().length()>0) {
						res.add(new BiosampleLinker(m.getBiotypeMetadata(), LinkerType.COMMENTS, b.getBiotype()));
					}

				}
			}
			
			//Create hierarchical links from this biosample
			Biosample b2 = biosample.getParent();
			int count = 0;
			while(b2!=null && count++<5) {
				if(!b2.getBiotype().equals(biosample.getBiotype())) {
					if(!b2.getBiotype().isHideSampleId()) { 
						res.add(new BiosampleLinker(b2.getBiotype(), LinkerType.SAMPLEID));
					}
					if(b2.getBiotype().getSampleNameLabel()!=null && b2.getSampleName()!=null && b2.getSampleName().length()>0) {
						res.add(new BiosampleLinker(b2.getBiotype(), LinkerType.SAMPLENAME));
					}
					for(Metadata m2: b2.getMetadataMap().values()) {
						if(m2.getValue()!=null && m2.getValue().length()>0) {
							res.add(new BiosampleLinker(b2.getBiotype(), m2.getBiotypeMetadata()));
						}
					}
					if(b2.getComments()!=null && b2.getComments().length()>0) {
						res.add(new BiosampleLinker(b2.getBiotype(), LinkerType.COMMENTS));
					}
				}

				b2 = b2.getParent();
			}
			
		}
		
		if(method==LinkerMethod.ALL_LINKS || method==LinkerMethod.DIRECT_LINKS) {
			//Create direct links from this biosample
			if(followLinks) {
				res.add(new BiosampleLinker(biosample.getBiotype(), LinkerType.SAMPLEID));
			} else {
				res.add(new BiosampleLinker(LinkerType.SAMPLEID, biosample.getBiotype()));
			}
			
			if(biosample.getBiotype().getSampleNameLabel()!=null && biosample.getSampleName()!=null) {
				if(followLinks) {
					res.add(new BiosampleLinker(biosample.getBiotype(), LinkerType.SAMPLENAME));
				} else {
					res.add(new BiosampleLinker(LinkerType.SAMPLENAME, biosample.getBiotype()));				
				}
			}
			//Retrieve metadata in the appropriate order
			for(BiotypeMetadata bm: biosample.getBiotype().getMetadata()) {
				Metadata m = biosample.getMetadata(bm);
				if(m==null) continue;
				
				if(m.getValue()!=null && m.getValue().length()>0) {
					if(followLinks) {
						if(m.getLinkedBiosample()==null) {
							res.add(new BiosampleLinker(biosample.getBiotype(), m.getBiotypeMetadata()));
						}
					} else {
						res.add(new BiosampleLinker(m.getBiotypeMetadata()));
					}
				}
			}
			if(biosample.getComments()!=null) {
				if(followLinks) {
					res.add(new BiosampleLinker(biosample.getBiotype(), LinkerType.COMMENTS));
				} else {
					res.add(new BiosampleLinker(LinkerType.COMMENTS, biosample.getBiotype()));
				}
			}
		}
		
		return res;
	}

	
	
	/**
	 * Create a linker to an aggregated data (ex cell->plasmid.metadata)
	 */
	public BiosampleLinker(BiotypeMetadata aggregatedMetadata, BiotypeMetadata biotypeMetadata) {
		if(aggregatedMetadata==null) throw new IllegalArgumentException("The aggregatedMetadata cannot be null");
		if(biotypeMetadata==null) throw new IllegalArgumentException("The biotypeMetadata cannot be null");
		this.hierarchyBiotype = null;
		this.aggregatedMetadata = aggregatedMetadata;
		this.type = LinkerType.METADATA;
		this.biotypeMetadata = biotypeMetadata;
		this.biotypeForLabel = biotypeMetadata.getBiotype();
	}
	
	
	/**
	 * Create a linker to an aggregated data (ex cell->plasmid.comments)
	 */
	public BiosampleLinker(BiotypeMetadata aggregatedMetadata, LinkerType type) {
		this(aggregatedMetadata, type, null);
	}

	/**
	 * Create a linker to an aggregated data (ex cell->plasmid.comments)
	 */
	public BiosampleLinker(BiotypeMetadata aggregatedMetadata, LinkerType type, Biotype typeForLabel) {
		if(aggregatedMetadata==null) throw new IllegalArgumentException("aggregatedMetadata cannot be null");
		this.hierarchyBiotype = null;
		this.aggregatedMetadata = aggregatedMetadata;
		this.type = type;
		this.biotypeMetadata = null;
		this.biotypeForLabel = typeForLabel;
	}


	/**
	 * Create a linker to an parent data (ex cell->cell line.metadata)
	 */
	public BiosampleLinker(Biotype hierarchyBiotype, BiotypeMetadata biotypeMetadata) {
		if(hierarchyBiotype==null) throw new IllegalArgumentException("hierarchyBiotype cannot be null");	
		if(biotypeMetadata==null) throw new IllegalArgumentException("biotypeMetadata cannot be null");	
		this.hierarchyBiotype = hierarchyBiotype;
		this.aggregatedMetadata = null;
		this.type = LinkerType.METADATA;
		this.biotypeMetadata = biotypeMetadata;
		this.biotypeForLabel = hierarchyBiotype;
	}
	
	/**
	 * Create a linker to an parent data (ex cell->cell line.comments)
	 */
	public BiosampleLinker(Biotype hierarchyBiotype, LinkerType type) {
		if(type==LinkerType.METADATA || type==null) throw new IllegalArgumentException("The type cannot be METADATA or null");
		this.hierarchyBiotype = hierarchyBiotype;
		this.aggregatedMetadata = null;
		this.type = type;
		this.biotypeMetadata = null;
		this.biotypeForLabel = hierarchyBiotype;
	}

	/**
	 * Create a linker to a non metadata attribute (ex cell.comments)
	 * The biotypeForLabel should be given to display the header correctly.
	 */	
	public BiosampleLinker(LinkerType type, Biotype biotypeForLabel) {
		this.hierarchyBiotype = null;
		this.aggregatedMetadata = null;
		this.type = type;
		this.biotypeMetadata = null;
		this.biotypeForLabel = biotypeForLabel; 
	}
	/**
	 * Create a linker to a metadata attribute (ex cell.metadata)
	 */	
	public BiosampleLinker(BiotypeMetadata biotypeMetadata) {
		this.hierarchyBiotype = null;
		this.aggregatedMetadata = null;
		this.type = LinkerType.METADATA;
		this.biotypeMetadata = biotypeMetadata;
		this.biotypeForLabel = biotypeMetadata.getBiotype(); 
	}
	
	public boolean isLinked() {
		return hierarchyBiotype != null || aggregatedMetadata != null;
	}
	
	public Biotype getHierarchyBiotype() {
		return hierarchyBiotype;
	}
	
	public LinkerType getType() {
		return type;
	}
	
	public BiotypeMetadata getBiotypeMetadata() {
		return biotypeMetadata;
	}
	public Biotype getBiotypeForLabel() {
		return biotypeForLabel;
	}
	/**
	 * Returns the linked biosample
	 * @param biosample
	 * @return
	 */
	public Biosample getLinked(Biosample biosample) {
		if(biosample==null) return null;
		if(!isLinked()) return biosample;
		
		Biosample b2 = biosample;
		
		if(aggregatedMetadata!=null) {
			//Aggregation
			if(b2.getBiotype().equals(aggregatedMetadata.getBiotype()) && b2.getMetadata(aggregatedMetadata)!=null) {
				return b2.getMetadata(aggregatedMetadata).getLinkedBiosample();
			}
		} else if(hierarchyBiotype!=null) {
			//Parent
			int count = 0;
			while(b2!=null && count++<5) {
				if(hierarchyBiotype.equals(b2.getBiotype())) {
					return b2;
				}
				b2 = b2.getParent();
			}
		}
		return null;
	}
	public BiotypeMetadata getAggregatedMetadata() {
		return aggregatedMetadata;
	}
	
	public String getValue(Biosample b) {
		b = getLinked(b);		
		if(b==null) return null;

		switch(type) {
		case SAMPLEID: return b.getSampleId();
		case SAMPLENAME: return b.getSampleName();
		case METADATA: return !b.getBiotype().equals(biotypeMetadata.getBiotype()) || b.getMetadata(biotypeMetadata)==null? null: b.getMetadata(biotypeMetadata).getValue();
		case COMMENTS: return b.getComments();
		default: return "??"+type;
		}
	}

	public boolean setValue(Biosample b, String value) {
		b = getLinked(b);		
		if(b==null) return false;

		switch(type) {
		case SAMPLEID: 
			b.setSampleId(value);
			return true;
		case SAMPLENAME:
			b.setSampleName(value);
			return true;
		case METADATA:
			if(!b.getBiotype().equals(biotypeMetadata.getBiotype()) || b.getMetadata(biotypeMetadata)==null) return false;
			b.setMetadata(biotypeMetadata, value);
			return true;
		case COMMENTS:
			b.setComments(value);
			return true;
		default: return false;
		}
	}

	@Override
	public boolean equals(Object o) {
		if(o==this) return true;
		if(!(o instanceof BiosampleLinker)) return false;
		return getLabel().equals(((BiosampleLinker)o).getLabel());
	}
	
	/**
	 * The order should be
	 * parentType1, parentType2, self, selfName, selfMetadata, selfToAgregratedMetadata, selfComments, selfContainer
	 */
	@Override
	public int compareTo(BiosampleLinker l) {
		//linker with HierarchyBiotype are first
		if(getHierarchyBiotype()!=null && l.getHierarchyBiotype()!=null) {
			int c = CompareUtils.compare(getHierarchyBiotype(), l.getHierarchyBiotype());
			if(c!=0) return c;
		} else if(getHierarchyBiotype()!=null && l.getHierarchyBiotype()==null) {
			return -1;
		} else if(getHierarchyBiotype()==null && l.getHierarchyBiotype()!=null) {
			return 1;			
		}
		
		//Compare the next link
		BiotypeMetadata toCompare1 = getAggregatedMetadata()!=null? getAggregatedMetadata(): getBiotypeMetadata(); 
		BiotypeMetadata toCompare2 = l.getAggregatedMetadata()!=null? l.getAggregatedMetadata(): l.getBiotypeMetadata();
		if(toCompare1!=null && toCompare2!=null) {
			int c = (int)(toCompare1.getIndex() - toCompare2.getIndex());
			if(c!=0) return c;			
		}
		
		//Compare type / and second type if needed
		if(getType()==LinkerType.METADATA && l.getType()==LinkerType.METADATA) {
			BiotypeMetadata toCompare1_2 = getAggregatedMetadata()!=null? getBiotypeMetadata(): null; 
			BiotypeMetadata toCompare2_2 = l.getAggregatedMetadata()!=null? l.getBiotypeMetadata(): null;
			return CompareUtils.compare(toCompare1_2, toCompare2_2);
		} else {
			return getType().compareTo(l.getType());
		}
		
	}

	
	/**
	 * Returns the label (one line), used in selectors
	 */
	@Override
	public String toString() {
		return getLabel().replace("\n", ".");
	}
	
	@Override
	public int hashCode() {
		return getLabel().hashCode();
	}
	
	private static String getLabel(LinkerType type, Biotype biotypeForLabel) {
		switch(type) {
		case SAMPLEID: return  "SampleId";
		case SAMPLENAME: return  biotypeForLabel==null? "SampleName" :biotypeForLabel.getSampleNameLabel();
		case COMMENTS: return "Comments";
		default: return "??"+type;
		}
	}
	
	
	/**
	 * Returns the label: biotypeOrMetadata\nMetadataOrType[.MetadataOrType]
	 * @return
	 */
	public String getLabel() {
		if(label==null) {			
			if(aggregatedMetadata!=null) {
				if(biotypeMetadata!=null) {
					label = aggregatedMetadata.getBiotype().getName() + "\n" + aggregatedMetadata.getName() + "." + biotypeMetadata.getName();
				} else if(type==LinkerType.SAMPLEID) {
					//Careful, make sure that the label is the same as in the other case (or the linker equality will fail)
					label = aggregatedMetadata.getBiotype().getName() + "\n" + aggregatedMetadata.getName();
				} else {
					label = aggregatedMetadata.getBiotype().getName() + "\n" + aggregatedMetadata.getName() + "." + getLabel(type, biotypeForLabel);			
				}
	
			} else if(hierarchyBiotype!=null) {
				if(biotypeMetadata!=null) {
					label = hierarchyBiotype.getName()+ "\n" + biotypeMetadata.getName();
				} else { 
					label = hierarchyBiotype.getName()+ "\n" + getLabel(type, hierarchyBiotype);
				}
			} else if(biotypeForLabel!=null) {
				if(biotypeMetadata!=null) {
					label = biotypeForLabel.getName()+ "\n" + biotypeMetadata.getName();
				} else { 
					label = biotypeForLabel.getName()+ "\n" + getLabel(type, biotypeForLabel);
				}
			} else {
				if(biotypeMetadata!=null) {
					label = biotypeMetadata.getName();
				} else { 
					label = getLabel(type, null);
				}
				
			}		
		}
		return label;
	}
	public String getLabelShort() {
		switch(type) {
		case SAMPLEID: return  "SampleId";
		case SAMPLENAME: return  biotypeForLabel==null? "SampleName" :biotypeForLabel.getSampleNameLabel();
		case COMMENTS: return "Comments";
		default:
			if(biotypeMetadata!=null) return biotypeMetadata.getName();
			return "??";
		}
	}
	
}
