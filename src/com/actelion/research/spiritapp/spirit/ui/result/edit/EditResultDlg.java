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

package com.actelion.research.spiritapp.spirit.ui.result.edit;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.actelion.research.spiritapp.spirit.Spirit;
import com.actelion.research.spiritapp.spirit.ui.util.POIUtils;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeListener;
import com.actelion.research.spiritapp.spirit.ui.util.SpiritChangeType;
import com.actelion.research.spiritapp.spirit.ui.util.closabletab.JClosableTabbedPane;
import com.actelion.research.spiritapp.spirit.ui.util.component.JSpiritEscapeDialog;
import com.actelion.research.spiritapp.spirit.ui.util.correction.Correction;
import com.actelion.research.spiritapp.spirit.ui.util.correction.CorrectionDlg;
import com.actelion.research.spiritapp.spirit.ui.util.correction.CorrectionMap;
import com.actelion.research.spiritcore.business.DataType;
import com.actelion.research.spiritcore.business.Quality;
import com.actelion.research.spiritcore.business.ValidationException;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.result.Result;
import com.actelion.research.spiritcore.business.result.ResultQuery;
import com.actelion.research.spiritcore.business.result.ResultValue;
import com.actelion.research.spiritcore.business.result.Test;
import com.actelion.research.spiritcore.business.result.TestAttribute;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOResult;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.util.MiscUtils;
import com.actelion.research.util.ui.JCustomLabel;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;
import com.actelion.research.util.ui.iconbutton.JIconButton;
import com.actelion.research.util.ui.iconbutton.JIconButton.IconType;

public class EditResultDlg extends JSpiritEscapeDialog {

	private final JClosableTabbedPane tabbedPane = new JClosableTabbedPane();
	private final List<EditResultTab> resultsTabs = new ArrayList<EditResultTab>();
	
	private JTextField elbTextField = new JCustomTextField(JCustomTextField.ALPHANUMERIC, 15);
	
	private boolean newExperiment;
	private final boolean editWholeExperiment;
	
	
	/**
	 * Constructor used to edit results from a elb
	 * @param elb
	 */
	public EditResultDlg(List<Result> results) {
		super(UIUtils.getMainFrame(), "Edit Results", EditResultDlg.class.getName());
		editWholeExperiment = false;
		if(results.size()>40000) {
			JExceptionDialog.showError(this, "The maximum number of results allowed is 40000.");
			return;
		}
		
		//Reload results in the current session
		results = DAOResult.reload(results);
		
		Collections.sort(results);

		try {
			for(Result result: results) {
				if(!SpiritRights.canEdit(result, Spirit.getUser()))	{
					throw new Exception("You are not allowed to edit "+result);	
				}
			}
			newExperiment = false;
			initTabbedPane(results);
			
			elbTextField.setEnabled(false);
			init();
		} catch (Exception e) {
			JExceptionDialog.showError(e);
		}
	}
	
	
	public EditResultDlg(final String elb, final Result selectedResult) {
		super(UIUtils.getMainFrame(), "Edit Results - " + elb, EditResultDlg.class.getName());
		editWholeExperiment = true;
		
		new SwingWorkerExtended("Loading results", tabbedPane, SwingWorkerExtended.FLAG_ASYNCHRONOUS) {			
			List<Result> results;			
			@Override
			protected void doInBackground() throws Exception {
				ResultQuery query = ResultQuery.createQueryForElb(elb);
				results = DAOResult.queryResults(query, null);
				Collections.sort(results);
		
				newExperiment = false;
				if(results.size()==0) {
					throw new Exception("The ELB didn't contain any results");
				} else if(results.size()>40000) {
					throw new Exception("The ELB contains " + results.size() +" results. The maximum allowed is 40000.");
				}				
				newExperiment = false;
			}
			@Override
			protected void done() {
				try {
					results = DAOResult.reload(results);
					initTabbedPane(results);
					setSelection(selectedResult);
				} catch (Exception e) {
					JExceptionDialog.showError(e);
					dispose();
					return;
				}
				
				elbTextField.setEnabled(false);

			}
		};
		init();

	}
	
	/**
	 * Constuctor used when entering a new experiment 
	 * @param initialResults
	 * @param phase
	 */
	public EditResultDlg(boolean askForElb, List<Result> initialResults) {
		super(UIUtils.getMainFrame(), "Results - " + (askForElb?"New":"Edit"), EditResultDlg.class.getName());
		assert initialResults!=null;
		
		editWholeExperiment = true;
		List<Result> toDisplay = new ArrayList<Result>();
		if(askForElb) {
			//Suggest elb (Studyid)
			String suggestedElb = null;
			for (Result r : initialResults) {
				if(r.getElb()!=null) {
					suggestedElb = r.getElb();
					break;
				} else if(r.getBiosample()!=null && r.getBiosample().getInheritedStudy()!=null) {
					suggestedElb = r.getBiosample().getInheritedStudy().getStudyId();
					break;						
				}
			}
			//New experiment, ask for the elb (new or existing)
			EditResultSelectElbDlg dlg = new EditResultSelectElbDlg(suggestedElb);
			String elb = dlg.getReturnedValue();
			if(elb==null) return;
			try {
				toDisplay.addAll(DAOResult.queryResults(ResultQuery.createQueryForElb(elb), null));
				if(toDisplay.size()>0) {
					newExperiment = false;
					//This is an existing elb, check the rights
					for (Result result : toDisplay) {
						if(!SpiritRights.canEdit(result, Spirit.getUser())) {
							throw new Exception("You are not allowed to edit / append results to this elb");
						}
					}
					toDisplay.addAll(initialResults);
				} else {
					newExperiment = true;
					elbTextField.setText(elb);
				}
					
			} catch (Exception e) {
				JExceptionDialog.showError(e);
				return;
			}
			
			
			for (Result result : initialResults) {
				result.setElb(elb);
			}
		} else {
			toDisplay.addAll(initialResults);
			for (Result result : initialResults) {
				if(result.getId()>=0) newExperiment = false;
			}
		}
		
		try {
			initTabbedPane(toDisplay);
			setSelection(initialResults);
			init();
		} catch (Exception e) {
			JExceptionDialog.showError(e);
		}
		
		
	}
	
	public void setSelection(Result result) {
		for (int index = 0; index < resultsTabs.size(); index++) {
			EditResultTab tab = resultsTabs.get(index);
			boolean success = tab.setSelection(result);
			if(success) {
				tabbedPane.setSelectedIndex(index);
			}
		}		
	}
	
	public void setSelection(Collection<Result> results) {
		for (int index = 0; index < resultsTabs.size(); index++) {
			EditResultTab tab = resultsTabs.get(index);
			boolean success = tab.setSelection(results);
			if(success) {
				tabbedPane.setSelectedIndex(index);
			}
		}		
	}
	
	private void init() {
		//Build the layout
		JPanel topPanel = new JPanel(new GridBagLayout());
		if(editWholeExperiment) {
			GridBagConstraints c = new GridBagConstraints();
			c.anchor = GridBagConstraints.WEST;
			c.gridy = 0; c.gridx = 0; topPanel.add(new JCustomLabel("ELB: ", Font.BOLD), c);
			c.gridy = 0; c.gridx = 1; topPanel.add(elbTextField, c);
			elbTextField.setEnabled(false);
			c.gridy = 0; c.gridx = 2; c.weightx = 1; topPanel.add(new JLabel(), c);			
			topPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(2, 2, 2, 2)));
		}
		
		JButton deleteButton = new JIconButton(IconType.DELETE, "Delete experiment");
		deleteButton.setVisible(editWholeExperiment && !newExperiment);
		deleteButton.setDefaultCapable(false);
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				eventDelete();
			}
		});
		

		JButton excelButton = new JIconButton(IconType.EXCEL, "To Excel");
		excelButton.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					EditResultTab tab = resultsTabs.get(tabbedPane.getSelectedIndex());
					POIUtils.exportToExcel(tab.getTable().getTabDelimitedTable(), POIUtils.ExportMode.HEADERS_TOP);
					
				} catch (Exception ex) {
					JExceptionDialog.showError(EditResultDlg.this, ex);
				}
			}
		});
		
		
		/**
		 * Save Action
		 */
		JButton okButton = new JIconButton(IconType.SAVE, editWholeExperiment? (!newExperiment? "Update Experiment": "Save Experiment"): "Update Results");

		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final long start = System.currentTimeMillis();
				final SpiritUser user = Spirit.getUser();
				assert user!=null;
				
				new SwingWorkerExtended("Saving", getContentPane(), false) {
					private Exception exception = null;
					private List<Result> toSave; 
					@Override
					protected void doInBackground() throws Exception {
						try {
							toSave = validateResults();		
							if(toSave==null) return;
							
							if(editWholeExperiment) {
								DAOResult.persistExperiment(newExperiment, elbTextField.getText().trim(), toSave, user);
								
							} else {
								//Save the visible results
								DAOResult.persistResults(toSave, user);
							}
							
						} catch (Exception e) {
							e.printStackTrace();
							exception = e;
						}			
					}
					@Override
					protected void done() {
						
						if(exception==null) {
							
							if(toSave==null) return;
							try {
								
								JOptionPane.showMessageDialog(EditResultDlg.this, toSave.size() + " Results saved", "Success", JOptionPane.INFORMATION_MESSAGE);
								SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_ADDED, Result.class, toSave);
								dispose();
								System.out.println("EditResultDlg.eventOk12() "+(System.currentTimeMillis()-start)+"ms");							
							} catch(Exception e) {
								JExceptionDialog.showError(EditResultDlg.this, e);
							}
							
						} else if(exception instanceof ValidationException) {
							ValidationException e = (ValidationException) exception;
							String col = e.getCol();	
							Result result = (Result) e.getRow();
							int index = -1;
							for (int i = 0; index<0 && i < resultsTabs.size(); i++) {
								EditResultTab tab = resultsTabs.get(i);
								index = tab.getTable().getRows().indexOf(result);
							}
							if(index>=0 && index<tabbedPane.getTabCount()) {
								tabbedPane.setSelectedIndex(index);
								EditResultTable table = resultsTabs.get(index).getTable();
								table.setSelection(result, col);								
							} else {
								System.err.println("Could not select "+e.getCol()+" "+e.getRow()+ ">? " + index);
							}
							JExceptionDialog.showError(EditResultDlg.this, e);
						} else {
							JExceptionDialog.showError(EditResultDlg.this, exception);							
						}
						
						
					}
					
				};
				
				
			}
		});
		
		

		
		JPanel content = new JPanel(new BorderLayout());
		if(topPanel.getComponentCount()>1) {
			content.add(BorderLayout.NORTH, topPanel);
		}
		content.add(BorderLayout.CENTER, tabbedPane);
		content.add(BorderLayout.SOUTH, UIUtils.createHorizontalBox(deleteButton, excelButton, Box.createHorizontalGlue(), okButton));
		setContentPane(content);
		setSize(new Dimension(1000, 780));
		setLocationRelativeTo(UIUtils.getMainFrame());		
		setVisible(true);
	}
	
	/**
	 * Set results and update where elb/events could be selected (top or in table)
	 * Must be called before init()
	 * @param results
	 */
	private void initTabbedPane(List<Result> results) throws Exception {
		//Check rights
		List<Result> notAllowed = new ArrayList<Result>();
		List<Result> allowed = new ArrayList<Result>();
		for (Result result : results) {
			if(!SpiritRights.canEdit(result, Spirit.getUser())) {
				notAllowed.add(result);
			} else {
				allowed.add(result);
			}			
		}
		results = allowed;
		if(results.size()>0 && allowed.size()==0) {
			throw new Exception("You are not allowed to edit those results");			
		} else if(notAllowed.size()>0) {
			throw new Exception("Due to limited rights, You are not allowed to edit all those results");
		}
		
		resultsTabs.clear();
		
		
		String elb = "";
		elbTextField.setEnabled(false);
		if(editWholeExperiment) {
			//There should be max 1 elb
			Set<String> elbs = Result.getElbs(results);
			if(elbs.size()==1) {
				elb = elbs.iterator().next();
				elbTextField.setText(elb);
			} else if(elbs.size()>1) {
				throw new Exception("The results should not be linked to 2 elbs ("+elbs+")");
			}
		}
			
		//Center panel
		tabbedPane.setBorder(BorderFactory.createEtchedBorder());
		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				int sel = tabbedPane.getSelectedIndex();
				if(sel<0) return;
				String title = tabbedPane.getTitleAt(sel);
				if(title.equals("+") && sel>=0 && sel==tabbedPane.getTabCount()-1) {
					tabbedPane.removeTabAt(sel);
					
					//Create a new resultsPanel
					EditResultTab resultsTab = resultsTabs.size()>0? resultsTabs.get(resultsTabs.size()-1): null;
					EditResultTab newPanel = new EditResultTab(EditResultDlg.this);
					newPanel.getTestChoice().setSelection(resultsTab==null? null: resultsTab.getTestChoice().getSelection());					
					newPanel.setStudyId(resultsTab.getStudyId());
					resultsTabs.add(newPanel);
					newPanel.setResults(new ArrayList<Result>());
					tabbedPane.addTab("Select Test", newPanel);
					tabbedPane.setSelectedIndex(sel);
					
					//Update the tab
					newPanel.getTestChoice().reset();
					newPanel.getTestChoice().setTestName(null);

					if(editWholeExperiment) {
						tabbedPane.addTab("+", new JPanel());					
						tabbedPane.setSelectedIndex(sel);
					}
				}
			}
		});	
		
		
		

		
	

		addResults(results, false);
		
	}
	
	protected EditResultTab getCurrentTab() {
		int index = tabbedPane.getSelectedIndex();
		if(index<0 || index>=resultsTabs.size()) return null;
		return resultsTabs.get(index);
	}
	
	protected void addResults(List<Result> results, boolean emptyCurrentTab) {
		EditResultTab current = getCurrentTab();
		
		Map<EditResultTab, List<Result>> tab2results = new HashMap<EditResultTab, List<Result>>();
		//Add results
		
		

		Map<Test, List<Result>> mapTest = Result.mapTest(results);
		List<Test> tests = new ArrayList<>(mapTest.keySet());
		MiscUtils.removeNulls(tests);
		
		Collections.sort(tests);
		for (Test test : tests) {
			if(test==null) continue;
			Map<Study, List<Result>> map = Result.mapStudy(mapTest.get(test));
			List<Study> studies = new ArrayList<>(map.keySet());
			Collections.sort(studies);
			Collections.reverse(studies);
			for (Study study : studies) {
				EditResultTab tab = new EditResultTab(this);
				tab2results.put(tab, map.get(study));
				resultsTabs.add(tab);			
			}
		}

		if(emptyCurrentTab) {
			removeTab(current);
		}
		
		//Add an empty tab, if there are no results
		if(resultsTabs.size()==0) {
			EditResultTab tab = new EditResultTab(this);
			resultsTabs.add(tab);
			tab2results.put(tab, new ArrayList<Result>());			
		}
		
		//Update the components
		System.out.println("EditResultDlg.addResults(1) > "+resultsTabs.size()+" tabs");
		updateCenterPanel();
		

		//Set the results
		for(EditResultTab tab: tab2results.keySet()) {
			tab.setResults(tab2results.get(tab));
		}
				
		//Delete tabs if nresults =0
		for (EditResultTab resultTab : new ArrayList<EditResultTab>(resultsTabs)) {
			if(resultsTabs.size()<=1) break;
			boolean empty = true;
			for(Result result: resultTab.getTable().getRows()) {
				if(!result.isEmpty()) {empty = false; break;}
			}
			if(empty) {
				removeTab(resultTab);
			}
		}
		System.out.println("EditResultDlg.addResults(2) > "+resultsTabs.size()+" tabs");

		tabbedPane.setSelectedIndex(Math.max(0, tabbedPane.getTabCount()-2));
	}
	
	
	protected void removeTab(EditResultTab tab) {
		int index = resultsTabs.indexOf(tab);
		if(index<0) return;
		resultsTabs.remove(index);		
		updateCenterPanel();
	}
	
	private void updateCenterPanel() {
		for (int i = tabbedPane.getTabCount()-1; i >=0 ; i--) {
			
			tabbedPane.removeTabAt(i);
		}
		
		for (EditResultTab tab : resultsTabs) {
			tabbedPane.addTab("", tab);
			tab.resetTabName();
			
		}
		if(editWholeExperiment) {
			tabbedPane.addTab("+", new JPanel());
		}		
	}
	
	
	public void eventDelete() {
		
//		List<Result> results = new ArrayList<Result>();			
//		for (EditResultTab resultsPanel : resultsTabs) {
//			for (Result result : resultsPanel.getTable().getRows()) {
//				if(result.getId()>0) results.add(result);
//			}
//		}

		
		try {
			String elb = elbTextField.getText();

			//Reload the results
			final List<Result> results = DAOResult.queryResults(ResultQuery.createQueryForElb(elb), Spirit.getUser());
			
			//Check rights to be really sure
			for (Result result : results) {
				if(!SpiritRights.canDelete(result, Spirit.getUser())) {
					throw new Exception("You are not allowed to delete "+result);
				}
			}
			
			int res = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete " + elb + " (" + results.size() + " results in "+Result.getTests(results).size()+" tests) ?", "Delete Experiment", JOptionPane.YES_NO_OPTION);
			if(res!=JOptionPane.YES_OPTION) return;
			
			
			new SwingWorkerExtended("Delete "+elb, getContentPane()) {
				@Override
				protected void doInBackground() throws Exception {
					DAOResult.deleteResults(results, Spirit.getUser());
				}
				@Override
				protected void done() {
					SpiritChangeListener.fireModelChanged(SpiritChangeType.MODEL_DELETED, Result.class, results);
					JOptionPane.showMessageDialog(EditResultDlg.this, results.size() + " Results deleted", "Success", JOptionPane.INFORMATION_MESSAGE);
					dispose();
				}
			};
		} catch (Exception e) {
			JExceptionDialog.showError(this, e);

		}
	}
	
	/**
	 * 
	 * @param validateOnly
	 * @return List of result to be saved, null if canceled
	 * @throws Exception if cannot be validated
	 */
	public List<Result> validateResults() throws Exception {
		
		long start = System.currentTimeMillis();
		
		System.out.println("EditResultDlg.eventOk1() "+(System.currentTimeMillis()-start)+"ms");
		//
		//Synchronize the results with the elb, test, phases
		final List<Result> toSave = new ArrayList<Result>();
		List<Result> warnQualityResults = new ArrayList<Result>();
		final String elb = elbTextField.getText().trim();
		for (EditResultTab tab : resultsTabs) {
			Test test = tab.getTable().getModel().getTest();
			if(test==null) continue; //last tab
			for(Result result: tab.getTable().getRows()) {
				if(result.isEmpty()) continue;

				if(elb!=null && elb.length()>0) result.setElb(elb);
				
				System.out.println("EditResultDlg.validateResults() "+result);
				
				if(result.getBiosample()==null || result.getBiosample().getSampleId().length()==0) {
					throw new ValidationException("SampleId is required", result, "SampleId");
				}
				if(result.getBiosample()!=null && result.getBiosample().getQuality()!=null && result.getBiosample().getQuality().getId()<Quality.VALID.getId() && (result.getQuality()==null || result.getQuality().getId()>result.getBiosample().getQuality().getId())) {
					warnQualityResults.add(result);
				}
				if(result.getBiosample()!=null && result.getBiosample().getId()<=0 && result.getBiosample().getSampleId().length()>0) {
					Biosample b = DAOBiosample.getBiosample(result.getBiosample().getSampleId());
					if(b==null) throw new ValidationException(result.getBiosample().getSampleId() + " is not a valid sampleId", result, "SampleId");
					result.setBiosample(b);
				}
				
				
				toSave.add(result);		
			}
		}
		System.out.println("EditResultDlg.eventOk2() "+(System.currentTimeMillis()-start)+"ms");
		
	
		
		/*
		//Assign phases??
		List<PhaseMapping> phaseMappings = new ArrayList<PhaseMapping>();
		for (EditResultTab tab : resultsTabs) {
			Test test = tab.getTable().getModel().getTest();
			if(test==null) continue; //last tab
			resultLoop: for(Result result: tab.getTable().getRows()) {
				if(result.isEmpty()) continue;
				if(result.getPhase()!=null) continue;
				if(result.getBiosample()==null || result.getBiosample().getInheritedGroup()==null) continue;
						
				//Find the latest sampling for this animals (assuming that necropsy happened)
				Group g = result.getBiosample().getInheritedGroup();
				Phase p = result.getBiosample().getInheritedPhase();
				if(p==null) {
					//No phase associated to the biosample, try to get the latest sampling
					Study s = g.getStudy();
					for(Phase p2 : s.getPhases()) {
						StudyAction a = result.getBiosample().getStudyAction(p2);
						if(a==null) continue;
						if(a.getNamedSampling1()!=null || a.getNamedSampling2()!=null) {
							if(p!=null) {
								//Skip this result if there were more than 2 samplings
								continue resultLoop;
							}
							p = p2;									
						} 
					}
				}
				if(p!=null) phaseMappings.add(new PhaseMapping(result, p));					
			}
		}

		if(phaseMappings.size()>0) {
			StringBuilder sb = new StringBuilder();
			sb.append("<html><table>");
			int count = 0;
			for (PhaseMapping phaseMapping : phaseMappings) {
				Biosample b = phaseMapping.getResult().getBiosample();
				sb.append("<tr>"
						+ "<td>" 
						+ (b.getInheritedGroup()==null?"":b.getInheritedGroup().getName())
						+ "</td>"
						+ "<td><b>"
						+ b.getSampleId()
						+ "</b></td>"
						+ "<td>--></td>"
						+ "<td><b>"
						+ phaseMapping.getPhase().getName()
						+ "</b></td>"
						+ "</tr>"); 
				if(count++>100) {
					sb.append("<tr><td colsan=99>"+phaseMappings.size()+" More...</td></tr>");
					break;
				}
				
			}
			sb.append("</table></html>");
			System.out.println("EditResultDlg.eventOk3b() "+(System.currentTimeMillis()-start)+"ms");
			
			int res = JOptionPaneScrollpane.showConfirmDialog(this, 
					"The following results are not associated to a phase.\nDo you want to make those associations?", 
					sb.toString(), 
					"No Phase?", 
					JOptionPane.YES_NO_OPTION);
			if(res==JOptionPane.NO_OPTION) {
				//OK
			} else if(res==JOptionPane.YES_OPTION) {
				for (PhaseMapping phaseMapping : phaseMappings) {
					phaseMapping.getResult().setPhase(phaseMapping.getPhase());						
				}
				SwingUtilities.invokeLater(new Runnable() {					
					@Override
					public void run() {
						try {
							initTabbedPane(toSave);
						} catch(Exception e) {
							e.printStackTrace();
						}
					}
				});
				
			} else {
				return null;
			}			
		}
		System.out.println("EditResultDlg.eventOk4() "+(System.currentTimeMillis()-start)+"ms");
		*/

		//Check that all results linked to a biosample have a study
		for (Result result : toSave) {
			Study s = result.getBiosample()!=null && result.getBiosample().getInheritedStudy()!=null? result.getBiosample().getInheritedStudy(): null; 
			if(s!=null) {
				if(result.getPhase()!=null && result.getPhase().getStudy().getId()!=s.getId()) {
					throw new ValidationException("The phase for the result "+result+" should be on study "+result.getPhase().getStudy().getIvv(), result, "Phase");						
				}
			}
		}
		System.out.println("EditResultDlg.eventOk5() "+(System.currentTimeMillis()-start)+"ms");


//		//Check that all results are linked to an organ, fluid,  (except for Physiology tests)
//		for (Result result : toSave) {
//			if(result.getBiosample()!=null && result.getBiosample().getBiotype().getCategory()==BiotypeCategory.ANIMAL) {
//				if(result.getTest().getCategory()!=TestCategory.PHYSIOLOGY && result.getTest().getCategory()==TestCategory.INVIVO) {
//					int res = JOptionPane.showConfirmDialog(this, "The results should not be associated directly to an animal.\nYou should right-click on the sampleId column\n and convert it to the appropriate sample.\nDo you want to correct it?", "Result linked to Animal?", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
//					if(res!=JOptionPane.NO_OPTION) {
//						return null;
//					}						
//				}
//			}
//		}
		System.out.println("EditResultDlg.eventOk6() "+(System.currentTimeMillis()-start)+"ms");

		
		//Check the autocompletion fields for approximate spelling
		CorrectionMap<TestAttribute, Result> correctionMap = new CorrectionMap<TestAttribute, Result>();
		int obviousProblems = 0;
		for (Result result : toSave) {
			Test test = result.getTest();
			for (TestAttribute att : test.getAttributes()) {
				ResultValue rv = result.getResultValue(att);
				if(rv==null || rv.getValue()==null || rv.getValue().length()==0) continue;
				String value = rv.getValue();

				if(att.getDataType()==DataType.LIST) {
					//Choice
					
					Set<String> possibleValues = new TreeSet<String>(Arrays.asList(att.getParametersArray()) );
					if(!possibleValues.contains(value)) {
						Correction<TestAttribute, Result> correction = correctionMap.getCorrection(att, value);
						
						if(correction==null) {
							correction = correctionMap.addCorrection(att, value, new ArrayList<String>(possibleValues), true);
						}
						correction.getAffectedData().add(result);
						obviousProblems++;
					}
					
				} else if(att.getDataType()==DataType.AUTO) {
					//Autocompletion
					Set<String> possibleValues = DAOTest.getAutoCompletionFields(att);					
					if(!possibleValues.contains(value)) {
						Correction<TestAttribute, Result> correction = correctionMap.getCorrection(att, value);
						if(correction==null) {
							correction = correctionMap.addCorrection(att, value, new ArrayList<String>(possibleValues), false);
						}
						correction.getAffectedData().add(result);
						if(correction.getSuggestedValue()!=null) obviousProblems++;							
					}							
//				} else if(att.getDataType()==DataType.DICO) {
//					//Nomenclature
//					Set<NomenclatureTuple> set = NomenclatureClientImp.instanceOfDefault().searchForMatchingNomenclature(value, 10);
//					Set<String> possibleValues = new HashSet<String>();
//					boolean contains = false;
//					for (NomenclatureTuple tuple : set) {
//						if(value.equals(tuple.controlledTerm)) {contains = true; break;} 
//						possibleValues.add(tuple.controlledTerm);
//					}					
//					if(contains) continue;
//					
//					Correction<TestAttribute, Result> correction = correctionMap.getCorrection(att, value);
//					if(correction==null) correction = correctionMap.addCorrection(att, value, new ArrayList<String>(possibleValues), true);					
//					correction.getAffectedData().add(result);
//					if(correction.getSuggestedValue()!=null) obviousProblems++;							

				}
			}
		}
		System.out.println("EditResultDlg.eventOk7() "+(System.currentTimeMillis()-start)+"ms");

		//Display the correction dialog
		System.out.println("EditResultDlg.eventOk() number of problems="+obviousProblems+"/"+correctionMap.getItemsWithSuggestions());
		if(correctionMap.getItemsWithSuggestions()>0) {
			CorrectionDlg<TestAttribute, Result> dlg = new CorrectionDlg<TestAttribute, Result>(this, correctionMap) {
				@Override
				public String getSuperCategory(TestAttribute att) {
					return att.getTest().getFullName();
				}
				@Override
				protected String getName(TestAttribute att) {
					return att.getName();
				}
				@Override
				protected void performCorrection(Correction<TestAttribute, Result> correction, String newValue) {
					for (Result result : correction.getAffectedData()) {
						result.getResultValue(correction.getAttribute()).setValue(newValue);							
					}						
				}
			};
			if(dlg.getReturnCode()!=CorrectionDlg.OK) return null;
		}
		System.out.println("EditResultDlg.eventOk8() "+(System.currentTimeMillis()-start)+"ms");

		
		
		//Check unicity of results
		Map<String, List<Result>> input2Results = new HashMap<>();
		List<Result> duplicated = new ArrayList<>();
		for(Result r: toSave) {
			String key = r.getTest().getId()+"_"+(r.getPhase()==null?"":r.getPhase().getId())+"_"+(r.getBiosample()==null?"":r.getBiosample().getSampleId())+"_"+r.getInputResultValuesAsString();
			List<Result> list = input2Results.get(key);
			if(list==null) {
				list = new ArrayList<Result>();
				input2Results.put(key, list);
			} else {
				duplicated.add(r);
			}
			list.add(r);
		}
		StringBuilder sb = new StringBuilder();
		String s = null;
		for (List<Result> list  : input2Results.values()) {
			if(list.size()>1) {
				Result r = list.get(0);
				String ns = "<b><u>" + r.getTest().getFullName() + " " + (r.getPhase()==null?"": r.getPhase().getStudy().getIvv()+" - "+ r.getPhase().getLabel()) + "</u></b><br>";
				if(!ns.equals(s)) {
					s = ns;
					sb.append(s);
				}
				sb.append("<table border=0>");
				for (Result r2 : list) {
					sb.append("<tr>");
					sb.append("<td><b>" + (r.getBiosample()==null?"": " " + r.getBiosample().getSampleId()) + "</b></td>");
					sb.append("<td>" + r.getInputResultValuesAsString() + "</td>");
					sb.append("<td>" + r2.getOutputResultValuesAsString() + "</td>");
					sb.append("</tr>");						
				}
				sb.append("<tr></tr>");
				sb.append("</table>");
			}
		}
		if(sb.length()>0) {
			
			int res = showConfirmDialog(this, 
					"Some results are duplicated. What do you want to do?", 
					"<html>"+sb.toString()+"</html>", 
					"Duplicated results?", 
					new String[] {"Keep duplicates", "Keep one result", "Cancel"});
			if(res==0) {
				//OK
			} else if(res==1) {
				toSave.removeAll(duplicated);
				JExceptionDialog.showInfo(this, duplicated.size()+" duplicated results removed");
				initTabbedPane(toSave);
			} else {
				return null;
			}
		}
		System.out.println("EditResultDlg.eventOk9() "+(System.currentTimeMillis()-start)+"ms");

		
		if(warnQualityResults.size()>0) {
			int res = JOptionPane.showConfirmDialog(this, "Some of the results are linked to biosamples with questionable or bogus quality.\nWould you like to set the quality of those results to questionable or bogus?", "Quality of the biosamples?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
			if(res==JOptionPane.NO_OPTION) {
				//OK
			} else if(res==JOptionPane.YES_OPTION) {
				for (Result result : warnQualityResults) {
					result.setQuality(result.getBiosample().getQuality());
				}
			} else {
				return null;
			}
		}
		
		System.out.println("EditResultDlg.eventOk10() "+(System.currentTimeMillis()-start)+"ms");

		return toSave;
	}
	
	public static int showConfirmDialog(Component parent, String header, String longMessage, String title, String[] options) {
		JEditorPane textArea = new JEditorPane("text/html", longMessage);
		textArea.setEditable(false);
		textArea.setCaretPosition(0);
		textArea.setPreferredSize(new Dimension(400, 400));
		
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(BorderLayout.NORTH, new JCustomLabel(header, Font.BOLD));
		panel.add(BorderLayout.CENTER, new JScrollPane(textArea));

		int res = JOptionPane.showOptionDialog(parent, panel, title, 0, JOptionPane.QUESTION_MESSAGE, null, options, null);
		return res;
		
	}	

	
	@Override
	protected boolean mustAskForExit() {
		if(super.mustAskForExit()) return true;
		for (EditResultTab tab : resultsTabs) {
			if(tab.getTable().getUndoManager().hasChanges()) return true;
		}
		return false;
	}
	

	public boolean isEditExperimentMode() {
		return editWholeExperiment;
	}

	public JTabbedPane getTabbedPane() {
		return tabbedPane;
	}
	public List<EditResultTab> getResultsTabs() {
		return resultsTabs;
	}
	
	public static void main(String[] args) throws Exception {
		SpiritUser user = DAOSpiritUser.loadUser("freyssj");
		Spirit.setUser(user);
		//new EditResultDlg();
		
//		List<Result> results = new ArrayList<Result>();
//		Result result = new Result();
//		results.add(result);
		Spirit.initUI();
		
		new EditResultDlg("S-00357", null);
	}
}
