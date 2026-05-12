package veterinaria.vargasvet.perfil;

import veterinaria.vargasvet.perfil.ProfileUpdateRequest;
import veterinaria.vargasvet.perfil.ProfileResponse;

public interface ProfileService {
    ProfileResponse getMyProfile();
    ProfileResponse updateMyProfile(ProfileUpdateRequest dto);
}
