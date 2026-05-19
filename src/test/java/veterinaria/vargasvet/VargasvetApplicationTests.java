package veterinaria.vargasvet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import veterinaria.vargasvet.dto.request.RoleCreateDTO;
import veterinaria.vargasvet.service.RoleService;
import java.util.Arrays;

@SpringBootTest
class VargasvetApplicationTests {

	@Autowired
	private RoleService roleService;

	@Test
	void testUpdateRole() {
		try {
			RoleCreateDTO dto = new RoleCreateDTO();
			dto.setName("ROLE_VETERINARIO");
			dto.setPermissionIds(java.util.Set.of(1, 2));
			dto.setMenuIds(java.util.Set.of(1));
			roleService.updateRole(3, dto);
			System.out.println("TEST UPDATE SUCCESSFUL!");
		} catch (Exception e) {
			System.out.println("TEST UPDATE FAILED: " + e.getClass().getName() + " -> " + e.getMessage());
			e.printStackTrace();
		}
	}

}

