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

package com.actelion.research.spiritapp;

import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.slf4j.LoggerFactory;

import com.actelion.research.spiritapp.ui.SpiritFrame;
import com.actelion.research.spiritapp.ui.SpiritTab;
import com.actelion.research.spiritapp.ui.admin.database.DatabaseSettingsDlg;
import com.actelion.research.spiritapp.ui.biosample.BiosampleTab;
import com.actelion.research.spiritapp.ui.home.HomeTab;
import com.actelion.research.spiritapp.ui.location.LocationTab;
import com.actelion.research.spiritapp.ui.result.ResultTab;
import com.actelion.research.spiritapp.ui.study.StudyTab;
import com.actelion.research.spiritapp.ui.util.LoginDlg;
import com.actelion.research.spiritapp.ui.util.SpiritAction;
import com.actelion.research.spiritapp.ui.util.component.JHeaderLabel;
import com.actelion.research.spiritcore.business.IAuditable;
import com.actelion.research.spiritcore.business.audit.DifferenceItem;
import com.actelion.research.spiritcore.business.audit.DifferenceList;
import com.actelion.research.spiritcore.business.property.PropertyKey;
import com.actelion.research.spiritcore.business.study.Study;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.DAOLocation;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAOStudy;
import com.actelion.research.spiritcore.services.dao.DAOTest;
import com.actelion.research.spiritcore.services.dao.JPAUtil;
import com.actelion.research.spiritcore.services.dao.SpiritProperties;
import com.actelion.research.spiritcore.services.migration.MigrationScript.FatalException;
import com.actelion.research.spiritcore.util.Pair;
import com.actelion.research.util.ArgumentParser;
import com.actelion.research.util.ui.JCustomTextField;
import com.actelion.research.util.ui.JEscapeDialog;
import com.actelion.research.util.ui.JExceptionDialog;
import com.actelion.research.util.ui.SplashScreen;
import com.actelion.research.util.ui.SwingWorkerExtended;
import com.actelion.research.util.ui.UIUtils;

/**
 * Spirit Main application
 *
 * @author freyssj
 *
 */
public class Spirit extends SpiritFrame {



	public Spirit(Runnable afterLogin) {
		super("Spirit", "Spirit - (C) Joel Freyss - Idorsia Pharmaceuticals Ltd", afterLogin);
	}

	@Override
	public List<SpiritTab> getTabs() {
		List<SpiritTab> tabs = new ArrayList<>();
		tabs.add(new HomeTab(this));
		tabs.add(new StudyTab(this));
		tabs.add(new BiosampleTab(this));
		tabs.add(new LocationTab(this));
		if(SpiritProperties.getInstance().isChecked(PropertyKey.SYSTEM_RESULT)) {
			tabs.add(new ResultTab(this));
		}
		return tabs;
	}

	public static void preLoadDAO() throws Exception {
		JPAUtil.getManager();
		DAOEmployee.getEmployeeGroups();
		DAOBiotype.getBiotypes();
		DAOLocation.getLocationRoots(null);
		DAOTest.getTests();
	}

	public static SpiritUser askForAuthentication() throws Exception {
		if(SpiritFrame.getUser()==null) {
			LoginDlg.openLoginDialog(UIUtils.getMainFrame(), "Spirit Login");
			if(SpiritFrame.getUser()==null) throw new Exception("You must be logged in");
		}
		return SpiritFrame.getUser();
	}


	/**
	 * Special executor for Actelion, to put Spirit toFront with the proper settings
	 * @param args
	 * @throws Exception
	 */
	public static void initSingleApplication(final String[] args) {
		if(_instance==null) {
			try {
				main(args);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		} else {
			_instance.toFront();
		}


		//Process arguments
		final ArgumentParser argumentParser = new ArgumentParser(args);
		final String studyId = argumentParser.getArgument("studyId");
		if(studyId!=null) {
			new Thread(()-> {
				do {try{Thread.sleep(100);}catch (Exception e) {e.printStackTrace();}} while(_instance==null);
				LoggerFactory.getLogger(Spirit.class).info("Init with studyId=" + studyId);
				SwingUtilities.invokeLater(()-> {
					Study s = DAOStudy.getStudyByStudyId(studyId);
					_instance.setStudy(s);
				});
			}).start();
		}
	}

	public static void main(final String[] args) throws Exception {


		SplashScreen.show(splashConfig);

		final ArgumentParser argumentParser = new ArgumentParser(args);
		try {
			argumentParser.validate("studyId");
		} catch(Exception e) {
			System.out.println("Invalid syntax: Spirit -studyId {S-######}");
			System.exit(1);
		}


		new SwingWorkerExtended("Starting Spirit", null, SwingWorkerExtended.FLAG_ASYNCHRONOUS) {
			private Throwable throwable = null;

			@Override
			protected void doInBackground() {
				try {
					SpiritAction.logUsage("Spirit");
					JPAUtil.getManager();
				} catch(Throwable e) {
					throwable = e;
				}
			}

			@Override
			protected void done() {
				initUI();
				if(throwable!=null) {
					JExceptionDialog.showError(throwable);
					if(throwable instanceof FatalException) System.exit(1);
					new DatabaseSettingsDlg(false);
				}
				Spirit spirit;
				try {
					LoggerFactory.getLogger(Spirit.class).debug("start Spirit");
					spirit = new Spirit(()-> {
						initSingleApplication(args);
					});
					JOptionPane.setRootFrame(spirit);
				} catch(Throwable e) {
					JExceptionDialog.showError(e);
					System.exit(1);
				}
			}

		};
	}

	@Override
	public void paintComponents(Graphics g) {
		UIUtils.applyDesktopProperties(g);
		super.paintComponents(g);
	}


	/**
	 * Checks if the given entities were updated from an older value. If so ask for a mandatory reason.
	 * Returns false if the user canceled the process
	 *
	 * The function is ignored and returns true if the audit trail's reason is disabled
	 *
	 * @return
	 */
	public static boolean askReasonForChange() {
		return askReasonForChangeIfUpdated(null);
	}

	/**
	 * Checks if the given entities were updated from an older value. If so ask for a mandatory reason.
	 * Returns false if and only if the user canceled the process
	 *
	 * The function is ignored and returns true if the audit trail's reason is disabled
	 *
	 * @param entities
	 * @return
	 */
	public static boolean askReasonForChangeIfUpdated(Collection<? extends IAuditable> entities) {
		if(!SpiritProperties.getInstance().isChecked(PropertyKey.SYSTEM_ASKREASON)) return true;

		Set<String> changedFields = new TreeSet<>();
		boolean askForChange;
		if(entities==null) {
			askForChange = true;
			changedFields.add("");
		} else {
			askForChange = false;
			//Check if there was a difference

			for (IAuditable entity : entities) {

				//Load last saved revision and check differences

				Pair<IAuditable, DifferenceList> res = DAORevision.getLastChange(entity);
				DifferenceList dl = res.getSecond();
				if(dl!=null) {
					for (DifferenceItem e : dl) {
						if(e.getOldValue()!=null && e.getOldValue().length()>0) {
							changedFields.add(e.getField());
							askForChange = true;
						}
					}
				}
			}
		}

		if(askForChange) {

			List<JCustomTextField> changeTextFields = new ArrayList<>();
			List<JComponent> comps = new ArrayList<>();
			for (String field : changedFields) {
				JCustomTextField tf = new JCustomTextField();
				changeTextFields.add(tf);

				comps.add(new JLabel(field.length()==0? "":  field + ": "));
				comps.add(tf);
			}

			AtomicBoolean res = new AtomicBoolean(false);
			JEscapeDialog dlg = new JEscapeDialog(UIUtils.getMainFrame(), "Reason of change", true);
			JButton okButton = new JButton("Ok");
			okButton.addActionListener(e-> {
				Map<String, String> reasons = new HashMap<>();
				int i = 0;
				for (String field : changedFields) {
					JCustomTextField tf = changeTextFields.get(i++);
					if(tf.getText().trim().length()==0) {
						JExceptionDialog.showError("The reason is required");
						return;
					} else if(tf.getText().trim().length()>100) {
						JExceptionDialog.showError("The reason cannot be longer than 100 characters");
						return;
					}

					reasons.put(field, tf.getText().trim());
				}
				res.set(true);
				JPAUtil.setReasonForChange(reasons);
				dlg.dispose();
			});
			dlg.setContentPane(UIUtils.createBox(UIUtils.createTitleBox("Changes", UIUtils.createTable(comps)),
					new JHeaderLabel("Please enter a reason of change for each updated field:"),
					UIUtils.createHorizontalBox(Box.createHorizontalGlue(), okButton)));
			UIUtils.adaptSize(dlg, -1, -1);
			dlg.setVisible(true);

			return res.get();
		}
		return true;
	}




}
