package ru.elvich.fa.students;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/db/students")
@RequiredArgsConstructor
public class StudentRestController {
    private final StudentService studentService;

    @GetMapping
    public List<StudentEntity> findAll() {
        return studentService.findAll();
    }

    @GetMapping("/{id}")
    public StudentEntity findById(@PathVariable Long id) {
        return studentService.findById(id);
    }

    @PostMapping
    public ResponseEntity<StudentEntity> create(@Valid @RequestBody StudentEntity student) {
        StudentEntity saved = studentService.create(student);
        return ResponseEntity.created(URI.create("/db/students/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public StudentEntity update(@PathVariable Long id, @Valid @RequestBody StudentUpdate input) {
        return studentService.update(id, input);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        studentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
