package backend;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "https://smart-civic-issue-system-rb1yw4euy.vercel.app/")
public class HelloController {

    @GetMapping("/api/health")
    public String health() {
        return "Civic Reporting Backend is working!";
    }
}