package veterinaria.vargasvet.service;

import veterinaria.vargasvet.dto.request.ProfileUpdateRequest;
import veterinaria.vargasvet.dto.response.ProfileResponse;

public interface ProfileService {
    ProfileResponse getMyProfile();
    ProfileResponse updateMyProfile(ProfileUpdateRequest dto);
}
