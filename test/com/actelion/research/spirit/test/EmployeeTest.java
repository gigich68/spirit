package com.actelion.research.spirit.test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.actelion.research.spiritcore.adapter.DBAdapter;
import com.actelion.research.spiritcore.business.audit.Revision;
import com.actelion.research.spiritcore.business.audit.RevisionQuery;
import com.actelion.research.spiritcore.business.biosample.Biosample;
import com.actelion.research.spiritcore.business.biosample.Biotype;
import com.actelion.research.spiritcore.business.biosample.BiotypeCategory;
import com.actelion.research.spiritcore.business.employee.Employee;
import com.actelion.research.spiritcore.business.employee.EmployeeGroup;
import com.actelion.research.spiritcore.services.SpiritRights;
import com.actelion.research.spiritcore.services.SpiritUser;
import com.actelion.research.spiritcore.services.dao.DAOBiosample;
import com.actelion.research.spiritcore.services.dao.DAOBiotype;
import com.actelion.research.spiritcore.services.dao.DAOEmployee;
import com.actelion.research.spiritcore.services.dao.DAORevision;
import com.actelion.research.spiritcore.services.dao.DAOSpiritUser;
import com.actelion.research.spiritcore.util.MiscUtils;

import junit.framework.AssertionFailedError;

public class EmployeeTest extends AbstractSpiritTest {

	private static EmployeeGroup eg0, eg1, eg2;
	private static Employee emp0, emp1a, emp1b, emp1c, emp2a;

	@BeforeClass
	public static void initSimpleTestData() throws Exception {

		//Create groups
		//     eg0
		//    /   \
		// eg1     eg2
		eg0 = new EmployeeGroup("G0");

		eg1 = new EmployeeGroup("G1");
		eg1.setParent(eg0);

		eg2 = new EmployeeGroup("G2");
		eg2.setParent(eg0);

		DAOEmployee.persistEmployeeGroups(MiscUtils.listOf(eg0, eg1, eg2), user);

		//Create employees
		//               eg0 (emp0)
		//              /   \
		//           eg1     eg2
		// (emp1a, emp1b, emp1c)    (emp2)
		emp0 = new Employee("U0");
		emp0.setEmployeeGroups(Collections.singleton(eg0));

		emp1a = new Employee("U1A");
		emp1a.setEmployeeGroups(Collections.singleton(eg1));

		emp1b = new Employee("U1B");
		emp1b.setEmployeeGroups(Collections.singleton(eg1));
		emp1b.setManager(emp1a);

		emp1c = new Employee("U1C");
		emp1c.setEmployeeGroups(Collections.singleton(eg1));
		emp1c.setManager(emp1a);

		emp2a = new Employee("U2A");
		emp2a.setEmployeeGroups(Collections.singleton(eg2));


		DAOEmployee.persistEmployees(MiscUtils.listOf(emp0, emp1a, emp1b, emp2a), user);


		//Create one biotype
		Biotype biotype = new Biotype("Test");
		biotype.setCategory(BiotypeCategory.PURIFIED);
		DAOBiotype.persistBiotype(biotype, user);
	}


	@Test
	public void testEmployees() throws Exception {
		Assert.assertTrue(DAOEmployee.getEmployees().size()>0);
		Assert.assertTrue(DAOEmployee.getEmployeeGroups().size()>0);

		EmployeeGroup eg = DAOEmployee.getEmployeeGroup("G1");
		Assert.assertTrue(eg!=null);
		Assert.assertTrue(eg.getParent()!=null);
		Assert.assertEquals("G0", eg.getParent().getName());

		//Check employee
		Employee emp = DAOEmployee.getEmployee("U1B");
		Assert.assertTrue(emp!=null);
		Assert.assertTrue(emp.getManager()!=null);
		Assert.assertEquals("U1A", emp.getManager().getUserName());
		Assert.assertTrue(emp.getEmployeeGroups().contains(eg1));

		emp = DAOEmployee.getEmployee("U1A");
		Assert.assertTrue(emp!=null);
		Assert.assertTrue(emp.getChildren().size()>0);
		Assert.assertTrue(new SpiritUser(emp).getManagedUsers().size()>0);
	}

	@Test
	public void testEmployeesCRUD() throws Exception {

		Employee emp3 = new Employee();
		emp3.setUserName("U4");

		//Create
		DAOEmployee.persistEmployees(Collections.singleton(emp3), user);

		emp3 = DAOEmployee.getEmployee("U4");
		Assert.assertNotNull(emp3);

		//Rename
		emp3.setUserName("U1A");
		try {
			DAOEmployee.persistEmployees(Collections.singleton(emp3), user);
			throw new AssertionFailedError("Should have thrown an exception: U1A exists already");
		} catch (Exception e) {
			//OK
		}
		emp3.setUserName("U4A");
		DAOEmployee.persistEmployees(Collections.singleton(emp3), user);

		emp3 = DAOEmployee.getEmployee("U4");
		Assert.assertNull(emp3);
		emp3 = DAOEmployee.getEmployee("U4A");
		Assert.assertNotNull(emp3);

		//Delete
		DAOEmployee.removeEmployee(emp3, user);
		emp3 = DAOEmployee.getEmployee("U4A");
		Assert.assertNull(emp3);

	}

	@Test
	public void testSpiritUser() throws Exception {

		char[] password = "MyPassword".toCharArray();

		Employee emp3 = new Employee();
		emp3.setUserName("Toto");
		emp3.setPassword(DBAdapter.getInstance().encryptPassword(password));
		emp3.setRoles(MiscUtils.setOf("Admin", "FM"));
		DAOEmployee.persistEmployees(Collections.singleton(emp3), user);

		//Authenticate and load user
		DAOSpiritUser.authenticateUser("Toto", password);
		SpiritUser u = DAOSpiritUser.loadUser("Toto");
		Assert.assertNotNull(u);
		Assert.assertEquals(2, u.getRoles().size());
		Assert.assertTrue(u.getRoles().contains("Admin"));
		Assert.assertTrue(u.getRoles().contains("FM"));

	}


	@Test
	public void testEmployeeGroupCRUD() throws Exception {
		System.out.println("SimpleDBTest.testEmployeeGroups() "+DAOEmployee.getEmployeeGroups());
		Assert.assertEquals(4, DAOEmployee.getEmployeeGroups().size());

		EmployeeGroup group1 = new EmployeeGroup("Root");
		DAOEmployee.persistEmployeeGroups(Collections.singleton(group1), user);
		Assert.assertTrue(group1.getId()>0);

		EmployeeGroup group2 = new EmployeeGroup("Child");
		group2.setParent(group1);
		DAOEmployee.persistEmployeeGroups(Collections.singleton(group2), user);
		Assert.assertEquals(4+2, DAOEmployee.getEmployeeGroups().size());

		DAOEmployee.removeEmployeeGroup(group2, user);
		Assert.assertEquals(4+1, DAOEmployee.getEmployeeGroups().size());

	}

	@Test
	public void testRights() throws Exception {

		//Save a sample and check rights
		Biosample b = new Biosample(DAOBiotype.getBiotype("Test"));
		DAOBiosample.persistBiosamples(Collections.singleton(b), new SpiritUser(emp1b));

		b = DAOBiosample.getBiosample(b.getSampleId());
		Assert.assertTrue(b.getSampleId()!=null);
		Assert.assertTrue(b.getCreUser().equals(emp1b.getUserName()));
		Assert.assertEquals(eg1, b.getEmployeeGroup());

		//People from the same group can edit
		Assert.assertTrue(SpiritRights.canEdit(b, new SpiritUser(emp1a)));
		Assert.assertTrue(SpiritRights.canEdit(b, new SpiritUser(emp1b)));
		Assert.assertTrue(SpiritRights.canEdit(b, new SpiritUser(emp1c)));

		//People from a non related group cannot edit
		Assert.assertTrue(!SpiritRights.canEdit(b, new SpiritUser(emp2a)));

		//People from a super group cannot edit
		Assert.assertTrue(!SpiritRights.canEdit(b, new SpiritUser(emp0)));

		//Only the creator, or its manager can delete
		Assert.assertTrue(!SpiritRights.canDelete(b, new SpiritUser(emp0)));
		Assert.assertTrue(SpiritRights.canDelete(b, new SpiritUser(emp1a)));
		Assert.assertTrue(SpiritRights.canDelete(b, new SpiritUser(emp1b)));
		Assert.assertTrue(!SpiritRights.canDelete(b, new SpiritUser(emp1c)));
		Assert.assertTrue(!SpiritRights.canDelete(b, new SpiritUser(emp2a)));

	}

	@Test
	public void testRevision() throws Exception {
		Employee emp3 = new Employee();
		emp3.setUserName("TestRev2");
		DAOEmployee.persistEmployees(Collections.singleton(emp3), user);


		//Load the last revision
		RevisionQuery q = new RevisionQuery();
		q.setAdmin(true);
		q.setFromDate(MiscUtils.addDays(new Date(), -1));
		List<Revision> revs = DAORevision.queryRevisions(q);
		Assert.assertTrue(revs.size()>0);
		Assert.assertTrue(revs.get(0).getEmployees().size()>0);

		//Load the history
		List<Employee> history = DAORevision.getHistory(emp3);
		Assert.assertTrue(history.size()==1);






	}

}
