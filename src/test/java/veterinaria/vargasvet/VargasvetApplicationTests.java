package veterinaria.vargasvet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import veterinaria.vargasvet.service.RoleService;

@SpringBootTest
class VargasvetApplicationTests {

	@Autowired
	private RoleService roleService;

	@Test
	void testUpdateRole() {
		try {
			roleService.updateRole(3, "ROLE_VETERINARIO", "Rol para médicos veterinarios");
			System.out.println("TEST UPDATE SUCCESSFUL!");
		} catch (Exception e) {
			System.out.println("TEST UPDATE FAILED: " + e.getClass().getName() + " -> " + e.getMessage());
			e.printStackTrace();
		}
	}

}
