import org.junit.*;
import java.util.*;

import play.modules.siena.SienaFixtures;
import play.test.*;
import models.*;

public class BasicTest extends UnitTest {

	@Test
    public void loadEmployee() {
		SienaFixtures.loadModels("data1.yml");
		
		// Bob
		Employee bob = Employee.all(Employee.class).filter("firstName", "Bob").get();
        assertNotNull(bob);
        assertEquals("Bob", bob.firstName);
        assertEquals("Smith", bob.lastName);
        assertEquals("password1", bob.pwd);
        assertEquals("{\"alpha1\": \"beta1\"}", bob.contactInfo.toString());
        // profileImage
        assertEquals("filename1.ext", bob.profileImage.filename);
        assertEquals("title1", bob.profileImage.title);
        assertEquals(1, bob.profileImage.views);
        assertEquals(Employee.MyEnum.VAL1, bob.profileImage.itemEnum);
        // otherImages[0]
        assertEquals("filename1.ext", bob.otherImages.get(0).filename);
        assertEquals("title1", bob.otherImages.get(0).title);
        assertEquals(1, bob.otherImages.get(0).views);
        assertEquals(Employee.MyEnum.VAL1, bob.otherImages.get(0).itemEnum);
        // otherImages[1]
        assertEquals("filename2.ext", bob.otherImages.get(1).filename);
        assertEquals("title2", bob.otherImages.get(1).title);
        assertEquals(2, bob.otherImages.get(1).views);
        assertEquals(Employee.MyEnum.VAL2, bob.otherImages.get(1).itemEnum);
        // stillImages[test1]
        assertEquals("filename1.ext", bob.stillImages.get("test1").filename);
        assertEquals("title1", bob.stillImages.get("test1").title);
        assertEquals(1, bob.stillImages.get("test1").views);
        assertEquals(Employee.MyEnum.VAL1, bob.stillImages.get("test1").itemEnum);
     	// stillImages[test2]
        assertEquals("filename2.ext", bob.stillImages.get("test2").filename);
        assertEquals("title2", bob.stillImages.get("test2").title);
        assertEquals(2, bob.stillImages.get("test2").views);
        assertEquals(Employee.MyEnum.VAL2, bob.stillImages.get("test2").itemEnum);
        // items[1]
        assertEquals("alpha1", bob.items.get(0).item);
        assertEquals("beta1", bob.items.get(0).item2);
        assertEquals(Employee.MyEnum.VAL1, bob.items.get(0).itemEnum);
        // items[2]
        assertEquals("alpha2", bob.items.get(1).item);
        assertEquals("beta2", bob.items.get(1).item2);
        assertEquals(Employee.MyEnum.VAL2, bob.items.get(1).itemEnum);
        // enumField
        assertEquals(Employee.MyEnum.VAL3, bob.enumField);
        
		// John
		Employee john = Employee.all(Employee.class).filter("firstName", "John").get();
        assertNotNull(john);
        assertEquals("John", john.firstName);
        assertEquals("Doe", john.lastName);
        assertEquals("password2", john.pwd);
        assertEquals("{\"alpha2\": \"beta2\"}", john.contactInfo.toString());
        assertEquals("filename2.ext", john.profileImage.filename);
        assertEquals("title2", john.profileImage.title);
        assertEquals(2, john.profileImage.views);
        assertEquals(Employee.MyEnum.VAL2, john.profileImage.itemEnum);
        assertEquals(bob.id, john.boss.id);
        
     	// emp1
		Employee emp1 = Employee.all(Employee.class).filter("firstName", "emp1").get();
        assertNotNull(emp1);
     	// emp2
		Employee emp2 = Employee.all(Employee.class).filter("firstName", "emp2").get();
        assertNotNull(emp2);
     	// boss
		Employee boss = Employee.all(Employee.class).filter("firstName", "boss").get();
        assertNotNull(boss);
        List<Employee> emps = boss.employees.fetch();
        assertEquals(emp1.id, emps.get(0).id);
        assertEquals(emp2.id, emps.get(1).id);
        assertEquals(emp1.boss.id, boss.id);
        assertEquals(emp2.boss.id, boss.id);
	}

	@Test
    public void loadEmployeeEnhanced() {
		SienaFixtures.loadModels("data2.yml");
		
		// Bob
		EmployeeEnhanced bob = EmployeeEnhanced.all().filter("firstName", "Bob").get();		
        assertNotNull(bob);
        assertEquals("Bob", bob.firstName);
        assertEquals("Smith", bob.lastName);
        assertEquals("password1", bob.pwd);
        assertEquals("{\"alpha1\": \"beta1\"}", bob.contactInfo.toString());
        // profileImage
        assertEquals("filename1.ext", bob.profileImage.filename);
        assertEquals("title1", bob.profileImage.title);
        assertEquals(1, bob.profileImage.views);
        assertEquals(EmployeeEnhanced.MyEnum.VAL1, bob.profileImage.itemEnum);
        // otherImages[0]
        assertEquals("filename1.ext", bob.otherImages.get(0).filename);
        assertEquals("title1", bob.otherImages.get(0).title);
        assertEquals(1, bob.otherImages.get(0).views);
        assertEquals(EmployeeEnhanced.MyEnum.VAL1, bob.otherImages.get(0).itemEnum);
        // otherImages[1]
        assertEquals("filename2.ext", bob.otherImages.get(1).filename);
        assertEquals("title2", bob.otherImages.get(1).title);
        assertEquals(2, bob.otherImages.get(1).views);
        assertEquals(EmployeeEnhanced.MyEnum.VAL2, bob.otherImages.get(1).itemEnum);
        // stillImages[test1]
        assertEquals("filename1.ext", bob.stillImages.get("test1").filename);
        assertEquals("title1", bob.stillImages.get("test1").title);
        assertEquals(1, bob.stillImages.get("test1").views);
        assertEquals(EmployeeEnhanced.MyEnum.VAL1, bob.stillImages.get("test1").itemEnum);
     	// stillImages[test2]
        assertEquals("filename2.ext", bob.stillImages.get("test2").filename);
        assertEquals("title2", bob.stillImages.get("test2").title);
        assertEquals(2, bob.stillImages.get("test2").views);
        assertEquals(EmployeeEnhanced.MyEnum.VAL2, bob.stillImages.get("test2").itemEnum);
        // items[1]
        assertEquals("alpha1", bob.items.get(0).item);
        assertEquals("beta1", bob.items.get(0).item2);
        assertEquals(EmployeeEnhanced.MyEnum.VAL1, bob.items.get(0).itemEnum);
        // items[2]
        assertEquals("alpha2", bob.items.get(1).item);
        assertEquals("beta2", bob.items.get(1).item2);
        assertEquals(EmployeeEnhanced.MyEnum.VAL2, bob.items.get(1).itemEnum);
        // enumField
        assertEquals(EmployeeEnhanced.MyEnum.VAL3, bob.enumField);
        
		// John
        EmployeeEnhanced john = EmployeeEnhanced.all().filter("firstName", "John").get();
        assertNotNull(john);
        assertEquals("John", john.firstName);
        assertEquals("Doe", john.lastName);
        assertEquals("password2", john.pwd);
        assertEquals("{\"alpha2\": \"beta2\"}", john.contactInfo.toString());
        assertEquals("filename2.ext", john.profileImage.filename);
        assertEquals("title2", john.profileImage.title);
        assertEquals(2, john.profileImage.views);
        assertEquals(EmployeeEnhanced.MyEnum.VAL2, john.profileImage.itemEnum);
        assertEquals(bob.id, john.boss.id);

     	// emp1
        EmployeeEnhanced emp1 = EmployeeEnhanced.all().filter("firstName", "emp1").get();
        assertNotNull(emp1);
     	// emp2
        EmployeeEnhanced emp2 = EmployeeEnhanced.all().filter("firstName", "emp2").get();
        assertNotNull(emp2);
     	// boss
        EmployeeEnhanced boss = EmployeeEnhanced.all(EmployeeEnhanced.class).filter("firstName", "boss").get();
        assertNotNull(boss);
        List<EmployeeEnhanced> emps = boss.employees.fetch();
        assertEquals(emp1.id, emps.get(0).id);
        assertEquals(emp2.id, emps.get(1).id);
        assertEquals(emp1.boss.id, boss.id);
        assertEquals(emp2.boss.id, boss.id);
	
	}

	@Test
    public void loadManualStringModel() {
		SienaFixtures.loadModels("data3.yml");
		
		// first
		ManualStringModel first = ManualStringModel.all().filter("id", "first").get();		
        assertNotNull(first);
        assertEquals("chboing1", first.alpha);
        assertEquals(1, first.beta);

        // second
		ManualStringModel second = ManualStringModel.all().filter("id", "second").get();		
        assertNotNull(second);
        assertEquals("chboing2", second.alpha);
        assertEquals(2, second.beta);
	}
	
	@Test
	public void loadOtherIdModel() {
		SienaFixtures.loadModels("data4.yml");
		
		// chboing
		OtherIdModel chboing = OtherIdModel.all().filter("alpha", "chboing").get();		
        assertNotNull(chboing);
        assertNotSame(0, chboing.myId);
        assertEquals("chboing", chboing.alpha);
        assertEquals(1, chboing.beta);
	}
	
	@Test
	public void loadUUIDModel() {
		SienaFixtures.loadModels("data5.yml");
		
		// chboing
		UUIDModel chboing = UUIDModel.all().filter("alpha", "chboing").get();		
        assertNotNull(chboing);
        assertNotSame(null, chboing.id);
        assertNotSame("", chboing.id);
        assertEquals("chboing", chboing.alpha);
        assertEquals(1, chboing.beta);
	}
	
	@Test
	public void loadContainerModel() {
		SienaFixtures.loadModels("data6.yml");
		
		// emb
		EmbeddedModel emb = EmbeddedModel.all().filter("id", "emb").get();		
        assertNotNull(emb);
		// cont
		ContainerModel cont = ContainerModel.all().filter("id", "cont").get();		
        assertNotNull(cont);
        assertEquals(cont.embed.id, emb.id);
        assertEquals(cont.embed.alpha, emb.alpha);
        assertEquals(cont.embed.beta, emb.beta);
	}
	
    @Before
    public void setUp() {
        SienaFixtures.deleteDatabase();
    }
}
