import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
public class ProfilesController {

    @Autowired
    private ProfileService profileService;

    @GetMapping("/get-profiles")
    public List<Profile> getAllProfiles() {
        return profileService.getAllProfiles();
    }
}
