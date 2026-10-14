package backend;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import backend.model.Issue;
import backend.repository.IssueRepository;

@RestController
@RequestMapping("/api/issues")
@CrossOrigin(origins = "https://smart-civic-issue-system-rb1yw4euy.vercel.app/")
public class IssueController {

    private final IssueRepository issueRepository;

    public IssueController(IssueRepository issueRepository) {
        this.issueRepository = issueRepository;
    }

    // Sort issues by priority: Critical, High, Medium, Low.
    private int priorityRank(String priority) {
        if (priority == null) {
            return 5;
        }

        return switch (priority.trim().toUpperCase(Locale.ROOT)) {
            case "CRITICAL" -> 1;
            case "HIGH" -> 2;
            case "MEDIUM" -> 3;
            case "LOW" -> 4;
            default -> 5;
        };
    }

    // Automatically calculate priority from category, title,
    // and description. No citizen-selected severity is required.
    private String calculatePriority(Issue issue) {

        String category = issue.getCategory() == null
                ? ""
                : issue.getCategory().trim().toLowerCase(Locale.ROOT);

        String title = issue.getTitle() == null
                ? ""
                : issue.getTitle().trim().toLowerCase(Locale.ROOT);

        String description = issue.getDescription() == null
                ? ""
                : issue.getDescription().trim().toLowerCase(Locale.ROOT);

        String text = title + " " + description;

        // CRITICAL: Immediate public safety hazards.
        if (text.contains("live wire")
                || text.contains("exposed wire")
                || text.contains("hanging wire")
                || text.contains("electric shock")
                || text.contains("fallen electric pole")
                || text.contains("open manhole")
                || text.contains("uncovered manhole")
                || text.contains("road collapse")
                || text.contains("road has collapsed")
                || text.contains("gas leak")
                || text.contains("flash flood")
                || text.contains("sewage overflow")
                || text.contains("fire")) {

            return "CRITICAL";
        }

        // HIGH: Problems requiring urgent attention.
        if (category.equals("pothole")
                || category.equals("road damage")
                || category.equals("drainage")
                || category.equals("electricity")) {

            return "HIGH";
        }

        if (text.contains("large pothole")
                || text.contains("deep pothole")
                || text.contains("blocked drain")
                || text.contains("drain blocked")
                || text.contains("overflowing drain")
                || text.contains("water pipe burst")
                || text.contains("traffic blocked")
                || text.contains("major road damage")) {

            return "HIGH";
        }

        // MEDIUM: General civic problems.
        if (category.equals("garbage")
                || category.equals("water supply")) {

            return "MEDIUM";
        }

        // LOW: Streetlight issues by default.
        if (category.equals("streetlight")) {
            return "LOW";
        }

        // MEDIUM: Default for other categories.
        return "MEDIUM";
    }

    // Get all issues, sorted by priority.
    @GetMapping
    public List<Issue> getAllIssues() {
        return issueRepository.findAll()
                .stream()
                .sorted(
                    Comparator.comparingInt(
                        (Issue issue) ->
                                priorityRank(issue.getPriority())
                    )
                )
                .toList();
    }

    // Create a new issue and automatically assign its priority.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Issue createIssue(@RequestBody Issue issue) {

        if (issue.getTitle() == null
                || issue.getTitle().isBlank()
                || issue.getDescription() == null
                || issue.getDescription().isBlank()
                || issue.getCategory() == null
                || issue.getCategory().isBlank()
                || issue.getLocation() == null
                || issue.getLocation().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Title, description, category, and location are required."
            );
        }

        // Generate a new issue ID.
        issue.setId(null);

        // Every newly reported issue starts as pending.
        issue.setStatus("PENDING");

        // Do not accept a citizen-provided user ID here.
        issue.setUserId(null);

        // Ignore any priority sent by the frontend and calculate it.
        issue.setPriority(calculatePriority(issue));

        return issueRepository.save(issue);
    }

    // Update the status of an existing issue.
    @PutMapping("/{id}/status")
    public Issue updateIssueStatus(
            @PathVariable Integer id,
            @RequestBody StatusUpdateRequest request) {

        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Issue not found"
                ));

        String newStatus = request.status();

        if (newStatus == null
                || !(newStatus.equals("PENDING")
                || newStatus.equals("IN_PROGRESS")
                || newStatus.equals("RESOLVED"))) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status must be PENDING, IN_PROGRESS, or RESOLVED."
            );
        }

        issue.setStatus(newStatus);

        return issueRepository.save(issue);
    }

    public record StatusUpdateRequest(String status) {
    }
}